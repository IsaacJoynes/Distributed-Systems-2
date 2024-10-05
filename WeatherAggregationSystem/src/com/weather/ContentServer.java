package com.weather;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import com.weather.LamportClock;

public class ContentServer {
    private String serverUrl;
    private String filePath;
    private LamportClock clock;

    public ContentServer(String serverUrl, String filePath) {
        this.serverUrl = serverUrl;
        this.filePath = filePath;
        this.clock = new LamportClock();
    }

    public void start() {
        try {
            JSONObject weatherData = readWeatherDataFromFile();
            sendPutRequest(weatherData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONObject readWeatherDataFromFile() throws Exception {
        JSONObject weatherData = new JSONObject();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    weatherData.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return weatherData;
    }

    private void sendPutRequest(JSONObject weatherData) throws Exception {
        URL url = new URL(serverUrl + "/weather.json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Lamport-Clock", String.valueOf(clock.getValue()));
        conn.setDoOutput(true);

        // Convert the JSON object to bytes and set the Content-Length
        byte[] postDataBytes = weatherData.toString().getBytes("UTF-8");
        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));

        System.out.println("Sending PUT request to: " + url);
        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
            writer.write(weatherData.toString());
            System.out.println("Sent data: " + weatherData.toString());
        }

        // Check the server's response code
        int responseCode = conn.getResponseCode();
        System.out.println("PUT Response Code: " + responseCode);

        // Read the server's response if the request was successful (2xx response codes)
        if (responseCode >= 200 && responseCode < 300) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println("Response body: " + response.toString());
            }
        } else {
            // Handle errors if the response code is not 2xx
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                System.out.println("Error response: " + errorResponse.toString());
            }
        }

        conn.disconnect(); // Close the connection properly
        clock.tick(); // Increment Lamport clock after sending
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ContentServer <server_url> <file_path>");
            return;
        }
        new ContentServer(args[0], args[1]).start();
    }
}
