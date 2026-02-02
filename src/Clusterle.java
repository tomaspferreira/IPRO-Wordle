import javafx.application.Application;
import javafx.stage.Stage;

public class Clusterle extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Clusterle");

        Navigator nav = new Navigator(stage);
        nav.goToStartMenu();

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
