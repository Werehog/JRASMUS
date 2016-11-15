package hu.rkoszegi.jrasmus;

import hu.rkoszegi.jrasmus.dao.StoredFileDAO;
import hu.rkoszegi.jrasmus.handler.GoogleDriveHandler;
import hu.rkoszegi.jrasmus.handler.OneDriveHandler;
import hu.rkoszegi.jrasmus.model.StoredFile;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.persistence.*;
import java.io.File;
import java.util.List;
import java.util.Optional;


public class Main extends Application {

    private OneDriveHandler oneDriveHandler;

    @Override
    public void start(final Stage stage) throws Exception{
       /* Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));*/

       DatabaseManager.initDB();

        try {

            // Create a loader object and load View and Controller
            final FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("sample.fxml"));
            final VBox viewRoot = (VBox) loader.load();

            // Get controller object and initialize it
            final View controller = loader.getController();
            controller.initData(stage);

            // Set scene (and the title of the window) and display it
            Scene scene = new Scene(viewRoot);
            stage.setScene(scene);
            stage.setTitle("JRASMUS");
            stage.show();

        } catch (Exception e) {

            e.printStackTrace();

        }

        /*oneDriveHandler = new OneDriveHandler();

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

        Button criptoTestButton = new Button("criptoTestButton");
        criptoTestButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Password");
                textInputDialog.setContentText("Please enter a password:");

                Optional<String> result = textInputDialog.showAndWait();
                if(result.isPresent()) {
                    new CryptoTest().TestIt(result.get().toCharArray());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Test finished!");
                    alert.setHeaderText("Such test much wow!");
                    alert.showAndWait();
                }
            }
        });

*//*
        DatabaseManager.initDB();
*//*

        Button persistenceTestButton = new Button("criptoTestButton");
        persistenceTestButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //StoredFileDAO dao = new StoredFileDAO();
                StoredFile file = new StoredFile();
                file.setName("TestFile");
               EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("JrasmusPersistenceUnit");
                 EntityManager entityManager = entityManagerFactory.createEntityManager();
                EntityTransaction tx = entityManager.getTransaction();
                tx.begin();
                entityManager.persist(file);
                tx.commit();

                Query q = entityManager.createQuery("SELECT f FROM StoredFile f");

                List<StoredFile> result = q.getResultList();

                System.out.println("Res meret: " + result.size());

                for (Object f: result) {
                    System.out.println(((StoredFile)f).getName());
                }
                System.out.printf("Such working code");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Test finished!");
                alert.setHeaderText("Such test much wow!");
                alert.showAndWait();
            }
        });

        stage.setScene(new Scene(new FlowPane(loginButton, uploadButton, downloadButton, listButton, deleteButton,
                GDLoginButton,gDriveUploadButton, gDriveUploadBIGButton, gDriveListFilesButton, gDriveDownloadButton, gDriveDeleteButton, criptoTestButton, persistenceTestButton), 400, 550));
        stage.show();*/
    }

    @Override
    public void stop() {
        DatabaseManager.closeDB();
    }



    public static void main(String[] args) {
        launch(args);
    }
}
