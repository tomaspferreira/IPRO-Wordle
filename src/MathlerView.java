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

public class MathlerView extends BorderPane {

    /**
     * Navigator used to switch between screens.
     */
    private final Navigator nav;

    /**
     * Game logic for Mathler.
     */
    private final MathlerLogic game;

    /**
     * Required length of an equation guess.
     */
    private final int len;

    /**
     * Number of allowed guesses.
     */
    private final int chances;

    /**
     * Current row index in the grid.
     */
    private int rowIndex = 0;

    /**
     * Current column index in the grid.
     */
    private int colIndex = 0;

    /**
     * Current guess characters being typed.
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
     * Tile labels by row and column.
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
     * Whether UI input is locked (game ended / give up).
     */
    private boolean uiLocked = false;

    public MathlerView(Navigator navigator, int numbersCount) {
        this.nav = navigator;
        this.game = new MathlerLogic(numbersCount);

        this.len = game.getEquationLength();
        this.chances = game.getChances();
        this.current = new char[len];

        this.remaining = new Label("Guesses left: " + chances);
        remaining.setStyle(GameStyles.INFO);
        message.setStyle(GameStyles.MSG_RED);

        Label title = new Label("Mathler");
        title.setStyle(GameStyles.TITLE);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 0, 10, 0));

        Label target = new Label("Target result: " + game.getTarget());
        target.setStyle("-fx-font-size: 24px;");

        VBox top = new VBox(8, title, target);
        top.setAlignment(Pos.CENTER);
        setTop(top);

        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20));

        tiles = new Label[chances][len];

        for (int r = 0; r < chances; r++) {
            for (int c = 0; c < len; c++) {
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

        KeyboardPane keyboard = buildMathKeyboard();

        HBox topActions = new HBox(12, backBtn);
        topActions.setAlignment(Pos.CENTER);

        HBox giveUpRow = new HBox(giveUpBtn);
        giveUpRow.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(10, topActions, remaining, message, keyboard, giveUpRow);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(14));
        setBottom(bottom);

        backBtn.setOnAction(_ -> nav.goToSettings("en", "Mathler"));

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
            if (rowIndex >= chances) {
                message.setText("No guesses left.");
                return;
            }
            if (colIndex < len) {
                message.setText("Not enough characters.");
                return;
            }

            String guess = new String(current);

            MathlerLogic.TurnResult r;
            try {
                r = game.submitGuess(guess);
            } catch (IllegalArgumentException ex) {
                message.setText(ex.getMessage());
                return;
            }

            for (int c = 0; c < len; c++) {
                Label t = tiles[rowIndex][c];
                char ch = r.getGuess().charAt(c);

                t.setText(String.valueOf(ch));
                t.setStyle(GameStyles.tileBase() + styleFor(r.getTiles()[c]));

                if (ch == 'x' || ch == 'X') {
                    ch = '*';
                }
                keyboardColors.promoteKey(ch, rankForMathTile(r.getTiles()[c]));
            }

            remaining.setText("Guesses left: " + r.getRemainingGuesses());
            message.setText("");

            if (r.isGameWon()) {
                uiLocked = true;
                showStandardWin(backBtn, giveUpBtn, "Correct!");
                return;
            }
            if (r.isGameOver()) {
                uiLocked = true;
                showStandardLose(backBtn, giveUpBtn, "Out of guesses.");
                return;
            }

            rowIndex++;
            colIndex = 0;
            for (int i = 0; i < len; i++) {
                current[i] = 0;
            }

            if (rowIndex < chances) {
                addRowToGrid(rowIndex);
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

            boolean shift = e.isShiftDown();

            if (e.getCode() == KeyCode.DIGIT1) {
                typeChar(shift ? '+' : '1');
                return;
            }
            if (e.getCode() == KeyCode.DIGIT3) {
                typeChar(shift ? '*' : '3');
                return;
            }
            if (e.getCode() == KeyCode.DIGIT7) {
                typeChar(shift ? '/' : '7');
                return;
            }

            if (e.getCode() == KeyCode.DIGIT0) {
                typeChar('0');
                return;
            }
            if (e.getCode() == KeyCode.DIGIT2) {
                typeChar('2');
                return;
            }
            if (e.getCode() == KeyCode.DIGIT4) {
                typeChar('4');
                return;
            }
            if (e.getCode() == KeyCode.DIGIT5) {
                typeChar('5');
                return;
            }
            if (e.getCode() == KeyCode.DIGIT6) {
                typeChar('6');
                return;
            }
            if (e.getCode() == KeyCode.DIGIT8) {
                typeChar('8');
                return;
            }
            if (e.getCode() == KeyCode.DIGIT9) {
                typeChar('9');
                return;
            }

            if (e.getCode() == KeyCode.NUMPAD0) {
                typeChar('0');
                return;
            }
            if (e.getCode() == KeyCode.NUMPAD1) {
                typeChar('1');
                return;
            }
            if (e.getCode() == KeyCode.NUMPAD2) {
                typeChar('2');
                return;
            }
            if (e.getCode() == KeyCode.NUMPAD3) {
                typeChar('3');
                return;
            }
            if (e.getCode() == KeyCode.NUMPAD4) {
                typeChar('4');
                return;
            }
            if (e.getCode() == KeyCode.NUMPAD5) {
                typeChar('5');
                return;
            }
            if (e.getCode() == KeyCode.NUMPAD6) {
                typeChar('6');
                return;
            }
            if (e.getCode() == KeyCode.NUMPAD7) {
                typeChar('7');
                return;
            }
            if (e.getCode() == KeyCode.NUMPAD8) {
                typeChar('8');
                return;
            }
            if (e.getCode() == KeyCode.NUMPAD9) {
                typeChar('9');
                return;
            }

            if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.ADD) {
                typeChar('+');
                return;
            }
            if (e.getCode() == KeyCode.MINUS || e.getCode() == KeyCode.SUBTRACT) {
                typeChar('-');
                return;
            }
            if (e.getCode() == KeyCode.SLASH || e.getCode() == KeyCode.DIVIDE) {
                typeChar('/');
                return;
            }
            if (e.getCode() == KeyCode.ASTERISK || e.getCode() == KeyCode.MULTIPLY) {
                typeChar('*');
                return;
            }

            String txt = e.getText();
            if (txt != null && txt.length() == 1) {
                char ch = txt.charAt(0);
                if (isAllowedMathChar(ch)) {
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

    private void addRowToGrid(int row) {
        for (int c = 0; c < len; c++) {
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
        if (colIndex >= len) {
            return;
        }

        if (ch == 'x' || ch == 'X') {
            ch = '*';
        }

        current[colIndex] = ch;

        Label t = tiles[rowIndex][colIndex];
        t.setText(String.valueOf(ch));
        t.setStyle(GameStyles.tileBase() + GameStyles.tileEmpty());

        colIndex++;
    }

    private void backspace() {
        if (uiLocked) {
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

    private boolean isAllowedMathChar(char ch) {
        return (ch >= '0' && ch <= '9')
                || ch == '+'
                || ch == '-'
                || ch == '*'
                || ch == '/'
                || ch == 'x'
                || ch == 'X';
    }

    private String styleFor(MathlerLogic.Tile tile) {
        if (tile == MathlerLogic.Tile.GREEN) {
            return GameStyles.tileGreen();
        }
        if (tile == MathlerLogic.Tile.YELLOW) {
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

        Label lost = new Label("You lost. The equation was:");
        lost.setStyle("-fx-font-size: 30px; -fx-font-weight: bold;");
        content.getChildren().add(lost);

        Label answer = new Label(game.getEquation());
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

    private KeyboardPane buildMathKeyboard() {
        String[][] rows = {
                {"1", "2", "3", "+", "-"},
                {"4", "5", "6", "*", "/"},
                {"7", "8", "9", "0"},
                {"ENTER", "⌫"}
        };

        KeyboardPane.KeySizing sizing = new KeyboardPane.KeySizing(
                42, 54, 140, 90,
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


    private int rankForMathTile(MathlerLogic.Tile t) {
        if (t == MathlerLogic.Tile.GREY) {
            return 1;
        }
        if (t == MathlerLogic.Tile.YELLOW) {
            return 2;
        }
        return 3;
    }
}
