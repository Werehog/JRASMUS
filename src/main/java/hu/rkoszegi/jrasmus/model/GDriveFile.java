package hu.rkoszegi.jrasmus.model;

/**
 * Created by rkoszegi on 13/11/2016.
 */
public class GDriveFile {

    private String id;
    private String name;
    private String downloadUrl;
    private long size;

    public GDriveFile(String id, String name, String downloadUrl, long size) {
        this.id = id;
        this.name = name;
        this.downloadUrl = downloadUrl;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public long getSize() {
        return size;
    }
}
