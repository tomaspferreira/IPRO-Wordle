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
import javafx.scene.layout.VBox;

public class WordleView extends BorderPane {

    WordleView(Navigator nav, String language, int letters, int wordsCount) {

        Language lang = new Language(language);
        WordleLogic game = new WordleLogic(wordsCount, letters, lang);

        int chances = game.getChances();

        Label title = new Label("Wordle");
        title.setStyle("-fx-font-size: 48px; -fx-font-weight: bold;");
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 0, 10, 0));

        Label message = new Label("");
        message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");

        Label remaining = new Label("Guesses left: " + chances);
        remaining.setStyle("-fx-font-size: 16px;");

        // Wrap boards
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

        // tiles[w][r][c] and rowBoxes[w][r]
        Label[][][] tiles = new Label[wordsCount][chances][letters];
        HBox[][] rowBoxes = new HBox[wordsCount][chances];
        Label[] solvedLabels = new Label[wordsCount];

        // Track frozen boards
        boolean[] solvedBefore = new boolean[wordsCount];

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
                    Label t = new Label(" ");
                    t.setMinSize(45, 45);
                    t.setAlignment(Pos.CENTER);
                    t.setStyle("-fx-border-color: #444; -fx-font-size: 18px; -fx-font-weight: bold;");
                    tiles[w][r][c] = t;
                    rowBox.getChildren().add(t);
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

        // Typing state (shared guess across all boards)
        final int[] row = {0};
        final int[] col = {0};
        final char[] current = new char[letters];

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

        // Losing screen (scrollable word list so buttons remain visible)
        Label lost = new Label("You lost. The words were:");
        lost.setStyle("-fx-font-size: 36px;");

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

        setTop(title);
        setCenter(boardsScroll);
        setBottom(bottom);

        backBtn.setOnAction(e -> nav.goToSettings(language, "Wordle"));

        java.util.function.Function<WordleLogic.Tile, String> styleFor = tile -> {
            if (tile == WordleLogic.Tile.GREEN) return "-fx-background-color: #4CAF50; -fx-text-fill: white;";
            if (tile == WordleLogic.Tile.YELLOW) return "-fx-background-color: #C9B458; -fx-text-fill: white;";
            return "-fx-background-color: #787C7E; -fx-text-fill: white;";
        };

        Runnable endGameUI = () -> {
            submitBtn.setDisable(true);
            giveUpBtn.setVisible(false);
            giveUpBtn.setManaged(false);
            backBtn.setVisible(true);
            backBtn.setManaged(true);
        };

        Runnable showLosing = () -> {
            losingContent.getChildren().clear();
            losingContent.getChildren().add(lost);

            String[] ws = game.getWords();
            for (int i = 0; i < ws.length; i++) {
                Label wLabel = new Label(ws[i]);
                wLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
                losingContent.getChildren().add(wLabel);
            }

            setCenter(losingScroll);
        };

        giveUpBtn.setOnAction(e -> {
            showLosing.run();
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

            WordleLogic.TurnResult r;
            try {
                r = game.submitGuess(guessRaw);
            } catch (IllegalArgumentException ex) {
                message.setText(ex.getMessage());
                return;
            }

            int rowIndex = game.getTries() - 1;

            // Paint boards
            for (int w = 0; w < wordsCount; w++) {

                // if board was solved earlier, never touch it again
                if (solvedBefore[w]) {
                    continue;
                }

                if (r.solved[w]) {
                    solvedLabels[w].setText("SOLVED!");
                    solvedBefore[w] = true;
                }

                for (int c = 0; c < letters; c++) {
                    Label t = tiles[w][rowIndex][c];
                    t.setText(String.valueOf(r.guess.charAt(c)));

                    String base =
                            "-fx-border-color: #444;" +
                                    "-fx-font-size: 18px;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-alignment: center;";

                    if (r.solved[w]) {
                        t.setStyle(base + "-fx-background-color: #4CAF50; -fx-text-fill: white;");
                    } else {
                        t.setStyle(base + styleFor.apply(r.tilesByWord[w][c]));
                    }
                }
            }

            remaining.setText("Guesses left: " + r.remainingGuesses);

            if (r.gameWon) {
                setCenter(won);
                message.setStyle("-fx-text-fill: green; -fx-font-size: 16px;");
                message.setText("You solved all words!");
                endGameUI.run();
                return;
            }

            if (r.gameOver) {
                showLosing.run();
                message.setText("Out of guesses.");
                endGameUI.run();
                return;
            }

            // Reveal next row only for unsolved boards
            int nextRow = rowIndex + 1;
            if (nextRow < chances) {
                for (int w = 0; w < wordsCount; w++) {
                    if (!solvedBefore[w]) {
                        rowBoxes[w][nextRow].setManaged(true);
                        rowBoxes[w][nextRow].setVisible(true);
                    }
                }
            }

            // Next row typing reset
            row[0]++;
            col[0] = 0;
            for (int i = 0; i < letters; i++) current[i] = 0;
            message.setText("");
        };

        submitBtn.setOnAction(e -> submit.run());

        // Keyboard typing directly into row tiles
        setFocusTraversable(true);

        setOnKeyPressed(e -> {
            if (game.isGameOver()) return;
            if (row[0] >= chances) return;

            KeyCode code = e.getCode();

            if (code == KeyCode.BACK_SPACE) {
                if (col[0] > 0) {
                    col[0]--;
                    current[col[0]] = 0;

                    // Clear current tile on all boards that are not solved
                    for (int w = 0; w < wordsCount; w++) {
                        if (!solvedBefore[w]) {
                            tiles[w][row[0]][col[0]].setText(" ");
                        }
                    }
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

                        // Write into ALL unsolved boards for the current row/col
                        for (int w = 0; w < wordsCount; w++) {
                            if (!solvedBefore[w]) {
                                tiles[w][row[0]][col[0]].setText(String.valueOf(ch));
                            }
                        }

                        col[0]++;
                    }
                }
            }
        });

        Platform.runLater(this::requestFocus);
    }
}
