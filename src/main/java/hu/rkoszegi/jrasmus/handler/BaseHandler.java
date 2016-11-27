package hu.rkoszegi.jrasmus.handler;

import hu.rkoszegi.jrasmus.Request;
import hu.rkoszegi.jrasmus.crypto.KeyManager;
import hu.rkoszegi.jrasmus.WebLogin;
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

    @Transient
    protected static final long UPLOAD_PACKET_SIZE = 7* 320 * 1024;
    @Transient
    protected static final int DOWNLOAD_PACKET_SIZE = 1048576;

    @Transient
    protected String propertyFileName;
    @Transient
    protected String bearer;

    public BaseHandler() {
        idProperty = new SimpleStringProperty();
        freeSizeProperty = new SimpleStringProperty();
        totalSizeProperty = new SimpleStringProperty();
    }

    public abstract void setDriveMetaData();

    public void login() {
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
            bearer = properties.getProperty("bearer");
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

    //TODO:abstract
    protected void getToken(String authCode, String clientId, String clientSecret) {}

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

    public StringProperty getIdProperty() {
        return idProperty;
    }

    public StringProperty getFreeSizeProperty() {
        return freeSizeProperty;
    }

    public StringProperty getTotalSizeProperty() {
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

    public abstract void refreshToken();

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
}
