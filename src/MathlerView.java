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

    public MathlerView(Navigator nav, int numbersCount) {

        MathlerLogic game = new MathlerLogic(numbersCount);

        int eqLen = game.getEquationLength();
        int chances = game.getChances();

        // ----------------- TOP -----------------
        Label title = new Label("Mathler");
        title.setStyle("-fx-font-size: 80px; -fx-font-weight: bold;");

        Label target = new Label("Target result: " + game.getTargetResult());
        target.setStyle("-fx-font-size: 34px;");

        VBox topBox = new VBox(10, title, target);
        topBox.setAlignment(Pos.TOP_CENTER);
        topBox.setPadding(new Insets(25, 0, 10, 0));

        // ----------------- MESSAGE + REMAINING -----------------
        Label message = new Label("");
        message.setStyle("-fx-text-fill: red; -fx-font-size: 18px;");

        Label remaining = new Label("Guesses left: " + chances);
        remaining.setStyle("-fx-font-size: 20px;");

        // ----------------- GRID -----------------
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setAlignment(Pos.TOP_CENTER);

        // store all labels, but only ADD rows when needed
        Label[][] tiles = new Label[chances][eqLen];
        for (int r = 0; r < chances; r++) {
            for (int c = 0; c < eqLen; c++) {
                Label t = new Label(" ");
                t.setMinSize(70, 70);
                t.setAlignment(Pos.CENTER);
                t.setStyle("-fx-border-color: #444; -fx-font-size: 26px; -fx-font-weight: bold;");
                tiles[r][c] = t;
            }
        }

        // ✅ add only first row initially
        for (int c = 0; c < eqLen; c++) {
            grid.add(tiles[0][c], c, 0);
        }

        // put grid in a scroll (helpful if user shrinks window)
        ScrollPane gridScroll = new ScrollPane(grid);
        gridScroll.setFitToWidth(true);
        gridScroll.setFitToHeight(true);
        gridScroll.setPannable(true);
        gridScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        gridScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // ----------------- BOTTOM BUTTONS -----------------
        Button submitBtn = new Button("Submit");
        submitBtn.setPrefWidth(160);
        submitBtn.setPrefHeight(60);
        submitBtn.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button giveUpBtn = new Button("Give up");
        giveUpBtn.setPrefWidth(160);
        giveUpBtn.setPrefHeight(60);
        giveUpBtn.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button backBtn = new Button("Back");
        backBtn.setPrefWidth(160);
        backBtn.setPrefHeight(60);
        backBtn.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        backBtn.setVisible(false);
        backBtn.setManaged(false);

        HBox buttons = new HBox(15, backBtn, giveUpBtn, submitBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(12, buttons, remaining, message);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(20, 0, 25, 0));

        // ----------------- LOSE SCREEN (scrollable) -----------------
        Label lostTitle = new Label("You lost. The equation was:");
        lostTitle.setStyle("-fx-font-size: 36px; -fx-font-weight: bold;");

        Label eqLabel = new Label(game.getEquation());
        eqLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");

        VBox losingContent = new VBox(15, lostTitle, eqLabel);
        losingContent.setAlignment(Pos.CENTER);
        losingContent.setPadding(new Insets(25));

        ScrollPane losingScroll = new ScrollPane(losingContent);
        losingScroll.setFitToWidth(true);
        losingScroll.setPannable(true);
        losingScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        losingScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Label won = new Label("You win!");
        won.setStyle("-fx-font-size: 60px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        // ----------------- LAYOUT -----------------
        setTop(topBox);
        setCenter(gridScroll);
        setBottom(bottom);

        // ----------------- typing state -----------------
        final int[] row = {0};
        final int[] col = {0};
        final char[] current = new char[eqLen];

        // helper: styles
        java.util.function.Function<MathlerLogic.Tile, String> styleFor = tile -> {
            if (tile == MathlerLogic.Tile.GREEN) {
                return "-fx-background-color: #4CAF50; -fx-text-fill: white;";
            }
            if (tile == MathlerLogic.Tile.YELLOW) {
                return "-fx-background-color: #C9B458; -fx-text-fill: white;";
            }
            return "-fx-background-color: #787C7E; -fx-text-fill: white;";
        };

        Runnable endGameUI = () -> {
            submitBtn.setDisable(true);
            giveUpBtn.setVisible(false);
            giveUpBtn.setManaged(false);
            backBtn.setVisible(true);
            backBtn.setManaged(true);
        };

        backBtn.setOnAction(e -> nav.goToSettings("en", "Mathler")); // adjust if you store language elsewhere

        giveUpBtn.setOnAction(e -> {
            setCenter(losingScroll);
            message.setText("You gave up.");
            endGameUI.run();
        });

        Runnable submit = () -> {
            if (game.isGameOver()) {
                message.setText("Game over.");
                return;
            }
            if (row[0] >= chances) {
                message.setText("No guesses left.");
                return;
            }
            if (col[0] < eqLen) {
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

            int rowIndex = game.getTries() - 1;

            // paint row
            for (int c = 0; c < eqLen; c++) {
                Label t = tiles[rowIndex][c];
                t.setText(String.valueOf(r.guess.charAt(c)));

                String base = "-fx-border-color: #444; -fx-font-size: 26px; -fx-font-weight: bold; -fx-alignment: center;";
                t.setStyle(base + styleFor.apply(r.tiles[c]));
            }

            remaining.setText("Guesses left: " + r.remainingGuesses);

            if (r.gameWon) {
                setCenter(won);
                message.setStyle("-fx-text-fill: green; -fx-font-size: 18px;");
                message.setText("Correct!");
                endGameUI.run();
                return;
            }

            if (r.gameOver) {
                setCenter(losingScroll);
                message.setStyle("-fx-text-fill: red; -fx-font-size: 18px;");
                message.setText("Out of guesses.");
                endGameUI.run();
                return;
            }

            // ✅ next row (rows appear gradually)
            row[0]++;
            col[0] = 0;
            for (int i = 0; i < eqLen; i++) current[i] = 0;

            if (row[0] < chances) {
                for (int c = 0; c < eqLen; c++) {
                    grid.add(tiles[row[0]][c], c, row[0]);
                    tiles[row[0]][c].setText(" ");
                    tiles[row[0]][c].setStyle("-fx-border-color: #444; -fx-font-size: 26px; -fx-font-weight: bold;");
                }
            }

            message.setStyle("-fx-text-fill: red; -fx-font-size: 18px;");
            message.setText("");
        };

        submitBtn.setOnAction(e -> submit.run());

        // ----------------- KEY INPUT (supports SHIFT+1/3/7) -----------------
        setFocusTraversable(true);

        setOnKeyPressed(e -> {
            if (game.isGameOver()) return;
            if (row[0] >= chances) return;

            KeyCode code = e.getCode();

            if (code == KeyCode.ENTER) {
                submit.run();
                return;
            }

            if (code == KeyCode.BACK_SPACE) {
                if (col[0] > 0) {
                    col[0]--;
                    current[col[0]] = 0;
                    tiles[row[0]][col[0]].setText(" ");
                }
                return;
            }

            // ✅ special SHIFT mappings (works on layouts where +* / require Shift+digits)
            if (e.isShiftDown()) {
                if (code == KeyCode.DIGIT1) { typeChar('+', tiles, row, col, current, eqLen); return; }
                if (code == KeyCode.DIGIT3) { typeChar('*', tiles, row, col, current, eqLen); return; }
                if (code == KeyCode.DIGIT7) { typeChar('/', tiles, row, col, current, eqLen); return; }
            }

            // Also allow direct operator keys if they exist on keyboard
            if (code == KeyCode.PLUS || code == KeyCode.ADD) { typeChar('+', tiles, row, col, current, eqLen); return; }
            if (code == KeyCode.SLASH || code == KeyCode.DIVIDE) { typeChar('/', tiles, row, col, current, eqLen); return; }
            if (code == KeyCode.ASTERISK || code == KeyCode.MULTIPLY) { typeChar('*', tiles, row, col, current, eqLen); return; }
            if (code == KeyCode.MINUS || code == KeyCode.SUBTRACT) { typeChar('-', tiles, row, col, current, eqLen); return; }

            // numbers (top row or numpad)
            String txt = e.getText();
            if (txt != null && txt.length() == 1) {
                char ch = txt.charAt(0);

                // accept digits and basic operators if OS provides them directly
                if ((ch >= '0' && ch <= '9') || ch == '+' || ch == '-' || ch == '*' || ch == '/') {
                    typeChar(ch, tiles, row, col, current, eqLen);
                }
            }
        });

        Platform.runLater(this::requestFocus);
    }

    private static void typeChar(char ch,
                                 Label[][] tiles,
                                 int[] row,
                                 int[] col,
                                 char[] current,
                                 int eqLen) {

        if (col[0] >= eqLen) return;

        current[col[0]] = ch;
        tiles[row[0]][col[0]].setText(String.valueOf(ch));
        col[0]++;
    }
}
