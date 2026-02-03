import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class Settings extends VBox {

    Settings(Navigator nav, String language, String mode) {

        Label title = new Label(mode);
        title.setStyle("-fx-font-size: 60px; -fx-font-weight: bold;");
        VBox.setMargin(title, new Insets(30, 0, 30, 0));

        Label lettersLabel = new Label("How many letters should the word have?");
        lettersLabel.setStyle("-fx-font-size: 24px;");

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

        Label numbersLabel = new Label("How many numbers do you want the equation to have?");
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

        lettersLabel.setVisible(!isMathler);
        lettersLabel.setManaged(!isMathler);
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

        backBtn.setOnAction(e -> nav.goToStartMenu());

        HBox buttons = new HBox(20, backBtn, playBtn);
        buttons.setAlignment(Pos.CENTER);
        VBox.setMargin(buttons, new Insets(50, 0, 0, 0));

        playBtn.setOnAction(e -> {
            int lettersChosen = lettersBox.getValue();
            Language lang = new Language(language);

            if (mode.equals("Wordle")) {
                int wordsChosen = wordsBox.getValue();
                nav.goToWordle(language, lettersChosen, wordsChosen);
            } else if (mode.equals("Xordle")) {
                nav.goToXordle(language, lettersChosen);
            } else if (mode.equals("Verticle")) {
                nav.goToVerticle(language, lettersChosen);
            } else if (mode.equals("Mathler")) {
                int n = numbersBox.getValue();
                new Mathler(n); // console for now
            }
        });

        setSpacing(10);
        setPadding(new Insets(20));
        setAlignment(Pos.TOP_CENTER);

        getChildren().addAll(
                title,
                lettersLabel, lettersBox,
                wordsLabel, wordsBox,
                numbersLabel, numbersBox,
                buttons
        );
    }
}
