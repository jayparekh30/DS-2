
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
    
    
    private static void handlePutRequest(BufferedReader in, PrintWriter out) throws IOException {
    lamportClock.incrementAndGet();
    String line;
    int contentLength = 0;

    // Read headers to find Content-Length
    while (!(line = in.readLine()).isEmpty()) {
        if (line.startsWith("Content-Length:")) {
            contentLength = Integer.parseInt(line.split(":")[1].trim());
        }
    }

    // Read the body based on Content-Length
    char[] body = new char[contentLength];
    in.read(body, 0, contentLength);
    String jsonString = new String(body);

    try {
        // Parse the JSON data
        System.out.println("Received PUT request with JSON: " + jsonString);
        JSONObject jsonObject = new JSONObject(jsonString);
        String id = jsonObject.getString("id");

        // Store the weather data with a timestamp
        dataStore.put(id, new WeatherData(jsonObject, System.currentTimeMillis()));
        System.out.println("Storing weather data with ID: " + id);
        out.println("HTTP/1.1 201 Created");
    } catch (Exception e) {
        System.err.println("Error processing PUT request: " + e.getMessage());
        e.printStackTrace();
        out.println("HTTP/1.1 500 Internal Server Error");
    }
}


    
    private static void startDataExpunger() {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(() -> {
        long currentTime = System.currentTimeMillis();
        dataStore.entrySet().removeIf(entry -> {
            boolean shouldRemove = (currentTime - entry.getValue().getTimestamp()) > EXPIRATION_TIME_MS;
            if (shouldRemove) {
                System.out.println("Removing expired data with ID: " + entry.getKey());
            }
            return shouldRemove;
        });
    }, 0, 5, TimeUnit.SECONDS);
}


    // Class to hold weather data with a timestamp
    static class WeatherData {
        private final JSONObject weatherJson;
        private final long timestamp;

        public WeatherData(JSONObject weatherJson, long timestamp) {
            this.weatherJson = weatherJson;
            this.timestamp = timestamp;
        }

        public JSONObject getWeatherJson() {
            return weatherJson;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}


