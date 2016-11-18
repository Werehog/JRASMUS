package hu.rkoszegi.jrasmus.handler;

import com.sun.deploy.net.URLEncoder;
import com.sun.org.apache.xpath.internal.SourceTree;
import hu.rkoszegi.jrasmus.model.GDriveFile;
import hu.rkoszegi.jrasmus.model.StoredFile;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;

/**
 * Created by rkoszegi on 01/11/2016.
 */
@Entity
@DiscriminatorValue("GOOGLEDRIVE")
public class GoogleDriveHandler extends BaseHandler {

    private static final long MAX_SMALL_FILE_SIZE = 5 * 1000 * 1000;

    public GoogleDriveHandler() {
        this.propertyFileName = "/GoogleDrive.properties";
    }

    protected void getToken(String authCode, String clientId, String clientSecret) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL("https://www.googleapis.com/oauth2/v4/token");
            connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            String content =  "code=" + authCode +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&redirect_uri=urn:ietf:wg:oauth:2.0:oob:auto" +
                   "&grant_type=authorization_code";

            //connection.setRequestProperty("Content-Length", Integer.toString(content.length()));
            connection.setFixedLengthStreamingMode(content.length());
            connection.setRequestProperty("content-type","application/x-www-form-urlencoded");

            //Kellenek-e
            connection.setUseCaches(false);
            connection.setDoOutput(true);//Post vagy putnal kell ha akarunk adatot kuldeni
            connection.setDoInput(true);//Kell ha a valaszbol olvasni akarunk

