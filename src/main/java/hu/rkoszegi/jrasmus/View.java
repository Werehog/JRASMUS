package hu.rkoszegi.jrasmus;

import hu.rkoszegi.jrasmus.crypto.KeyManager;
import hu.rkoszegi.jrasmus.dao.HandlerDAO;
import hu.rkoszegi.jrasmus.dao.StoredFileDAO;
import hu.rkoszegi.jrasmus.exception.HostUnavailableException;
import hu.rkoszegi.jrasmus.handler.BaseHandler;
import hu.rkoszegi.jrasmus.handler.GoogleDriveHandler;
import hu.rkoszegi.jrasmus.handler.OneDriveHandler;
import hu.rkoszegi.jrasmus.model.StoredFile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.UnknownHostException;
import java.util.*;

/**
 * Created by rkoszegi on 14/11/2016.
 */
public class View {

    List<StoredFile> storedFiles;

    StoredFileDAO storedFileDAO;


    //Layouts
    @FXML
    private VBox rootLayout;


    // Tabs
    @FXML
    private TabPane tabPane;

    @FXML
    private Tab optionsTab;
    @FXML
    private Tab statisticsTab;
    @FXML
    private Tab mainTab;
    @FXML
    private Tab providersTab;


    // Tables
    @FXML
    private TableView searchTable;
    @FXML
    private TableView statisticsTable;


    // Titles and map keys of table columns search
    String searchColumnTitles[] = new String[]{"ISBN", "Title", "Author", "Price"};
    String searchColumnKeys[] = new String[]{"col1", "col2", "col3", "col4"};

    // Titles and map keys of table columns statistics
    String statisticsColumnTitles[] = new String[]{"ISBN", "Author", "Title"};
    String statisticsColumnKeys[] = new String[]{"col1", "col2", "col3"};


    //TODO: testing
    BaseHandler oneDriveHandler;
    BaseHandler googleDriveHandler;
    HandlerDAO handlerDAO;


    @FXML
    private ListView<String> listBoxMain;

    //<Files Tab>
    private ObservableList<StoredFile> storedFileList = FXCollections.observableArrayList();

    @FXML
    private TableView<StoredFile> filesTable;
    @FXML
    private TableColumn<StoredFile, String> nameColumn;
    @FXML
    private TableColumn<StoredFile, String> pathColumn;
    /*@FXML
    private TableColumn<StoredFile, Long> sizeColumn;*/
    @FXML
    private TableColumn<StoredFile, String> lastUploadedColumn;
    //</Files Tab>

    //<Providers Tab>
    private ObservableList<BaseHandler> handlerList = FXCollections.observableArrayList();

    @FXML
    private TableView<BaseHandler> handlersTable;
    @FXML
    private TableColumn<BaseHandler, String> handlerIdColumn;
    @FXML
    private TableColumn<BaseHandler, String> handlerFreesSizeColumn;
    @FXML
    private TableColumn<BaseHandler, String> handlerTotalSizeColumn;
    //</Providers Tab>

    /**
     * View initialization, it will be called after view was prepared
     */
    @FXML
    public void initialize() {
        //Files Tab
        filesTable.setItems(storedFileList);

        nameColumn.setCellValueFactory(cellData -> cellData.getValue().getNameProperty());
        pathColumn.setCellValueFactory(cellData -> cellData.getValue().getPathProperty());
        lastUploadedColumn.setCellValueFactory(cellData -> cellData.getValue().getDateProperty());

        //<Providers Tab>
        handlersTable.setItems(handlerList);
        handlerIdColumn.setCellValueFactory(cellData -> cellData.getValue().getIdProperty());
        handlerFreesSizeColumn.setCellValueFactory(cellData -> cellData.getValue().getFreeSizeProperty());
        handlerTotalSizeColumn.setCellValueFactory(cellData -> cellData.getValue().getTotalSizeProperty());
        //</Providers Tab>


        // Disable buttons to start
        BtnDelete.setDisable(true);

        //TODO: delete handler gomb
        // Add a ChangeListener to ListView to look for change in focus
        /*listBoxMain.focusedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (listBoxMain.isFocused()) {
                    BtnDelete.setDisable(false);
                }
            }
        });*/

        storedFiles = new ArrayList<StoredFile>();

        storedFileDAO = new StoredFileDAO();
        handlerDAO = new HandlerDAO();

        // Clear username and password textfields and display status
        // 'disconnected'
        /*usernameField.setText("");
        passwordField.setText("");
        connectionStateLabel.setText("Connection: disconnected");
        connectionStateLabel.setTextFill(Color.web("#ee0000"));*/

        // Create table (search table) columns
        for (int i = 0; i < searchColumnTitles.length; i++) {
            // Create table column
            TableColumn<Map, String> column = new TableColumn<>(searchColumnTitles[i]);
            // Set map factory
            column.setCellValueFactory(new MapValueFactory(searchColumnKeys[i]));
            // Set width of table column
            column.prefWidthProperty().bind(searchTable.widthProperty().divide(4));
            // Add column to the table
            searchTable.getColumns().add(column);
        }

        // Create table (statistics table) columns
        for (int i = 0; i < statisticsColumnTitles.length; i++) {
            // Create table column
            TableColumn<Map, String> column = new TableColumn<>(statisticsColumnTitles[i]);
            // Set map factory
            column.setCellValueFactory(new MapValueFactory(statisticsColumnKeys[i]));
            // Set width of table column
            // column.prefWidthProperty().bind(statisticsTable.widthProperty().divide(3));
            // Add column to the table
            // statisticsTable.getColumns().add(column);
        }

    }

