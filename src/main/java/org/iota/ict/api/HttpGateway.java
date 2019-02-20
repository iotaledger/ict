package org.iota.ict.api;

import org.iota.ict.utils.IOHelper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpGateway {

    private HttpGateway() {
    }

    /*
    TODO reimplement for GitHub requests

    public static String sendGithubPostRequest(String path) {
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
    }*/

    public static String sendPostRequest(String path) {
        return sendPostRequest(path, new HashMap<String, String>());
    }

    public static String sendPostRequest(String path, Map<String, String> params) {
        try {
            URL url = new URL(path);
            HttpURLConnection connection = connect(url);
            writeParams(connection, params);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new RuntimeException("Failed to connect to " + url + ". Bad response code: " + connection.getResponseCode());
            return IOHelper.readInputStream(connection.getInputStream());
        } catch (IOException t) {
            throw new RuntimeException(t);
        }
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

    private static HttpURLConnection connect(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) (url.openConnection());
        connection.setRequestMethod("POST");
        connection.setReadTimeout(10000);
        connection.setConnectTimeout(10000);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }
}
