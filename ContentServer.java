import java.io.*;
import java.net.*;
import java.util.LinkedHashMap;
import org.json.JSONObject;

// The ContentServer class reads weather data , transforming it into JSON, and provide data to AggregationServer.
public class ContentServer {
    // Initialise the LamportClock for tracking time for synchronization
    private static LamportClock clock = new LamportClock(); 

    // Check for the arguments (Server, Port, Datafield)
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java ContentServer <server> <port> <datafile>");
            return;
        }

        // Extracting server name, port number, and file path from command line arguments
        String server = args[0];
        int port = Integer.parseInt(args[1]);
        String filePath = args[2];


        try {
            // Convert the weather data text file content into a LinkedHashMap to preserve order
            LinkedHashMap<String, String> weatherData = convertFileToLinkedHashMap(filePath);
            
            // Create a JSON Object for pretty-print JSON data 
            JSONObject weatherJson = new JSONObject(weatherData);
            // Printing parsed weather data
            System.out.println("Parsed weather data:\n" + weatherJson.toString(4));  

            // Increment Lamport Clock before PUT request to check clock is reflected for the event
            clock.tick(); 
            System.out.println("Lamport clock before PUT: " + clock.getClock());
            
            // Sending PUT request to AggregationServer 
            sendPutRequest(server, port, weatherData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Read the entire file line by line and spliting each line into key-value pair
    static LinkedHashMap<String, String> convertFileToLinkedHashMap(String filePath) throws IOException {
         // Use LinkedHashMap to preserve order
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Read each line
            while ((line = br.readLine()) != null) {
                // Split line by ':'
                String[] parts = line.split(":", 2);
                // Trim the white space
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    // Add key-value pair to map
                    map.put(key, value);  
                }
            }
        }
        return map;
    }

    // Send PUT request and weather data to AggregationServer
    static void sendPutRequest(String server, int port, LinkedHashMap<String, String> weatherData) {

        // Creating socket based connection to the server
        try (Socket socket = new Socket(server, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             // For receiving server response
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Display message --> Connected to AggregationServer successful
            System.out.println("Connected to AggregationServer");

            // Create a JSON object
            JSONObject jsonObject = new JSONObject(weatherData);
            String jsonString = jsonObject.toString(4);

            // Send PUT request HTTP/1.1 to AggregationServer
            out.println("PUT /weather.json HTTP/1.1");
            out.println("Host: " + server + ":" + port);
            out.println("User-Agent: ContentServer/1.0");
            out.println("Content-Type: application/json"); // Ensure data type is in JSON format
            out.println("Content-Length: " + jsonString.length()); // Size of the JSON body
            out.println("Lamport-Clock: " + clock.getClock()); // Current value of LamportClock
            out.println();

            // Send the JSON data 
            out.println(jsonString);
            System.out.println("Sending JSON data:\n" + jsonString);

            // Read and print the server response
            String response;
            // Variable to store LamportClock data 
            int receivedClock = -1;
            while ((response = in.readLine()) != null) {
                System.out.println("Response from server: " + response);
                if (response.startsWith("Lamport-Clock:")) {
                    receivedClock = Integer.parseInt(response.split(":")[1].trim());
                }
            }

            // Update the Lamport Clock if it is received
            if (receivedClock != -1) {
                clock.update(receivedClock);
                System.out.println("Lamport clock updated after PUT: " + clock.getClock());
            }

        } catch (IOException e) {
            // Handle exceptions 
            e.printStackTrace();
            System.out.println("Failed to connect to AggregationServer at " + server + ":" + port);
        }
    }
}