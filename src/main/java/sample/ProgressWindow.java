package sample;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Created by Richárd on 2016.10.09..
 */
public class ProgressWindow {

    private Stage stage;
    private ProgressBar progressBar;


    public ProgressWindow(String title) {

        stage = new Stage();
        stage.setTitle(title);

        progressBar = new ProgressBar(0);

        Label label = new Label();
        label.setText("Your file is uploading");

        Group group = new Group();
        Scene scene = new Scene(group, 300, 150);

        VBox vb = new VBox();

        vb.getChildren().addAll(label, progressBar);
        scene.setRoot(vb);

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);

        stage.show();
    }

    public void setProgress(double value) {
        progressBar.setProgress(value);
    }

    public void close() {
        stage.close();
    }
}
