package hu.rkoszegi.jrasmus.model;

import hu.rkoszegi.jrasmus.crypto.CryptoHelper;
import hu.rkoszegi.jrasmus.crypto.KeyHelper;
import hu.rkoszegi.jrasmus.handler.BaseHandler;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import javax.crypto.SecretKey;
import javax.persistence.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
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

    private String salt;
    private String pwHash;

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

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getPwHash() {
        return pwHash;
    }

    public void setPwHash(String pwHash) {
        this.pwHash = pwHash;
    }

    //properties for JavaFx

    @Transient
    private StringProperty pathProperty;
    @Transient
    private StringProperty nameProperty;
    @Transient
    private StringProperty dateProperty;

    public StringProperty getNameProperty() {
        this.nameProperty.set(this.uploadName);
        return nameProperty;
    }

    public StringProperty getPathProperty() {
        this.pathProperty.set(this.path);
        return pathProperty;
    }

    public StringProperty getDateProperty() {
        this.dateProperty.set(this.lastModified.toString());
        return dateProperty;
    }

    public void generateUploadName(String fileName) {
        ByteArrayInputStream bais = new ByteArrayInputStream(fileName.getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SecretKey key = KeyHelper.getFileNameKey();
        CryptoHelper.encrypt(key,bais,baos);
        uploadName = Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public String getDecodedUploadName() {
        byte[] baseDecodedByteArray = Base64.getDecoder().decode(uploadName);
        ByteArrayInputStream bais = new ByteArrayInputStream(baseDecodedByteArray);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SecretKey key = KeyHelper.getFileNameKey();
        CryptoHelper.decrypt(key,bais,baos);
        return new String(baos.toByteArray());
    }
}
