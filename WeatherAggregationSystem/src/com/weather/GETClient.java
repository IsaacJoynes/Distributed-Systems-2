package com.weather;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class GETClient {
    private String serverUrl;
    private String stationId;
    private LamportClock clock;

    // Initialise the URL, ID, and Lamport clock
    public GETClient(String serverUrl, String stationId) {
        this.serverUrl = serverUrl;
        this.stationId = stationId;
        this.clock = new LamportClock();
    }

    // Starts the GET request
    public void start() {
        try {
            JSONObject weatherData = sendGetRequest();
            displayWeatherData(weatherData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Sends a GET request to retrieve data
    private JSONObject sendGetRequest() throws Exception {
        URL url = new URL(serverUrl + "/weather.json" + (stationId != null ? "?id=" + stationId : ""));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Lamport-Clock", String.valueOf(clock.getValue()));

        // Get the response code from the server
        int responseCode = conn.getResponseCode();
        System.out.println("GET Response Code: " + responseCode);

        // Read the response
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                clock.tick();
                return new JSONObject(response.toString());
            }
        }
        return null;
    }

    // Displays data in the console
    private void displayWeatherData(JSONObject weatherData) {
        if (weatherData != null) {
            for (String key : weatherData.keySet()) {
                System.out.println(key + ": " + weatherData.get(key));
            }
        } else {
            System.out.println("No weather data available.");
        }
    }

    // Main method
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: java GETClient <server_url> [station_id]");
            return;
        }
        String stationId = args.length == 2 ? args[1] : null;
        new GETClient(args[0], stationId).start();
    }
}
