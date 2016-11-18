package hu.rkoszegi.jrasmus.model;

import hu.rkoszegi.jrasmus.handler.BaseHandler;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by rkoszegi on 03/11/2016.
 */
@Entity
@Table(name = "STOREDFILE")
public class StoredFile {
    private String path;
    private Date lastModified;
    private String uploadName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HANDLER_ID")
    private BaseHandler handler;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    public StoredFile() {

        pathProperty = new SimpleStringProperty();
        nameProperty = new SimpleStringProperty();
        dateProperty = new SimpleStringProperty();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        this.pathProperty.set(path);
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
        this.dateProperty.set(lastModified.toString());
    }

    public String getUploadName() {
        return uploadName;
    }

    public void setUploadName(String uploadName) {
        this.uploadName = uploadName;
        this.nameProperty.set(uploadName);
    }

    public BaseHandler getHandler() {
        return handler;
    }

    public void setHandler(BaseHandler handler) {
        this.handler = handler;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    //properties for JavaFx

    @Transient
    private StringProperty pathProperty;
    @Transient
    private StringProperty nameProperty;
    @Transient
    private StringProperty dateProperty;

    public StringProperty getNameProperty() {
        return nameProperty;
    }

    public StringProperty getPathProperty() {
        return pathProperty;
    }

    public StringProperty getDateProperty() {
        return dateProperty;
    }
}
