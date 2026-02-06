import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class OnScreenKeyboard extends VBox {

    public enum Mode { WORD, MATH }

    public OnScreenKeyboard(KeyReceiver receiver, Mode mode) {
        setSpacing(10);
        setPadding(new Insets(10));
        setAlignment(Pos.CENTER);

        if (mode == Mode.WORD) {
            getChildren().addAll(
                    row(receiver, "QWERTZUIOP"),   // Swiss/German layout feels natural
                    row(receiver, "ASDFGHJKL"),
                    bottomRowWord(receiver)
            );
        } else {
            getChildren().addAll(
                    row(receiver, "1234567890"),
                    row(receiver, "+-*/"),
                    bottomRowMath(receiver)
            );
        }
    }

    private HBox row(KeyReceiver receiver, String chars) {
        HBox r = new HBox(8);
        r.setAlignment(Pos.CENTER);

        for (int i = 0; i < chars.length(); i++) {
            char ch = chars.charAt(i);
            Button b = keyButton(String.valueOf(ch));
            b.setOnAction(e -> receiver.onChar(ch));
            r.getChildren().add(b);
        }
        return r;
    }

    private HBox bottomRowWord(KeyReceiver receiver) {
        HBox r = new HBox(8);
        r.setAlignment(Pos.CENTER);

        Button enter = keyButton("ENTER");
        enter.setPrefWidth(130);
        enter.setOnAction(e -> receiver.onEnter());

        Button back = keyButton("⌫");
        back.setPrefWidth(90);
        back.setOnAction(e -> receiver.onBackspace());

        // add a few more letters (Z row)
        HBox letters = new HBox(8);
        letters.setAlignment(Pos.CENTER);
        for (char ch : "YXCVBNM".toCharArray()) {
            Button b = keyButton(String.valueOf(ch));
            b.setOnAction(e -> receiver.onChar(ch));
            letters.getChildren().add(b);
        }

        r.getChildren().addAll(enter, letters, back);
        return r;
    }

    private HBox bottomRowMath(KeyReceiver receiver) {
        HBox r = new HBox(8);
        r.setAlignment(Pos.CENTER);

        Button enter = keyButton("ENTER");
        enter.setPrefWidth(160);
        enter.setOnAction(e -> receiver.onEnter());

        Button back = keyButton("⌫");
        back.setPrefWidth(100);
        back.setOnAction(e -> receiver.onBackspace());

        r.getChildren().addAll(enter, back);
        return r;
    }

    private Button keyButton(String text) {
        Button b = new Button(text);
        b.setPrefWidth(60);
        b.setPrefHeight(55);
        b.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        return b;
    }
}
