import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.input.KeyCode;

public class StartMenu extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("Clusterle");
        title.setStyle("-fx-font-size: 80px; -fx-font-weight: bold;");
        VBox.setMargin(title, new Insets(30, 0, 30, 0));

        Label langLabel = new Label("Word list language");
        langLabel.setStyle("-fx-font-size: 24px");

        ComboBox<String> langBox = new ComboBox<>();
        VBox.setMargin(langBox, new Insets(0, 0, 10, 0));
        langBox.setStyle("-fx-font-size: 24px;");
        langBox.getItems().addAll("en", "de");
        langBox.setValue("en");

        Label modeLabel = new Label("Game mode");
        modeLabel.setStyle("-fx-font-size: 24px;");

        ComboBox<String> modeBox = new ComboBox<>();
        modeBox.setStyle("-fx-font-size: 24px;");
        modeBox.getItems().addAll("Wordle", "Xordle", "Verticle", "Mathler");
        modeBox.setValue("Wordle");

        Label info = new Label("");
        info.setStyle("-fx-text-fill: red;");

        Button startBtn = new Button("Continue");
        startBtn.setDefaultButton(true);
        startBtn.setPrefWidth(150);
        startBtn.setPrefHeight(75);
        startBtn.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        VBox.setMargin(startBtn, new Insets(50, 0, 0, 0));

        // Keep the start menu scene so we can come back to it
        VBox root = new VBox(10,
                title,
                langLabel, langBox,
                modeLabel, modeBox,
                startBtn,
                info
        );
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        Scene startMenuScene = new Scene(root, 1000, 800);

        startBtn.setOnAction(e -> {
            String language = langBox.getValue();
            String mode = modeBox.getValue();

            if (language == null || mode == null) {
                info.setText("Please select language and game mode.");
                return;
            }

            try {
                HunspellChecker.init(language);

                if (mode.equals("Wordle") || mode.equals("Xordle") || mode.equals("Verticle") || mode.equals("Mathler")) {
                    stage.setScene(SettingsScene(stage, startMenuScene, language, mode));
                    return;
                }

                // For now: other modes still use console
                Language lang = new Language(language);
                new Gamemode(mode, lang);

            } catch (Exception ex) {
                info.setText("Failed to start: " + ex.getMessage());
            }
        });

        stage.setTitle("Clusterle");
        stage.setScene(startMenuScene);
        stage.show();
    }

    private Scene SettingsScene(Stage stage, Scene startMenuScene, String language, String mode) {
        Label title = new Label(mode.substring(0, 1).toUpperCase() + mode.substring(1));
        title.setStyle("-fx-font-size: 60px; -fx-font-weight: bold;");
        VBox.setMargin(title, new Insets(30, 0, 30, 0));

        Label letters = new Label("How many letters should the word have?");
        letters.setStyle("-fx-font-size: 24px;");

        ComboBox<Integer> lettersBox = new ComboBox<>();
        lettersBox.setStyle("-fx-font-size: 24px;");
        lettersBox.getItems().addAll(4, 5, 6, 7);
        lettersBox.setValue(5);

        Label wordsLabel = new Label("How many words do you want to guess?");
        wordsLabel.setStyle("-fx-font-size: 24px;");

        ComboBox<Integer> wordsBox = new ComboBox<>();
        wordsBox.setStyle("-fx-font-size: 24px;");
        wordsBox.getItems().addAll(1, 2, 4, 8, 16, 32);
        wordsBox.setValue(1);

        boolean isWordle = mode.equals("Wordle");
        wordsLabel.setVisible(isWordle);
        wordsLabel.setManaged(isWordle);
        wordsBox.setVisible(isWordle);
        wordsBox.setManaged(isWordle);

        Label numbersLabel = new Label("How many numbers do you want to the equation to have?");
        numbersLabel.setStyle("-fx-font-size: 24px;");

        ComboBox<Integer> numbersBox = new ComboBox<>();
        numbersBox.setStyle("-fx-font-size: 24px;");
        numbersBox.getItems().addAll(2, 3, 4);
        numbersBox.setValue(2);

        boolean isMathler = mode.equals("Mathler");
        numbersLabel.setVisible(isMathler);
        numbersLabel.setManaged(isMathler);
        numbersBox.setVisible(isMathler);
        numbersBox.setManaged(isMathler);

        letters.setVisible(!isMathler);
        letters.setManaged(!isMathler);
        lettersBox.setVisible(!isMathler);
        lettersBox.setManaged(!isMathler);

        Button playBtn = new Button("Play");
        playBtn.setPrefWidth(150);
        playBtn.setPrefHeight(60);
        playBtn.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button backBtn = new Button("Back");
        backBtn.setPrefWidth(150);
        backBtn.setPrefHeight(60);
        backBtn.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        backBtn.setOnAction(e -> stage.setScene(startMenuScene));

        HBox buttons = new HBox(20, backBtn, playBtn);
        buttons.setAlignment(Pos.CENTER);
        VBox.setMargin(buttons, new Insets(50, 0, 0, 0));

        VBox root = new VBox(10, title, letters, lettersBox, wordsLabel, wordsBox, numbersLabel, numbersBox, buttons);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20));

        Scene settingsScene = new Scene(root, 1000, 800);
        playBtn.setOnAction(e -> {
            int lettersChosen = lettersBox.getValue();
            Language lang = new Language(language);

            if (mode.equals("Wordle")) {
                int wordsChosen = wordsBox.getValue();
                new Wordle(wordsChosen, lettersChosen, lang);

            } else if (mode.equals("Xordle")) {
                stage.setScene(createXordleScene(stage, settingsScene, language, lettersChosen));

            } else if (mode.equals("Verticle")) {
                new Verticle(lettersChosen, lang);

            } else if (mode.equals("Mathler")) {
                int numbers = numbersBox.getValue();
                new Mathler(numbers);
            }
        });

        return settingsScene;
    }

    private Scene createXordleScene(Stage stage, Scene backScene, String language, int letters) {

        int chances = letters + 4;

        Label title = new Label("Xordle");
        title.setStyle("-fx-font-size: 60px; -fx-font-weight: bold;");
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 0, 10, 0));

        Label message = new Label("");
        message.setStyle("-fx-text-fill: red; -fx-font-size: 16px;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);

        Label[][] tiles = new Label[chances][letters];

        for (int r = 0; r < chances; r++) {
            for (int c = 0; c < letters; c++) {
                Label t = new Label(" ");
                t.setMinSize(50, 50);
                t.setAlignment(Pos.CENTER);
                t.setStyle("-fx-border-color: #444; -fx-font-size: 20px; -fx-font-weight: bold;");
                tiles[r][c] = t;
                grid.add(t, c, r);
            }
        }

        TextField input = new TextField();
        input.setPromptText("Type guess here (" + letters + " letters)");
        input.setStyle("-fx-font-size: 18px;");
        input.setMaxWidth(300);

        Button submitBtn = new Button("Submit");
        submitBtn.setPrefWidth(120);
        submitBtn.setPrefHeight(45);
        submitBtn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button backBtn = new Button("Back");
        backBtn.setPrefWidth(120);
        backBtn.setPrefHeight(45);
        backBtn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        HBox bottomButtons = new HBox(15, backBtn, submitBtn);
        bottomButtons.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(10, input, bottomButtons, message);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(20));

        BorderPane root = new BorderPane();
        root.setTop(title);
        root.setCenter(grid);
        root.setBottom(bottom);

        // --- STATE: current row ---
        final int[] row = {0};

        Runnable submit = () -> {
            String guessRaw = input.getText().trim();

            if (guessRaw.length() != letters) {
                message.setText("Guess must be " + letters + " letters.");
                return;
            }

            if (!HunspellChecker.isValidWord(guessRaw)) {
                message.setText("Not a valid word.");
                return;
            }

            if (row[0] >= chances) {
                message.setText("No guesses left.");
                return;
            }

            String guess = guessRaw.toUpperCase();

            // Fill the current row with letters (placeholder coloring: grey)
            for (int c = 0; c < letters; c++) {
                char character = guess.charAt(c);
                Label t = tiles[row[0]][c];
                t.setText(String.valueOf(character));

                // Placeholder: all grey for now (we will replace with Xordle colors later)
                t.setStyle("-fx-border-color: #444; -fx-font-size: 20px; -fx-font-weight: bold;"
                        + "-fx-background-color: #777; -fx-text-fill: white;");
            }

            row[0]++;
            input.clear();
            message.setText("");
        };

        submitBtn.setOnAction(e -> submit.run());

        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                submit.run();
            }
        });

        backBtn.setOnAction(e -> stage.setScene(backScene));

        return new Scene(root, 1000, 800);
    }

}
