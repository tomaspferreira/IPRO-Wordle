import javafx.scene.Scene;
import javafx.stage.Stage;

public class Navigator {
    private final Stage stage;

    // fixed window size for now (you can remove later)
    private static final int W = 1000;
    private static final int H = 800;

    public Navigator(Stage stage) {
        this.stage = stage;
    }

    public void goToStartMenu() {
        stage.setScene(new Scene(new StartMenu(this), W, H));
    }

    public void goToSettings(String language, String mode) {
        stage.setScene(new Scene(new Settings(this, language, mode), W, H));
    }

    public void goToWordle(String language, int letters, int wordsCount) {
        stage.setScene(new Scene(new WordleView(this, language, letters, wordsCount), W, H));
    }

    public void goToXordle(String language, int letters) {
        stage.setScene(new Scene(new XordleView(this, language, letters), W, H));
    }

    public void goToVerticle(String language, int letters) {
        stage.setScene(new Scene(new VerticleView(this, language, letters), W, H));
    }
}
