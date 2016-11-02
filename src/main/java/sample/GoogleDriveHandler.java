package sample;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by rkoszegi on 01/11/2016.
 */
public class GoogleDriveHandler {

    private String accessToken;

    private static final int MAX_SMALL_UPLOAD_FILE_SIZE = 5 * 1000 * 1000;

    private static final int UPLOAD_PACKET_SIZE = 20 * 320 * 1024;//1 MiB

    public void login() {
        String url = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=839521493139-rpbb7aoacjre017qun9jb80432215bfp.apps.googleusercontent.com" +
                //TODO: átírni appfolderre
               // "&scope=https://www.googleapis.com/auth/drive.appfolder" +
                "&scope=https://www.googleapis.com/auth/drive" +
                "&response_type=code" +
                "&redirect_uri=urn:ietf:wg:oauth:2.0:oob:auto";

        WebLogin login = new WebLogin(url, "code=");
        login.showAndWait();

        getToken(login.getAuthCode());

        System.out.println("Access token: " + accessToken);
    }

    private void getToken(String authCode) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL("https://www.googleapis.com/oauth2/v4/token");
            connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            String content =  "code=" + authCode +
                    "&client_id=839521493139-rpbb7aoacjre017qun9jb80432215bfp.apps.googleusercontent.com" +
                    "&client_secret=pTBcmdJ5AsEdgFg52OftsuMs" +
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

            connection.setRequestProperty("Content-Length", Long.toString(file.length() + builder.length() + lastBoundary.length()));

            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.writeBytes(builder.toString());
            wr.write(Files.readAllBytes(file.toPath()),0, Math.toIntExact(file.length()));
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
            URL url = new URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable");
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
        int packageNumber = (totalSize / UPLOAD_PACKET_SIZE) + 1;
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

                int packetSize;
                int startByteNumber = currentPacketNr * UPLOAD_PACKET_SIZE;
                String rangeHeader;
                if (totalSize - uploadedBytesNr < UPLOAD_PACKET_SIZE) {
                    int endByteNumber = totalSize - 1;
                    rangeHeader = "bytes " + startByteNumber + "-" + endByteNumber + "/" + totalSize;
                    packetSize = totalSize - uploadedBytesNr;
                } else {
                    int endByteNumber = (currentPacketNr + 1) * UPLOAD_PACKET_SIZE - 1;
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

                System.out.println("Response: " + connection.getResponseCode() + " " + connection.getResponseMessage());

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

        //TODO: nextlink ha 200nal tobb van
    }



    private String getObjectFromJSONInput(InputStream inputStream, String name) {
        JsonReader jSonReader = Json.createReader(inputStream);
        JsonObject obj = jSonReader.readObject();
        return obj.getString(name);
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
