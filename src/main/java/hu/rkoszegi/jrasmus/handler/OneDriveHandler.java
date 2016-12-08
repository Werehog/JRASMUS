package hu.rkoszegi.jrasmus.handler;

import java.net.URLEncoder;
import hu.rkoszegi.jrasmus.Request;
import hu.rkoszegi.jrasmus.RequestType;
import hu.rkoszegi.jrasmus.model.StoredFile;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.Calendar;
import java.util.Properties;

/**
 * Created by Richárd on 2016.09.13..
 */

@Entity
@DiscriminatorValue("ONEDRIVE")
public class OneDriveHandler extends BaseHandler {


    public OneDriveHandler() {
        this.propertyFileName = "/OneDrive.properties";
        connectionTestLink = "www.onedrive.live.com";
    }

    @Override
    protected void getTokenImpl(String authCode, String clientId, String clientSecret) {
        Request r = new Request();

        r.setRequestUrl("https://login.live.com/oauth20_token.srf");
        r.setRequestType(RequestType.POST);
        String content = "client_id=" + clientId +
                "&redirect_uri=https://login.live.com/oauth20_desktop.srf" +
                "&client_secret=" + clientSecret +
                "&code=" + authCode + "&grant_type=authorization_code";
        r.setRequestData(content.getBytes());

        r.setInputData(true);
        executeRequest(r);

        JsonReader jSonReader = Json.createReader(r.getResponseStream());
        JsonObject obj = jSonReader.readObject();

        accessToken = obj.getString("access_token");
        refreshToken = obj.getString("refresh_token");

        tokenExpireTime = Calendar.getInstance();
        tokenExpireTime.add(Calendar.SECOND, obj.getInt("expires_in") - 10);
        System.out.println("Expires in: " + tokenExpireTime.toString());
    }

