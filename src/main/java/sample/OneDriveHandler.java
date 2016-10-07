package sample;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by Richárd on 2016.09.13..
 */
public class OneDriveHandler {

    private String accessToken;

    private final int packetSize = 20 * 320 * 1024;

    //TODO: ujraauthentikalast megirni
    public void login() {
        String url = "https://login.live.com/oauth20_authorize.srf?" +
                "client_id=000000004818EA82" +
                "&scope=onedrive.readwrite%2Cwl.signin%2Cwl.offline_access" +
                "&response_type=code" +
                "&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf";

        WebLogin login = new WebLogin(url, "code=");
        login.showAndWait();
        System.out.println("Flag: " + login.getFlag());

        getToken(login.getFlag());
    }

    private void getToken(String authCode) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL("https://login.live.com/oauth20_token.srf");
            connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            //connection.setRequestProperty("Content-Length", "0");
            /*connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("client_id", "000000004818EA82");
            connection.setRequestProperty("redirect_uri", "https://login.live.com/oauth20_desktop.srf");
            connection.setRequestProperty("code", authCode);
            connection.setRequestProperty("grant_type", "authorization_code");
            connection.setRequestProperty("client_secret", "ktbuoDz9sBvHlcjtPFRO4-Gh7fdI-bzH");*/


            String content = "client_id=000000004818EA82" +
                    "&redirect_uri=https://login.live.com/oauth20_desktop.srf&client_secret=ktbuoDz9sBvHlcjtPFRO4-Gh7fdI-bzH" +
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


            System.out.println("Response code: " + connection.getResponseCode());
            System.out.println("Response message: " + connection.getResponseMessage());


            //Print response body start
            /*BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            System.out.println("Response Str: " + response.toString());*/

            accessToken = getObjectFromJSONInput(connection.getInputStream(), "access_token");

            System.out.println("access_token: " + accessToken);


            //Print response package header start
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

            System.out.println("Builder");
            System.out.println(builder);

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
            String uploadFileName= file.getName();
            System.out.println("Old file name: " + uploadFileName);
            if(uploadFileName.contains(" ")) {
                uploadFileName = uploadFileName.replace(" ", "%20");
            }
            System.out.println("New file name: " + uploadFileName);

            URL url = new URL("https://api.onedrive.com/v1.0/drive/special/approot:/" + uploadFileName + ":/content");
            connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setRequestProperty("Authorization", "bearer " + accessToken);
            connection.setRequestProperty("Content-Length", Long.toString(file.length()));
            connection.setDoOutput(true);
            connection.setDoInput(true);

            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.write(Files.readAllBytes(file.toPath()),0, Math.toIntExact(file.length()));
            wr.close();

            System.out.println("Upload response: " + connection.getResponseCode());
            System.out.println(connection.getResponseMessage());

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            System.out.println("Response Str: " + response.toString());

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

    private void uploadLargeFile(File file) {
        System.out.println("uploadLargeFile called");

        String uploadLink = createUploadSession(file);

        uploadFragments(file, uploadLink);
    }

    private String createUploadSession(File file) {
        String uploadLink = null;
        HttpsURLConnection createUploadConnection = null;
        try {
            String uploadFileName= file.getName();
            if(uploadFileName.contains(" ")) {
                uploadFileName = uploadFileName.replace(" ", "%20");
            }

            URL url = new URL("https://api.onedrive.com/v1.0/drive/special/approot:/" + uploadFileName + ":/upload.createSession");
            createUploadConnection = (HttpsURLConnection) url.openConnection();

            createUploadConnection.setRequestMethod("POST");
            createUploadConnection.setRequestProperty("Content-Type", "text/plain");
            createUploadConnection.setRequestProperty("Authorization", "bearer " + accessToken);
            //connection.setDoOutput(true);
            createUploadConnection.setDoInput(true);

            //Content-Length helyett ez a ket sor
            createUploadConnection.setDoOutput(true);
            createUploadConnection.setFixedLengthStreamingMode(0);

            System.out.println("Upload response: " + createUploadConnection.getResponseCode());
            System.out.println(createUploadConnection.getResponseMessage());

            uploadLink = getObjectFromJSONInput(createUploadConnection.getInputStream(), "uploadUrl");

            //print result
            System.out.println("Upload Link: " + uploadLink);

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
        int packageNumber = ( totalSize % packetSize ) + 1;
        int uploadedBytesNr = 0;


        for(int currentPacketNr = 0; currentPacketNr < packageNumber; currentPacketNr++) {
            HttpsURLConnection connection = null;
            try {
                URL url = new URL(uploadLink);
                connection = (HttpsURLConnection) url.openConnection();

                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Authorization", "bearer " + accessToken);

                connection.setDoOutput(true);

                if(totalSize - uploadedBytesNr < packetSize) {
                    connection.setRequestProperty("Content-Length", Integer.toString(totalSize - uploadedBytesNr));
                    connection.setRequestProperty("Content-Range", "bytes " + Integer.toString(currentPacketNr * packetSize) + "-" + Integer.toString(totalSize - 1) + "/" + Integer.toString(totalSize));

                    DataOutputStream wr = new DataOutputStream(
                            connection.getOutputStream());
                    wr.write(Files.readAllBytes(file.toPath()), totalSize - uploadedBytesNr,currentPacketNr * packetSize);
                    wr.close();
                }
                else {
                    connection.setRequestProperty("Content-Length", Integer.toString(packetSize));
                    connection.setRequestProperty("Content-Range", "bytes " + Integer.toString(currentPacketNr * packetSize) + "-" + Integer.toString(currentPacketNr + 1) + "/" + Integer.toString(totalSize));

                    DataOutputStream wr = new DataOutputStream(
                            connection.getOutputStream());
                    wr.write(Files.readAllBytes(file.toPath()), packetSize,currentPacketNr * packetSize);
                    wr.close();
                }

                System.out.println(Integer.toString(currentPacketNr + 1) + ". fragment response: " + connection.getResponseCode());
                System.out.println(connection.getResponseMessage());


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                //openconn miatt
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
}
