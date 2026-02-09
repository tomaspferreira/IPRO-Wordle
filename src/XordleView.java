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

    /** Navigator used to switch screens. */
    private final Navigator nav;

    /** Language code (e.g., "en", "de"). */
    private final String language;

    /** Game logic instance. */
    private final XordleLogic game;

    /** Number of letters per guess. */
    private final int letters;

    /** Number of allowed guesses. */
    private final int chances;

    /** Current row (guess index). */
    private int rowIndex;

    /** Current column (typed letters in this row). */
    private int colIndex;

    /** Current typed letters. */
    private final char[] current;

    /** Status / error message label. */
    private final Label message;

    /** Remaining guesses label. */
    private final Label remaining;

    /** Board grid. */
    private final GridPane grid;

    /** Tile labels indexed by [row][col]. */
    private final Label[][] tiles;

    /** Shared keyboard coloring (never downgrade). */
    private final KeyboardColorManager keyboardColors;

    /** Enter action (submit). */
    private Runnable enterAction;

    /** When true, UI ignores input. */
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

        backBtn.setOnAction(_ -> nav.goToSettings(language, "Xordle"));

        giveUpBtn.setOnAction(_ -> {
            uiLocked = true;
            showStandardLose(backBtn, giveUpBtn, "You gave up.");
        });

        this.enterAction = () -> submit(backBtn, giveUpBtn);

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
    }

    private void submit(Button backBtn, Button giveUpBtn) {
        if (uiLocked) {
            return;
        }

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

        if (!HunspellChecker.isValidWord(guess)) {
            message.setText("Not a valid word.");
            return;
        }

        XordleLogic.TurnResult result;
        try {
            result = game.submitGuess(guess);
        } catch (IllegalArgumentException ex) {
            message.setText(ex.getMessage());
            return;
        }

        paintRow(result);
        remaining.setText("Guesses left: " + result.getRemainingGuesses());

        setSolvedMessage(result);

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

        moveToNextRow();
        requestFocus();
    }

    private void paintRow(XordleLogic.TurnResult result) {
        String guess = result.getGuess();
        XordleLogic.Tile[] rowTiles = result.getTiles();

        for (int c = 0; c < letters; c++) {
            Label t = tiles[rowIndex][c];
            char ch = guess.charAt(c);

            t.setText(String.valueOf(ch));
            t.setStyle(GameStyles.tileBase() + styleFor(rowTiles[c]));

            keyboardColors.promoteKey(ch, rankForXordleTile(rowTiles[c]));
        }
    }

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

    private void moveToNextRow() {
        rowIndex++;
        colIndex = 0;

        for (int i = 0; i < letters; i++) {
            current[i] = 0;
        }

        if (rowIndex < chances) {
            addRowToGrid(rowIndex);
        }
    }

    private void addRowToGrid(int row) {
        for (int c = 0; c < letters; c++) {
            grid.add(tiles[row][c], c, row);
        }
    }

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
