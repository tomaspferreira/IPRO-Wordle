import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class StartMenu extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("Wordle");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label langLabel = new Label("Language");
        ComboBox<String> langBox = new ComboBox<>();
        langBox.getItems().addAll("en", "de");
        langBox.setValue("en");

        Label modeLabel = new Label("Game mode");
        ComboBox<String> modeBox = new ComboBox<>();
        modeBox.getItems().addAll("wordle", "xordle", "verticle", "mathler");
        modeBox.setValue("wordle");

        Label info = new Label("");
        info.setStyle("-fx-text-fill: red;");

        Button startBtn = new Button("Start");
        startBtn.setDefaultButton(true);

        startBtn.setOnAction(e -> {
            String language = langBox.getValue();
            String mode = modeBox.getValue();

            if (language == null || mode == null) {
                info.setText("Please select language and game mode.");
                return;
            }

            try {
                // init hunspell once per run (safe if called multiple times)
                HunspellChecker.init(language);

                // create language object
                Language lang = new Language(language);

                // start the game
                new Gamemode(mode, lang);

            } catch (Exception ex) {
                info.setText("Failed to start: " + ex.getMessage());
            }
        });

        VBox root = new VBox(10,
                title,
                langLabel, langBox,
                modeLabel, modeBox,
                startBtn,
                info
        );
        root.setPadding(new Insets(20));

        stage.setTitle("Wotdle - Start Menu");
        stage.setScene(new Scene(root, 320, 260));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
