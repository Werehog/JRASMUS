package sample;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;

import java.io.File;


public class Main extends Application {

    private OneDriveHandler oneDriveHandler;

    @Override
    public void start(final Stage stage) throws Exception{
       /* Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));*/



        /*WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();
        webEngine.load(url);
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(browser, 400, 550));
        primaryStage.show();*/

        oneDriveHandler = new OneDriveHandler();

        Button loginButton = new Button("Login to OneDrive");
        loginButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                oneDriveHandler.login();
            }
        });

        Button uploadButton = new Button("Upload to OneDrive");
        uploadButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser fileChooser = new FileChooser();
                File file = fileChooser.showOpenDialog(stage);
                oneDriveHandler.uploadFile(file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Upload finished!");
                alert.setHeaderText("File uploaded to OneDrive!");
                alert.showAndWait();
            }
        });

        Button downloadButton = new Button("Download from OneDrive");
        uploadButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                oneDriveHandler.downloadFile("InfoSecIntro2014.pdf");
                //oneDriveHandler.downloadFile("zh15_16_tavasz-info.pdf");
                //oneDriveHandler.downloadFile("Hello.txt");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Download finished!");
                alert.setHeaderText("File downloaded from OneDrive!");
                alert.showAndWait();
            }
        });

        stage.setScene(new Scene(new FlowPane(loginButton, uploadButton, downloadButton), 400, 550));
        stage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
