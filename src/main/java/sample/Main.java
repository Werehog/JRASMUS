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
        downloadButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                oneDriveHandler.downloadFile("Introduction to Java Programming, Comprehensive Version, 9 edition.pdf");
                //oneDriveHandler.downloadFile("zh15_16_tavasz-info.pdf");
                //oneDriveHandler.downloadFile("Hello.txt");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Download finished!");
                alert.setHeaderText("File downloaded from OneDrive!");
                alert.showAndWait();
            }
        });

        Button listButton = new Button("List root dir");
        listButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                oneDriveHandler.listFolder();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("List finished!");
                alert.setHeaderText("Listing finished from OneDrive!");
                alert.showAndWait();
            }
        });

        Button deleteButton = new Button("Delete file");
        deleteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                oneDriveHandler.deleteFile("Introduction to Java Programming, Comprehensive Version, 9 edition.pdf");

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("File deleted!");
                alert.setHeaderText("Deleting from OneDrive finished!");
                alert.showAndWait();
            }
        });

        GoogleDriveHandler googleDriveHandler = new GoogleDriveHandler();

        Button GDLoginButton = new Button("Google Drive Login");
        GDLoginButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                googleDriveHandler.login();
            }
        });

        Button gDriveUploadButton = new Button("Upload to GoogleDrive");
        gDriveUploadButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser fileChooser = new FileChooser();
                File file = fileChooser.showOpenDialog(stage);
                googleDriveHandler.uploadSmallFile(file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Upload finished!");
                alert.setHeaderText("File uploaded to GoogleDrive!");
                alert.showAndWait();
            }
        });

        Button gDriveUploadBIGButton = new Button("UploadBIG to GoogleDrive");
        gDriveUploadBIGButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser fileChooser = new FileChooser();
                File file = fileChooser.showOpenDialog(stage);
                googleDriveHandler.uploadLargeFile(file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Upload finished!");
                alert.setHeaderText("File uploaded to GoogleDrive!");
                alert.showAndWait();
            }
        });

        Button gDriveListFilesButton = new Button("List GoogleDrive");
        gDriveListFilesButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                googleDriveHandler.listFolder();
            }
        });

        Button gDriveDownloadButton = new Button("Download from GoogleDrive");
        gDriveDownloadButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                googleDriveHandler.downloaFile("Java The Complete Reference Ninth Edition.pdf");
                //googleDriveHandler.downloaFile("Java 2 Enterprise Edition 1.4 Bible - ISBN 0764539663.pdf");
            }
        });

        Button gDriveDeleteButton = new Button("Delete from GoogleDrive");
        gDriveDeleteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //googleDriveHandler.downloaFile("Java The Complete Reference Ninth Edition.pdf");
                googleDriveHandler.deleteFile("Java 2 Enterprise Edition 1.4 Bible - ISBN 0764539663.pdf");
            }
        });

        stage.setScene(new Scene(new FlowPane(loginButton, uploadButton, downloadButton, listButton, deleteButton,
                GDLoginButton,gDriveUploadButton, gDriveUploadBIGButton, gDriveListFilesButton, gDriveDownloadButton, gDriveDeleteButton), 400, 550));
        stage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