            //Send request
            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.writeBytes(content);
            wr.close();
            printAllResponseHeaders(connection);
            accessToken = getObjectFromJSONInput(connection.getInputStream(), "access_token");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            //openconn miatt
            e.printStackTrace();
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void uploadSmallFile(File file) {
        System.out.println("uploadSmallFile called");
        String boundary = "foo_bar_baz";
        HttpsURLConnection connection = null;
        try {
            URL url = new URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart");
            connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            connection.setDoOutput(true);

            StringBuilder builder = new StringBuilder();
            builder.append("--").append(boundary).append("\n");
            builder.append("Content-Type: application/json; charset=UTF-8\n\n");
            builder.append("{\"name\":\"" + file.getName() + "\"}\n\n");
            builder.append("--" + boundary + "\n");
            builder.append("Content-Type: text/plain\n\n");

            System.out.println(builder.toString());

            String lastBoundary = "\n--" + boundary + "--";

            byte[] data = encryptToOutputStream(new FileInputStream(file));

            connection.setRequestProperty("Content-Length", Long.toString(data.length + builder.length() + lastBoundary.length()));

            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.writeBytes(builder.toString());
            //wr.write(Files.readAllBytes(file.toPath()),0, Math.toIntExact(file.length()));
            wr.write(data);
            wr.writeBytes(lastBoundary);
            wr.close();

            System.out.println("Response: " + connection.getResponseCode());//ha nem hivom nem toltodik fel
            System.out.println("Response message: " + connection.getResponseMessage());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            //openconn miatt
            e.printStackTrace();
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    public void uploadLargeFile(File file) {
        System.out.println("uploadLargeFile called");
        String uploadLink = createUploadSession(file);
        System.out.println(uploadLink);
        uploadFragments(file, uploadLink);
    }

    private String createUploadSession(File file) {
        String uploadLink = null;
        HttpsURLConnection createUploadConnection = null;
        try {
            URL url = new URL("https://www.googleapis.com/upload/drive/v2/files?uploadType=resumable");
            createUploadConnection = (HttpsURLConnection) url.openConnection();

            createUploadConnection.setRequestMethod("POST");
            createUploadConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            createUploadConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
            createUploadConnection.setDoInput(true);

            String metadata = "{\"name\":\"" + file.getName() + "\"}";
            createUploadConnection.setRequestProperty("Content-Length", Integer.toString(metadata.length()));
            createUploadConnection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream (
                    createUploadConnection.getOutputStream());
            wr.writeBytes(metadata);
            wr.close();

            uploadLink = createUploadConnection.getHeaderField("Location");
            //printAllResponseHeaders(createUploadConnection);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            //openconn miatt
            e.printStackTrace();
        }
        /*finally {
            if (createUploadConnection != null) {
                createUploadConnection.disconnect();
            }
        }*/
        return uploadLink;
    }

    protected void uploadFragments(File file, String uploadLink) {
        int totalSize = Math.toIntExact(file.length());
        int packageNumber = Math.toIntExact(totalSize / UPLOAD_PACKET_SIZE) + 1;
        int uploadedBytesNr = 0;
        System.out.println("Package number: " + packageNumber);
        for (int currentPacketNr = 0; currentPacketNr < packageNumber; currentPacketNr++) {
            HttpsURLConnection connection = null;
            try {
                URL url = new URL(uploadLink);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setDoOutput(true);
                connection.setUseCaches(false);

                int packetSize;
                int startByteNumber = currentPacketNr * Math.toIntExact(UPLOAD_PACKET_SIZE);
                String rangeHeader;
                if (totalSize - uploadedBytesNr < UPLOAD_PACKET_SIZE) {
                    int endByteNumber = totalSize - 1;
                    rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + totalSize;
                    packetSize = totalSize - uploadedBytesNr;
                } else {
                    int endByteNumber = (currentPacketNr + 1) * Math.toIntExact(UPLOAD_PACKET_SIZE) - 1;
                    rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + totalSize;
                    packetSize = Math.toIntExact(UPLOAD_PACKET_SIZE);
                }
                System.out.println("Range header: " + rangeHeader);
                connection.setRequestProperty("Content-Range", rangeHeader);
                connection.setFixedLengthStreamingMode(packetSize);

                DataOutputStream wr = new DataOutputStream(
                        connection.getOutputStream());
                wr.write(Files.readAllBytes(file.toPath()), uploadedBytesNr, packetSize);
                wr.flush();
                wr.close();

                uploadedBytesNr += packetSize;
                System.out.println("Response: " + connection.getResponseCode() + " " + connection.getResponseMessage());
                printAllResponseHeaders(connection);


            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }


    public void listFolder() {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL("https://www.googleapis.com/drive/v2/files");
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            System.out.println("Response: " + connection.getResponseCode() + " " + connection.getResponseMessage());
            JsonReader jsonReader = Json.createReader(connection.getInputStream());
            JsonObject jsonObject = jsonReader.readObject();
            JsonArray array = jsonObject.getJsonArray("items");
            for(int i=0; i<array.size(); i++) {
                JsonObject object = array.getJsonObject(i);
                String out = "Name: " + object.getString("title");
                /*if(object.containsKey("folder")) {
                    out += " (folder)";
                }*/
                System.out.println(out);
            }

        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }

        //TODO: nextPageToken amig van még ahonnen ez jött
    }


    public void downloaFile(String fileName) {
        GDriveFile gDriveFile = getStoredFileData(fileName);
        if(gDriveFile.getSize() > MAX_SMALL_FILE_SIZE) {
            downloadLargeFile(gDriveFile);
        } else {
            downloadFileInOnePacket(gDriveFile);
        }
    }

    private GDriveFile getStoredFileData(String fileName) {
        HttpsURLConnection connection = null;
        GDriveFile gDriveFile = null;
        try {
            String query = "?q=" + URLEncoder.encode("name=\'" + fileName + "\'", "UTF-8") + "&fields=files(" + URLEncoder.encode("id,modifiedTime,name,size,webContentLink", "UTF-8") + ")";
            System.out.println("Query: " + query);
            URL url = new URL("https://www.googleapis.com/drive/v3/files" + query);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            System.out.println("Response: " + connection.getResponseCode() + " " + connection.getResponseMessage());
            JsonReader jsonReader = Json.createReader(connection.getInputStream());
            JsonObject jsonObject = jsonReader.readObject();
            JsonArray array = jsonObject.getJsonArray("files");
            System.out.println("Found files: " + array.size());
            for(int i=0; i<array.size(); i++) {
                JsonObject object = array.getJsonObject(i);
                String out = "Name: " + object.getString("name");
                System.out.println(out);
                System.out.println("Link: " + object.getString("webContentLink"));
                System.out.println("ID: " + object.getString("id"));
                //TODO:tobb talalat
                gDriveFile = new GDriveFile(object.getString("id"), object.getString("name"), object.getString("webContentLink"), Long.parseLong(object.getString("size")));
            }

        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
        return gDriveFile;
    }

    private void downloadFileInOnePacket(GDriveFile gDriveFile) {
        Cipher cipher = getDecryptorCipher();
        try (CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(new File(gDriveFile.getName())), cipher)) {
            URL url = new URL(gDriveFile.getDownloadUrl() + "?alt=media");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            InputStream inputStream = connection.getInputStream();
            decryptToOutputStream(cos, inputStream);
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeToFileFromInputStream(InputStream inputStream, FileOutputStream fileOutputStream) throws IOException {
        byte[] buf = new byte[512];
        while (true) {
            int len = inputStream.read(buf);
            if (len == -1) {
                break;
            }
            fileOutputStream.write(buf, 0, len);
        }
        fileOutputStream.flush();
    }

    private void downloadLargeFile(GDriveFile gDriveFile) {
        int downloadedByteNr = 0;
        int packetNumber = Math.toIntExact( gDriveFile.getSize() / DOWNLOAD_PACKET_SIZE) + 1;
        System.out.println("Packet number: " + packetNumber);
        int fileSize = Math.toIntExact(gDriveFile.getSize());

        String downloadUrl = gDriveFile.getDownloadUrl() + "?alt=media";

        if(fileSize > 25000000) {

            HttpsURLConnection connection = null;
            try {
                URL url = new URL(gDriveFile.getDownloadUrl() + "?alt=media");
                connection = (HttpsURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);

                byte[] buffer = new byte[5000];
                int bufferSize = 0;

                System.out.println("Response: " + connection.getResponseCode() + " " + connection.getResponseMessage());

                InputStream inputStream = connection.getInputStream();
                byte[] buf = new byte[512];
                while (true) {
                    int len = inputStream.read(buf);
                    if (len == -1) {
                        break;
                    }
                    for(int i = 0; i < len; i++) {
                        buffer[bufferSize++] = buf[i];
                    }
                }
                inputStream.read(buffer);
                String message = new String(buffer);
                int idIndex = message.indexOf("uc-download-link");
                message = message.substring(idIndex);
                int hrefIndex = message.indexOf("href=");
                int endIndex = message.indexOf("\">");
                String parsedUrl = (message.substring(hrefIndex + 6, endIndex)).replace("&amp;","&");
                System.out.println("ParsedUrl: " + parsedUrl);
                downloadUrl = "https://docs.google.com" + parsedUrl;
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Cipher cipher = getDecryptorCipher();

        try (CipherOutputStream cipherOutputStream = new CipherOutputStream(new FileOutputStream(new File(gDriveFile.getName())), cipher)) {
            URL url = new URL(downloadUrl);
            //URL url = new URL("https://drive.google.com/uc?export=download&confirm="+ URLEncoder.encode(storedFile.getId(), "UTF-8"));
            System.out.println("Link: " + url.toString());
            int encryptedFileSize = (fileSize / 16 + 1) * 16;
            while(downloadedByteNr < encryptedFileSize) {
                HttpsURLConnection connection = null;
                try {
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                    int currentPacketSize;
                    if (encryptedFileSize - downloadedByteNr > DOWNLOAD_PACKET_SIZE) {
                        currentPacketSize = DOWNLOAD_PACKET_SIZE;
                        String byteString = "bytes=" + downloadedByteNr + "-" + (downloadedByteNr + DOWNLOAD_PACKET_SIZE - 1);
                        System.out.println(byteString);
                        connection.setRequestProperty("Range", byteString);
                    } else {
                        currentPacketSize = encryptedFileSize - downloadedByteNr;
                        String byteString = "bytes=" + downloadedByteNr + "-" + (encryptedFileSize - 1);
                        System.out.println(byteString);
                        connection.setRequestProperty("Range", byteString);
                    }
                    System.out.println("Response: " + connection.getResponseCode() + " " + connection.getResponseMessage());
                    InputStream inputStream = connection.getInputStream();
                    //writeToFileFromInputStream(inputStream, fileOutputStream);
                    decryptToOutputStream(cipherOutputStream,inputStream);
                    inputStream.close();
                    downloadedByteNr += currentPacketSize;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void deleteFile(String fileName) {
        GDriveFile gDriveFile = getStoredFileData(fileName);
        try {
            URL url = new URL("https://www.googleapis.com/drive/v3/files/" + gDriveFile.getId());
            System.out.println("Link: " + url.toString());
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            //connection.setDoOutput(true);
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            System.out.println("Response: " + connection.getResponseCode() + " " + connection.getResponseMessage());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDriveMetaData() {
        try {
            URL url = new URL("https://www.googleapis.com/drive/v2/about");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setDoInput(true);

            JsonReader jSonReader = Json.createReader(connection.getInputStream());
            JsonObject rootObject = jSonReader.readObject();

            this.setTotalSize(Long.parseLong(rootObject.getString("quotaBytesTotal")));
            JsonArray array = rootObject.getJsonArray("quotaBytesByService");
            long usedSize = 0;
            for(int i = 0; i< array.size(); i++) {
                usedSize += Long.parseLong(array.getJsonObject(i).getString("bytesUsed"));
            }

            this.setFreeSize(this.getTotalSize() - usedSize);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
