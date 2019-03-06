package org.iota.ict.api;

import org.iota.ict.utils.IOHelper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class HttpGateway {

    private HttpGateway() {
    }

    public static String sendGetRequest(String urlString, Map<String, String> params, Map<String, String> requestProperties) {
        return sendRequest(urlString, RequestMethod.GET, params, requestProperties);
    }

    public static String sendPostRequest(String urlString, Map<String, String> params, Map<String, String> requestProperties) {
        return sendRequest(urlString, RequestMethod.POST, params, requestProperties);
    }

    public static String sendRequest(String urlString, RequestMethod method, Map<String, String> params, Map<String, String> requestProperties) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = createConnection(url, method);
            applyRequestProperties(connection, requestProperties);
            if(method == RequestMethod.POST)
                writeParams(connection, params);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new RuntimeException("Failed to connect to " + url + ". Bad response code: " + connection.getResponseCode());
            return IOHelper.readInputStream(connection.getInputStream());
        } catch (IOException t) {
            throw new RuntimeException(t);
        }
    }

    private static void applyRequestProperties(HttpURLConnection connection, Map<String, String> requestProperties) {
        for(Map.Entry<String, String> requestProperty : requestProperties.entrySet())
            connection.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
    }

    private static void writeParams(HttpURLConnection connection, Map<String, String> params) throws IOException {
        String queryString = generateQueryString(params);
        try(OutputStream os = connection.getOutputStream()) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
                writer.write(queryString);
                writer.flush();
            }
        }
    }

    private static String generateQueryString(Map<String, String> params) {
        StringBuilder queryString = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()) {
            if(!first)
                queryString.append("&");
            queryString.append(URLEncoder.encode(entry.getKey()))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue()));
            first = false;
        }
        return queryString.toString();
    }

    private static HttpURLConnection createConnection(URL url, RequestMethod method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) (url.openConnection());
        connection.setRequestMethod(method.name());
        connection.setReadTimeout(10000);
        connection.setConnectTimeout(10000);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }

    private enum RequestMethod {
        POST, GET
    }
}
