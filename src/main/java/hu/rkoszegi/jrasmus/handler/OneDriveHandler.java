package hu.rkoszegi.jrasmus.handler;

import com.sun.deploy.net.URLEncoder;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import javax.persistence.Entity;
import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.*;

/**
 * Created by Richárd on 2016.09.13..
 */

//TODO:finally blokkok a connectionnak vagy using blokk
public class OneDriveHandler extends BaseHandler {


    public OneDriveHandler() {
        this.propertyFileName = "/OneDrive.properties";
    }

    //TODO: ujraauthentikalast megirni
    protected void getToken(String authCode, String clientId, String clientSecret) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL("https://login.live.com/oauth20_token.srf");
            connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            String content = "client_id=" + clientId +
                    "&redirect_uri=https://login.live.com/oauth20_desktop.srf" +
                    "&client_secret=" + clientSecret +
                    "&code=" + authCode + "&grant_type=authorization_code";

            connection.setRequestProperty("Content-Length", Integer.toString(content.length()));

            //Kellenek-e
            connection.setUseCaches(false);
            connection.setDoOutput(true);//Post vagy putnal kell ha akarunk adatot kuldeni
            connection.setDoInput(true);//Kell ha a valaszbol olvasni akarunk

            //Send request
            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.writeBytes(content);
            wr.close();

            accessToken = getObjectFromJSONInput(connection.getInputStream(), "access_token");

            //Print response package header start
            printAllResponseHeaders(connection);

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

    public void uploadFile(File file) {
        if(file.length() < 10000000) {
            uploadSmallFile(file);
        } else {
            uploadLargeFile(file);
        }
    }

