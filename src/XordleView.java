import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class XordleView extends BorderPane {

    /**
     * Navigator used to switch screens.
     */
    private final Navigator nav;

    /**
     * Language code (e.g., "en", "de").
     */
    private final String language;

    /**
     * Game logic instance.
     */
    private final XordleLogic game;

    /**
     * Number of letters per guess.
     */
    private final int letters;

    /**
     * Number of allowed guesses.
     */
    private final int chances;

    /**
     * Current row (guess index).
     */
    private int rowIndex;

    /**
     * Current column (typed letters in this row).
     */
    private int colIndex;

    /**
     * Current typed letters.
     */
    private final char[] current;

    /**
     * Status / error message label.
     */
    private final Label message;

    /**
     * Remaining guesses label.
     */
    private final Label remaining;

    /**
     * Board grid.
     */
    private final GridPane grid;

    /**
     * Tile labels indexed by [row][col].
     */
    private final Label[][] tiles;

    /**
     * Shared keyboard coloring (never downgrade).
     */
    private final KeyboardColorManager keyboardColors;

    /**
     * Enter action (submit).
     */
    private Runnable enterAction;

    /**
     * When true, UI ignores input.
     */
    private boolean uiLocked;

    public XordleView(Navigator navigator, String languageValue, int lettersValue) {
        this.nav = navigator;
        this.language = languageValue;

        Language lang = new Language(languageValue);
        this.game = new XordleLogic(lettersValue, lang);

        this.letters = game.getLetters();
        this.chances = game.getChances();

        this.rowIndex = 0;
        this.colIndex = 0;
        this.current = new char[this.letters];

        this.message = new Label("");
        this.remaining = new Label("Guesses left: " + chances);

        this.grid = new GridPane();
        this.tiles = new Label[chances][this.letters];

        this.keyboardColors = new KeyboardColorManager();
        this.uiLocked = false;

        remaining.setStyle(GameStyles.INFO);
        message.setStyle(GameStyles.MSG_RED);

        buildTop();
        buildGrid();
        buildBottom();

        Platform.runLater(() -> {
            applyCss();
            layout();
            requestFocus();
        });
    }

    private void buildTop() {
        Label title = new Label("Xordle");
        title.setStyle(GameStyles.TITLE);

        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 0, 10, 0));
        setTop(title);
    }

    private void buildGrid() {
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20));

        for (int r = 0; r < chances; r++) {
            for (int c = 0; c < letters; c++) {
                Label t = new Label(" ");
                t.setMinSize(GameStyles.TILE_SIZE, GameStyles.TILE_SIZE);
                t.setPrefSize(GameStyles.TILE_SIZE, GameStyles.TILE_SIZE);
                t.setMaxSize(GameStyles.TILE_SIZE, GameStyles.TILE_SIZE);
                t.setAlignment(Pos.CENTER);
                t.setStyle(GameStyles.tileBase() + GameStyles.tileEmpty());
                tiles[r][c] = t;
            }
        }

        addRowToGrid(0);

        ScrollPane centerScroll = new ScrollPane(grid);
        centerScroll.setFitToWidth(true);
        centerScroll.setFitToHeight(true);
        centerScroll.setPannable(true);
        centerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        centerScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setCenter(centerScroll);
    }

    private void buildBottom() {
        Button backBtn = new Button("Back");
        backBtn.setPrefWidth(140);
        backBtn.setPrefHeight(44);
        backBtn.getStyleClass().add("big");
        backBtn.setFocusTraversable(false);
        backBtn.setVisible(false);
        backBtn.setManaged(false);

        Button giveUpBtn = new Button("Give up");
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

        backBtn.setOnAction(_ -> nav.goToSettings(language, "Xordle"));

        giveUpBtn.setOnAction(_ -> {
            uiLocked = true;
            showStandardLose(backBtn, giveUpBtn, "You gave up.");
        });

        this.enterAction = () -> submit(backBtn, giveUpBtn);

        setFocusTraversable(true);
        setOnMousePressed(_ -> requestFocus());

        // ----- Physical keyboard input handler (ENTER / BACKSPACE / A-Z typing) -----
        setOnKeyPressed(e -> {

            // Ignore input after the game ends or after Give Up / win / loss screens
            if (uiLocked) {
                return;
            }
            if (game.isGameOver()) {
                return;
            }

            // ENTER submits the current guess
            if (e.getCode() == KeyCode.ENTER) {
                if (enterAction != null) {
                    enterAction.run();
                }
                return;
            }

            // BACKSPACE deletes the last typed letter
            if (e.getCode() == KeyCode.BACK_SPACE) {
                backspace();
                return;
            }

            // Any single typed character: accept only A-Z and add it to the row
            String txt = e.getText();
            if (txt != null && txt.length() == 1) {
                char ch = Character.toUpperCase(txt.charAt(0));
                if (ch >= 'A' && ch <= 'Z') {
                    typeChar(ch);
                }
            }
        });
    }

    /**
     * Submits the current row as a guess:
     * validates input, checks dictionary, calls XordleLogic, paints the row,
     * updates messages/counters, and handles win/lose or moves to next row.
     */
    private void submit(Button backBtn, Button giveUpBtn) {
        if (uiLocked) {
            return;
        }

        // ----- Basic game state checks -----
        if (game.isGameOver()) {
            message.setText("Game over.");
            return;
        }

        // ----- Bounds checks for guesses -----
        if (rowIndex >= chances) {
            message.setText("No guesses left.");
            return;
        }

        // ----- Must type a full word before submitting -----
        if (colIndex < letters) {
            message.setText("Not enough letters.");
            return;
        }

        // Build guess string from typed characters
        String guess = new String(current);

        // ----- Dictionary validation (Hunspell) -----
        if (!HunspellChecker.isValidWord(guess)) {
            message.setText("Not a valid word.");
            return;
        }

        // ----- Submit to logic (may throw if invalid) -----
        XordleLogic.TurnResult result;
        try {
            result = game.submitGuess(guess);
        } catch (IllegalArgumentException ex) {
            message.setText(ex.getMessage());
            return;
        }

        // ----- Paint tiles for this row + update keyboard colors -----
        paintRow(result);

        // Update remaining guesses label
        remaining.setText("Guesses left: " + result.getRemainingGuesses());

        // Show “Word 1 solved!” / “Word 2 solved!” feedback if it happened this turn
        setSolvedMessage(result);

        // ----- Endgame handling -----
        if (result.isGameWon()) {
            uiLocked = true;
            showStandardWin(backBtn, giveUpBtn, "Solved both words!");
            return;
        }

        if (result.isGameOver()) {
            uiLocked = true;
            showStandardLose(backBtn, giveUpBtn, "Out of guesses.");
            return;
        }

        // ----- Continue to next row -----
        moveToNextRow();
        requestFocus();
    }

    /**
     * Paints the current row using the tile colors returned by XordleLogic.
     * Also promotes keyboard colors so keys never downgrade.
     */
    private void paintRow(XordleLogic.TurnResult result) {
        String guess = result.getGuess();
        XordleLogic.Tile[] rowTiles = result.getTiles();

        for (int c = 0; c < letters; c++) {
            Label t = tiles[rowIndex][c];
            char ch = guess.charAt(c);

            t.setText(String.valueOf(ch));
            t.setStyle(GameStyles.tileBase() + styleFor(rowTiles[c]));

            // Promote keyboard key color based on strongest information seen so far
            keyboardColors.promoteKey(ch, rankForXordleTile(rowTiles[c]));
        }
    }

    /**
     * Shows a short status message if one or both secret words were solved
     * on this specific turn; otherwise clears the message.
     */
    private void setSolvedMessage(XordleLogic.TurnResult result) {
        boolean[] newly = result.getNewlySolved();

        if (newly[0] && newly[1]) {
            message.setText("✅ Both words solved!");
            return;
        }
        if (newly[0]) {
            message.setText("✅ Word 1 solved!");
            return;
        }
        if (newly[1]) {
            message.setText("✅ Word 2 solved!");
            return;
        }

        message.setText("");
    }

    /**
     * Advances to the next guess row:
     * resets typing state and adds the next row of tiles to the grid if needed.
     */
    private void moveToNextRow() {
        rowIndex++;
        colIndex = 0;

        // Clear the current typed buffer
        for (int i = 0; i < letters; i++) {
            current[i] = 0;
        }

        // Reveal/build next row in the grid
        if (rowIndex < chances) {
            addRowToGrid(rowIndex);
        }
    }

    /**
     * Adds one row of tile Labels to the GridPane at the given row index.
     */
    private void addRowToGrid(int row) {
        for (int c = 0; c < letters; c++) {
            grid.add(tiles[row][c], c, row);
        }
    }

    /**
     * Types a character into the current row (if there is space),
     * updates the visible tile, and advances the column cursor.
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
        current[colIndex] = upper;

        Label t = tiles[rowIndex][colIndex];
        t.setText(String.valueOf(upper));
        t.setStyle(GameStyles.tileBase() + GameStyles.tileEmpty());

        colIndex++;
    }

    /**
     * Removes the last typed character from the current row and clears that tile.
     */
    private void backspace() {
        if (uiLocked) {
            return;
        }
        if (game.isGameOver()) {
            return;
        }
        if (colIndex <= 0) {
            return;
        }

        colIndex--;
        current[colIndex] = 0;

        Label t = tiles[rowIndex][colIndex];
        t.setText(" ");
        t.setStyle(GameStyles.tileBase() + GameStyles.tileEmpty());
    }

    /**
     * Maps an Xordle tile to a CSS style string for the board.
     */
    private String styleFor(XordleLogic.Tile tile) {
        if (tile == XordleLogic.Tile.BLUE) {
            return GameStyles.tileBlue();
        } else if (tile == XordleLogic.Tile.GREEN) {
            return GameStyles.tileGreen();
        } else if (tile == XordleLogic.Tile.YELLOW) {
            return GameStyles.tileYellow();
        } else {
            return GameStyles.tileGrey();
        }
    }

    /**
     * Displays a standard win screen in the center, updates message styling,
     * and switches button visibility (hide Give up, show Back).
     */
    private void showStandardWin(Button backBtn, Button giveUpBtn, String messageText) {
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));

        Label won = new Label("You win!");
        won.setStyle(GameStyles.TITLE);
        content.getChildren().add(won);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setCenter(scroll);

        message.setStyle(GameStyles.MSG_GREEN);
        message.setText(messageText);

        giveUpBtn.setVisible(false);
        giveUpBtn.setManaged(false);
        backBtn.setVisible(true);
        backBtn.setManaged(true);

        requestFocus();
    }

    /**
     * Displays a standard lose screen in the center (including both secret words),
     * updates message styling, and switches button visibility.
     */
    private void showStandardLose(Button backBtn, Button giveUpBtn, String msg) {
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));

        Label lost = new Label("You lost. The words were:");
        lost.setStyle("-fx-font-size: 30px; -fx-font-weight: bold;");
        content.getChildren().add(lost);

        String[] ws = game.getWords();
        for (String w : ws) {
            Label wLabel = new Label(w);
            wLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
            content.getChildren().add(wLabel);
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setCenter(scroll);

        message.setStyle(GameStyles.MSG_RED);
        message.setText(msg);

        giveUpBtn.setVisible(false);
        giveUpBtn.setManaged(false);
        backBtn.setVisible(true);
        backBtn.setManaged(true);

        requestFocus();
    }

    /**
     * Builds the on-screen QWERTZ keyboard and connects it to:
     * ENTER -> submit, ⌫ -> backspace, letters -> typeChar.
     * Also registers keys with KeyboardColorManager so they can be recolored.
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
                () -> {
                    if (enterAction != null) {
                        enterAction.run();
                    }
                },
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

    /**
     * Converts an Xordle tile into a keyboard "rank" so keys never downgrade:
     * GREY=1, YELLOW=2, GREEN=3, BLUE=4.
     */
    private int rankForXordleTile(XordleLogic.Tile tile) {
        if (tile == XordleLogic.Tile.GREY) {
            return 1;
        } else if (tile == XordleLogic.Tile.YELLOW) {
            return 2;
        } else if (tile == XordleLogic.Tile.GREEN) {
            return 3;
        } else if (tile == XordleLogic.Tile.BLUE) {
            return 4;
        } else {
            return 1;
        }
    }
}
