import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import org.json.JSONObject;
import java.util.concurrent.atomic.AtomicLong;

public class AggregationServer {

    // Default port for the server - 4567
    private static final int SERVER_PORT = 4567;  
    // Weather data expiry time - 30 Seconds
    private static final int DATA_EXPIRY_TIME_MS = 30000;
    
    // ConcurrentHashMap for storing weather data - Take key as ID
    private static final ConcurrentHashMap<String, WeatherRecord> weatherDataMap = new ConcurrentHashMap<>();  
    
    // For simulate LamportClock used AtomicLong
    private static final AtomicLong LamportClock = new AtomicLong(0);  

    public static void main(String[] args) {
        // Verify whether the port is given as an argument; if not, use the default
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : SERVER_PORT;
        
        // Creat Server Socket to receive client connection
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server running on port " + port);

            // Calling initiateDataCleanupTask() to remove expired weather data
            initiateDataCleanupTask();  

            // Accept client connection continuously
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // creating new thread to handle each client for concurrency 
                new Thread(() -> processClientRequest(clientSocket)).start();  
            }
        } catch (IOException e) {
            // Print any I/O Error 
            e.printStackTrace();
        }
    }

    // Handles client requests PUT or GET
    private static void processClientRequest(Socket clientSocket) {
        try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter outputWriter = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            // Reading first line of request (GET or PUT)
            String clientRequest = inputReader.readLine();
            // Log incoming request
            System.out.println("Request received: " + clientRequest);
            
            // Handle GET requests
            if (clientRequest.startsWith("GET")) {
                handleGetRequest(outputWriter);
            
            // Handle PUT requests
            } else if (clientRequest.startsWith("PUT")) {
                // Log that we are handling a PUT request
                System.out.println("Handling PUT request...");
                handlePutRequest(inputReader, outputWriter);
            } else {
                outputWriter.println("HTTP/1.1 400 Bad Request"); // Respond with bad request
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handle GET requests and return weather data in JSON format
    private static void handleGetRequest(PrintWriter output) {
        LamportClock.incrementAndGet();  // Increment lamport clock

        // Used jsonBuilder to display output in correct format
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n"); 

        // Iteratively go through each weather record to generate the JSON response
        for (WeatherRecord record : weatherDataMap.values()) {
            JSONObject weatherJson = record.getWeatherData();
            // Append each weather data
            jsonBuilder.append("  \"").append(weatherJson.getString("id")).append("\": ").append(weatherJson.toString(4)).append(",\n");
        }

        // Remove trailing comma
        if (jsonBuilder.length() > 2) {
            jsonBuilder.setLength(jsonBuilder.length() - 2);
        }
        jsonBuilder.append("\n}"); // Closing JSON object

        String prettyPrintedJson = jsonBuilder.toString();

        // HTTP response 
        output.println("HTTP/1.1 200 OK");
        output.println("Content-Type: application/json");
        output.println("Content-Length: " + prettyPrintedJson.length()); 
        output.println();
        output.println(prettyPrintedJson); // Sending JSON response
        System.out.println("Sent GET response:\n" + prettyPrintedJson);  // Log JSON response
    }
    

    // handlePutRequest() method to Handle PUT requests and store weather data
    private static void handlePutRequest(BufferedReader input, PrintWriter output) throws IOException {
        LamportClock.incrementAndGet();  // Increment lamport clock

        String line;
        int contentLength = 0;

        // Read and log headers from the request
        System.out.println("Reading PUT request headers...");
        while (!(line = input.readLine()).isEmpty()) {
            System.out.println(line);
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim()); // Extracting content length
            }
        }

        // Respond 400 Bad Request if data is not provided
        if (contentLength == 0) {
            System.out.println("Error: Content-Length is 0. No data provided.");
            output.println("HTTP/1.1 400 Bad Request");
            return;
        }

        // Display the received content length
        System.out.println("Content-Length: " + contentLength);

        // Depending on the content length, read the request's body weather data
        char[] body = new char[contentLength];
        input.read(body, 0, contentLength);
        String receivedJsonString = new String(body);

        // Record the JSON string you received for Debugging
        System.out.println("Received JSON body:\n" + receivedJsonString);

        try {
            // Parsing received JSON string
            JSONObject weatherJson = new JSONObject(receivedJsonString);
            String id = weatherJson.getString("id");

            // Store the weather data with time
            weatherDataMap.put(id, new WeatherRecord(weatherJson, System.currentTimeMillis())); 
            System.out.println("Weather data stored for ID: " + id);

            // Respond with success after storing weather data
            output.println("HTTP/1.1 201 Created");
        } catch (Exception e) {
            System.err.println("Error processing PUT request: " + e.getMessage());
            e.printStackTrace();
            // If failure then respond with 500
            output.println("HTTP/1.1 500 Internal Server Error");
        }
    }

    // Cleanup task to remove expired weather data
    private static void initiateDataCleanupTask() {
        ScheduledExecutorService cleanupScheduler = Executors.newScheduledThreadPool(1);
        cleanupScheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();

            // Remove entries where data has expired --> older than DATA_EXPIRY_TIME_MS
            weatherDataMap.entrySet().removeIf(entry -> {
                boolean shouldRemove = (currentTime - entry.getValue().getTimestamp()) > DATA_EXPIRY_TIME_MS;
                if (shouldRemove) {
                    System.out.println("Removing expired data with ID: " + entry.getKey());
                }
                return shouldRemove;
            });
        }, 0, 5, TimeUnit.SECONDS); // Every five seconds, clean up
    }
    


    // Class to represent weather data
    static class WeatherRecord { 
        // JSON object to store weather data
        private final JSONObject weatherJson;
        // timestamp when it received data
        private final long timestamp;

        public WeatherRecord(JSONObject weatherJson, long timestamp) {
            this.weatherJson = weatherJson;
            this.timestamp = timestamp;
        }

        // Get the stored weather data
        public JSONObject getWeatherData() {
            return weatherJson;
        }

        // Get the timestamp of stored data
        public long getTimestamp() {
            return timestamp;
        }
    }
}