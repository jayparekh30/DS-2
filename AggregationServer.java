
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import org.json.JSONObject;
import java.util.concurrent.atomic.AtomicLong;

public class AggregationServer {

    private static final int SERVER_PORT = 4567;  
    private static final int DATA_EXPIRY_TIME_MS = 30000;  
    private static final ConcurrentHashMap<String, WeatherRecord> weatherDataMap = new ConcurrentHashMap<>();  
    private static final AtomicLong lamportTimestamp = new AtomicLong(0);  

    public static void main(String[] args) {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : SERVER_PORT;
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server running on port " + port);
            initiateDataCleanupTask();  

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> processClientRequest(clientSocket)).start();  
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

// It communicate with Client
    private static void processClientRequest(Socket clientSocket) {
        try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter outputWriter = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String clientRequest = inputReader.readLine();
            System.out.println("Request received: " + clientRequest);

            if (clientRequest.startsWith("GET")) {
                handleGetRequest(outputWriter);
            } else if (clientRequest.startsWith("PUT")) {
                handlePutRequest(inputReader, outputWriter);
            } else {
                outputWriter.println("HTTP/1.1 400 Bad Request");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

// Handle Get Request
private static void handleGetRequest(PrintWriter output) {
        lamportTimestamp.incrementAndGet();  // Increment lamport clock

        JSONObject jsonResponse = new JSONObject();
        for (WeatherRecord record : weatherDataMap.values()) {
            jsonResponse.put(record.getWeatherData().getString("id"), record.getWeatherData());
        }

        output.println("HTTP/1.1 200 OK");
        output.println("Content-Type: application/json");
        output.println();
        output.println(jsonResponse.toString(4));
    }
    

    private static void handlePutRequest(BufferedReader input, PrintWriter output) throws IOException {
        lamportTimestamp.incrementAndGet();  // Increment lamport clock

        String line;
        int contentLength = 0;

        while (!(line = input.readLine()).isEmpty()) {
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }

        if (contentLength == 0) {
            System.out.println("Error: Content-Length is 0. No data provided.");
            output.println("HTTP/1.1 400 Bad Request");
            return;
        }

        char[] body = new char[contentLength];
        input.read(body, 0, contentLength);
        String receivedJsonString = new String(body);

        try {
            JSONObject weatherJson = new JSONObject(receivedJsonString);
            String id = weatherJson.getString("id");

            weatherDataMap.put(id, new WeatherRecord(weatherJson, System.currentTimeMillis())); 
            System.out.println("Weather data stored for ID: " + id);

            output.println("HTTP/1.1 201 Created");
        } catch (Exception e) {
            System.err.println("Error processing PUT request: " + e.getMessage());
            e.printStackTrace();
            output.println("HTTP/1.1 500 Internal Server Error");
        }
    }


   private static void initiateDataCleanupTask() {
        ScheduledExecutorService cleanupScheduler = Executors.newScheduledThreadPool(1);
        cleanupScheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            weatherDataMap.entrySet().removeIf(entry -> {
                boolean shouldRemove = (currentTime - entry.getValue().getTimestamp()) > DATA_EXPIRY_TIME_MS;
                if (shouldRemove) {
                    System.out.println("Removing expired data with ID: " + entry.getKey());
                }
                return shouldRemove;
            });
        }, 0, 5, TimeUnit.SECONDS);
    }

    // Class to represent weather data with timestamp
    static class WeatherRecord { 
        private final JSONObject weatherJson;
        private final long timestamp;

        public WeatherRecord(JSONObject weatherJson, long timestamp) {
            this.weatherJson = weatherJson;
            this.timestamp = timestamp;
        }

        public JSONObject getWeatherData() {
            return weatherJson;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}