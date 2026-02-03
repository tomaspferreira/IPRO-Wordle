import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class VerticleView extends BorderPane {

    VerticleView(Navigator nav, String language, int letters) {

        Language lang = new Language(language);
        VerticleLogic game = new VerticleLogic(letters, lang);

        Label title = new Label("Verticle");
        title.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 0, 10, 0));

        Label message = new Label("Type letters (A-Z), Backspace to delete, Enter to submit.");
        message.setStyle("-fx-text-fill: #444; -fx-font-size: 14px;");

        Label remaining = new Label("Columns left: " + game.getChances());
        remaining.setStyle("-fx-font-size: 16px;");

        // This holds ONLY the columns already played (so unused columns take no space)
        HBox columnsBox = new HBox(12);
        columnsBox.setAlignment(Pos.CENTER);
        columnsBox.setPadding(new Insets(20));

        // Buttons
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

        HBox bottomButtons = new HBox(10, backBtn, giveUpBtn, submitBtn);
        bottomButtons.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(10, bottomButtons, remaining, message);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(20));

        setTop(title);
        setCenter(columnsBox);
        setBottom(bottom);

        // ---------- Win/Lose screens (like Wordle/Xordle) ----------
        Label won = new Label("You win!");
        won.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");
        BorderPane.setAlignment(won, Pos.CENTER);

        Label lostTitle = new Label("You lost. The word was:");
        lostTitle.setStyle("-fx-font-size: 36px;");

        Label revealWord = new Label(game.getWord());
        revealWord.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        VBox losing = new VBox(15, lostTitle, revealWord);
        losing.setAlignment(Pos.CENTER);
        losing.setPadding(new Insets(20));
        BorderPane.setAlignment(losing, Pos.CENTER);

        Runnable endGameUI = () -> {
            submitBtn.setDisable(true);

            giveUpBtn.setVisible(false);
            giveUpBtn.setManaged(false);

            backBtn.setVisible(true);
            backBtn.setManaged(true);
        };

        backBtn.setOnAction(e -> nav.goToSettings(language, "Verticle"));

        // ---------- Typing state (current column) ----------
        final int[] row = {0};                // row inside current column [0..letters]
        final char[] current = new char[letters];
        final Label[][] currentColTiles = { new Label[letters] };

        // create an empty column and attach to columnsBox
        Runnable createNewColumn = () -> {
            VBox col = new VBox(8);
            col.setAlignment(Pos.CENTER);

            Label colHeader = new Label("Col " + (game.getTries() + 1));
            colHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            VBox tilesBox = new VBox(6);
            tilesBox.setAlignment(Pos.CENTER);

            Label[] colTiles = new Label[letters];
            for (int r = 0; r < letters; r++) {
                Label t = new Label(" ");
                t.setMinSize(50, 50);
                t.setAlignment(Pos.CENTER);
                t.setStyle("-fx-border-color: #444; -fx-font-size: 20px; -fx-font-weight: bold;");
                colTiles[r] = t;
                tilesBox.getChildren().add(t);
            }

            currentColTiles[0] = colTiles;
            col.getChildren().addAll(colHeader, tilesBox);
            columnsBox.getChildren().add(col);
        };

        // first column
        createNewColumn.run();

        // color helper
        java.util.function.Function<VerticleLogic.Tile, String> styleFor = tile -> {
            if (tile == VerticleLogic.Tile.GREEN) {
                return "-fx-background-color: #4CAF50; -fx-text-fill: white;";
            }
            if (tile == VerticleLogic.Tile.YELLOW) {
                return "-fx-background-color: #C9B458; -fx-text-fill: white;";
            }
            return "-fx-background-color: #787C7E; -fx-text-fill: white;";
        };

        // Show losing layout (center replaced)
        Runnable showLosing = () -> {
            setCenter(losing);
            message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
        };

        // Show win layout (center replaced)
        Runnable showWin = () -> {
            setCenter(won);
            message.setStyle("-fx-text-fill: green; -fx-font-size: 16px;");
            message.setText("CORRECT! You got the word!");
        };

        // Give up action
        giveUpBtn.setOnAction(e -> {
            message.setText("You gave up.");
            showLosing.run();
            endGameUI.run();
        });

        // Submit current column
        Runnable submit = () -> {
            if (game.isGameOver()) {
                message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
                message.setText("Game over.");
                return;
            }

            if (row[0] < letters) {
                message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
                message.setText("Not enough letters.");
                return;
            }

            String guessRaw = new String(current); // already uppercase chars

            if (!HunspellChecker.isValidWord(guessRaw)) {
                message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
                message.setText("Not a valid word.");
                return;
            }

            VerticleLogic.TurnResult r;
            try {
                r = game.submitGuess(guessRaw);
            } catch (IllegalArgumentException ex) {
                message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");
                message.setText(ex.getMessage());
                return;
            }

            // Color this column tiles
            for (int i = 0; i < letters; i++) {
                Label t = currentColTiles[0][i];
                String base = "-fx-border-color: #444; -fx-font-size: 20px; -fx-font-weight: bold; -fx-alignment: center;";
                t.setStyle(base + styleFor.apply(r.tiles[i]));
            }

            remaining.setText("Columns left: " + r.remainingGuesses);

            if (r.gameWon) {
                showWin.run();
                endGameUI.run();
                return;
            }

            if (r.gameOver) {
                message.setText("Out of columns.");
                showLosing.run();
                endGameUI.run();
                return;
            }

            // prepare for next column
            row[0] = 0;
            for (int i = 0; i < letters; i++) current[i] = 0;

            createNewColumn.run();

            message.setStyle("-fx-text-fill: #444; -fx-font-size: 14px;");
            message.setText("Type letters (A-Z), Backspace to delete, Enter to submit.");
        };

        submitBtn.setOnAction(e -> submit.run());

        // Keyboard capture
        setFocusTraversable(true);

        setOnKeyPressed(e -> {
            if (game.isGameOver()) return;

            KeyCode code = e.getCode();

            if (code == KeyCode.BACK_SPACE) {
                if (row[0] > 0) {
                    row[0]--;
                    current[row[0]] = 0;
                    currentColTiles[0][row[0]].setText(" ");
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
                    if (row[0] < letters) {
                        current[row[0]] = ch;
                        currentColTiles[0][row[0]].setText(String.valueOf(ch));
                        row[0]++;
                    }
                }
            }
        });

        Platform.runLater(this::requestFocus);
    }
}