    @Override
    protected void refreshTokenImpl() {
        System.out.println("refreshToken called");

        Properties properties = new Properties();
        String clientId;
        String clientSecret;
        String redirectUri;
        try(InputStream propertyInputStream = BaseHandler.class.getResourceAsStream( propertyFileName)){
            properties.load(propertyInputStream);
            clientId = properties.getProperty("clientId");
            clientSecret = properties.getProperty("clientSecret");
            redirectUri = properties.getProperty("redirectUri");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Request request = new Request();
        request.setRequestUrl("https://login.live.com/oauth20_token.srf");
        request.setRequestType(RequestType.POST);

        String content = "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&client_secret=" + clientSecret +
                "&refresh_token=" + refreshToken +
                "&grant_type=refresh_token";
        request.setRequestData(content.getBytes());
        request.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");

        request.setInputData(true);
        executeRequest(request);

        JsonReader jSonReader = Json.createReader(request.getResponseStream());
        JsonObject obj = jSonReader.readObject();

        accessToken = obj.getString("access_token");

        tokenExpireTime = Calendar.getInstance();
        tokenExpireTime.add(Calendar.SECOND, obj.getInt("expires_in") - 10);
        System.out.println("Expires in: " + tokenExpireTime.toString());
    }

    @Override
    protected void uploadSmallFile(File file, String uploadedFileName) {
        System.out.println("uploadSmallFile called");
        Request request = new Request();
        String urlEncodedFileName;
        try {
            urlEncodedFileName = URLEncoder.encode(uploadedFileName, "UTF-8");
            request.setRequestUrl("https://api.onedrive.com/v1.0/drive/special/approot:/" + urlEncodedFileName + ":/content");
            request.setRequestType(RequestType.PUT);
            request.addRequestHeader("Content-Type", "text/plain");
            request.addRequestHeader("Authorization", "bearer " + accessToken);
            ByteArrayInputStream bais = new ByteArrayInputStream(Files.readAllBytes(file.toPath()));
            byte[] data = encryptToOutputStream(bais);
            request.setRequestData(data);
            executeRequest(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void uploadLargeFile(File file, String uploadedFileName) {
        System.out.println("uploadLargeFile called");
        String uploadLink = createUploadSession(uploadedFileName);
        uploadFragments(file, uploadLink);
    }

    private String createUploadSession(String uploadedFileName) {
        String uploadLink = null;
        Request request = new Request();
        try {
            String urlEncodedFileName = URLEncoder.encode(uploadedFileName, "UTF-8");
            request.setRequestUrl("https://api.onedrive.com/v1.0/drive/special/approot:/" + urlEncodedFileName + ":/upload.createSession");
            request.setRequestType(RequestType.POST);
            request.addRequestHeader("Content-Type", "text/plain");
            request.addRequestHeader("Authorization", "bearer " + accessToken);
            request.setInputData(true);

            executeRequest(request);

            uploadLink = getObjectFromJSONInput(request.getResponseStream(), "uploadUrl");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return uploadLink;
    }

    protected void uploadFragments(File file, String uploadLink) {
        long totalFileSize = file.length();
        long encryptedFileSize = (totalFileSize / 16 + 1) * 16;
        long packageNumber = ( encryptedFileSize / UPLOAD_PACKET_SIZE) + 1;

        int uploadedBytesNr = 0;

        try(CipherInputStream cis = new CipherInputStream(new FileInputStream(file), getEncryptorCipher())) {
            for (int currentPacketNr = 0; currentPacketNr < packageNumber; currentPacketNr++) {
                System.out.println((currentPacketNr + 1) + "/" + packageNumber + " package");

                Request request = new Request();
                request.setRequestUrl(uploadLink);
                request.setRequestType(RequestType.PUT);
                request.addRequestHeader("Authorization", "bearer " + accessToken);

                int packetSize;
                int startByteNumber = uploadedBytesNr;
                String rangeHeader;
                if (encryptedFileSize - uploadedBytesNr < UPLOAD_PACKET_SIZE) {
                    long endByteNumber = encryptedFileSize - 1;
                    rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + encryptedFileSize;
                    packetSize = Math.toIntExact(encryptedFileSize - uploadedBytesNr);
                } else {
                    long endByteNumber = startByteNumber + UPLOAD_PACKET_SIZE - 1;
                    rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + encryptedFileSize;
                    packetSize = Math.toIntExact(UPLOAD_PACKET_SIZE);
                }
                request.addRequestHeader("Content-Range", rangeHeader);


                try(ByteArrayOutputStream wr = new ByteArrayOutputStream()) {

                    for (int i = 0; i < (packetSize / 8); i++) {
                        byte[] b = new byte[8];
                        cis.read(b);
                        wr.write(b);
                    }
                    request.setRequestData(wr.toByteArray());
                }

                executeRequest(request);

                uploadedBytesNr += packetSize;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TODO: utolso uzenetet olvasni
    }

    private void cancelUpload(String uploadLink) {
        //TODO: ellenorizni, kell-e
        System.out.println("cancelUpload called");
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(uploadLink);
            connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setRequestProperty("Authorization", "bearer " + accessToken);
            connection.setRequestProperty("Content-Length", "0");

            System.out.println("Cancel response: " + connection.getResponseCode());
            System.out.println(connection.getResponseMessage());



        } catch (IOException e) {
            //openconn es malformed url miatt
            //TODO: malformedet lekezelni, io-t tovabbdobni: nincs internet detektalas :D
            e.printStackTrace();
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void downloadFileImpl(StoredFile storedFile) {
        System.out.println("downloadFile called");
        String downloadUrl = getDownloadUrl(storedFile.getUploadName());
        String newFilePath = storedFile.getPath() + "\\" + storedFile.getDecodedUploadName();
        downloadContent(downloadUrl, newFilePath);
    }

    private String getDownloadUrl(String fileName) {
        String encodedFileName = null;
        try {
            encodedFileName = URLEncoder.encode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String downloadLink = "https://api.onedrive.com/v1.0/drive/special/approot:/" + encodedFileName + ":/content";

        Request request = new Request();
        request.setRequestUrl(downloadLink);
        request.setRequestType(RequestType.GET);
        request.addRequestHeader("Authorization", "bearer " + accessToken);

        executeRequest(request);

        return request.getResponseHeader("Content-Location");
    }

    private void downloadContent(String location, String filePath) {
        Cipher cipher = getDecryptorCipher();
        try (CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(new File(filePath)), cipher)) {
            int fileSize = downloadFirstPart(location, cos);

            if(fileSize > DOWNLOAD_PACKET_SIZE) {
                int downloadedByteNr = DOWNLOAD_PACKET_SIZE;

                while(downloadedByteNr < fileSize) {
                   downloadedByteNr = downloadNextPart(location, downloadedByteNr, cos);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int downloadFirstPart(String url, CipherOutputStream cipherOutputStream) {
        Request request = new Request();
        request.setRequestUrl(url);
        request.setRequestType(RequestType.GET);
        request.addRequestHeader("Range", "bytes=0-" + Integer.toString(DOWNLOAD_PACKET_SIZE - 1));
        request.setInputData(true);

        executeRequest(request);

        String contentRange = request.getResponseHeader("Content-Range");
        int fileSize = Integer.parseInt(contentRange.substring(contentRange.indexOf("/") + 1));
        decryptToOutputStream(cipherOutputStream, request.getResponseStream());
        return fileSize;
    }

    private int downloadNextPart(String url, int downloadedByteNr, CipherOutputStream cipherOutputStream) {
        Request request = new Request();
        request.setRequestUrl(url);
        request.setRequestType(RequestType.GET);
        int endByteNumber = downloadedByteNr + DOWNLOAD_PACKET_SIZE - 1;
        request.addRequestHeader("Range", "bytes=" + downloadedByteNr + "-" + endByteNumber);
        request.setInputData(true);

        executeRequest(request);

        int contentLength = Integer.parseInt(request.getResponseHeader("Content-Length"));
        decryptToOutputStream(cipherOutputStream, request.getResponseStream());

        return downloadedByteNr + contentLength;
    }

    @Override
    protected void listFolderImpl() {
        Request request = new Request();
        request.setRequestUrl("https://api.onedrive.com/v1.0/drive/special/approot/children");
        request.setRequestType(RequestType.GET);
        request.addRequestHeader("Authorization", "bearer " + accessToken);

        request.setInputData(true);
        executeRequest(request);

        JsonReader jsonReader = Json.createReader(request.getResponseStream());
        JsonObject jsonObject = jsonReader.readObject();
        JsonArray array = jsonObject.getJsonArray("value");
        for(int i=0; i<array.size(); i++) {
            JsonObject object = array.getJsonObject(i);
            String out = "Name: " + object.getString("name");
            if (object.containsKey("folder")) {
                out += " (folder)";
            }
            System.out.println(out);
        }
        //TODO: nextlink ha 200nal tobb van
    }

    @Override
    protected void deleteFileImpl(String fileName) {
        if(fileName.contains("/")) {
            fileName = fileName.substring(0, fileName.indexOf("/"));
        }
        System.out.println(fileName);
        Request request = new Request();
        request.setRequestUrl("https://api.onedrive.com/v1.0/drive/special/approot:/" + fileName);
        request.setRequestType(RequestType.DELETE);
        request.addRequestHeader("Authorization", "bearer " + accessToken);
        executeRequest(request);
    }

    @Override
    protected void setDriveMetaDataImpl() {
        Request request = new Request();
        request.setRequestUrl("https://api.onedrive.com/v1.0/drive");
        request.setRequestType(RequestType.GET);
        request.addRequestHeader("Authorization", "bearer " + accessToken);

        request.setInputData(true);
        executeRequest(request);

        JsonReader jSonReader = Json.createReader(request.getResponseStream());
        JsonObject rootObject = jSonReader.readObject();
        JsonObject qoutaObject = rootObject.getJsonObject("quota");

        this.setTotalSize(qoutaObject.getJsonNumber("total").longValueExact());
        this.setFreeSize(qoutaObject.getJsonNumber("remaining").longValueExact());
    }
}
