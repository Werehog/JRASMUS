package sample;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.*;
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
        int packageNumber = ( totalSize / packetSize ) + 1;
        int uploadedBytesNr = 0;

        ProgressWindow progressWindow = new ProgressWindow("Uploading file");

        System.out.println("Package nr: " + packageNumber);
        for(int currentPacketNr = 0; currentPacketNr < packageNumber; currentPacketNr++) {
            System.out.println(Integer.toString(currentPacketNr +1) + ". Package:");
            HttpsURLConnection connection = null;
            try {
                URL url = new URL(uploadLink);
                connection = (HttpsURLConnection) url.openConnection();

                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Authorization", "bearer " + accessToken);

                connection.setDoOutput(true);

                if(totalSize - uploadedBytesNr < packetSize) {
                    //connection.setRequestProperty("Content-Length", Integer.toString(totalSize - uploadedBytesNr));
                    String rangeHeader = "bytes " + Integer.toString(currentPacketNr * packetSize) + "-" + Integer.toString(totalSize - 1) + "/" + Integer.toString(totalSize);
                    connection.setRequestProperty("Content-Range", rangeHeader);
                    System.out.println("Content-Range: " + rangeHeader);

                    connection.setFixedLengthStreamingMode(totalSize - uploadedBytesNr);

                    DataOutputStream wr = new DataOutputStream(
                            connection.getOutputStream());
                    wr.write(Files.readAllBytes(file.toPath()), uploadedBytesNr, totalSize - uploadedBytesNr);
                    wr.close();

                    uploadedBytesNr = totalSize;
                }
                else {
                    //connection.setRequestProperty("Content-Length", Integer.toString(packetSize));
                    String rangeHeader = "bytes " + Integer.toString(currentPacketNr * packetSize) + "-" + Integer.toString((currentPacketNr+1)*packetSize - 1) + "/" + Integer.toString(totalSize);
                    connection.setRequestProperty("Content-Range", rangeHeader);
                    System.out.println("Content-Range: " + rangeHeader);

                    connection.setFixedLengthStreamingMode(packetSize);

                    DataOutputStream wr = new DataOutputStream(
                            connection.getOutputStream());
                    wr.write(Files.readAllBytes(file.toPath()), uploadedBytesNr, packetSize);
                    wr.close();

                    uploadedBytesNr += packetSize;
                }

                double progressPercent = ((double)uploadedBytesNr) / totalSize;
                System.out.println(Double.toString(progressPercent) + " percent");
                progressWindow.setProgress(progressPercent);

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

        progressWindow.close();

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

    public void downloadFile(String fileName) {
        System.out.println("downloadFile called");

        String downloadUrl = getDownloadUrl(fileName);

        downloadContent(downloadUrl, fileName);
    }

    private String getDownloadUrl(String fileName) {
        String downloadLink = "https://api.onedrive.com/v1.0/drive/special/approot:/" + fileName + ":/content";
        URL url = null;
        HttpsURLConnection connection = null;
        try {
            url = new URL(downloadLink);

            connection = (HttpsURLConnection) url.openConnection();

            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "bearer " + accessToken);



            System.out.println(connection.getResponseCode());
            System.out.println(connection.getResponseMessage());

            String location = connection.getHeaderField("Content-Location");
            return location;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void downloadContent(String location, String fileName) {
        int packetSize = 1048576; //1 MiB
        URL url = null;
        HttpsURLConnection connection = null;
        try {
            url = new URL(location);

            connection = (HttpsURLConnection) url.openConnection();

            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Range", "bytes=0-" + Integer.toString(packetSize - 1));

            printAllResponseHeaders(connection);

            String contentRange = connection.getHeaderField("Content-Range");

            int fileSize = Integer.parseInt(contentRange.substring(contentRange.indexOf("/") + 1));
            System.out.println("FileSize: " + fileSize);


            int contentLength = connection.getContentLength();
            System.out.println("Content length from code: " + contentLength);
            InputStream is = connection.getInputStream();
            FileOutputStream fos = new FileOutputStream(new File(fileName));
            byte[] buf = new byte[512];
            while (true) {
                int len = is.read(buf);
                if (len == -1) {
                    break;
                }
                fos.write(buf, 0, len);
            }
            is.close();
            fos.flush();

            if(fileSize > packetSize) {
                int downloadedByteNr = packetSize;

                while(downloadedByteNr < fileSize) {
                    URL url2 = null;
                    HttpsURLConnection connection2 = null;
                    try {
                        url2 = new URL(location);

                        connection2 = (HttpsURLConnection) url2.openConnection();

                        connection2.setDoOutput(true);
                        connection2.setRequestMethod("GET");
                        connection2.setRequestProperty("Range", "bytes=" + Integer.toString(downloadedByteNr) + "-" + Integer.toString(downloadedByteNr + packetSize - 1));

                        printAllResponseHeaders(connection2);

                        int contentLength2 = connection2.getContentLength();
                        InputStream is2 = connection2.getInputStream();
                        while (true) {
                            int len = is2.read(buf);
                            if (len == -1) {
                                break;
                            }
                            fos.write(buf, 0, len);
                        }
                        is2.close();
                        fos.flush();

                        downloadedByteNr += contentLength2;

                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            fos.close();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
}
