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

public class VerticleView extends BorderPane {

    /**
     * Navigator used to switch between screens.
     */
    private final Navigator nav;

    /**
     * Current language code (e.g. "en", "de").
     */
    private final String language;

    /**
     * Game logic for Verticle.
     */
    private final VerticleLogic game;

    /**
     * Number of letters in the secret word.
     */
    private final int letters;

    /**
     * Number of allowed guesses/columns.
     */
    private final int chances;

    /**
     * Index of how many letters were typed in the current column (goes down).
     */
    private int typedIndex = 0;

    /**
     * Current guess characters for the column being typed.
     */
    private final char[] current;

    /**
     * Message label shown under remaining guesses.
     */
    private final Label message = new Label("");

    /**
     * Label showing remaining guesses.
     */
    private final Label remaining;

    /**
     * Grid holding the tile labels.
     */
    private final GridPane grid = new GridPane();

    /**
     * Tile labels indexed by [row][col].
     */
    private final Label[][] tiles;

    /**
     * Keyboard coloring manager (never downgrade).
     */
    private final KeyboardColorManager keyboardColors = new KeyboardColorManager();

    /**
     * Action executed when ENTER is pressed.
     */
    private Runnable enterAction;

    /**
     * Whether the UI input is locked (game ended / give up).
     */
    private boolean uiLocked = false;

    public VerticleView(Navigator navigator, String languageValue, int lettersInput) {
        this.nav = navigator;
        this.language = languageValue;

        Language lang = new Language(language);
        this.game = new VerticleLogic(lettersInput, lang);

        this.letters = game.getLetters();
        this.chances = game.getChances();
        this.current = new char[this.letters];

        this.remaining = new Label("Guesses left: " + chances);
        remaining.setStyle(GameStyles.INFO);
        message.setStyle(GameStyles.MSG_RED);

        Label title = new Label("Verticle");
        title.setStyle(GameStyles.TITLE);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 0, 10, 0));
        setTop(title);

        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20));

        tiles = new Label[letters][chances];

        for (int r = 0; r < letters; r++) {
            for (int c = 0; c < chances; c++) {
                Label t = new Label(" ");
                t.setMinSize(GameStyles.TILE_SIZE, GameStyles.TILE_SIZE);
                t.setPrefSize(GameStyles.TILE_SIZE, GameStyles.TILE_SIZE);
                t.setMaxSize(GameStyles.TILE_SIZE, GameStyles.TILE_SIZE);
                t.setAlignment(Pos.CENTER);
                t.setStyle(GameStyles.tileBase() + GameStyles.tileEmpty());
                tiles[r][c] = t;
            }
        }

        addColumnToGrid(0);

        ScrollPane centerScroll = new ScrollPane(grid);
        centerScroll.setFitToWidth(true);
        centerScroll.setFitToHeight(true);
        centerScroll.setPannable(true);
        centerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        centerScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setCenter(centerScroll);

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

        backBtn.setOnAction(_ -> nav.goToSettings(language, "Verticle"));

        giveUpBtn.setOnAction(_ -> {
            uiLocked = true;
            showStandardLose(backBtn, giveUpBtn, "You gave up.");
        });

        // ----- ENTER action for Verticle: validate + submit + paint current column -----
        this.enterAction = () -> {

            // Ignore submit if UI is locked (game ended / give up screen)
            if (uiLocked) {
                return;
            }

            // Extra safety: don't submit if logic says game is already over
            if (game.isGameOver()) {
                message.setText("Game over.");
                return;
            }

            // Must fill the whole "vertical guess" (letters characters) before submitting
            if (typedIndex < letters) {
                message.setText("Not enough letters.");
                return;
            }

            // Build guess from typed characters
            String guess = new String(current);

            // Dictionary validation (only allow real words)
            if (!HunspellChecker.isValidWord(guess)) {
                message.setText("Not a valid word.");
                return;
            }

            // Submit guess to the Verticle logic (may throw if invalid)
            VerticleLogic.TurnResult r;
            try {
                r = game.submitGuess(guess);
            } catch (IllegalArgumentException ex) {
                message.setText(ex.getMessage());
                return;
            }

            // In Verticle: each guess fills a COLUMN, and tryIndex tells which column
            int col = r.getTryIndex();

            // Paint all rows in that column with green/yellow/grey feedback
            for (int row = 0; row < letters; row++) {
                Label t = tiles[row][col];
                char ch = r.getGuess().charAt(row);

                t.setText(String.valueOf(ch));
                t.setStyle(GameStyles.tileBase() + styleFor(r.getTiles()[row]));

                // Update keyboard colors (keys never downgrade)
                keyboardColors.promoteKey(ch, rankForVertTile(r.getTiles()[row]));
            }

            // Update remaining guesses label and clear message
            remaining.setText("Guesses left: " + r.getRemainingGuesses());
            message.setText("");

            // Reset typing buffer for the next column
            typedIndex = 0;
            for (int i = 0; i < letters; i++) {
                current[i] = 0;
            }

            // Reveal/build the next column if the game continues
            if (!r.isGameOver() && col + 1 < chances) {
                addColumnToGrid(col + 1);
            }

            // If game ended, show win/lose screen and lock UI
            if (r.isGameWon()) {
                uiLocked = true;
                showStandardWin(backBtn, giveUpBtn, "Correct!");
            } else if (r.isGameOver()) {
                uiLocked = true;
                showStandardLose(backBtn, giveUpBtn, "Out of guesses.");
            }

            // Keep focus so physical keyboard input continues to work
            requestFocus();
        };


