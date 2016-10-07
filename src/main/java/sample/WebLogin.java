package sample;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Created by Richárd on 2016.09.13..
 */
public class WebLogin {

    private WebView browser;
    private WebEngine webEngine;

    private String flag;

    private Stage stage;

    public WebLogin(String url, final String trigger) {


        stage = new Stage();
        stage.setTitle("Login");
        //stage.setWidth(400);
        //stage.setHeight(550);
        Scene scene = new Scene(new Group());

        VBox root = new VBox();

        browser = new WebView();
        webEngine = browser.getEngine();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(browser);

        webEngine.getLoadWorker().stateProperty().addListener(
                new ChangeListener<Worker.State>() {
                    @Override public void changed(ObservableValue ov, Worker.State oldState, Worker.State newState) {

                        if (newState == Worker.State.SUCCEEDED && webEngine.getLocation().contains(trigger)) {
                            //flag = webEngine.getLocation();
                            System.out.println("url changed: " + webEngine.getLocation());

                            int codeStart = webEngine.getLocation().indexOf(trigger);
                            flag = webEngine.getLocation().substring(codeStart + 5);
                            int codeEnd = flag.indexOf("&");
                            if(codeEnd != -1)
                                flag = flag.substring(0, codeEnd);
                            System.out.println(flag);
                            stage.close();
                        }

                    }
                });
        webEngine.load(url);

        root.getChildren().addAll(scrollPane);
        scene.setRoot(root);

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
    }

    public String getFlag() {
        return flag;
    }

    public void showAndWait() {
        stage.showAndWait();
    }
}
