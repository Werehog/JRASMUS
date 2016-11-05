package hu.rkoszegi.jrasmus;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;


public class Main extends Application {

    private OneDriveHandler oneDriveHandler;

    @Override
    public void start(final Stage stage) throws Exception{
       /* Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));*/

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
                TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Download File Name");
                textInputDialog.setContentText("Please enter the file name:");

                Optional<String> result = textInputDialog.showAndWait();
                if(result.isPresent()) {
                    oneDriveHandler.downloadFile(result.get());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Download finished!");
                    alert.setHeaderText("File downloaded from OneDrive!");
                    alert.showAndWait();
                }
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
                TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Delete file from OneDrive");
                textInputDialog.setContentText("Please enter the file name:");

                Optional<String> result = textInputDialog.showAndWait();
                if(result.isPresent()) {
                    oneDriveHandler.deleteFile(result.get());

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("File deleted!");
                    alert.setHeaderText("Deleting from OneDrive finished!");
                    alert.showAndWait();
                }
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
                TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Download File Name");
                textInputDialog.setContentText("Please enter the file name:");

                Optional<String> result = textInputDialog.showAndWait();
                if(result.isPresent()) {
                    googleDriveHandler.downloaFile(result.get());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Download finished!");
                    alert.setHeaderText("File downloaded from GoogleDrive!");
                    alert.showAndWait();
                }
            }
        });

        Button gDriveDeleteButton = new Button("Delete from GoogleDrive");
        gDriveDeleteButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Delete from GoogleDrive");
                textInputDialog.setContentText("Please enter the file name:");

                Optional<String> result = textInputDialog.showAndWait();
                if(result.isPresent()) {
                    googleDriveHandler.deleteFile(result.get());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Delete finished!");
                    alert.setHeaderText("File deleted from GoogleDrive!");
                    alert.showAndWait();
                }
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
