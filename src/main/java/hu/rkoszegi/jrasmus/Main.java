package hu.rkoszegi.jrasmus;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(final Stage stage) throws Exception{
       DatabaseManager.initDB();
        try {

            // Create a loader object and load View and Controller
            final FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("sample.fxml"));
            final VBox viewRoot = (VBox) loader.load();

            // Get controller object and initialize it
            final Controller controller = loader.getController();
            controller.initData(stage);

            // Set scene (and the title of the window) and display it
            Scene scene = new Scene(viewRoot);
            stage.setScene(scene);
            stage.setTitle("JRASMUS");
            stage.show();
        } catch (Exception e) {

            e.printStackTrace();

        }
    }

    @Override
    public void stop() {
        DatabaseManager.closeDB();
    }



    public static void main(String[] args) {
        launch(args);
    }
}
