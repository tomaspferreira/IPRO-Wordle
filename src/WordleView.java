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

import java.util.HashSet;
import java.util.Set;

public class WordleView extends BorderPane {

    /**
     * Single tile cell with a hint label behind and a main label on top.
     */
    private static class Cell extends StackPane {

        private final Label hint = new Label("");
        private final Label main = new Label("");

        Cell() {
            setMinSize(GameStyles.TILE_SIZE, GameStyles.TILE_SIZE);
            setPrefSize(GameStyles.TILE_SIZE, GameStyles.TILE_SIZE);
            setMaxSize(GameStyles.TILE_SIZE, GameStyles.TILE_SIZE);

            // Match other games: dark empty tile
            setStyle(GameStyles.tileBase() + GameStyles.tileEmpty());

            // Hint should be faint WHITE on dark background
            hint.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.25);");
            // Main should be white (like other games)
            main.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

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

        void setTileBgEmpty() {
            setStyle(GameStyles.tileBase() + GameStyles.tileEmpty());
            // keep main white
            main.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        }

        void setTileBgGrey() {
            setStyle(GameStyles.tileBase() + GameStyles.tileGrey());
            main.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        }

        void setTileBgYellow() {
            setStyle(GameStyles.tileBase() + GameStyles.tileYellow());
            main.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        }

        void setTileBgGreen() {
            setStyle(GameStyles.tileBase() + GameStyles.tileGreen());
            main.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        }
    }


    /**
     * Navigator used to switch screens.
     */
    private final Navigator nav;

    /**
     * Current language code.
     */
    private final String language;

    /**
     * Game logic instance.
     */
    private final WordleLogic game;

    /** Back button (shown after game ends). */
    private final Button backBtn;

    /** Give up button. */
    private final Button giveUpBtn;


    /**
     * Letters per word.
     */
    private final int letters;

    /**
     * Number of words to solve.
     */
    private final int wordsCount;

    /**
     * Number of allowed guesses.
     */
    private final int chances;

    /**
     * Current guess row index.
     */
    private int rowIndex = 0;

    /**
     * Current column index (letters typed in this row).
     */
    private int colIndex = 0;

    /**
     * Current typed letters for this guess.
     */
    private final char[] current;

    /**
     * Message label for errors/status.
     */
    private final Label message = new Label("");

    /**
     * Remaining guesses label.
     */
    private final Label remaining;

    /**
     * Cells indexed by [word][row][col].
     */
    private final Cell[][][] cells;

    /**
     * Row boxes indexed by [word][row].
     */
    private final HBox[][] rowBoxes;

    /**
     * SOLVED labels per word.
     */
    private final Label[] solvedLabels;

    /**
     * Track which words were solved before (so we stop painting/typing them).
     */
    private final boolean[] solvedBefore;

    /**
     * Known green letters to show as hints per word.
     */
    private final char[][] knownGreens;

    /**
     * Set of used letters (A-Z only) for keyboard greying logic.
     */
    private final Set<Character> usedLetters = new HashSet<>();

    /**
     * Keyboard coloring manager (keys registered by KeyboardPane).
     */
    private final KeyboardColorManager keyboardColors = new KeyboardColorManager();

    /**
     * Whether UI input is locked (game ended / gave up).
     */
    private boolean uiLocked = false;

