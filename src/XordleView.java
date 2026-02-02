import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class XordleView extends BorderPane {

    XordleView(Navigator nav, String language, int letters) {

        Language lang = new Language(language);
        XordleLogic game = new XordleLogic(letters, lang);

        int chances = game.getChances();

        Label title = new Label("Xordle");
        title.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 0, 10, 0));

        Label message = new Label("");
        message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");

        Label remaining = new Label("Guesses left: " + chances);
        remaining.setStyle("-fx-font-size: 16px;");

        // --- BOARD: VBox of row HBoxes (one row visible at a time) ---
        VBox board = new VBox(8);
        board.setAlignment(Pos.CENTER);

        HBox[] rowBoxes = new HBox[chances];
        Label[][] tiles = new Label[chances][letters];

        for (int r = 0; r < chances; r++) {
            HBox rowBox = new HBox(8);
            rowBox.setAlignment(Pos.CENTER);

            for (int c = 0; c < letters; c++) {
                Label t = new Label(" ");
                t.setMinSize(50, 50);
                t.setAlignment(Pos.CENTER);
                t.setStyle("-fx-border-color: #444; -fx-font-size: 20px; -fx-font-weight: bold;");
                tiles[r][c] = t;
                rowBox.getChildren().add(t);
            }

            // show only first row at start
            if (r > 0) {
                rowBox.setVisible(false);
                rowBox.setManaged(false);
            }

            rowBoxes[r] = rowBox;
            board.getChildren().add(rowBox);
        }

        // typing state
        final int[] row = {0};
        final int[] col = {0};
        final char[] current = new char[letters];

        // --- Buttons ---
        Button submitBtn = new Button("Submit");
        submitBtn.setPrefWidth(120);
        submitBtn.setPrefHeight(45);
        submitBtn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button giveUpBtn = new Button("Give up");
        giveUpBtn.setPrefWidth(120);
        giveUpBtn.setPrefHeight(45);
        giveUpBtn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button backBtn = new Button("Back");
        backBtn.setPrefWidth(120);
        backBtn.setPrefHeight(45);
        backBtn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        backBtn.setVisible(false);
        backBtn.setManaged(false);

        // Losing screen
        Label lost = new Label("You lost. The words were:");
        lost.setStyle("-fx-font-size: 36px;");

        Label wordOne = new Label(game.getWords()[0]);
        wordOne.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label wordTwo = new Label(game.getWords()[1]);
        wordTwo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        VBox losing = new VBox(15, lost, wordOne, wordTwo);
        losing.setAlignment(Pos.CENTER);
        losing.setPadding(new Insets(20));

        // Win screen
        Label won = new Label("You win!");
        won.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");

        HBox bottomButtons = new HBox(10, backBtn, giveUpBtn, submitBtn);
        bottomButtons.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(10, bottomButtons, remaining, message);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(20));

        setTop(title);
        setCenter(board);
        setBottom(bottom);

        backBtn.setOnAction(e -> nav.goToSettings(language, "Xordle"));

        java.util.function.Function<XordleLogic.Tile, String> styleFor = tile -> {
            if (tile == XordleLogic.Tile.GREEN) return "-fx-background-color: #4CAF50; -fx-text-fill: white;";
            if (tile == XordleLogic.Tile.YELLOW) return "-fx-background-color: #C9B458; -fx-text-fill: white;";
            if (tile == XordleLogic.Tile.BLUE) return "-fx-background-color: #3B82F6; -fx-text-fill: white;";
            return "-fx-background-color: #787C7E; -fx-text-fill: white;";
        };

        Runnable endGameUI = () -> {
            submitBtn.setDisable(true);
            giveUpBtn.setVisible(false);
            giveUpBtn.setManaged(false);
            backBtn.setVisible(true);
            backBtn.setManaged(true);
        };

        giveUpBtn.setOnAction(e -> {
            setCenter(losing);
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
            if (col[0] < letters) {
                message.setText("Not enough letters.");
                return;
            }

            String guessRaw = new String(current);

            if (!HunspellChecker.isValidWord(guessRaw)) {
                message.setText("Not a valid word.");
                return;
            }

            XordleLogic.TurnResult r;
            try {
                r = game.submitGuess(guessRaw);
            } catch (IllegalArgumentException ex) {
                message.setText(ex.getMessage());
                return;
            }

            int rowIndex = game.getTries() - 1;

            for (int c = 0; c < letters; c++) {
                Label t = tiles[rowIndex][c];
                t.setText(String.valueOf(r.guess.charAt(c)));

                String base = "-fx-border-color: #444; -fx-font-size: 20px; -fx-font-weight: bold; -fx-alignment: center;";
                t.setStyle(base + styleFor.apply(r.tiles[c]));
            }

            remaining.setText("Guesses left: " + r.remainingGuesses);

            if (r.gameWon) {
                setCenter(won);
                message.setStyle("-fx-text-fill: green; -fx-font-size: 16px;");
                message.setText("You solved both words!");
                endGameUI.run();
                return;
            }

            if (r.gameOver) {
                setCenter(losing);
                message.setText("Out of guesses.");
                endGameUI.run();
                return;
            }

            // Reveal next row
            int nextRow = rowIndex + 1;
            if (nextRow < chances) {
                rowBoxes[nextRow].setManaged(true);
                rowBoxes[nextRow].setVisible(true);
            }

            // reset typing buffer
            row[0]++;
            col[0] = 0;
            for (int i = 0; i < letters; i++) current[i] = 0;
            message.setText("");
        };

        submitBtn.setOnAction(e -> submit.run());

        // Keyboard typing directly into current visible row
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (game.isGameOver()) return;
            if (row[0] >= chances) return;

            KeyCode code = e.getCode();

            if (code == KeyCode.BACK_SPACE) {
                if (col[0] > 0) {
                    col[0]--;
                    current[col[0]] = 0;
                    tiles[row[0]][col[0]].setText(" ");
                }
                return;
            }

            if (code == KeyCode.ENTER) {
                submit.run();
                return;
            }

            String txt = e.getText();
            if (txt != null && txt.length() == 1) {
                char ch = Character.toUpperCase(txt.charAt(0));
                if (ch >= 'A' && ch <= 'Z') {
                    if (col[0] < letters) {
                        current[col[0]] = ch;
                        tiles[row[0]][col[0]].setText(String.valueOf(ch));
                        col[0]++;
                    }
                }
            }
        });

        Platform.runLater(this::requestFocus);
    }
}
