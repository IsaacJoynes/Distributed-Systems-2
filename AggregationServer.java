import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public class AggregationServer {
    private static final int DEFAULT_PORT = 4567;
    private int port;
    private ConcurrentHashMap<String, WeatherData> weatherDataMap;
    private ConcurrentHashMap<String, Long> lastUpdateMap;
    private long lamportClock;

    public AggregationServer(int port) {
        this.port = port;
        this.weatherDataMap = new ConcurrentHashMap<>();
        this.lastUpdateMap = new ConcurrentHashMap<>();
        this.lamportClock = 0;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Aggregation Server started on port " + port);

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(this::removeStaleData, 0, 5, TimeUnit.SECONDS);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, this)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void removeStaleData() {
        long currentTime = System.currentTimeMillis();
        lastUpdateMap.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > 30000);
        weatherDataMap.keySet().retainAll(lastUpdateMap.keySet());
    }

    // Need to create methods for handling PUT and GET requests, updating Lamport clock here


    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new AggregationServer(port).start();
    }
}

class WeatherData {
    // Fields to store weather information
    // JSON parsing
}

class ClientHandler implements Runnable {
    // Handle client connections
}