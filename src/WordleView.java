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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WordleView extends BorderPane {

    // ---------- Tile Cell (hint behind + main on top) ----------
    private static class Cell extends StackPane {
        private final Label hint = new Label("");
        private final Label main = new Label("");

        Cell() {
            setMinSize(45, 45);
            setPrefSize(45, 45);
            setMaxSize(45, 45);

            // Tile border/background belongs to the StackPane
            setStyle(baseTileStyle() + emptyBgStyle());

            // Hint behind (bigger, grey, overwritable)
            hint.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: rgba(0,0,0,0.25);");

            // Main on top (IMPORTANT: dark text on empty tiles)
            main.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111;");

            getChildren().addAll(hint, main);
            StackPane.setAlignment(hint, Pos.CENTER);
            StackPane.setAlignment(main, Pos.CENTER);
        }

        void setHint(char ch) {
            hint.setText(String.valueOf(ch));
        }

        void clearHint() {
            hint.setText("");
        }

        boolean hasMain() {
            return main.getText() != null && !main.getText().isEmpty();
        }

        void setMain(char ch) {
            main.setText(String.valueOf(ch));
        }

        void clearMain() {
            main.setText("");
        }

        void setMainColorBlack() {
            main.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111;");
        }

        void setMainColorWhite() {
            main.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        }

        void setTileBgEmpty() {
            setStyle(baseTileStyle() + emptyBgStyle());
            setMainColorBlack();
        }

        void setTileBgGrey() {
            setStyle(baseTileStyle() + "-fx-background-color: #787C7E;");
            setMainColorWhite();
        }

        void setTileBgYellow() {
            setStyle(baseTileStyle() + "-fx-background-color: #C9B458;");
            setMainColorWhite();
        }

        void setTileBgGreen() {
            setStyle(baseTileStyle() + "-fx-background-color: #4CAF50;");
            setMainColorWhite();
        }

        private static String baseTileStyle() {
            return "-fx-border-color: #444; -fx-border-width: 2; -fx-alignment: center;";
        }

        private static String emptyBgStyle() {
            return "-fx-background-color: white;";
        }
    }

    // ---------- View State ----------
    private final Navigator nav;
    private final String language;

    private final WordleLogic game;
    private final int letters;
    private final int wordsCount;
    private final int chances;

    // typing buffer (one guess shared across all boards)
    private int rowIndex = 0;
    private int colIndex = 0;
    private final char[] current;

    private final Label message = new Label("");
    private final Label remaining;

    // Boards: cells[w][row][col] and rowBoxes[w][row]
    private final Cell[][][] cells;
    private final HBox[][] rowBoxes;
    private final Label[] solvedLabels;

    // freeze solved boards
    private final boolean[] solvedBefore;

    // For hint system: known greens per word/pos (0 = unknown)
    private final char[][] knownGreens;

    // Keyboard UI
    private final Map<Character, Button> keyButtons = new HashMap<>();
    private final Set<Character> usedLetters = new HashSet<>();

    public WordleView(Navigator nav, String language, int letters, int wordsCount) {
        this.nav = nav;
        this.language = language;

        Language lang = new Language(language);
        this.game = new WordleLogic(wordsCount, letters, lang);

        this.letters = letters;
        this.wordsCount = wordsCount;
        this.chances = game.getChances();
        this.current = new char[letters];

        this.remaining = new Label("Guesses left: " + chances);
        remaining.setStyle("-fx-font-size: 16px;");
        message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");

        this.cells = new Cell[wordsCount][chances][letters];
        this.rowBoxes = new HBox[wordsCount][chances];
        this.solvedLabels = new Label[wordsCount];
        this.solvedBefore = new boolean[wordsCount];
        this.knownGreens = new char[wordsCount][letters];

        // ---------- TOP ----------
        Label title = new Label("Wordle");
        title.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 0, 10, 0));
        setTop(title);

        // ---------- CENTER: Boards (wrapping + scroll) ----------
        FlowPane boardsPane = new FlowPane();
        boardsPane.setHgap(25);
        boardsPane.setVgap(25);
        boardsPane.setPadding(new Insets(15));
        boardsPane.setAlignment(Pos.TOP_CENTER);

        ScrollPane boardsScroll = new ScrollPane(boardsPane);
        boardsScroll.setFitToWidth(true);
        boardsScroll.setFitToHeight(false);
        boardsScroll.setPannable(true);
        boardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        boardsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        for (int w = 0; w < wordsCount; w++) {
            Label header = new Label("Word " + (w + 1));
            header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

            Label solvedLabel = new Label("");
            solvedLabel.setStyle("-fx-text-fill: green; -fx-font-size: 14px;");
            solvedLabels[w] = solvedLabel;

            VBox board = new VBox(8);
            board.setAlignment(Pos.TOP_CENTER);
            board.getChildren().addAll(header, solvedLabel);

            for (int r = 0; r < chances; r++) {
                HBox rowBox = new HBox(6);
                rowBox.setAlignment(Pos.CENTER);

                for (int c = 0; c < letters; c++) {
                    Cell cell = new Cell();
                    cells[w][r][c] = cell;
                    rowBox.getChildren().add(cell);
                }

                // show only first row initially
                if (r > 0) {
                    rowBox.setVisible(false);
                    rowBox.setManaged(false);
                }
                rowBoxes[w][r] = rowBox;
                board.getChildren().add(rowBox);
            }

            boardsPane.getChildren().add(board);
        }

        setCenter(boardsScroll);

        // ---------- BOTTOM: Back + Give up + (small) keyboard ----------
        Button backBtn = new Button("Back");
        backBtn.setPrefWidth(140);
        backBtn.setPrefHeight(44);
        backBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        backBtn.setVisible(false);
        backBtn.setManaged(false);
        backBtn.setOnAction(e -> nav.goToSettings(language, "Wordle"));

        Button giveUpBtn = new Button("Give up");
        giveUpBtn.setPrefWidth(160);
        giveUpBtn.setPrefHeight(44);
        giveUpBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox keyboard = buildKeyboard(
                () -> submit(),
                () -> backspace(),
                ch -> typeChar(ch)
        );

        // Put "Give up" UNDER keyboard (like you wanted)
        HBox topActions = new HBox(12, backBtn);
        topActions.setAlignment(Pos.CENTER);

        HBox giveUpRow = new HBox(giveUpBtn);
        giveUpRow.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(10, topActions, remaining, message, keyboard, giveUpRow);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(14));
        setBottom(bottom);

        // Losing screen content (keeps bottom visible)
        VBox losingContent = new VBox(10);
        losingContent.setAlignment(Pos.CENTER);
        losingContent.setPadding(new Insets(20));

        ScrollPane losingScroll = new ScrollPane(losingContent);
        losingScroll.setFitToWidth(true);
        losingScroll.setPannable(true);
        losingScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        losingScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Label won = new Label("You win!");
        won.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");

        // Give up action
        giveUpBtn.setOnAction(e -> {
            showLosing(losingContent);
            setCenter(losingScroll);
            message.setText("You gave up.");
            endGameUI(backBtn, giveUpBtn);
        });

        // Physical keyboard support too
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
                if (ch >= 'A' && ch <= 'Z') {
                    typeChar(ch);
                }
            }
        });

        // Ensure first row is laid out and focus works immediately
        Platform.runLater(() -> {
            applyCss();
            layout();
            requestFocus();
            refreshHintsForTypingRow();
        });

        // ----- local helper (win) -----
        Runnable showWin = () -> {
            setCenter(won);
            message.setStyle("-fx-text-fill: green; -fx-font-size: 16px;");
            message.setText("You solved all words!");
            endGameUI(backBtn, giveUpBtn);
        };

        // Store for submit() use
        this.onWin = showWin;
        this.onLose = () -> {
            showLosing(losingContent);
            setCenter(losingScroll);
            message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
            message.setText("Out of guesses.");
            endGameUI(backBtn, giveUpBtn);
        };
    }

    // ---------- Endgame callbacks ----------
    private Runnable onWin;
    private Runnable onLose;

    // ---------- Submit Logic ----------
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

        String guessRaw = new String(current);

        if (!HunspellChecker.isValidWord(guessRaw)) {
            message.setText("Not a valid word.");
            return;
        }

        // mark letters as "used" for keyboard coloring rule
        for (int i = 0; i < guessRaw.length(); i++) {
            char ch = Character.toUpperCase(guessRaw.charAt(i));
            if (ch >= 'A' && ch <= 'Z') usedLetters.add(ch);
        }

        WordleLogic.TurnResult r;
        try {
            r = game.submitGuess(guessRaw);
        } catch (IllegalArgumentException ex) {
            message.setText(ex.getMessage());
            return;
        }

        int paintedRow = game.getTries() - 1;

        // Paint each board row if not frozen
        for (int w = 0; w < wordsCount; w++) {
            if (solvedBefore[w]) continue;

            // update known greens for hint system + paint
            boolean solvedNow = r.solved[w];
            if (solvedNow) {
                solvedLabels[w].setText("SOLVED!");
                solvedBefore[w] = true;
            }

            for (int c = 0; c < letters; c++) {
                Cell cell = cells[w][paintedRow][c];
                char ch = r.guess.charAt(c);

                cell.clearHint();
                cell.setMain(ch);

                WordleLogic.Tile tile = r.tilesByWord[w][c];

                if (solvedNow) {
                    // solved word row: force all green
                    cell.setTileBgGreen();
                    knownGreens[w][c] = ch;
                } else {
                    if (tile == WordleLogic.Tile.GREEN) {
                        cell.setTileBgGreen();
                        knownGreens[w][c] = ch;
                    } else if (tile == WordleLogic.Tile.YELLOW) {
                        cell.setTileBgYellow();
                    } else {
                        cell.setTileBgGrey();
                    }
                }
            }
        }

        remaining.setText("Guesses left: " + r.remainingGuesses);

        // Update keyboard greying rule (only if used letters)
        updateKeyboardGreying();

        if (r.gameWon) {
            onWin.run();
            return;
        }
        if (r.gameOver) {
            onLose.run();
            return;
        }

        // Reveal next row only for unsolved boards
        int nextRow = paintedRow + 1;
        if (nextRow < chances) {
            for (int w = 0; w < wordsCount; w++) {
                if (!solvedBefore[w]) {
                    rowBoxes[w][nextRow].setManaged(true);
                    rowBoxes[w][nextRow].setVisible(true);
                }
            }
        }

        // move to next row typing
        rowIndex++;
        colIndex = 0;
        for (int i = 0; i < letters; i++) current[i] = 0;

        message.setText("");
        refreshHintsForTypingRow();
        requestFocus();
    }

    // ---------- Typing into current row ----------
    private void typeChar(char ch) {
        if (game.isGameOver()) return;
        if (rowIndex >= chances) return;
        if (colIndex >= letters) return;

        current[colIndex] = ch;

        // Write main text to ALL unsolved boards in typing row
        for (int w = 0; w < wordsCount; w++) {
            if (solvedBefore[w]) continue;
            Cell cell = cells[w][rowIndex][colIndex];
            cell.setMain(ch);
            // keep empty bg while typing
            cell.setTileBgEmpty();
        }

        colIndex++;
        refreshHintsForTypingRow();
    }

    private void backspace() {
        if (game.isGameOver()) return;
        if (rowIndex >= chances) return;
        if (colIndex <= 0) return;

        colIndex--;
        current[colIndex] = 0;

        for (int w = 0; w < wordsCount; w++) {
            if (solvedBefore[w]) continue;
            Cell cell = cells[w][rowIndex][colIndex];
            cell.clearMain();
            cell.setTileBgEmpty();
        }

        refreshHintsForTypingRow();
    }

    // ---------- Hint system (show known greens behind typing row) ----------
    private void refreshHintsForTypingRow() {
        if (rowIndex >= chances) return;

        for (int w = 0; w < wordsCount; w++) {
            if (solvedBefore[w]) continue;

            for (int c = 0; c < letters; c++) {
                Cell cell = cells[w][rowIndex][c];

                // reset empty tile visuals for typing row cells we haven't colored yet
                cell.setTileBgEmpty();

                char hint = knownGreens[w][c];
                if (hint != 0 && !cell.hasMain()) {
                    cell.setHint(hint);
                } else {
                    cell.clearHint();
                }
            }
        }
    }

    // ---------- Keyboard (small) ----------
    private VBox buildKeyboard(Runnable onEnter, Runnable onBackspace, java.util.function.Consumer<Character> onLetter) {
        // German QWERTZ layout
        String[] r1 = {"Q","W","E","R","T","Z","U","I","O","P"};
        String[] r2 = {"A","S","D","F","G","H","J","K","L"};
        String[] r3 = {"ENTER","Y","X","C","V","B","N","M","⌫"};

        VBox kb = new VBox(8,
                keyboardRow(r1, onEnter, onBackspace, onLetter),
                keyboardRow(r2, onEnter, onBackspace, onLetter),
                keyboardRow(r3, onEnter, onBackspace, onLetter)
        );
        kb.setAlignment(Pos.CENTER);
        kb.setPadding(new Insets(6, 0, 0, 0));
        return kb;
    }

    private HBox keyboardRow(String[] keys, Runnable onEnter, Runnable onBackspace, java.util.function.Consumer<Character> onLetter) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER);

        for (String k : keys) {
            Button b = new Button(k);

            // Smaller keyboard
            b.setPrefHeight(42);
            if (k.equals("ENTER")) b.setPrefWidth(120);
            else if (k.equals("⌫")) b.setPrefWidth(70);
            else b.setPrefWidth(50);

            b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            if (k.length() == 1 && Character.isLetter(k.charAt(0))) {
                keyButtons.put(k.charAt(0), b);
            }

            b.setOnAction(e -> {
                if (game.isGameOver()) return;

                if (k.equals("ENTER")) {
                    onEnter.run();
                } else if (k.equals("⌫")) {
                    onBackspace.run();
                } else {
                    onLetter.accept(k.charAt(0));
                }
                requestFocus();
            });

            row.getChildren().add(b);
        }
        return row;
    }

    // ---------- Keyboard greying rule ----------
    // Grey a key ONLY if:
    // 1) player has used that letter in guesses (usedLetters contains it)
    // AND
    // 2) either the letter is in none of the target words OR it's fully "resolved" (all its occurrences are confirmed green in every word that contains it)
    // Never disable the key.
    private void updateKeyboardGreying() {
        String[] targetWords = game.getWords(); // expected to be uppercase

        for (Map.Entry<Character, Button> entry : keyButtons.entrySet()) {
            char L = entry.getKey();
            Button btn = entry.getValue();

            // default
            btn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            if (!usedLetters.contains(L)) {
                continue; // do NOT reveal info before use
            }

            boolean appearsSomewhere = false;
            boolean fullyResolvedEverywhere = true;

            for (int w = 0; w < wordsCount; w++) {
                String word = targetWords[w];
                boolean appearsInThisWord = false;
                boolean allOccurrencesGreen = true;

                for (int pos = 0; pos < letters; pos++) {
                    if (word.charAt(pos) == L) {
                        appearsInThisWord = true;
                        appearsSomewhere = true;

                        // must be known green at that exact pos
                        if (knownGreens[w][pos] != L) {
                            allOccurrencesGreen = false;
                        }
                    }
                }

                if (appearsInThisWord && !allOccurrencesGreen) {
                    fullyResolvedEverywhere = false;
                }
            }

            boolean shouldGrey = (!appearsSomewhere) || fullyResolvedEverywhere;

            if (shouldGrey) {
                // Grey (but still clickable)
                btn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #787C7E; -fx-text-fill: white;");
            }
        }
    }

    // ---------- Endgame UI ----------
    private void endGameUI(Button backBtn, Button giveUpBtn) {
        giveUpBtn.setVisible(false);
        giveUpBtn.setManaged(false);

        backBtn.setVisible(true);
        backBtn.setManaged(true);

        requestFocus();
    }

    private void showLosing(VBox losingContent) {
        losingContent.getChildren().clear();

        Label lost = new Label("You lost. The words were:");
        lost.setStyle("-fx-font-size: 30px; -fx-font-weight: bold;");
        losingContent.getChildren().add(lost);

        String[] ws = game.getWords();
        for (String w : ws) {
            Label wLabel = new Label(w);
            wLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
            losingContent.getChildren().add(wLabel);
        }
    }
}
