package hu.rkoszegi.jrasmus;

/**
 * Created by rkoszegi on 03/11/2016.
 */
public class StoredFile {
    private String name;
    private String downloadUrl;
    private long size;
    private String id;

    public StoredFile(String id, String name, String downloadUrl, long size) {
        this.id = id;
        this.name = name;
        this.downloadUrl = downloadUrl;
        this.size = size;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
