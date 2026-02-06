import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;

public class WordleView extends BorderPane {

    private final Navigator nav;
    private final WordleLogic game;

    private final int letters;
    private final int wordsCount;
    private final int chances;

    // only reveal rows progressively
    private int rowIndex = 0;
    private int colIndex = 0;
    private final char[] current;

    // tiles[w][row][col]
    private final TileCell[][][] cells;

    // green hints per word/position (discovered greens)
    private final char[][] greenHint; // [word][pos]

    // solved flags (so solved boards stop updating)
    private final boolean[] solvedSoFar;

    // keyboard state
    private final Button[] keyButtons = new Button[26];
    private final boolean[] usedKey = new boolean[26];

    // letter knowledge per word: 0=unknown/absent so far, 1=seen yellow, 2=seen green
    private final int[][] perWordLetterState; // [word][26]

    private final Label message = new Label("");
    private final Label remaining = new Label("");

    public WordleView(Navigator nav, String language, int letters, int wordsCount) {
        this.nav = nav;

        Language lang = new Language(language);
        this.game = new WordleLogic(wordsCount, letters, lang);

        this.letters = letters;
        this.wordsCount = wordsCount;
        this.chances = game.getChances();

        this.current = new char[letters];
        this.cells = new TileCell[wordsCount][chances][letters];

        this.greenHint = new char[wordsCount][letters];
        this.solvedSoFar = new boolean[wordsCount];
        this.perWordLetterState = new int[wordsCount][26];

        // ---------- TOP ----------
        Label title = new Label("Wordle");
        title.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(18, 0, 6, 0));

        message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
        remaining.setStyle("-fx-font-size: 16px;");
        remaining.setText("Guesses left: " + chances);

        VBox top = new VBox(6, title);
        top.setAlignment(Pos.CENTER);
        setTop(top);

        // ---------- BOARDS ----------
        FlowPane boardsPane = new FlowPane();
        boardsPane.setHgap(26);
        boardsPane.setVgap(26);
        boardsPane.setPadding(new Insets(12));
        boardsPane.setAlignment(Pos.TOP_CENTER);

        ScrollPane boardsScroll = new ScrollPane(boardsPane);
        boardsScroll.setFitToWidth(true);
        boardsScroll.setPannable(true);
        boardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        boardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        for (int w = 0; w < wordsCount; w++) {
            Label header = new Label("Word " + (w + 1));
            header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

            VBox board = new VBox(10);
            board.setAlignment(Pos.TOP_CENTER);
            board.getChildren().add(header);

            // grid built as VBox of rows (so we can hide rows easily)
            VBox rowsBox = new VBox(8);
            rowsBox.setAlignment(Pos.CENTER);

            for (int r = 0; r < chances; r++) {
                HBox rowBox = new HBox(8);
                rowBox.setAlignment(Pos.CENTER);

                // reveal only row 0 initially
                rowBox.setVisible(r == 0);
                rowBox.setManaged(r == 0);

                for (int c = 0; c < letters; c++) {
                    TileCell cell = new TileCell();
                    cell.setMinSize(52, 52);
                    cells[w][r][c] = cell;
                    rowBox.getChildren().add(cell);
                }
                rowsBox.getChildren().add(rowBox);
            }

            board.getChildren().add(rowsBox);
            boardsPane.getChildren().add(board);
        }

        setCenter(boardsScroll);

        // ---------- BOTTOM ----------
        VBox keyboard = buildKeyboard();

        Button giveUpBtn = new Button("Give up");
        giveUpBtn.setPrefWidth(180);
        giveUpBtn.setPrefHeight(40);
        giveUpBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button backBtn = new Button("Back");
        backBtn.setPrefWidth(180);
        backBtn.setPrefHeight(40);
        backBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        backBtn.setVisible(false);
        backBtn.setManaged(false);

