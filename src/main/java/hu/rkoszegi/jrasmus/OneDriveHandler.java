package hu.rkoszegi.jrasmus;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Richárd on 2016.09.13..
 */

//TODO:finally blokkok a connectionnak vagy using blokk
public class OneDriveHandler {

    private String accessToken;

    private static final int UPLOAD_PACKET_SIZE = 20 * 320 * 1024;//1 MiB
    private static final int DOWNLOAD_PACKET_SIZE = 1048576;

    //TODO: ujraauthentikalast megirni
    public void login() {

        String clientId = null;
        String clientSecret = null;
        Properties properties = new Properties();
        try (InputStream propertyInputStream = OneDriveHandler.class.getResourceAsStream("/OneDrive.properties")) {
            properties.load(propertyInputStream);
            clientId = properties.getProperty("clientId");
            clientSecret = properties.getProperty("clientSecret");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String url = "https://login.live.com/oauth20_authorize.srf?" +
                "client_id=" + clientId +
                "&scope=onedrive.readwrite%2Cwl.signin%2Cwl.offline_access" +
                "&response_type=code" +
                "&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf";

        WebLogin login = new WebLogin(url, "code=");
        login.showAndWait();

        getToken(login.getAuthCode(), clientId, clientSecret);
    }

    private void getToken(String authCode, String clientId, String clientSecret) {
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

    private String getObjectFromJSONInput(InputStream inputStream, String name) {
        JsonReader jSonReader = Json.createReader(inputStream);
        JsonObject obj = jSonReader.readObject();
        return obj.getString(name);
    }

    public void uploadFile(File file) {
        if(file.length() < 100000000) {
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
            connection.setRequestProperty("Content-Length", Long.toString(file.length()));
            connection.setDoOutput(true);
            //connection.setDoInput(true);



            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.write(Files.readAllBytes(file.toPath()),0, Math.toIntExact(file.length()));
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
            String uploadFileName= replaceCharactersInFileName(file.getName());

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

    private void uploadFragments(File file, String uploadLink) {
        int totalSize = Math.toIntExact(file.length());
        int packageNumber = ( totalSize / UPLOAD_PACKET_SIZE) + 1;
        int uploadedBytesNr = 0;

        for(int currentPacketNr = 0; currentPacketNr < packageNumber; currentPacketNr++) {
            HttpsURLConnection connection = null;
            try {
                URL url = new URL(uploadLink);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Authorization", "bearer " + accessToken);
                connection.setDoOutput(true);

                int packetSize;
                int startByteNumber = currentPacketNr * UPLOAD_PACKET_SIZE;
                String rangeHeader;
                if(totalSize - uploadedBytesNr < UPLOAD_PACKET_SIZE) {
                    int endByteNumber = totalSize - 1;
                    rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + totalSize;
                    packetSize = totalSize - uploadedBytesNr;
                } else {
                    int endByteNumber = (currentPacketNr+1)* UPLOAD_PACKET_SIZE - 1;
                    rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + totalSize;
                    packetSize = UPLOAD_PACKET_SIZE;
                }
                connection.setRequestProperty("Content-Range", rangeHeader);
                connection.setFixedLengthStreamingMode(packetSize);

                DataOutputStream wr = new DataOutputStream(
                        connection.getOutputStream());
                wr.write(Files.readAllBytes(file.toPath()), uploadedBytesNr, packetSize);
                wr.close();

                uploadedBytesNr += packetSize;

            } catch (IOException e ) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
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


    //FIle nevet convertalni itt es deletnel
    public void downloadFile(String fileName) {
        System.out.println("downloadFile called");
        String downloadUrl = getDownloadUrl(fileName);
        downloadContent(downloadUrl, fileName);
    }

    private String getDownloadUrl(String fileName) {
        String downloadLink = "https://api.onedrive.com/v1.0/drive/special/approot:/" + fileName + ":/content";
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

        try {
            URL url = new URL(location);
            FileOutputStream fileOutputStream = new FileOutputStream(new File(fileName));
            fileSize = downloadFirstPart(url, fileSize, fileOutputStream);

            if(fileSize > DOWNLOAD_PACKET_SIZE) {
                int downloadedByteNr = DOWNLOAD_PACKET_SIZE;

                while(downloadedByteNr < fileSize) {
                   downloadedByteNr = downloadNextPart(url, downloadedByteNr, fileOutputStream);
                }
            }

            fileOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int downloadFirstPart(URL url, int fileSize, FileOutputStream fileOutputStream) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Range", "bytes=0-" + Integer.toString(DOWNLOAD_PACKET_SIZE - 1));

        String contentRange = connection.getHeaderField("Content-Range");
        fileSize = Integer.parseInt(contentRange.substring(contentRange.indexOf("/") + 1));

        InputStream inputStream = connection.getInputStream();
        writeToFileFromInputStream(inputStream, fileOutputStream);
        inputStream.close();

        return fileSize;
    }

    private int downloadNextPart(URL url, int downloadedByteNr, FileOutputStream fileOutputStream) {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            int endByteNumber = downloadedByteNr + DOWNLOAD_PACKET_SIZE - 1;
            connection.setRequestProperty("Range", "bytes=" + downloadedByteNr + "-" + endByteNumber);

            int contentLength = connection.getContentLength();
            InputStream inputStream = connection.getInputStream();
            writeToFileFromInputStream(inputStream, fileOutputStream);
            inputStream.close();

             return downloadedByteNr + contentLength;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return downloadedByteNr;
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

    private void printAllResponseHeaders(HttpsURLConnection connection) throws IOException {
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

    private String readResponseBody(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }
}