// ----- Make this BorderPane focusable so it can receive key events -----
        setFocusTraversable(true);

// Clicking anywhere should give focus back (so keyboard keeps working)
        setOnMousePressed(_ -> requestFocus());


// ----- Physical keyboard handler: ENTER / BACKSPACE / A-Z typing -----
        setOnKeyPressed(e -> {

            // Ignore input when UI is locked or game ended
            if (uiLocked) {
                return;
            }
            if (game.isGameOver()) {
                return;
            }

            // ENTER submits using the prepared enterAction
            if (e.getCode() == KeyCode.ENTER) {
                if (enterAction != null) {
                    enterAction.run();
                }
                return;
            }

            // BACKSPACE deletes the last typed letter in the current column
            if (e.getCode() == KeyCode.BACK_SPACE) {
                backspace();
                return;
            }

            // Any typed character: accept only A-Z and type it
            String txt = e.getText();
            if (txt != null && txt.length() == 1) {
                char ch = Character.toUpperCase(txt.charAt(0));
                if (ch >= 'A' && ch <= 'Z') {
                    typeChar(ch);
                }
            }
        });


// ----- After the UI is shown, apply CSS/layout and request focus -----
        Platform.runLater(() -> {
            applyCss();
            layout();
            requestFocus();
        });

    }

    /**
     * Adds one entire column of tiles to the GridPane at the given column index.
     * (Verticle fills columns over time, not rows.)
     */
    private void addColumnToGrid(int col) {
        for (int r = 0; r < letters; r++) {
            grid.add(tiles[r][col], col, r);
        }
    }

    /**
     * Types one letter into the current column at row = typedIndex.
     * Uses game.getTries() to figure out which column we're currently filling.
     */
    private void typeChar(char ch) {
        if (uiLocked) {
            return;
        }
        if (game.isGameOver()) {
            return;
        }

        // Current column equals the number of tries already used
        int col = game.getTries();
        if (col >= chances) {
            return;
        }

        // Stop if column is full
        if (typedIndex >= letters) {
            return;
        }

        // Store typed char in buffer and show it on the board
        ch = Character.toUpperCase(ch);
        current[typedIndex] = ch;

        Label t = tiles[typedIndex][col];
        t.setText(String.valueOf(ch));
        t.setStyle(GameStyles.tileBase() + GameStyles.tileEmpty());

        typedIndex++;
    }

    /**
     * Deletes the last typed letter in the current column
     * and clears that tile on the board.
     */
    private void backspace() {
        if (uiLocked) {
            return;
        }
        if (typedIndex <= 0) {
            return;
        }

        typedIndex--;
        current[typedIndex] = 0;

        int col = game.getTries();
        Label t = tiles[typedIndex][col];
        t.setText(" ");
        t.setStyle(GameStyles.tileBase() + GameStyles.tileEmpty());
    }

    /**
     * Maps VerticleLogic tile feedback to board CSS style.
     */
    private String styleFor(VerticleLogic.Tile tile) {
        if (tile == VerticleLogic.Tile.GREEN) {
            return GameStyles.tileGreen();
        }
        if (tile == VerticleLogic.Tile.YELLOW) {
            return GameStyles.tileYellow();
        }
        return GameStyles.tileGrey();
    }

    /**
     * Shows a standard win screen in the center and swaps buttons:
     * hide Give up, show Back.
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
     * Shows a standard lose screen (revealing the secret word),
     * then swaps buttons and locks the view to end input.
     */
    private void showStandardLose(Button backBtn, Button giveUpBtn, String msg) {
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));

        Label lost = new Label("You lost. The word was:");
        lost.setStyle("-fx-font-size: 30px; -fx-font-weight: bold;");
        content.getChildren().add(lost);

        Label answer = new Label(game.getWord());
        answer.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        content.getChildren().add(answer);

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
     * Builds the on-screen keyboard and wires it to:
     * ENTER -> enterAction, ⌫ -> backspace, letters -> typeChar.
     * Also registers keys for recoloring via KeyboardColorManager.
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
     * Converts Verticle tile feedback into a keyboard "rank":
     * GREY=1, YELLOW=2, GREEN=3 (so keys never downgrade).
     */
    private int rankForVertTile(VerticleLogic.Tile t) {
        if (t == VerticleLogic.Tile.GREY) {
            return 1;
        }
        if (t == VerticleLogic.Tile.YELLOW) {
            return 2;
        }
        return 3;
    }
}