    private void uploadSmallFile(File file) {
        System.out.println("uploadSmallFile called");
        HttpsURLConnection connection = null;
        try {
            String uploadFileName= replaceCharactersInFileName(file.getName());

            URL url = new URL("https://api.onedrive.com/v1.0/drive/special/approot:/" + uploadFileName + ":/content");
            connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setRequestProperty("Authorization", "bearer " + accessToken);
            connection.setDoOutput(true);

            ByteArrayInputStream bais = new ByteArrayInputStream(Files.readAllBytes(file.toPath()));
            byte[] data = encryptToOutputStream(bais);
            connection.setFixedLengthStreamingMode(data.length);

            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.write(data);

            wr.close();


            System.out.println(connection.getResponseCode());//ha nem hivom nem toltodik fel

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

    private String replaceCharactersInFileName(String fileName) {
        if(fileName.contains(" ")) {
            fileName = fileName.replace(" ", "%20");
        }
        return fileName;
    }

    private void uploadLargeFile(File file) {
        System.out.println("uploadLargeFile called");
        String uploadLink = createUploadSession(file);
        uploadFragments(file, uploadLink);
    }

    private String createUploadSession(File file) {
        String uploadLink = null;
        HttpsURLConnection createUploadConnection = null;
        try {
            //String uploadFileName= replaceCharactersInFileName(file.getName());
            String uploadFileName = URLEncoder.encode(file.getName(), "UTF-8");

            URL url = new URL("https://api.onedrive.com/v1.0/drive/special/approot:/" + uploadFileName + ":/upload.createSession");
            createUploadConnection = (HttpsURLConnection) url.openConnection();

            createUploadConnection.setRequestMethod("POST");
            createUploadConnection.setRequestProperty("Content-Type", "text/plain");
            createUploadConnection.setRequestProperty("Authorization", "bearer " + accessToken);
            createUploadConnection.setDoInput(true);

            //Content-Length helyett ez a ket sor
            createUploadConnection.setDoOutput(true);
            createUploadConnection.setFixedLengthStreamingMode(0);

            uploadLink = getObjectFromJSONInput(createUploadConnection.getInputStream(), "uploadUrl");

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            //openconn miatt
            e.printStackTrace();
        }
        finally {
            if (createUploadConnection != null) {
                createUploadConnection.disconnect();
            }
        }
        return uploadLink;
    }

    /*private void uploadFragments(File file, String uploadLink) {
        int totalFileSize = Math.toIntExact(file.length());
        int encryptedFileSize = (totalFileSize / 16 + 1) * 16;
        int packageNumber = ( encryptedFileSize / UPLOAD_PACKET_SIZE) + 1;
        int uploadedBytesNr = 0;
        int readBytesFromFile = 0;

        final int readBytesNumberFromFile = (UPLOAD_PACKET_SIZE / 16 - 1) * 16;

        try(FileInputStream fis = new FileInputStream(file)) {
            for (int currentPacketNr = 0; currentPacketNr < packageNumber; currentPacketNr++) {
                HttpsURLConnection connection = null;
                try {
                    URL url = new URL(uploadLink);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestMethod("PUT");
                    connection.setRequestProperty("Authorization", "bearer " + accessToken);
                    connection.setDoOutput(true);


                    int packetSize;
                    int startByteNumber = uploadedBytesNr;
                    String rangeHeader;
                    int readSize = 0;
                    if (encryptedFileSize - uploadedBytesNr < UPLOAD_PACKET_SIZE) {
                        int endByteNumber = encryptedFileSize - 1;
                        rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + encryptedFileSize;
                        packetSize = encryptedFileSize - uploadedBytesNr;
                        readSize = totalFileSize - readBytesFromFile;
                    } else {
                        int endByteNumber = startByteNumber + UPLOAD_PACKET_SIZE - 1;
                        rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + encryptedFileSize;
                        packetSize = UPLOAD_PACKET_SIZE;
                        readSize = readBytesNumberFromFile;
                    }
                    connection.setRequestProperty("Content-Range", rangeHeader);
                    connection.setFixedLengthStreamingMode(packetSize);

                    byte[] buffer = new byte[readSize];
                    fis.read(buffer, readBytesFromFile, readSize);
                    ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                    byte[] data = encryptToOutputStream(bais);

                    DataOutputStream wr = new DataOutputStream(
                            connection.getOutputStream());
                    wr.write(data);
                    wr.close();

                    readBytesFromFile += readSize;
                    uploadedBytesNr += packetSize;

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


    //FIle nevet convertalni itt es deletnel
    public void downloadFile(String fileName) {
        System.out.println("downloadFile called");
        String downloadUrl = getDownloadUrl(fileName);
        downloadContent(downloadUrl, fileName);
    }

    private String getDownloadUrl(String fileName) {
        String encodedFileName = null;
        try {
            encodedFileName = URLEncoder.encode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String downloadLink = "https://api.onedrive.com/v1.0/drive/special/approot:/" + encodedFileName + ":/content";
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(downloadLink);
            connection = (HttpsURLConnection) url.openConnection();

            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "bearer " + accessToken);

            String location = connection.getHeaderField("Content-Location");
            return location;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void downloadContent(String location, String fileName) {
        int fileSize = 0;

        Cipher cipher = getDecryptorCipher();
        try (CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(new File(fileName)), cipher)) {
            URL url = new URL(location);

            //FileOutputStream fileOutputStream = new FileOutputStream(new File(fileName));
            fileSize = downloadFirstPart(url, fileSize, cos);

            if(fileSize > DOWNLOAD_PACKET_SIZE) {
                int downloadedByteNr = DOWNLOAD_PACKET_SIZE;

                while(downloadedByteNr < fileSize) {
                   downloadedByteNr = downloadNextPart(url, downloadedByteNr, cos);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int downloadFirstPart(URL url, int fileSize, CipherOutputStream cipherOutputStream) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Range", "bytes=0-" + Integer.toString(DOWNLOAD_PACKET_SIZE - 1));

        String contentRange = connection.getHeaderField("Content-Range");
        fileSize = Integer.parseInt(contentRange.substring(contentRange.indexOf("/") + 1));

        InputStream inputStream = connection.getInputStream();
        decryptToOutputStream(cipherOutputStream, inputStream);
        inputStream.close();

        return fileSize;
    }

    private int downloadNextPart(URL url, int downloadedByteNr, CipherOutputStream cipherOutputStream) {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            int endByteNumber = downloadedByteNr + DOWNLOAD_PACKET_SIZE - 1;
            connection.setRequestProperty("Range", "bytes=" + downloadedByteNr + "-" + endByteNumber);

            int contentLength = connection.getContentLength();
            InputStream inputStream = connection.getInputStream();
            decryptToOutputStream(cipherOutputStream, inputStream);
            inputStream.close();

             return downloadedByteNr + contentLength;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return downloadedByteNr;
    }
    

    public void listFolder() {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL("https://api.onedrive.com/v1.0/drive/special/approot/children");
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "bearer " + accessToken);

            JsonReader jsonReader = Json.createReader(connection.getInputStream());
            JsonObject jsonObject = jsonReader.readObject();
            JsonArray array = jsonObject.getJsonArray("value");
            for(int i=0; i<array.size(); i++) {
                JsonObject object = array.getJsonObject(i);
                String out = "Name: " + object.getString("name");
                if(object.containsKey("folder")) {
                    out += " (folder)";
                }
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

        //TODO: nextlink ha 200nal tobb van
    }

    public void deleteFile(String fileName) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL("https://api.onedrive.com/v1.0/drive/special/approot:/" + fileName);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Authorization", "bearer " + accessToken);

            connection.getResponseCode();//ha nincs response code, akkor nem hajtodik vegre

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
    }
}
