package hu.rkoszegi.jrasmus;

import hu.rkoszegi.jrasmus.exception.ServiceUnavailableException;
import hu.rkoszegi.jrasmus.exception.UnauthorizedException;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by rkoszegi on 26/11/2016.
 */
public class Request {

    private Map<String, String> requestHeaders = new HashMap<>();
    private String requestUrl;
    private byte[] requestData;
    private RequestType requestType;
    private Map<String, List<String>> responseHeaders;
    private byte[] responseData;
    private boolean isRequestData;
    private boolean isResponseData;
    private int responseCode;
    private String responseMessage;

    public Request() {
        /*requestData = new byte[0];*/
        isRequestData = false;
        isResponseData = false;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public void setRequestData(byte[] requestData) {
        this.isRequestData = true;
        this.requestData = requestData;
    }

    public byte[] getResponseData() {
        return responseData;
    }


    public boolean isRequestData() {
        return isRequestData;
    }

    public boolean isResponseData() {
        return isResponseData;
    }

    public void setInputData(boolean responseData) {
        isResponseData = responseData;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public void makeRequest() throws UnauthorizedException, ServiceUnavailableException {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(requestUrl);
            connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod(requestType.toString());

            Iterator iterator = requestHeaders.entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry<String, String> entry = (Map.Entry)iterator.next();
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            connection.setDoInput(true);
            connection.setUseCaches(false);

            if(isRequestData) {
                connection.setFixedLengthStreamingMode(requestData.length);
                connection.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream (
                        connection.getOutputStream());
                wr.write(requestData);
                wr.close();
            } else {
                if(requestType.equals(RequestType.POST)) {
                    connection.setDoOutput(true);
                    connection.setFixedLengthStreamingMode(0);
                } else {
                    connection.setRequestProperty("Content-Length", "0");
                }
            }

            responseCode = connection.getResponseCode();
            responseMessage = connection.getResponseMessage();

            System.out.println(responseCode + " " + responseMessage);

            if(responseCode == 401) {
                throw new UnauthorizedException("Client not authenticated");
            } else if(responseCode >= 500 ) {
                throw new ServiceUnavailableException("Server Error");
            }

            responseHeaders = connection.getHeaderFields();

            //printAllResponseHeaders(connection);

            if(isResponseData) {
                InputStream is = connection.getInputStream();
                try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[2048];
                    int size = is.read(buffer);
                    while (size != -1) {
                        baos.write(buffer, 0, size);
                        size = is.read(buffer);
                    }
                    responseData = baos.toByteArray();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            //TODO: nem vart hiba
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void addRequestHeader(String key, String value) {
        requestHeaders.put(key, value);
    }

    public String getResponseHeader(String key) {
        return responseHeaders.get(key).get(0);
    }

    public ByteArrayInputStream getResponseStream() {
        return new ByteArrayInputStream(responseData);
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
