import java.io.BufferedReader;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

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

        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
            writer.write(weatherData.toString());
        }

        int responseCode = conn.getResponseCode();
        System.out.println("PUT Response Code: " + responseCode);

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