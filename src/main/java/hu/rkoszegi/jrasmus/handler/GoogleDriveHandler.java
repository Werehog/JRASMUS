package hu.rkoszegi.jrasmus.handler;

import com.sun.deploy.net.URLEncoder;
import hu.rkoszegi.jrasmus.Request;
import hu.rkoszegi.jrasmus.RequestType;
import hu.rkoszegi.jrasmus.model.GDriveFile;
import hu.rkoszegi.jrasmus.model.StoredFile;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.json.*;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.*;
import java.util.Properties;

/**
 * Created by rkoszegi on 01/11/2016.
 */
@Entity
@DiscriminatorValue("GOOGLEDRIVE")
public class GoogleDriveHandler extends BaseHandler {

    private static final long MAX_SMALL_FILE_SIZE = 5 * 1000 * 1000;

    public GoogleDriveHandler() {
        this.propertyFileName = "/GoogleDrive.properties";
        connectionTestLink = "www.drive.google.com";
    }

    @Override
    protected void getTokenImpl(String authCode, String clientId, String clientSecret) {
        Request request = new Request();
        request.setRequestUrl("https://www.googleapis.com/oauth2/v4/token");
        request.setRequestType(RequestType.POST);
        String content =  "code=" + authCode +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&redirect_uri=urn:ietf:wg:oauth:2.0:oob:auto" +
                "&grant_type=authorization_code";

        request.addRequestHeader("content-type","application/x-www-form-urlencoded");
        request.setRequestData(content.getBytes());
        request.setInputData(true);

        executeRequest(request);

        JsonReader jSonReader = Json.createReader(request.getResponseStream());
        JsonObject obj = jSonReader.readObject();
        accessToken = obj.getString("access_token");
        refreshToken = obj.getString("refresh_token");
    }

