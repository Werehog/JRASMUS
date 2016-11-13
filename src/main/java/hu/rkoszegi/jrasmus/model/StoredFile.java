package hu.rkoszegi.jrasmus.model;

import javax.persistence.*;

/**
 * Created by rkoszegi on 03/11/2016.
 */
@Entity
@Table(name = "StoredFile")
public class StoredFile {
    private String name;
    private String downloadUrl;
    private long size;
    private String driveId;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    public StoredFile(String driveId, String name, String downloadUrl, long size) {
        this.driveId = driveId;
        this.name = name;
        this.downloadUrl = downloadUrl;
        this.size = size;
    }

    public StoredFile() {
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getDownloadUrl() {

        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(String driveId) {
        this.driveId = driveId;
    }
}
