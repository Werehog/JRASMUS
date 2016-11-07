package hu.rkoszegi.jrasmus;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by rkoszegi on 07/11/2016.
 */
public abstract class BaseHandler {

    protected String accessToken;

    protected static final int UPLOAD_PACKET_SIZE = 20 * 320 * 1024;//1 MiB
    protected static final int DOWNLOAD_PACKET_SIZE = 1048576;

    protected String propertyFileName;

    public void login() {
        Properties properties = new Properties();
        String clientId = null;
        String clientSecret = null;
        String loginUrl = null;
        String redirectUri = null;
        String scope = null;
        try(InputStream propertyInputStream = GoogleDriveHandler.class.getResourceAsStream(propertyFileName)){
            properties.load(propertyInputStream);
            clientId = properties.getProperty("clientId");
            clientSecret = properties.getProperty("clientSecret");
            loginUrl = properties.getProperty("loginUrl");
            redirectUri = properties.getProperty("redirectUri");
            scope = properties.getProperty("scope");
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

        getToken(login.getAuthCode(), clientId, clientSecret);
    }

    protected abstract void getToken(String authCode, String clientId, String clientSecret);

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

}
