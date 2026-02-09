import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.Function;

public class KeyboardPane extends VBox {

    /** Special key label for the ENTER key. */
    public static final String KEY_ENTER = "ENTER";

    /** Special key label for the backspace key. */
    public static final String KEY_BACKSPACE = "⌫";

    public record KeySizing(
            double height,
            double letterWidth,
            double enterWidth,
            double backspaceWidth,
            double hGap,
            double vGap,
            Insets padding
    ) { }

    /**
     * Bundles all key callbacks into one object (reduces constructor parameters).
     *
     * @param onEnter       callback for ENTER
     * @param onBackspace   callback for ⌫
     * @param onChar        callback for single-character keys
     * @param afterAnyKey   optional callback after any key press (can be null)
     */
    public record Handlers(
            Runnable onEnter,
            Runnable onBackspace,
            Consumer<Character> onChar,
            Runnable afterAnyKey
    ) { }

    /** Key handlers. */
    private final Handlers handlers;

    public KeyboardPane(
            String[][] rows,
            KeySizing sizing,
            Function<String, String> keyStyler,
            KeyboardColorManager colors,
            Handlers handlersValue
    ) {
        this.handlers = handlersValue;

        setSpacing(sizing.vGap());
        setAlignment(Pos.CENTER);
        setPadding(sizing.padding());

        for (String[] rowKeys : rows) {
            getChildren().add(buildRow(rowKeys, sizing, keyStyler, colors));
        }
    }

    private HBox buildRow(
            String[] keys,
            KeySizing sizing,
            Function<String, String> keyStyler,
            KeyboardColorManager colors
    ) {
        HBox row = new HBox(sizing.hGap());
        row.setAlignment(Pos.CENTER);

        for (String k : keys) {
            Button b = new Button(k);
            b.setPrefHeight(sizing.height());

            if (KEY_ENTER.equals(k)) {
                b.setPrefWidth(sizing.enterWidth());
            } else if (KEY_BACKSPACE.equals(k)) {
                b.setPrefWidth(sizing.backspaceWidth());
            } else {
                b.setPrefWidth(sizing.letterWidth());
            }

            b.setStyle(keyStyler.apply(k));
            b.setFocusTraversable(false);

            if (colors != null && k.length() == 1) {
                colors.registerKey(k.charAt(0), b);
            }

            b.setOnAction(_ -> {
                handleKey(k);
                if (handlers != null && handlers.afterAnyKey() != null) {
                    handlers.afterAnyKey().run();
                }
            });

            row.getChildren().add(b);
        }

        return row;
    }

    private void handleKey(String k) {
        if (handlers == null) {
            return;
        }

        if (KEY_ENTER.equals(k)) {
            if (handlers.onEnter() != null) {
                handlers.onEnter().run();
            }
            return;
        }

        if (KEY_BACKSPACE.equals(k)) {
            if (handlers.onBackspace() != null) {
                handlers.onBackspace().run();
            }
            return;
        }

        if (k.length() == 1) {
            if (handlers.onChar() != null) {
                handlers.onChar().accept(k.charAt(0));
            }
        }
    }
}
