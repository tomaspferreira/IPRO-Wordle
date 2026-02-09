import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Simple navigator that swaps scenes to move between screens.
 */
public class Navigator {

    /**
     * Primary JavaFX stage used to display scenes.
     */
    private final Stage stage;

    /**
     * Default scene width.
     */
    private static final int W = 1000;

    /**
     * Default scene height.
     */
    private static final int H = 800;

    /**
     * Default background color in hex.
     */
    private static final String BG_HEX = "#1E1F22";

    public Navigator(Stage primaryStage) {
        this.stage = primaryStage;
    }

    private void setScene(Parent root) {
        Scene scene = new Scene(root, W, H);
        scene.setFill(Color.web(BG_HEX));

        URL css = Navigator.class.getResource("/theme.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            root.setStyle("-fx-background-color: " + BG_HEX + ";");
        }

        stage.setScene(scene);
    }

    public void goToStartMenu() {
        setScene(new StartMenu(this));
    }

    public void goToSettings(String language, String mode) {
        setScene(new Settings(this, language, mode));
    }

    public void goToWordle(String language, int letters, int wordsCount) {
        setScene(new WordleView(this, language, letters, wordsCount));
    }

    public void goToXordle(String language, int letters) {
        setScene(new XordleView(this, language, letters));
    }

    public void goToVerticle(String language, int letters) {
        setScene(new VerticleView(this, language, letters));
    }

    public void goToMathler(int numbers) {
        setScene(new MathlerView(this, numbers));
    }
}