    /**
     * Initialize controller with data from AppMain (now only sets stage)
     *
     * @param stage The top level JavaFX container
     */
    public void initData(Stage stage) {

        // Set 'onClose' event handler (of the container)
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent winEvent) {
                //TODO 4.2
            }
        });

        initDebugTab();
    }

    private void showHostUnavailableAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Host unavailable!");
        alert.setHeaderText("Can not connect to host. Please check the internet connection!");
        alert.showAndWait();
    }

    private void initDebugTab() {
        oneDriveHandler = new OneDriveHandler();

        googleDriveHandler = new GoogleDriveHandler();

        Button loginButton = new Button("Login to OneDrive");
        loginButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    oneDriveHandler.login();
                } catch (HostUnavailableException e) {
                    showHostUnavailableAlert();
                }
            }
        });

        Button uploadButton = new Button("Upload to OneDrive");
        uploadButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    FileChooser fileChooser = new FileChooser();
                    File file = fileChooser.showOpenDialog(rootLayout.getScene().getWindow());
                    oneDriveHandler.uploadFile(file);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Upload finished!");
                    alert.setHeaderText("File uploaded to OneDrive!");
                    alert.showAndWait();
                } catch (HostUnavailableException e) {
                    showHostUnavailableAlert();
                }
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
                if (result.isPresent()) {
                    try {
                        oneDriveHandler.downloadFile(result.get());
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Download finished!");
                        alert.setHeaderText("File downloaded from OneDrive!");
                        alert.showAndWait();
                    } catch (HostUnavailableException e) {
                        showHostUnavailableAlert();
                    }
                }
            }
        });

        Button listButton = new Button("List root dir");
        listButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    oneDriveHandler.listFolder();

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("List finished!");
                    alert.setHeaderText("Listing finished from OneDrive!");
                    alert.showAndWait();
                } catch (HostUnavailableException e) {
                    showHostUnavailableAlert();
                }
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
                if (result.isPresent()) {
                    try {
                        oneDriveHandler.deleteFile(result.get());

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("File deleted!");
                        alert.setHeaderText("Deleting from OneDrive finished!");
                        alert.showAndWait();
                    } catch (HostUnavailableException e) {
                        showHostUnavailableAlert();
                    }
                }
            }
        });

        Button refreshOdrButton = new Button("Refresh ODR token");
        refreshOdrButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    oneDriveHandler.refreshToken();
                } catch (HostUnavailableException e) {
                    showHostUnavailableAlert();
                }
            }
        });

        GoogleDriveHandler googleDriveHandler = new GoogleDriveHandler();

        Button GDLoginButton = new Button("Google Drive Login");
        GDLoginButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    googleDriveHandler.login();
                } catch (HostUnavailableException e) {
                    showHostUnavailableAlert();
                }
            }
        });

        Button refreshGDButton = new Button("Refresh GD token");
        refreshGDButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    googleDriveHandler.refreshToken();
                } catch (HostUnavailableException e) {
                    showHostUnavailableAlert();
                }
            }
        });

        Button gDriveUploadButton = new Button("Upload to GoogleDrive");
        gDriveUploadButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    FileChooser fileChooser = new FileChooser();
                    File file = fileChooser.showOpenDialog(rootLayout.getScene().getWindow());
                    googleDriveHandler.uploadFile(file);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Upload finished!");
                    alert.setHeaderText("File uploaded to GoogleDrive!");
                    alert.showAndWait();
                } catch (HostUnavailableException e) {
                    showHostUnavailableAlert();
                }
            }
        });

        Button gDriveListFilesButton = new Button("List GoogleDrive");
        gDriveListFilesButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    googleDriveHandler.listFolder();
                } catch (HostUnavailableException e) {
                    showHostUnavailableAlert();
                }
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
                if (result.isPresent()) {
                    try {
                        googleDriveHandler.downloadFile(result.get());
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Download finished!");
                        alert.setHeaderText("File downloaded from GoogleDrive!");
                        alert.showAndWait();
                    } catch (HostUnavailableException e) {
                        showHostUnavailableAlert();
                    }
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
                if (result.isPresent()) {
                    try {
                        googleDriveHandler.deleteFile(result.get());
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Delete finished!");
                        alert.setHeaderText("File deleted from GoogleDrive!");
                        alert.showAndWait();
                    } catch (HostUnavailableException e) {
                        showHostUnavailableAlert();
                    }
                }
            }
        });

        Button criptoTestButton = new Button("criptoTestButton");
        criptoTestButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
               /* TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Password");
                textInputDialog.setContentText("Please enter a password:");

                Optional<String> result = textInputDialog.showAndWait();
                if (result.isPresent()) {
                    new CryptoTest().TestIt(result.get().toCharArray());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Test finished!");
                    alert.setHeaderText("Such test much wow!");
                    alert.showAndWait();
                }*/

                FileChooser fileChooser = new FileChooser();
                File file = fileChooser.showOpenDialog(rootLayout.getScene().getWindow());
                CryptoTest.decryptSuchFile(KeyManager.getKey(), file);
            }
        });

        Button getOdrMetaButton = new Button("getOdrMetaButton");
        getOdrMetaButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    oneDriveHandler.setDriveMetaData();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Test finished!");
                    alert.setHeaderText("Such test much wow!");
                    alert.showAndWait();
                } catch (HostUnavailableException e) {
                    showHostUnavailableAlert();
                }
            }
        });

        Button getGdrMetaButton = new Button("getGdrMetaButton");
        getGdrMetaButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    googleDriveHandler.setDriveMetaData();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Test finished!");
                    alert.setHeaderText("Such test much wow!");
                    alert.showAndWait();
                } catch (HostUnavailableException e) {
                    showHostUnavailableAlert();
                }
            }
        });

        Button connectionTest = new Button("connectionTest");
        connectionTest.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                boolean odrAvailable;
                try {
                    odrAvailable = InetAddress.getByName("www.onedrive.live.com").isReachable(1000);

                } catch (java.net.UnknownHostException e) {
                    e.printStackTrace();
                    odrAvailable = false;
                } catch (IOException e) {
                    e.printStackTrace();
                    odrAvailable = false;
                }
                boolean gdrAvailable;
                try {
                    gdrAvailable = InetAddress.getByName("www.drive.google.com").isReachable(1000);
                } catch (java.net.UnknownHostException e) {
                    e.printStackTrace();
                    gdrAvailable = false;
                } catch (IOException e) {
                    e.printStackTrace();
                    gdrAvailable = false;
                }
                System.out.println("ODR: " + odrAvailable);
                System.out.println("GDH: " + gdrAvailable);

            }
        });


        FlowPane flowPane = new FlowPane(loginButton, uploadButton, downloadButton, listButton, deleteButton, refreshOdrButton,
                GDLoginButton, refreshGDButton, gDriveUploadButton, gDriveListFilesButton, gDriveDownloadButton, gDriveDeleteButton,
                criptoTestButton, getOdrMetaButton, getGdrMetaButton, connectionTest);
        Tab tab = new Tab("Test");
        tab.setContent(flowPane);
        tabPane.getTabs().add(tab);
    }


    public void addFile() {
        FileChooser fileChooser = new FileChooser();
        List<File> fileList = fileChooser.showOpenMultipleDialog(rootLayout.getScene().getWindow());

        for (File file : fileList) {
            StoredFile storedFile = new StoredFile();
            storedFile.setLastModified(new Date(file.lastModified()));
            storedFile.setPath(file.getPath());
            //TODO: handler es fajlnev

            storedFileDAO.persist(storedFile);

            //File tab
            storedFileList.add(storedFile);
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

    final ObservableList<String> listItems = FXCollections.observableArrayList("Add Items here");

    @FXML
    private Button BtnAdd;

    @FXML
    private Button BtnDelete;


    @FXML
    private void addAction(ActionEvent action) {


        List<String> choices = new ArrayList<>();
        choices.add("OneDrive");
        choices.add("GoogleDrive");

        ChoiceDialog<String> dialog = new ChoiceDialog<>("OneDrive", choices);
        dialog.setTitle("Choose a Provider");
        dialog.setHeaderText("Please, choose a provider");
        dialog.setContentText("Provider:");

// Traditional way to get the response value.
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
                handlerDAO.persist(newHandler);
                newHandler.setIdProperty(Long.toString(newHandler.getId()));
                handlerList.add(newHandler);
            }
        }
    }


    @FXML
    private void deleteAction(ActionEvent action) {
        //int selectedItem = listBoxMain.getSelectionModel().getSelectedIndex();
        //listItems.remove(selectedItem);
        System.out.println("Persisted handlers: " + handlerDAO.getAllStoredHandler().size());
    }
}
