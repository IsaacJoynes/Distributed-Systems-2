package com.weather;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import org.json.JSONObject;

public class AggregationServer {
    private static final int DEFAULT_PORT = 4567;
    private int port;
    private ConcurrentHashMap<String, WeatherData> weatherDataMap;
    private ConcurrentHashMap<String, Long> lastUpdateMap;
    private LamportClock lamportClock;

    // initialise port and data
    public AggregationServer(int port) {
        this.port = port;
        this.weatherDataMap = new ConcurrentHashMap<>();
        this.lastUpdateMap = new ConcurrentHashMap<>();
        this.lamportClock = new LamportClock();
    }

    // Starts the server
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Aggregation Server started on port " + port);

            // Periodic data removal
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(this::removeStaleData, 0, 5, TimeUnit.SECONDS);

            // Handle client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, this)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Removes data within 30 seconds
    private synchronized void removeStaleData() {
        long currentTime = System.currentTimeMillis();
        lastUpdateMap.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > 30000);
        weatherDataMap.keySet().retainAll(lastUpdateMap.keySet());
    }

    // Updates weather data and clock sync
    public synchronized void updateWeatherData(String stationId, WeatherData data, long clientLamportClock) {
        lamportClock.update(clientLamportClock);
        weatherDataMap.put(stationId, data);
        lastUpdateMap.put(stationId, System.currentTimeMillis());
    }

    // Collects the data
    public synchronized WeatherData getWeatherData(String stationId, long clientLamportClock) {
        lamportClock.update(clientLamportClock);
        return weatherDataMap.get(stationId);
    }

    // Main method
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new AggregationServer(port).start();
    }

    // Handles client requests
    class ClientHandler implements Runnable {
        private Socket clientSocket;
        private AggregationServer server;

        public ClientHandler(Socket socket, AggregationServer server) {
            this.clientSocket = socket;
            this.server = server;
        }

        // Process the client requests
        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                String inputLine = in.readLine();
                System.out.println("Received request: " + inputLine);

                if (inputLine != null) {
                    String[] parts = inputLine.split(" ");
                    if (parts.length >= 2) {
                        String method = parts[0];
                        String path = parts[1];

                        // Get Lamport clock value
                        String line;
                        long clientLamportClock = 0;
                        while ((line = in.readLine()) != null && !line.isEmpty()) {
                            if (line.startsWith("Lamport-Clock:")) {
                                try {
                                    clientLamportClock = Long.parseLong(line.split(":")[1].trim());
                                    lamportClock.update(clientLamportClock);
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid Lamport-Clock value: " + line);
                                }
                                break;
                            }
                        }

                        // Process GET or PUT requests
                        if ("GET".equalsIgnoreCase(method)) {
                            handleGetRequest(path, lamportClock.getValue(), out);
                        } else if ("PUT".equalsIgnoreCase(method)) {
                            handlePutRequest(in, lamportClock.getValue(), out);
                        } else {
                            out.println("HTTP/1.1 400 Bad Request");
                        }

                        lamportClock.tick();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Returns weather data as JSON
        private void handleGetRequest(String path, long clientLamportClock, PrintWriter out) {
            String stationId = null;
            if (path.contains("?id=")) {
                stationId = path.substring(path.lastIndexOf('=') + 1);
            }

            JSONObject responseData = new JSONObject();
            if (stationId != null) {
                WeatherData data = server.getWeatherData(stationId, clientLamportClock);
                if (data != null) {
                    responseData = data.getData();
                }
            } else {
                for (Map.Entry<String, WeatherData> entry : server.weatherDataMap.entrySet()) {
                    responseData.put(entry.getKey(), entry.getValue().getData());
                }
            }

            // Send response to client
            if (!responseData.isEmpty()) {
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println();
                out.println(responseData.toString());
            } else {
                out.println("HTTP/1.1 404 Not Found");
            }
        }

        // Handles PUT requests
        private void handlePutRequest(BufferedReader in, long clientLamportClock, PrintWriter out) throws Exception {
            StringBuilder payload = new StringBuilder();
            String line;
            int contentLength = 0;

            // Read headers to get content length
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }
            }

            // Read request body
            if (contentLength > 0) {
                char[] buffer = new char[contentLength];
                in.read(buffer, 0, contentLength);
                payload.append(buffer);
            }

            // Parse
            JSONObject jsonData = new JSONObject(payload.toString());
            String stationId = jsonData.getString("id");
            WeatherData weatherData = new WeatherData(jsonData);

            server.updateWeatherData(stationId, weatherData, clientLamportClock);

            // Respond to client
            out.println("HTTP/1.1 200 OK");
            out.println("{\"status\": \"success\"}");
        }
    }
}
