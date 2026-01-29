import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.*;

public class JavaFX_Practice extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("Practice app");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label name = new Label("Write your name:");
        TextField nameBox = new TextField();
        nameBox.setPromptText("");

        Button startBtn = new Button("Enter");
        startBtn.setDefaultButton(true);
        Label text = new Label("");

        startBtn.setOnAction(e -> {

            if (nameBox.getText().isEmpty()) {
                text.setText("Please enter a name.");
            } else {
                text.setText("Welcome " + nameBox.getText());
            }

        });
        VBox root = new VBox(10,
                title,
                name,
                nameBox,
                startBtn,
                text);

        root.setPadding(new Insets(20));
        stage.setTitle("I don't know");
        stage.setScene(new Scene(root, 320, 260));
        stage.show();
    }
}