    @Override
    protected void uploadSmallFile(File file, String uploadedFileName) {
        System.out.println("uploadSmallFile called");
        String boundary = "foo_bar_baz";
        Request request = new Request();
        request.setRequestUrl("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart");

        request.setRequestType(RequestType.POST);
        request.addRequestHeader("Content-Type", "multipart/related; boundary=" + boundary);
        request.addRequestHeader("Authorization", "Bearer " + accessToken);

        StringBuilder builder = new StringBuilder();
        builder.append("--").append(boundary).append("\n");
        builder.append("Content-Type: application/json; charset=UTF-8\n\n");
        builder.append("{\"name\":\"" + uploadedFileName + "\"}\n\n");
        builder.append("--" + boundary + "\n");
        builder.append("Content-Type: text/plain\n\n");

        String lastBoundary = "\n--" + boundary + "--";

        try(FileInputStream fis = new FileInputStream(file)){
            byte[] data = encryptToOutputStream(fis);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(builder.toString().getBytes());
            outputStream.write(data);
            outputStream.write(lastBoundary.getBytes());
            request.setRequestData(outputStream.toByteArray());

            executeRequest(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void uploadLargeFile(File file, String uploadedFileName) {
        System.out.println("uploadLargeFile called");
        String uploadLink = createUploadSession(file, uploadedFileName);
        System.out.println(uploadLink);
        uploadFragments(file, uploadLink);
    }

    private String createUploadSession(File file, String uploadedFileName) {
        Request request = new Request();
        request.setRequestUrl("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable");
        request.setRequestType(RequestType.POST);

        request.addRequestHeader("Content-Type", "application/json; charset=UTF-8");
        request.addRequestHeader("Authorization", "Bearer " + accessToken);
        request.addRequestHeader("X-Upload-Content-Type", "text/plain");
        request.setInputData(true);
        String metadata = "{\"name\":\"" + uploadedFileName + "\"}";
        request.setRequestData(metadata.getBytes());

        executeRequest(request);

        return  request.getResponseHeader("Location");
    }

    protected void uploadFragments(File file, String uploadLink) {

        long UPLOAD_PACKET_SIZE = 256 * 1024 * 7;

        long totalFileSize = file.length();
        long encryptedFileSize = (totalFileSize / 16 + 1) * 16;
        long packageNumber = 0;

        int uploadedBytesNr = 0;

        try(CipherInputStream cis = new CipherInputStream(new FileInputStream(file), getEncryptorCipher())) {
            while(uploadedBytesNr < encryptedFileSize) {
                System.out.println((++packageNumber) + ". package");

                try {
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
                        packetSize =Math.toIntExact(encryptedFileSize - uploadedBytesNr);
                    } else {
                        long endByteNumber = startByteNumber + UPLOAD_PACKET_SIZE - 1;
                        rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + encryptedFileSize;
                        packetSize = Math.toIntExact(UPLOAD_PACKET_SIZE);
                    }
                    request.addRequestHeader("Content-Range", rangeHeader);

                    int pckCounter = packetSize;
                    byte[] b = new byte[packetSize];
                    while(pckCounter - 16 >= 0) {
                        cis.read(b, packetSize - pckCounter,16);
                        pckCounter-=16;
                    }

                    if(pckCounter != 0) {
                        int remainedByteNr = pckCounter % 16;
                        cis.read(b, packetSize - remainedByteNr, remainedByteNr);
                    }
                    request.setRequestData(b);

                    executeRequest(request);

                    if(request.getResponseCode() == 200)
                        break;

                    String responseRangeHeader = request.getResponseHeader("Range");
                    String nextStartIndexString = responseRangeHeader.substring(responseRangeHeader.indexOf("-") + 1);
                    int nextStartIndex = Integer.parseInt(nextStartIndexString);

                    uploadedBytesNr = nextStartIndex + 1;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO: utolso uzenetet olvasni
    }


    @Override
    protected void refreshTokenImpl() {
        System.out.println("refreshToken called");
        System.out.println("Old access token: " + accessToken);

        Properties properties = new Properties();
        String clientId;
        String clientSecret;
        try(InputStream propertyInputStream = BaseHandler.class.getResourceAsStream( propertyFileName)){
            properties.load(propertyInputStream);
            clientId = properties.getProperty("clientId");
            clientSecret = properties.getProperty("clientSecret");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Request request = new Request();
        request.setRequestUrl("https://www.googleapis.com/oauth2/v4/token");
        request.setRequestType(RequestType.POST);

        String content = "client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&refresh_token=" + refreshToken +
                "&grant_type=refresh_token";

        request.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setRequestData(content.getBytes());
        request.setInputData(true);

        executeRequest(request);

        JsonReader jSonReader = Json.createReader(request.getResponseStream());
        JsonObject obj = jSonReader.readObject();
        accessToken = obj.getString("access_token");
        System.out.println("NEW Access token: " + accessToken);
    }

    @Override
    protected void listFolderImpl() {
        Request request = new Request();
        request.setRequestUrl("https://www.googleapis.com/drive/v3/files");
        request.setRequestType(RequestType.GET);
        request.addRequestHeader("Authorization", "Bearer " + accessToken);
        request.setInputData(true);

        executeRequest(request);

        JsonReader jsonReader = Json.createReader(request.getResponseStream());
        JsonObject jsonObject = jsonReader.readObject();
        JsonArray array = jsonObject.getJsonArray("files");
        for(int i=0; i<array.size(); i++) {
            JsonObject object = array.getJsonObject(i);
            String out = "Name: " + object.getString("name");
                /*if(object.containsKey("folder")) {
                    out += " (folder)";
                }*/
            System.out.println(out);
        }
        //TODO: nextPageToken amig van még ahonnen ez jött
    }

    @Override
    protected void downloadFileImpl(StoredFile storedFile) {
        GDriveFile gDriveFile = getStoredFileData(storedFile.getUploadName());
        String newFilePath = storedFile.getPath() + "\\" + storedFile.getDecodedUploadName();
        if(gDriveFile.getSize() > MAX_SMALL_FILE_SIZE) {
            downloadLargeFile(gDriveFile, newFilePath);
        } else {
            downloadFileInOnePacket(gDriveFile, newFilePath);
        }
    }

    private GDriveFile getStoredFileData(String fileName) {
        GDriveFile gDriveFile = null;
        Request request = new Request();
        String query = null;
        try {
            query = "?q=" + URLEncoder.encode("name=\'" + fileName + "\'", "UTF-8") + "&fields=files(" + URLEncoder.encode("id,modifiedTime,name,size,webContentLink", "UTF-8") + ")";
        } catch (IOException e) {
            e.printStackTrace();
        }
        request.setRequestUrl("https://www.googleapis.com/drive/v3/files" + query);
        request.setRequestType(RequestType.GET);
        request.addRequestHeader("Authorization", "Bearer " + accessToken);
        request.setInputData(true);

        executeRequest(request);

        JsonReader jsonReader = Json.createReader(request.getResponseStream());
        JsonObject jsonObject = jsonReader.readObject();
        JsonArray array = jsonObject.getJsonArray("files");
        for(int i=0; i<array.size(); i++) {
            JsonObject object = array.getJsonObject(i);
            //TODO:tobb talalat
            gDriveFile = new GDriveFile(object.getString("id"), object.getString("name"), object.getString("webContentLink"), Long.parseLong(object.getString("size")));
        }
        return gDriveFile;
    }

    private void downloadFileInOnePacket(GDriveFile gDriveFile, String newFilePath) {
        Cipher cipher = getDecryptorCipher();
        try (CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(new File(newFilePath)), cipher)) {
            Request request = new Request();
            request.setRequestUrl(gDriveFile.getDownloadUrl() + "?alt=media");
            request.setInputData(true);
            request.setRequestType(RequestType.GET);
            request.addRequestHeader("Authorization", "Bearer " + accessToken);

            executeRequest(request);

            decryptToOutputStream(cos, request.getResponseStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadLargeFile(GDriveFile gDriveFile, String newFilePath) {
        int downloadedByteNr = 0;
        int packetNumber = Math.toIntExact( gDriveFile.getSize() / DOWNLOAD_PACKET_SIZE) + 1;
        System.out.println("Packet number: " + packetNumber);
        int fileSize = Math.toIntExact(gDriveFile.getSize());

        String downloadUrl = gDriveFile.getDownloadUrl() + "?alt=media";

        if(fileSize > 25000000) {
            Request request = new Request();
            request.setRequestUrl(gDriveFile.getDownloadUrl() + "?alt=media");
            request.setRequestType(RequestType.GET);
            request.addRequestHeader("Authorization", "Bearer " + accessToken);
            request.setInputData(true);
            executeRequest(request);

            String message = new String(request.getResponseData());
            int idIndex = message.indexOf("uc-download-link");
            message = message.substring(idIndex);
            int hrefIndex = message.indexOf("href=");
            int endIndex = message.indexOf("\">");
            String parsedUrl = (message.substring(hrefIndex + 6, endIndex)).replace("&amp;","&");
            System.out.println("ParsedUrl: " + parsedUrl);
            downloadUrl = "https://docs.google.com" + parsedUrl;
        }

        Cipher cipher = getDecryptorCipher();



        try (CipherOutputStream cipherOutputStream = new CipherOutputStream(new FileOutputStream(new File(newFilePath)), cipher)) {
            int encryptedFileSize = (fileSize / 16 + 1) * 16;
            while(downloadedByteNr < encryptedFileSize) {
                Request request = new Request();
                request.setRequestUrl(downloadUrl);
                request.setRequestType(RequestType.GET);
                request.addRequestHeader("Authorization", "Bearer " + accessToken);
                String byteString;
                int currentPacketSize;
                if (encryptedFileSize - downloadedByteNr > DOWNLOAD_PACKET_SIZE) {
                    currentPacketSize = DOWNLOAD_PACKET_SIZE;
                    byteString = "bytes=" + downloadedByteNr + "-" + (downloadedByteNr + DOWNLOAD_PACKET_SIZE - 1);
                } else {
                    currentPacketSize = encryptedFileSize - downloadedByteNr;
                    byteString = "bytes=" + downloadedByteNr + "-" + (encryptedFileSize - 1);
                }
                request.addRequestHeader("Range", byteString);
                request.setInputData(true);

                executeRequest(request);

                decryptToOutputStream(cipherOutputStream,request.getResponseStream());
                downloadedByteNr += currentPacketSize;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void deleteFileImpl(String fileName) {
        GDriveFile gDriveFile = getStoredFileData(fileName);
        Request request = new Request();
        request.setRequestUrl("https://www.googleapis.com/drive/v3/files/" + gDriveFile.getId());
        request.setRequestType(RequestType.DELETE);
        request.addRequestHeader("Authorization", "Bearer " + accessToken);
        executeRequest(request);
    }

    @Override
    protected void setDriveMetaDataImpl() {
        Request request = new Request();
        request.setRequestUrl("https://www.googleapis.com/drive/v2/about");
        request.setRequestType(RequestType.GET);
        request.addRequestHeader("Authorization", "Bearer " + accessToken);
        request.setInputData(true);

        executeRequest(request);

        JsonReader jSonReader = Json.createReader(request.getResponseStream());
        JsonObject rootObject = jSonReader.readObject();

        this.setTotalSize(Long.parseLong(rootObject.getString("quotaBytesTotal")));
        JsonArray array = rootObject.getJsonArray("quotaBytesByService");
        long usedSize = 0;
        for(int i = 0; i< array.size(); i++) {
            usedSize += Long.parseLong(array.getJsonObject(i).getString("bytesUsed"));
        }

        this.setFreeSize(this.getTotalSize() - usedSize);
    }
}
