package hu.rkoszegi.jrasmus;

import hu.rkoszegi.jrasmus.crypto.KeyHelper;
import hu.rkoszegi.jrasmus.dao.HandlerDAO;
import hu.rkoszegi.jrasmus.dao.StoredFileDAO;
import hu.rkoszegi.jrasmus.exception.HostUnavailableException;
import hu.rkoszegi.jrasmus.handler.BaseHandler;
import hu.rkoszegi.jrasmus.handler.GoogleDriveHandler;
import hu.rkoszegi.jrasmus.handler.OneDriveHandler;
import hu.rkoszegi.jrasmus.model.StoredFile;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.crypto.SecretKey;
import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.*;

/**
 * Created by rkoszegi on 14/11/2016.
 */
public class Controller {

    private HandlerDAO handlerDAO;
    private StoredFileDAO storedFileDAO;

    @FXML
    private VBox rootLayout;

    //<Files Tab>
    private ObservableList<StoredFile> storedFileList = FXCollections.observableArrayList();

    @FXML
    private TableView<StoredFile> filesTable;
    @FXML
    private TableColumn<StoredFile, String> nameColumn;
    @FXML
    private TableColumn<StoredFile, String> pathColumn;
    @FXML
    private TableColumn<StoredFile, String> driveLabelColumn;
    //</Files Tab>

    //<Drives Tab>
    private ObservableList<BaseHandler> handlerList = FXCollections.observableArrayList();

    @FXML
    private TableView<BaseHandler> handlersTable;
    @FXML
    private TableColumn<BaseHandler, String> handlerLabelColumn;
    @FXML
    private TableColumn<BaseHandler, String> handlerFreesSizeColumn;
    @FXML
    private TableColumn<BaseHandler, String> handlerTotalSizeColumn;
    //</Drives Tab>


    //Buttons
    @FXML
    private Button downloadFileButton;

    @FXML
    private Button removeFileButton;


