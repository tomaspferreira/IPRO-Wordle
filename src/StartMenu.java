import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class StartMenu extends VBox {

    StartMenu(Navigator nav) {
        Label title = new Label("Clusterle");
        title.setStyle("-fx-font-size: 80px; -fx-font-weight: bold;");
        VBox.setMargin(title, new Insets(30, 0, 30, 0));

        Label langLabel = new Label("Word list language");
        langLabel.setStyle("-fx-font-size: 24px");

        ComboBox<String> langBox = new ComboBox<>();
        langBox.setStyle("-fx-font-size: 24px;");
        langBox.getItems().addAll("en", "de");
        langBox.setValue("en");
        VBox.setMargin(langBox, new Insets(0, 0, 10, 0));

        Label modeLabel = new Label("Game mode");
        modeLabel.setStyle("-fx-font-size: 24px;");

        ComboBox<String> modeBox = new ComboBox<>();
        modeBox.setStyle("-fx-font-size: 24px;");
        modeBox.getItems().addAll("Wordle", "Xordle", "Verticle", "Mathler");
        modeBox.setValue("Wordle");

        Label info = new Label("");
        info.setStyle("-fx-text-fill: red;");

        Button continueBtn = new Button("Continue");
        continueBtn.setDefaultButton(true);
        continueBtn.setPrefWidth(150);
        continueBtn.setPrefHeight(75);
        continueBtn.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        VBox.setMargin(continueBtn, new Insets(50, 0, 0, 0));

        continueBtn.setOnAction(e -> {
            String language = langBox.getValue();
            String mode = modeBox.getValue();

            if (language == null || mode == null) {
                info.setText("Please select language and game mode.");
                return;
            }

            try {
                // init hunspell once per app run (your HunspellChecker guards re-init)
                HunspellChecker.init(language);

                nav.goToSettings(language, mode);
            } catch (Exception ex) {
                info.setText("Failed to start: " + ex.getMessage());
            }
        });

        setSpacing(10);
        setPadding(new Insets(20));
        setAlignment(Pos.TOP_CENTER);

        getChildren().addAll(
                title,
                langLabel, langBox,
                modeLabel, modeBox,
                continueBtn,
                info
        );
    }
}
