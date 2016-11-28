package hu.rkoszegi.jrasmus.handler;

import hu.rkoszegi.jrasmus.Request;
import hu.rkoszegi.jrasmus.crypto.KeyManager;
import hu.rkoszegi.jrasmus.WebLogin;
import hu.rkoszegi.jrasmus.exception.HostUnavailableException;
import hu.rkoszegi.jrasmus.exception.ServiceUnavailableException;
import hu.rkoszegi.jrasmus.exception.UnauthorizedException;
import hu.rkoszegi.jrasmus.model.AbstractEntity;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by rkoszegi on 07/11/2016.
 */
@Entity
@DiscriminatorColumn(name="HANDLER_TYPE")
@Table(name="HANDLER")
public abstract class BaseHandler extends AbstractEntity {

    @Lob
    protected String accessToken;

    @Lob
    protected String refreshToken;

    private long totalSize;
    private long freeSize;

    private String label;

    @Transient
    protected static final long UPLOAD_PACKET_SIZE = 7* 320 * 1024;
    @Transient
    protected static final int DOWNLOAD_PACKET_SIZE = 1048576;

    @Transient
    protected String propertyFileName;

    @Transient
    protected String connectionTestLink;

    public BaseHandler() {
        idProperty = new SimpleStringProperty();
        freeSizeProperty = new SimpleStringProperty();
        totalSizeProperty = new SimpleStringProperty();
        labelProperty = new SimpleStringProperty();
    }

    public void login() {
        checkInternetConnection();

        Properties properties = new Properties();
        String clientId = null;
        String clientSecret = null;
        String loginUrl = null;
        String redirectUri = null;
        String scope = null;
        try(InputStream propertyInputStream = BaseHandler.class.getResourceAsStream( propertyFileName)){
            properties.load(propertyInputStream);
            clientId = properties.getProperty("clientId");
            clientSecret = properties.getProperty("clientSecret");
            loginUrl = properties.getProperty("loginUrl");
            redirectUri = properties.getProperty("redirectUri");
            scope = properties.getProperty("scope");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String url = loginUrl +
                "client_id=" + clientId +
                //TODO: átírni appfolderre
                // "&scope=https://www.googleapis.com/auth/drive.appfolder" +
                "&scope=" + scope +
                "&response_type=code" +
                "&redirect_uri=" + redirectUri;

        WebLogin login = new WebLogin(url, "code=");
        login.showAndWait();

        String authCode = login.getAuthCode();

        if(authCode != null && !"".equals(authCode)) {
            getToken(login.getAuthCode(), clientId, clientSecret);
        }
    }

    protected void getToken(String authCode, String clientId, String clientSecret) {
        checkInternetConnection();
        getTokenImpl(authCode, clientId, clientSecret);
    }

    protected abstract void getTokenImpl(String authCode, String clientId, String clientSecret);

    public void refreshToken() {
        checkInternetConnection();
        refreshTokenImpl();
    }

    protected abstract void refreshTokenImpl();

    public void setDriveMetaData() {
        checkInternetConnection();
        setDriveMetaDataImpl();
    }

    protected abstract void setDriveMetaDataImpl();

    public void uploadFile(File file) {
        checkInternetConnection();
        if(file.length() < 10000000) {
            uploadSmallFile(file);
        } else {
            uploadLargeFile(file);
        }
    }

    protected abstract void uploadSmallFile(File file);

    protected abstract void uploadLargeFile(File file);

    public void downloadFile(String fileName) {
        checkInternetConnection();
        downloadFileImpl(fileName);
    }

    protected abstract void downloadFileImpl(String fileName);

    public void listFolder () {
        checkInternetConnection();
        listFolderImpl();
    }

    protected abstract void listFolderImpl();

    public void deleteFile(String fileName) {
        checkInternetConnection();
        deleteFileImpl(fileName);
    }

    protected abstract void deleteFileImpl(String fileName);

    protected String getObjectFromJSONInput(InputStream inputStream, String name) {
        JsonReader jSonReader = Json.createReader(inputStream);
        JsonObject obj = jSonReader.readObject();
        return obj.getString(name);
    }

    protected byte[] encryptToOutputStream(InputStream in) {
        Cipher cipher = getEncryptorCipher();
        ByteArrayOutputStream bos = null;
        try (CipherInputStream cis = new CipherInputStream(in, cipher)) {
            bos = new ByteArrayOutputStream();
            byte[] b = new byte[8];
            int i = cis.read(b);
            while (i != -1) {
                bos.write(b,0,i);
                i = cis.read(b);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return  bos.toByteArray();
    }

    protected Cipher getEncryptorCipher() {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, KeyManager.getKey());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return cipher;
    }

    protected void decryptToOutputStream(CipherOutputStream cos, InputStream in) {
        byte[] b = new byte[8];
        try {
            int i = in.read(b);
            while (i != -1) {
                cos.write(b, 0, i);
                i = in.read(b);
            }
            cos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Cipher getDecryptorCipher() {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, KeyManager.getKey());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return cipher;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    //FXML miatt
    @Transient
    private StringProperty idProperty;
    @Transient
    private StringProperty freeSizeProperty;
    @Transient
    private StringProperty totalSizeProperty;
    @Transient
    private StringProperty labelProperty;

    public StringProperty getIdProperty() {
        return idProperty;
    }

    public StringProperty getLabelProperty() {
        this.labelProperty.set(this.label);
        return labelProperty;
    }

    public StringProperty getFreeSizeProperty() {
        this.freeSizeProperty.set(Long.toString(this.freeSize));
        return freeSizeProperty;
    }

    public StringProperty getTotalSizeProperty() {
        this.totalSizeProperty.set(Long.toString(this.totalSize));
        return totalSizeProperty;
    }

    public void setIdProperty(String id) {
        this.idProperty.set(id);
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
        this.totalSizeProperty.set(Long.toString(totalSize));
    }

    public long getFreeSize() {
        return freeSize;
    }

    public void setFreeSize(long freeSize) {
        this.freeSize = freeSize;
        this.freeSizeProperty.set(Long.toString(freeSize));
    }

    public void setId(long id) {
        this.id = id;
        this.idProperty.set(Long.toString(id));
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.labelProperty.set(label);
        this.label = label;
    }

    protected void executeRequest(Request request) {
        boolean isRequestInProgress = true;
        int backoffIndex = 0;
        Random rand = null;
        while(isRequestInProgress) {
            try {
                request.makeRequest();
                isRequestInProgress = false;
            } catch (UnauthorizedException e) {
                refreshToken();
            } catch (ServiceUnavailableException e) {
                System.out.println("Server error");
                if(backoffIndex==4) {
                    System.out.println("Nagyon server error");
                    throw new RuntimeException("Service heavily unavailable");
                }
                if(rand == null) {
                    rand = new Random();
                }
                long sleepTime = ((long) Math.pow(2, backoffIndex)) * 1000 + (rand.nextLong() % 1000);
                System.out.println("Sleeping for: "+ sleepTime);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException inex) {
                    inex.printStackTrace();
                }
                backoffIndex++;
            }
        }
    }

    private void checkInternetConnection() {
        boolean available;
        try {
            available = InetAddress.getByName(connectionTestLink).isReachable(1000);
        } catch (IOException e) {
            HostUnavailableException hostUnavailableException = new HostUnavailableException("Host is unavailable");
            hostUnavailableException.initCause(e);
            throw hostUnavailableException;
        }
        if(!available) {
            throw new HostUnavailableException("Host is unavailable");
        }
    }
}
