package com.weather;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class ContentServer {
    private String serverUrl;
    private String filePath;
    private LamportClock clock;

    // Initializes URL, path, and Lamport clock
    public ContentServer(String serverUrl, String filePath) {
        this.serverUrl = serverUrl;
        this.filePath = filePath;
        this.clock = new LamportClock();
    }

    // Starts the content server and sends weather data
    public void start() {
        try {
            JSONObject weatherData = readWeatherDataFromFile();
            sendPutRequest(weatherData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Reads weather data from the specified file
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

    // Sends a PUT request with weather data
    private void sendPutRequest(JSONObject weatherData) throws Exception {
        clock.tick();
        long currentClock = clock.getValue();

        URL url = new URL(serverUrl + "/weather.json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Lamport-Clock", String.valueOf(clock.getValue()));
        conn.setDoOutput(true);

        byte[] postDataBytes = weatherData.toString().getBytes("UTF-8");
        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));

        System.out.println("Sending PUT request to: " + url);
        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
            writer.write(weatherData.toString());
            System.out.println("Sent data: " + weatherData.toString());
        }

        // Get the response code from the server
        int responseCode = conn.getResponseCode();
        System.out.println("PUT Response Code: " + responseCode);

        // Print the response body
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
            // Print the error response
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                System.out.println("Error response: " + errorResponse.toString());
            }
        }

        // Update Lamport clock
        String serverClockStr = conn.getHeaderField("Lamport-Clock");
        if (serverClockStr != null) {
            long serverClock = Long.parseLong(serverClockStr);
            clock.update(serverClock);
        }

        System.out.println("Local Lamport clock after request: " + clock.getValue());
    }

    // Main method
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ContentServer <server_url> <file_path>");
            return;
        }
        new ContentServer(args[0], args[1]).start();
    }
}