    @FXML
    public void initialize() {
        //Make GDR download work
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

        //Files Tab
        filesTable.setItems(storedFileList);

        nameColumn.setCellValueFactory(cellData -> cellData.getValue().getNameProperty());
        pathColumn.setCellValueFactory(cellData -> cellData.getValue().getPathProperty());
        driveLabelColumn.setCellValueFactory(cellData -> cellData.getValue().getHandler().getLabelProperty());

        //<Drives Tab>
        handlersTable.setItems(handlerList);
        handlerLabelColumn.setCellValueFactory(cellData -> cellData.getValue().getLabelProperty());
        handlerFreesSizeColumn.setCellValueFactory(cellData -> cellData.getValue().getFreeSizeProperty());
        handlerTotalSizeColumn.setCellValueFactory(cellData -> cellData.getValue().getTotalSizeProperty());
        //</Drives Tab>

        handlersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                BtnDelete.setDisable(false);
            } else {
                BtnDelete.setDisable(true);
            }
        });

        filesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                downloadFileButton.setDisable(false);
                removeFileButton.setDisable(false);
            } else {
                downloadFileButton.setDisable(true);
                removeFileButton.setDisable(true);
            }
        });


        // Disable buttons to start
        BtnDelete.setDisable(true);
        downloadFileButton.setDisable(true);
        removeFileButton.setDisable(true);

        storedFileDAO = new StoredFileDAO();
        handlerDAO = new HandlerDAO();
    }


    public void initData(Stage stage) {
        handlerList.addAll(handlerDAO.getAllStoredHandler());
        storedFileList.addAll(storedFileDAO.getAllStoredFile());
    }

    private void showHostUnavailableAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Host unavailable!");
        alert.setHeaderText("Can not connect to host. Please check the internet connection!");
        alert.showAndWait();
    }

    private void showFileDownloadedAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("File downloaded!");
        alert.setHeaderText("The file have been downloaded to the chosen directory!");
        alert.showAndWait();
    }

    private void showHandlerAddedAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Handler added!");
        alert.setHeaderText("Handler added to the database!");
        alert.showAndWait();
    }

    private void showHandlerDeletedAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Handler deleted!");
        alert.setHeaderText("Handler removed from the database!");
        alert.showAndWait();
    }

    private void showFileDeletedAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("File deleted!");
        alert.setHeaderText("The chosen file have been deleted!");
        alert.showAndWait();
    }

    private Dialog<char[]> passwordDialog() {
        Dialog<char[]> dialog = new Dialog<>();
        dialog.setTitle("Password Dialog");
        dialog.setHeaderText("Please, enter a password for the file!");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Password:"), 0, 0);
        grid.add(password, 1, 0);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        password.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(() -> password.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return password.getText().toCharArray();
            }
            return null;
        });

        return dialog;
    }

    private ChoiceDialog<BaseHandler> baseHandlerChoiceDialog() {
        List<String> choices = new ArrayList<>();
        for (BaseHandler handler : handlerList) {
            choices.add(handler.getLabel());
        }

        ChoiceDialog<BaseHandler> choiceDialog = new ChoiceDialog<>(handlerList.get(0), handlerList);
        choiceDialog.setTitle("Choose the destination drive!");
        choiceDialog.setHeaderText("Please, choose a Drive where the file will be uploaded!");
        choiceDialog.setContentText("Chosen Drive label:");

        return choiceDialog;
    }


    public void addFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            List<File> fileList = fileChooser.showOpenMultipleDialog(rootLayout.getScene().getWindow());

            Dialog<char[]> passwordDialog = passwordDialog();

            ChoiceDialog<BaseHandler> baseHandlerChoiceDialog = baseHandlerChoiceDialog();

            Optional<char[]> result = passwordDialog.showAndWait();
            if (result.isPresent()) {
                char[] password = result.get();

                Optional<BaseHandler> handlerResult = baseHandlerChoiceDialog.showAndWait();
                if (handlerResult.isPresent()) {
                    BaseHandler handler = handlerResult.get();

                    for (File file : fileList) {
                        StoredFile storedFile = new StoredFile();
                        storedFile.setLastModified(new Date(file.lastModified()));
                        storedFile.setPath(file.getPath());
                        storedFile.generateUploadName(file.getName());

                        byte[] salt = KeyHelper.generateSalt();
                        SecretKey key = KeyHelper.generateSecretKeyFromPassword(password, salt);

                        Base64.Encoder base64Encoder = Base64.getEncoder();
                        storedFile.setSalt(base64Encoder.encodeToString(salt));
                        storedFile.setPwHash(base64Encoder.encodeToString(KeyHelper.generatePasswordHash(key)));

                        handler.setKey(key);
                        handler.uploadFile(file, storedFile.getUploadName());
                        storedFile.setHandler(handler);

                        storedFileList.add(storedFile);

                        storedFileDAO.persist(storedFile);
                    }

                    for (StoredFile file : storedFileDAO.getAllStoredFile()) {
                        System.out.println(file.getPath());
                    }
                    System.out.println("Stored file nr: " + storedFileDAO.getAllStoredFile().size());

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("File Added!");
                    alert.setHeaderText("File added to one of your storage!");
                    alert.showAndWait();
                }
            }
        } catch (HostUnavailableException e) {
            showHostUnavailableAlert();
        }

    }

    public void downloadFile() {
        try {
            StoredFile file = filesTable.getSelectionModel().getSelectedItem();
            if (file != null) {
                Dialog<char[]> passwordDialog = passwordDialog();
                Optional<char[]> passwordResult = passwordDialog.showAndWait();

                if (passwordResult.isPresent()) {
                    byte[] salt = Base64.getDecoder().decode(file.getSalt());
                    SecretKey key = KeyHelper.generateSecretKeyFromPassword(passwordResult.get(), salt);
                    if (Base64.getEncoder().encodeToString(KeyHelper.generatePasswordHash(key)).equals(file.getPwHash())) {
                        DirectoryChooser directoryChooser = new DirectoryChooser();
                        File selectedDir = directoryChooser.showDialog(rootLayout.getScene().getWindow());
                        if (selectedDir != null) {
                            StoredFile downloadFile = new StoredFile();
                            downloadFile.setPath(selectedDir.getPath());
                            downloadFile.setUploadName(file.getUploadName());
                            BaseHandler handler = file.getHandler();
                            handler.setKey(key);
                            handler.downloadFile(downloadFile);
                            showFileDownloadedAlert();
                        }
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Wrong Password");
                        alert.setHeaderText("The given password is incorrect!");
                        alert.showAndWait();
                        return;
                    }
                }


            }
        } catch (HostUnavailableException e) {
            showHostUnavailableAlert();
        }
    }

    public void deleteFile() {
        try {
            StoredFile file = filesTable.getSelectionModel().getSelectedItem();
            if (file != null) {
                BaseHandler handler = file.getHandler();
                handler.deleteFile(file.getUploadName());
                storedFileDAO.deleteByReference(file);
                storedFileList.remove(storedFileList.indexOf(file));
                showFileDeletedAlert();
            }
        } catch (HostUnavailableException e) {
            showHostUnavailableAlert();
        }
    }

    @FXML
    private Button BtnAdd;

    @FXML
    private Button BtnDelete;


    @FXML
    private void addAction(ActionEvent action) {
        try {
            List<String> choices = new ArrayList<>();
            choices.add("OneDrive");
            choices.add("GoogleDrive");

            ChoiceDialog<String> dialog = new ChoiceDialog<>("OneDrive", choices);
            dialog.setTitle("Choose a Provider");
            dialog.setHeaderText("Please, choose a provider");
            dialog.setContentText("Provider:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                System.out.println("Your choice: " + result.get());
                BaseHandler newHandler = null;
                switch (result.get()) {
                    case "OneDrive":
                        newHandler = new OneDriveHandler();
                        break;
                    case "GoogleDrive":
                        newHandler = new GoogleDriveHandler();
                        break;
                }
                if (newHandler != null) {
                    newHandler.login();
                    newHandler.setDriveMetaData();

                    TextInputDialog textInputDialog = new TextInputDialog();
                    textInputDialog.setTitle("Handler Label");
                    textInputDialog.setContentText("Please enter a label for the new drive:");

                    Optional<String> label = textInputDialog.showAndWait();
                    if (label.isPresent()) {
                        newHandler.setLabel(label.get());
                    }

                    handlerDAO.persist(newHandler);
                    newHandler.setIdProperty(Long.toString(newHandler.getId()));
                    handlerList.add(newHandler);
                    showHandlerAddedAlert();
                }
            }
        } catch (HostUnavailableException e) {
            showHostUnavailableAlert();
        }
    }


    @FXML
    private void deleteAction(ActionEvent action) {
        try {
            BaseHandler selectedHandler = handlersTable.getSelectionModel().getSelectedItem();
            if (selectedHandler != null) {
                for (StoredFile file : storedFileDAO.findByHandler(selectedHandler)) {
                    selectedHandler.deleteFile(file.getUploadName());
                    storedFileDAO.deleteByReference(file);
                    storedFileList.remove(storedFileList.indexOf(file));
                }

                handlerDAO.deleteByName(selectedHandler.getLabel());
                handlerList.remove(handlerList.indexOf(selectedHandler));
                showHandlerDeletedAlert();
            }
        } catch (HostUnavailableException e) {
            showHostUnavailableAlert();
        }
    }
}
