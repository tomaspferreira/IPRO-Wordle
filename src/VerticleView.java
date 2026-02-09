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
        backBtn.setStyle(GameStyles.bigButton());
        backBtn.setFocusTraversable(false);
        backBtn.setVisible(false);
        backBtn.setManaged(false);

        Button giveUpBtn = new Button("Give up");
        giveUpBtn.setPrefWidth(160);
        giveUpBtn.setPrefHeight(44);
        giveUpBtn.setStyle(GameStyles.bigButton());
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

        this.enterAction = () -> {
            if (uiLocked) {
                return;
            }

            if (game.isGameOver()) {
                message.setText("Game over.");
                return;
            }

            if (typedIndex < letters) {
                message.setText("Not enough letters.");
                return;
            }

            String guess = new String(current);

            if (!HunspellChecker.isValidWord(guess)) {
                message.setText("Not a valid word.");
                return;
            }

            VerticleLogic.TurnResult r;
            try {
                r = game.submitGuess(guess);
            } catch (IllegalArgumentException ex) {
                message.setText(ex.getMessage());
                return;
            }

            int col = r.getTryIndex();

            for (int row = 0; row < letters; row++) {
                Label t = tiles[row][col];
                char ch = r.getGuess().charAt(row);

                t.setText(String.valueOf(ch));
                t.setStyle(GameStyles.tileBase() + styleFor(r.getTiles()[row]));

                keyboardColors.promoteKey(ch, rankForVertTile(r.getTiles()[row]));
            }

            remaining.setText("Guesses left: " + r.getRemainingGuesses());
            message.setText("");

            typedIndex = 0;
            for (int i = 0; i < letters; i++) {
                current[i] = 0;
            }

            if (!r.isGameOver() && col + 1 < chances) {
                addColumnToGrid(col + 1);
            }

            if (r.isGameWon()) {
                uiLocked = true;
                showStandardWin(backBtn, giveUpBtn, "Correct!");
            } else if (r.isGameOver()) {
                uiLocked = true;
                showStandardLose(backBtn, giveUpBtn, "Out of guesses.");
            }

            requestFocus();
        };

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
                if (enterAction != null) {
                    enterAction.run();
                }
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
        });
    }

    private void addColumnToGrid(int col) {
        for (int r = 0; r < letters; r++) {
            grid.add(tiles[r][col], col, r);
        }
    }

    private void typeChar(char ch) {
        if (uiLocked) {
            return;
        }
        if (game.isGameOver()) {
            return;
        }

        int col = game.getTries();
        if (col >= chances) {
            return;
        }
        if (typedIndex >= letters) {
            return;
        }

        ch = Character.toUpperCase(ch);
        current[typedIndex] = ch;

        Label t = tiles[typedIndex][col];
        t.setText(String.valueOf(ch));
        t.setStyle(GameStyles.tileBase() + GameStyles.tileEmpty());

        typedIndex++;
    }

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

    private String styleFor(VerticleLogic.Tile tile) {
        if (tile == VerticleLogic.Tile.GREEN) {
            return GameStyles.tileGreen();
        }
        if (tile == VerticleLogic.Tile.YELLOW) {
            return GameStyles.tileYellow();
        }
        return GameStyles.tileGrey();
    }

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