    public WordleView(Navigator navigator, String languageValue, int lettersValue, int wordsCountValue) {
        this.nav = navigator;
        this.language = languageValue;

        Language lang = new Language(language);
        this.game = new WordleLogic(wordsCountValue, lettersValue, lang);

        this.letters = lettersValue;
        this.wordsCount = wordsCountValue;
        this.chances = game.getChances();
        this.current = new char[letters];

        this.remaining = new Label("Guesses left: " + chances);
        remaining.setStyle(GameStyles.INFO);
        message.setStyle(GameStyles.MSG_RED);

        this.cells = new Cell[wordsCount][chances][letters];
        this.rowBoxes = new HBox[wordsCount][chances];
        this.solvedLabels = new Label[wordsCount];
        this.solvedBefore = new boolean[wordsCount];
        this.knownGreens = new char[wordsCount][letters];

        Label title = new Label("Wordle");
        title.setStyle(GameStyles.TITLE);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 0, 10, 0));
        setTop(title);

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
                HBox rowBox = new HBox(8);
                rowBox.setAlignment(Pos.CENTER);

                for (int c = 0; c < letters; c++) {
                    Cell cell = new Cell();
                    cells[w][r][c] = cell;
                    rowBox.getChildren().add(cell);
                }

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

        this.backBtn = new Button("Back");
        backBtn.setPrefWidth(140);
        backBtn.setPrefHeight(44);
        backBtn.getStyleClass().add("big");
        backBtn.setFocusTraversable(false);
        backBtn.setVisible(false);
        backBtn.setManaged(false);
        backBtn.setOnAction(_ -> nav.goToSettings(language, "Wordle"));

        this.giveUpBtn = new Button("Give up");
        giveUpBtn.setPrefWidth(160);
        giveUpBtn.setPrefHeight(44);
        giveUpBtn.getStyleClass().add("big");
        giveUpBtn.setFocusTraversable(false);


        KeyboardPane keyboard = buildLetterKeyboard();

        HBox topActions = new HBox(12, backBtn);
        topActions.setAlignment(Pos.CENTER);

        HBox giveUpRow = new HBox(giveUpBtn);
        giveUpRow.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(10, topActions, remaining, message, keyboard, giveUpRow);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(14));
        setBottom(bottom);

        giveUpBtn.setOnAction(_ -> showStandardLoseWordle("You gave up."));

        setFocusTraversable(true);
        setOnMousePressed(_ -> requestFocus());

        setOnKeyPressed(e -> {
            if (uiLocked) {
                return;
            }
            if (game.isGameOver()) {
                return;
            }

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

        Platform.runLater(() -> {
            applyCss();
            layout();
            requestFocus();
            refreshHintsForTypingRow();
        });

    }

    // ---------- Submit ----------

    /**
     * Main "ENTER" handler: validates the current guess, submits it to the logic,
     * paints the result on all active boards, updates keyboard + counters,
     * and advances to the next row (or ends the game).
     */
    private void submit() {
        if (uiLocked) {
            return;
        }

        if (!canSubmit()) {
            return;
        }

        String guessRaw = new String(current);

        if (!isValidGuessWord(guessRaw)) {
            return;
        }

        addUsedLetters(guessRaw);

        WordleLogic.TurnResult result = submitToLogic(guessRaw);
        if (result == null) {
            return;
        }

        applyTurnResult(result);

        remaining.setText("Guesses left: " + result.getRemainingGuesses());

        updateKeyboardGreying();

        if (handleEndIfNeeded(result)) {
            return;
        }

        advanceToNextRow();
    }

    /**
     * Checks whether a guess can be submitted right now:
     * game not over, still have rows left, and the current row is fully typed.
     * Also sets an error message if something is wrong.
     */
    private boolean canSubmit() {
        if (game.isGameOver()) {
            message.setText("Game over.");
            return false;
        }
        if (rowIndex >= chances) {
            message.setText("No guesses left.");
            return false;
        }
        if (colIndex < letters) {
            message.setText("Not enough letters.");
            return false;
        }
        return true;
    }

    /**
     * Validates the guess using Hunspell (dictionary check).
     * If invalid, shows an error message.
     */
    private boolean isValidGuessWord(String guessRaw) {
        if (!HunspellChecker.isValidWord(guessRaw)) {
            message.setText("Not a valid word.");
            return false;
        }
        return true;
    }

    /**
     * Adds each letter from the guess to the "used letters" set,
     * so the keyboard can be updated later.
     */
    private void addUsedLetters(String guessRaw) {
        for (int i = 0; i < guessRaw.length(); i++) {
            char ch = Character.toUpperCase(guessRaw.charAt(i));
            if (ch >= 'A' && ch <= 'Z') {
                usedLetters.add(ch);
            }
        }
    }

    /**
     * Calls the game logic to score the guess and produce a TurnResult.
     * Catches and displays any validation error coming from the logic.
     *
     * @return the TurnResult, or null if submission failed
     */
    private WordleLogic.TurnResult submitToLogic(String guessRaw) {
        try {
            return game.submitGuess(guessRaw);
        } catch (IllegalArgumentException ex) {
            message.setText(ex.getMessage());
            return null;
        }
    }

    /**
     * Applies the TurnResult to the UI for the current row:
     * paints each unsolved board, updates SOLVED labels, and freezes boards
     * that become solved this turn.
     */
    private void applyTurnResult(WordleLogic.TurnResult result) {
        int paintedRow = rowIndex;

        String guess = result.getGuess();
        WordleLogic.Tile[][] tilesByWord = result.getTilesByWord();
        boolean[] solvedNowArr = result.getSolved();

        for (int w = 0; w < wordsCount; w++) {
            if (solvedBefore[w]) {
                continue;
            }

            boolean solvedNow = solvedNowArr[w];
            if (solvedNow) {
                solvedLabels[w].setText("SOLVED!");
                solvedBefore[w] = true;
            }

            paintBoardRow(w, paintedRow, guess, tilesByWord, solvedNow);
        }
    }

    /**
     * Paints a single board row (one word board, one guess row).
     * Also records newly discovered green letters into knownGreens for hints.
     */
    private void paintBoardRow(
            int wordIndex,
            int paintedRow,
            String guess,
            WordleLogic.Tile[][] tilesByWord,
            boolean solvedNow
    ) {
        for (int c = 0; c < letters; c++) {
            Cell cell = cells[wordIndex][paintedRow][c];
            char ch = guess.charAt(c);

            cell.clearHint();
            cell.setMain(ch);

            WordleLogic.Tile tile = tilesByWord[wordIndex][c];

            if (solvedNow) {
                cell.setTileBgGreen();
                knownGreens[wordIndex][c] = ch;
            } else if (tile == WordleLogic.Tile.GREEN) {
                cell.setTileBgGreen();
                knownGreens[wordIndex][c] = ch;
            } else if (tile == WordleLogic.Tile.YELLOW) {
                cell.setTileBgYellow();
            } else {
                cell.setTileBgGrey();
            }
        }
    }

    /**
     * If the game ended this turn, shows the win/lose screen and locks the UI.
     *
     * @return true if the game ended and the caller should stop processing
     */
    private boolean handleEndIfNeeded(WordleLogic.TurnResult result) {
        if (result.isGameWon()) {
            showStandardWinWordle();
            return true;
        }
        if (result.isGameOver()) {
            showStandardLoseWordle("Out of guesses.");
            return true;
        }
        return false;
    }

    /**
     * Moves input state to the next row:
     * reveals the next row (for unsolved boards), resets typed letters,
     * clears messages, refreshes hints, and re-focuses the view.
     */
    private void advanceToNextRow() {
        int nextRow = rowIndex + 1;
        if (nextRow < chances) {
            revealNextRow(nextRow);
        }

        rowIndex++;
        colIndex = 0;
        for (int i = 0; i < letters; i++) {
            current[i] = 0;
        }

        message.setText("");
        refreshHintsForTypingRow();
        requestFocus();
    }

    /**
     * Makes the given row visible on every board that is not yet solved.
     * (Solved boards stay frozen and don't reveal further rows.)
     */
    private void revealNextRow(int nextRow) {
        for (int w = 0; w < wordsCount; w++) {
            if (!solvedBefore[w]) {
                rowBoxes[w][nextRow].setManaged(true);
                rowBoxes[w][nextRow].setVisible(true);
            }
        }
    }

    /**
     * Types one letter into the current row (all unsolved boards),
     * updates the displayed tiles, advances the column,
     * and refreshes hint letters behind empty tiles.
     */
    private void typeChar(char ch) {
        if (uiLocked) {
            return;
        }
        if (game.isGameOver()) {
            return;
        }
        if (rowIndex >= chances) {
            return;
        }
        if (colIndex >= letters) {
            return;
        }

        char upper = Character.toUpperCase(ch);
        if (upper < 'A' || upper > 'Z') {
            return;
        }

        current[colIndex] = upper;

        for (int w = 0; w < wordsCount; w++) {
            if (solvedBefore[w]) {
                continue;
            }
            Cell cell = cells[w][rowIndex][colIndex];
            cell.setMain(upper);
            cell.setTileBgEmpty();
        }

        colIndex++;
        refreshHintsForTypingRow();
    }

    /**
     * Deletes the last typed character in the current row (all unsolved boards),
     * clears the tile, and refreshes hints.
     */
    private void backspace() {
        if (uiLocked) {
            return;
        }
        if (game.isGameOver()) {
            return;
        }
        if (rowIndex >= chances) {
            return;
        }
        if (colIndex <= 0) {
            return;
        }

        colIndex--;
        current[colIndex] = 0;

        for (int w = 0; w < wordsCount; w++) {
            if (solvedBefore[w]) {
                continue;
            }
            Cell cell = cells[w][rowIndex][colIndex];
            cell.clearMain();
            cell.setTileBgEmpty();
        }

        refreshHintsForTypingRow();
    }

    /**
     * Refreshes hint letters (known green positions) for the row being typed:
     * for each unsolved board, show a faint hint behind empty cells.
     */
    private void refreshHintsForTypingRow() {
        if (rowIndex >= chances) {
            return;
        }

        for (int w = 0; w < wordsCount; w++) {
            if (solvedBefore[w]) {
                continue;
            }

            for (int c = 0; c < letters; c++) {
                Cell cell = cells[w][rowIndex][c];

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

    /**
     * Greys out keyboard letters that are either:
     * - not present in any target word, OR
     * - fully resolved everywhere (all occurrences are known green).
     * This is your "smart greying" rule for multi-word Wordle.
     */
    private void updateKeyboardGreying() {
        String[] targetWords = game.getWords();

        for (char letter : usedLetters) {
            if (letter < 'A' || letter > 'Z') {
                continue;
            }

            boolean appearsSomewhere = false;
            boolean fullyResolvedEverywhere = true;

            for (int w = 0; w < wordsCount; w++) {
                String word = targetWords[w];
                boolean appearsInThisWord = false;
                boolean allOccurrencesGreen = true;

                for (int pos = 0; pos < letters; pos++) {
                    if (word.charAt(pos) == letter) {
                        appearsInThisWord = true;
                        appearsSomewhere = true;

                        if (knownGreens[w][pos] != letter) {
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
                keyboardColors.promoteKey(letter, 1);
            }
        }
    }

    /**
     * Locks the game UI after win/lose:
     * disables further input (uiLocked), hides Give Up, shows Back.
     */
    private void endGameUI() {
        uiLocked = true;

        giveUpBtn.setVisible(false);
        giveUpBtn.setManaged(false);

        backBtn.setVisible(true);
        backBtn.setManaged(true);

        requestFocus();
    }

    /**
     * Wraps a VBox in a ScrollPane so end screens don't overflow on small windows
     * (useful when many words must be shown).
     */
    private ScrollPane wrapCenterInScroll(VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scroll;
    }

    /**
     * Replaces the center of the view with a win screen and locks the UI.
     */
    private void showStandardWinWordle() {
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));

        Label won = new Label("You win!");
        won.setStyle(GameStyles.TITLE);
        content.getChildren().add(won);

        setCenter(wrapCenterInScroll(content));

        message.setStyle(GameStyles.MSG_GREEN);
        message.setText("You solved all words!");

        endGameUI();
    }

    /**
     * Replaces the center of the view with a loose screen (listing all target words)
     * and locks the UI.
     */
    private void showStandardLoseWordle(String msg) {
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));

        Label lost = new Label("You lost. The words were:");
        lost.setStyle("-fx-font-size: 30px; -fx-font-weight: bold;");
        content.getChildren().add(lost);

        for (String w : game.getWords()) {
            Label wLabel = new Label(w);
            wLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
            content.getChildren().add(wLabel);
        }

        setCenter(wrapCenterInScroll(content));

        message.setStyle(GameStyles.MSG_RED);
        message.setText(msg);

        endGameUI();
    }

    /**
     * Builds the on-screen QWERTZ keyboard and wires it to submit/backspace/typeChar.
     * Also registers keys with KeyboardColorManager so they can be recolored later.
     */
    private KeyboardPane buildLetterKeyboard() {
        String[][] rows = {
                {"Q", "W", "E", "R", "T", "Z", "U", "I", "O", "P"},
                {"A", "S", "D", "F", "G", "H", "J", "K", "L"},
                {"ENTER", "Y", "X", "C", "V", "B", "N", "M", "⌫"}
        };

        KeyboardPane.KeySizing sizing = new KeyboardPane.KeySizing(
                42, 50, 120, 70,
                8, 8,
                new Insets(6, 0, 0, 0)
        );

        KeyboardPane.Handlers handlers = new KeyboardPane.Handlers(
                this::submit,
                this::backspace,
                this::typeChar,
                this::requestFocus
        );

        return new KeyboardPane(
                rows,
                sizing,
                _ -> GameStyles.keyBase(),
                keyboardColors,
                handlers
        );
    }
}
