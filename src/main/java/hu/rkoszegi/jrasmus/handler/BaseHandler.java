package hu.rkoszegi.jrasmus.handler;

import hu.rkoszegi.jrasmus.crypto.KeyManager;
import hu.rkoszegi.jrasmus.WebLogin;
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
import javax.net.ssl.HttpsURLConnection;
import javax.persistence.*;
import java.io.*;
import java.net.URL;
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


    //Functions for debugging:
    protected void printAllResponseHeaders(HttpsURLConnection connection) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(connection.getResponseCode())
                .append(" ")
                .append(connection.getResponseMessage())
                .append("\n");
        Map<String, List<String>> map = connection.getHeaderFields();
        for (Map.Entry<String, List<String>> entry : map.entrySet())
        {
            if (entry.getKey() == null)
                continue;
            builder.append( entry.getKey())
                    .append(": ");

            List<String> headerValues = entry.getValue();
            Iterator<String> it = headerValues.iterator();
            if (it.hasNext()) {
                builder.append(it.next());

                while (it.hasNext()) {
                    builder.append(", ")
                            .append(it.next());
                }
            }

            builder.append("\n");
        }
        System.out.println("Response Headers:");
        System.out.println(builder);
    }

    protected String readResponseBody(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
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

    /*protected void uploadFragments(File file, String uploadLink) {
        long totalFileSize = file.length();
        long encryptedFileSize = (totalFileSize / 16 + 1) * 16;
        long packageNumber = ( encryptedFileSize / UPLOAD_PACKET_SIZE) + 1;
        long uploadedBytesNr = 0;
        long readBytesFromFile = 0;

        System.out.println("File Size: " + totalFileSize);
        System.out.println("Encrypted Size: " + encryptedFileSize);

        final long readBytesNumberFromFile = (UPLOAD_PACKET_SIZE / 16 - 1) * 16;

        int currentPacketNr = 1;
        try(FileInputStream fis = new FileInputStream(file)) {
            int backoffIndex = 0;
            Random rand = null;
            while (uploadedBytesNr < encryptedFileSize) {
                HttpsURLConnection connection = null;
                System.out.println((currentPacketNr++) + "/" + packageNumber + ". package");
                try {
                    URL url = new URL(uploadLink);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestMethod("PUT");
                    connection.setRequestProperty("Authorization", bearer + " " + accessToken);
                    connection.setRequestProperty("Content-Type", "text/plain");
                    connection.setDoOutput(true);


                    int packetSize;
                    long startByteNumber = uploadedBytesNr;
                    String rangeHeader;
                    int readSize = 0;
                    if (encryptedFileSize - uploadedBytesNr < UPLOAD_PACKET_SIZE) {
                        long endByteNumber = encryptedFileSize - 1;
                        rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + encryptedFileSize;
                        packetSize = Math.toIntExact(encryptedFileSize - uploadedBytesNr);
                        readSize = Math.toIntExact(totalFileSize - readBytesFromFile);
                    } else {
                        long endByteNumber = startByteNumber + UPLOAD_PACKET_SIZE - 1;
                        rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + encryptedFileSize;
                        packetSize = Math.toIntExact(UPLOAD_PACKET_SIZE);
                        readSize = Math.toIntExact(readBytesNumberFromFile);
                    }
                    connection.setRequestProperty("Content-Range", rangeHeader);
                    System.out.println("Content-Range: " + rangeHeader);

                    System.out.println("Packet size, read size: " + packetSize + ", " + readSize);
                    byte[] buffer = new byte[readSize];
                    //fis.read(buffer, readBytesFromFile, readSize);
                    fis.read(buffer);
                    ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                    byte[] data = encryptToOutputStream(bais);
                    connection.setFixedLengthStreamingMode(data.length);
                    System.out.println("Data size: " + data.length);
                    DataOutputStream wr = new DataOutputStream(
                            connection.getOutputStream());
                    wr.write(data);
                    wr.flush();
                    wr.close();

                    System.out.println("Read bytes from file: " + readBytesFromFile + "/" + totalFileSize);
                    System.out.println("Uploaded bytes nr: " + uploadedBytesNr + "/" + encryptedFileSize);

                    //printAllResponseHeaders(connection);
                    int responseCode = connection.getResponseCode();
                    System.out.println("Response code: "+ responseCode + " " + connection.getResponseMessage());
                    if(responseCode >= 500) {
                        System.out.println("Server error");
                        if(backoffIndex==4) {
                            System.out.println("Nagyon server error");
                            return;
                        }
                        if(rand == null) {
                            rand = new Random();
                        }
                        long sleepTime = ((long) Math.pow(2, backoffIndex)) * 1000 + (rand.nextLong() % 1000);
                        System.out.println("Sleeping for: "+ sleepTime);
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    } else {
                        readBytesFromFile += readSize;
                        uploadedBytesNr += packetSize;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } *//*finally {
                    *//**//*if (connection != null) {
                        connection.disconnect();
                    }*//**//*
                }*//*
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: utolso uzenetet olvasni
    }*/


    /*protected void uploadFragments(File file, String uploadLink) {
        long totalFileSize = file.length();
        long encryptedFileSize = (totalFileSize / 16 + 1) * 16;
        long packageNumber = ( encryptedFileSize / UPLOAD_PACKET_SIZE) + 1;

        int uploadedBytesNr = 0;
        int readBytesFromFile = 0;

        final long readBytesNumberFromFile = (UPLOAD_PACKET_SIZE / 16 - 1) * 16;

        *//*long packageNumber = (totalFileSize / readBytesNumberFromFile) + 1;
        long lastEncodedChunkSize = ((totalFileSize % readBytesNumberFromFile) / 16 + 1) * 16;
        long encryptedFileSize = packageNumber * UPLOAD_PACKET_SIZE + lastEncodedChunkSize;*//*

        System.out.println("total file size: " + totalFileSize);
        System.out.println("encrypted file size: " + encryptedFileSize);
        System.out.println("Package nr : " + packageNumber);

        try(CipherInputStream cis = new CipherInputStream(new FileInputStream(file), getEncryptorCipher())) {
            for (int currentPacketNr = 0; currentPacketNr < packageNumber; currentPacketNr++) {
                System.out.println((currentPacketNr + 1) + "/" + packageNumber + " package");
                HttpsURLConnection connection = null;
                try {
                    URL url = new URL(uploadLink);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestMethod("PUT");
                    connection.setRequestProperty("Authorization", "bearer " + accessToken);
                    connection.setDoOutput(true);

                    //TEST
                    connection.setDoInput(true);


                    int packetSize;
                    int startByteNumber = uploadedBytesNr;
                    String rangeHeader;
                    int readSize = 0;
                    if (encryptedFileSize - uploadedBytesNr < UPLOAD_PACKET_SIZE) {
                        long endByteNumber = encryptedFileSize - 1;
                        rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + encryptedFileSize;
                        packetSize =Math.toIntExact(encryptedFileSize - uploadedBytesNr);
                        //readSize = Math.toIntExact(totalFileSize - readBytesFromFile);
                    } else {
                        long endByteNumber = startByteNumber + UPLOAD_PACKET_SIZE - 1;
                        rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + encryptedFileSize;
                        packetSize = Math.toIntExact(UPLOAD_PACKET_SIZE);
                        //readSize = Math.toIntExact(readBytesNumberFromFile);
                    }
                    connection.setRequestProperty("Content-Range", rangeHeader);
                    connection.setFixedLengthStreamingMode(packetSize);

                    byte[] data = new byte[packetSize];
                    //int currread = cis.read(data, uploadedBytesNr, packetSize);
                   *//* ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                    byte[] data = encryptToOutputStream(bais);*//*

                    System.out.println(rangeHeader);
                    System.out.println("Packet size: " + packetSize);
                    *//*System.out.println("Array size: " + data.length);
                    System.out.println("Currently read: " + currread);*//*


                    DataOutputStream wr = new DataOutputStream(
                            connection.getOutputStream());

                    for(int i = 0; i < (packetSize / 512); i++) {
                        byte[] b = new byte[512];
                        cis.read(b);
                        wr.write(b);

                    }
                    wr.flush();
                    //wr.write(data);
                    wr.close();

                    //readBytesFromFile += readSize;
                    uploadedBytesNr += packetSize;

                    System.out.println(connection.getResponseCode() + " " + connection.getResponseMessage());
                    printAllResponseHeaders(connection);

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: utolso uzenetet olvasni
    }*/


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
}
