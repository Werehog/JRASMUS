package hu.rkoszegi.jrasmus.model;

import hu.rkoszegi.jrasmus.handler.BaseHandler;

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
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getUploadName() {
        return uploadName;
    }

    public void setUploadName(String uploadName) {
        this.uploadName = uploadName;
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
}