        VBox bottom = new VBox(8,
                remaining,
                message,
                keyboard,
                giveUpBtn,
                backBtn
        );
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(10, 0, 16, 0));
        setBottom(bottom);

        backBtn.setOnAction(e -> nav.goToSettings(language, "Wordle"));

        giveUpBtn.setOnAction(e -> {
            showLose(backBtn, giveUpBtn, "You gave up.");
        });

        // Physical keyboard support (optional)
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (game.isGameOver()) return;

            if (e.getCode() == KeyCode.ENTER) {
                submit();
                return;
            }
            if (e.getCode() == KeyCode.BACK_SPACE) {
                backspace();
                return;
            }

            String txt = e.getText();
            if (txt != null && txt.length() == 1) {
                char ch = Character.toUpperCase(txt.charAt(0));
                if (ch >= 'A' && ch <= 'Z') typeChar(ch);
            }
        });

        Platform.runLater(() -> {
            applyCss();
            layout();
            requestFocus();
            // show hints in row 0 from the beginning (empty now, but safe)
            refreshRowHints(rowIndex);
        });
    }

    // =========================
    // Actions
    // =========================

    private void submit() {
        if (game.isGameOver()) {
            message.setText("Game over.");
            return;
        }
        if (rowIndex >= chances) {
            message.setText("No guesses left.");
            return;
        }
        if (colIndex < letters) {
            message.setText("Not enough letters.");
            return;
        }

        String guess = new String(current);

        // Only validate if you want (kept like before)
        if (!HunspellChecker.isValidWord(guess)) {
            message.setText("Not a valid word.");
            return;
        }

        WordleLogic.TurnResult r;
        try {
            r = game.submitGuess(guess);
        } catch (IllegalArgumentException ex) {
            message.setText(ex.getMessage());
            return;
        }

        int paintedRow = game.getTries() - 1;

        // Update boards + record knowledge
        for (int w = 0; w < wordsCount; w++) {

            // If already solved before this turn -> do nothing
            if (solvedSoFar[w] && !r.solved[w]) {
                continue;
            }

            // Paint row for this word
            for (int c = 0; c < letters; c++) {
                char ch = r.guess.charAt(c);
                TileCell cell = cells[w][paintedRow][c];

                cell.setMain(ch);
                cell.setTileColor(r.tilesByWord[w][c]);

                // record hint greens
                if (r.tilesByWord[w][c] == WordleLogic.Tile.GREEN) {
                    greenHint[w][c] = ch;
                }

                // record per-word letter state
                if (ch >= 'A' && ch <= 'Z') {
                    int idx = ch - 'A';
                    int val = (r.tilesByWord[w][c] == WordleLogic.Tile.GREEN) ? 2 :
                            (r.tilesByWord[w][c] == WordleLogic.Tile.YELLOW) ? 1 : 0;
                    perWordLetterState[w][idx] = Math.max(perWordLetterState[w][idx], val);
                }
            }

            // mark solved
            if (r.solved[w]) solvedSoFar[w] = true;
        }

        // keyboard update (based only on letters actually used)
        updateKeyboardFromThisGuess(guess);

        remaining.setText("Guesses left: " + r.remainingGuesses);
        message.setText("");

        if (r.gameWon) {
            showWin();
            return;
        }

        if (r.gameOver) {
            showLoseButtonsOnly("Out of guesses.");
            showLoseRevealWords();
            return;
        }

        // move to next row
        rowIndex++;
        colIndex = 0;
        Arrays.fill(current, (char) 0);

        // reveal next row in ALL boards
        revealRow(rowIndex);

        // show hint letters behind in the new row
        refreshRowHints(rowIndex);
    }

    private void typeChar(char ch) {
        if (game.isGameOver()) return;
        if (rowIndex >= chances) return;
        if (colIndex >= letters) return;

        current[colIndex] = ch;

        // Put typed letter into ALL boards on this row (main text)
        for (int w = 0; w < wordsCount; w++) {
            if (solvedSoFar[w]) continue; // ✅ don't show typing on solved boards
            cells[w][rowIndex][colIndex].setMain(ch);
        }

        // mark used for keyboard coloring rules
        usedKey[ch - 'A'] = true;

        colIndex++;

        // ensure hints visible behind other empty slots
        refreshRowHints(rowIndex);
    }

    private void backspace() {
        if (game.isGameOver()) return;
        if (rowIndex >= chances) return;
        if (colIndex <= 0) return;

        colIndex--;
        current[colIndex] = 0;

        for (int w = 0; w < wordsCount; w++) {
            if (solvedSoFar[w]) continue; // ✅ don't clear typing on solved boards
            cells[w][rowIndex][colIndex].clearMain();
        }


        // bring hint back
        refreshRowHints(rowIndex);
    }

    // =========================
    // Row reveal + hints
    // =========================

    private void revealRow(int r) {
        if (r < 0 || r >= chances) return;

        for (int w = 0; w < wordsCount; w++) {

            // ✅ if this board is solved, NEVER reveal more rows
            if (solvedSoFar[w]) continue;

            var parent = cells[w][r][0].getParent();
            if (parent != null) {
                parent.setVisible(true);
                parent.setManaged(true);
            }
        }
    }

    private void refreshRowHints(int r) {
        if (r < 0 || r >= chances) return;

        for (int w = 0; w < wordsCount; w++) {

            // ✅ solved board: don't touch any further rows
            if (solvedSoFar[w]) continue;

            for (int c = 0; c < letters; c++) {
                TileCell cell = cells[w][r][c];

                char hintCh = greenHint[w][c];
                if (hintCh != 0) cell.setHint(hintCh);
                else cell.clearHint();
            }
        }
    }


    // =========================
    // Keyboard
    // =========================

    private VBox buildKeyboard() {
        String[] r1 = {"Q","W","E","R","T","Z","U","I","O","P"};
        String[] r2 = {"A","S","D","F","G","H","J","K","L"};
        String[] r3 = {"ENTER","Y","X","C","V","B","N","M","⌫"};

        VBox kb = new VBox(6,
                keyboardRow(r1),
                keyboardRow(r2),
                keyboardRow(r3)
        );
        kb.setAlignment(Pos.CENTER);
        kb.setPadding(new Insets(6, 0, 0, 0));
        return kb;
    }

    private HBox keyboardRow(String[] keys) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);

        for (String k : keys) {
            Button b = new Button(k);
            b.setPrefHeight(42);
            if (k.equals("ENTER")) b.setPrefWidth(115);
            else if (k.equals("⌫")) b.setPrefWidth(70);
            else b.setPrefWidth(48);

            b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            b.setOnAction(e -> {
                if (k.equals("ENTER")) {
                    submit();
                } else if (k.equals("⌫")) {
                    backspace();
                } else {
                    typeChar(k.charAt(0));
                }
                requestFocus();
            });

            // store A-Z buttons for coloring
            if (k.length() == 1) {
                char ch = k.charAt(0);
                if (ch >= 'A' && ch <= 'Z') {
                    keyButtons[ch - 'A'] = b;
                }
            }

            row.getChildren().add(b);
        }

        return row;
    }

    private void updateKeyboardFromThisGuess(String guess) {
        // mark used letters (already done while typing, but safe)
        for (int i = 0; i < guess.length(); i++) {
            char ch = guess.charAt(i);
            if (ch >= 'A' && ch <= 'Z') usedKey[ch - 'A'] = true;
        }

        // Apply coloring rules
        for (int li = 0; li < 26; li++) {
            Button b = keyButtons[li];
            if (b == null) continue;

            // Do NOT reveal anything until the letter has been used at least once
            if (!usedKey[li]) {
                b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                continue;
            }

            // Determine which words have evidence that this letter exists (yellow or green seen)
            boolean anyEvidence = false;
            boolean allEvidenceAtLeastYellow = true;
            boolean allEvidenceGreen = true;

            for (int w = 0; w < wordsCount; w++) {
                int st = perWordLetterState[w][li]; // 0/1/2
                if (st > 0) {
                    anyEvidence = true;
                    if (st < 1) allEvidenceAtLeastYellow = false;
                    if (st < 2) allEvidenceGreen = false;
                }
            }

            if (!anyEvidence) {
                // used, and never appeared yellow/green anywhere => grey
                b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #787C7E; -fx-text-fill: white;");
            } else if (allEvidenceGreen) {
                // Your rule: instead of green -> grey (no more need)
                b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #787C7E; -fx-text-fill: white;");
            } else if (allEvidenceAtLeastYellow) {
                b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #C9B458; -fx-text-fill: white;");
            } else {
                // evidence exists but not consistent -> keep neutral (no spoiler)
                b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            }
        }
    }

    // =========================
    // Win / Lose
    // =========================

    private void showWin() {
        Label won = new Label("You win!");
        won.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");
        setCenter(won);

        message.setStyle("-fx-text-fill: green; -fx-font-size: 16px;");
        message.setText("You solved all words!");
    }

    private void showLoseButtonsOnly(String msg) {
        message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
        message.setText(msg);
    }

    private void showLoseRevealWords() {
        Label lost = new Label("You lost. The words were:");
        lost.setStyle("-fx-font-size: 36px;");

        VBox losingContent = new VBox(10);
        losingContent.setAlignment(Pos.CENTER);
        losingContent.setPadding(new Insets(20));
        losingContent.getChildren().add(lost);

        for (String w : game.getWords()) {
            Label wLabel = new Label(w);
            wLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            losingContent.getChildren().add(wLabel);
        }

        ScrollPane losingScroll = new ScrollPane(losingContent);
        losingScroll.setFitToWidth(true);
        losingScroll.setPannable(true);
        losingScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        losingScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        setCenter(losingScroll);
    }

    private void showLose(Button backBtn, Button giveUpBtn, String msg) {
        showLoseButtonsOnly(msg);
        showLoseRevealWords();
    }

    // =========================
    // TileCell (hint behind + main on top)
    // =========================

    private static class TileCell extends StackPane {
        private final Label hint = new Label("");
        private final Label main = new Label("");

        TileCell() {
            setMinSize(52, 52);

            // IMPORTANT: visible empty tile background
            setStyle(
                    "-fx-border-color: #444;" +
                            "-fx-border-width: 3;" +
                            "-fx-background-color: white;" +
                            "-fx-alignment: center;"
            );

            // Hint behind (big, grey, overwritable)
            hint.setStyle(
                    "-fx-font-size: 22px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: rgba(0,0,0,0.25);"
            );

            // Main on top (visible on white tiles)
            main.setStyle(
                    "-fx-font-size: 22px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: #111;"
            );

            getChildren().addAll(hint, main);
        }

        void setHint(char ch) {
            hint.setText(String.valueOf(ch));
        }

        void clearHint() {
            hint.setText("");
        }

        void setMain(char ch) {
            main.setText(String.valueOf(ch));
        }

        void clearMain() {
            main.setText("");
        }

        void setTileColor(WordleLogic.Tile tile) {
            // Keep border always the same, only change background + main text color
            String base =
                    "-fx-border-color: #444;" +
                            "-fx-border-width: 3;" +
                            "-fx-alignment: center;";

            if (tile == WordleLogic.Tile.GREEN) {
                setStyle(base + "-fx-background-color: #4CAF50;");
                main.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
            } else if (tile == WordleLogic.Tile.YELLOW) {
                setStyle(base + "-fx-background-color: #C9B458;");
                main.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
            } else {
                setStyle(base + "-fx-background-color: #787C7E;");
                main.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
            }
        }
    }
}
