package hu.rkoszegi.jrasmus;

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
import org.w3c.dom.NodeList;

/**
 * Created by Richárd on 2016.09.13..
 */
public class WebLogin {

    private WebView browser;
    private WebEngine webEngine;

    private String authCode;

    private Stage stage;

    public WebLogin(String url, final String trigger) {


        stage = new Stage();
        stage.setTitle("Login");
        Scene scene = new Scene(new Group());

        VBox root = new VBox();

        browser = new WebView();
        webEngine = browser.getEngine();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(browser);

        webEngine.getLoadWorker().stateProperty().addListener(
                new ChangeListener<Worker.State>() {
                    @Override public void changed(ObservableValue ov, Worker.State oldState, Worker.State newState) {
                        if(newState == Worker.State.SUCCEEDED && webEngine.getLocation().contains("approval")) {
                            NodeList nodeList = webEngine.getDocument().getElementsByTagName("title");
                            String content = nodeList.item(0).getTextContent();
                            int equalIndex = content.indexOf("=");
                            authCode = content.substring(equalIndex + 1);
                            stage.close();
                        }
                        if (newState == Worker.State.SUCCEEDED && webEngine.getLocation().contains(trigger)) {
                            int codeStart = webEngine.getLocation().indexOf(trigger);
                            authCode = webEngine.getLocation().substring(codeStart + 5);
                            int codeEnd = authCode.indexOf("&");
                            if(codeEnd != -1)
                                authCode = authCode.substring(0, codeEnd);
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

    public String getAuthCode() {
        return authCode;
    }

    public void showAndWait() {
        stage.showAndWait();
    }
}
