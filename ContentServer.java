import java.io.*;
import java.net.*;
import java.util.LinkedHashMap;
import org.json.JSONObject;

public class ContentServer {
    private static LamportClock clock = new LamportClock();  // Initialize Lamport clock

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java ContentServer <server> <port> <datafile>");
            return;
        }

        String server = args[0];
        int port = Integer.parseInt(args[1]);
        String filePath = args[2];

        try {
            // Convert the weather_data.txt file content into a LinkedHashMap to preserve order
            LinkedHashMap<String, String> weatherData = convertFileToLinkedHashMap(filePath);
            
            // Create a JSON object from the parsed weather data for pretty printing
            JSONObject weatherJson = new JSONObject(weatherData);
            System.out.println("Parsed weather data:\n" + weatherJson.toString(4));  // Pretty-print the JSON data

            clock.tick();  // Increment clock before sending data
            System.out.println("Lamport clock before PUT: " + clock.getClock());

            sendPutRequest(server, port, weatherData);  // Send the weather data
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static LinkedHashMap<String, String> convertFileToLinkedHashMap(String filePath) throws IOException {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();  // Use LinkedHashMap to preserve order
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":", 2);  // Split each line by ':'
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    map.put(key, value);  // Add key-value pair to LinkedHashMap to preserve order
                }
            }
        }
        return map;
    }

    private static void sendPutRequest(String server, int port, LinkedHashMap<String, String> weatherData) {
        try (Socket socket = new Socket(server, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Log connection success
            System.out.println("Connected to AggregationServer");

            // Create JSON string
            JSONObject jsonObject = new JSONObject(weatherData);
            String jsonString = jsonObject.toString(4);  // Format with 4 spaces

            // Send PUT request headers
            out.println("PUT /weather.json HTTP/1.1");
            out.println("Host: " + server + ":" + port);
            out.println("User-Agent: ContentServer/1.0");
            out.println("Content-Type: application/json");
            out.println("Content-Length: " + jsonString.length());
            out.println("Lamport-Clock: " + clock.getClock());  // Send the Lamport clock in headers
            out.println();  // Blank line to indicate end of headers

            // Send the JSON data in the body
            out.println(jsonString);
            System.out.println("Sending JSON data:\n" + jsonString);

            // Read and print the server response
            String response;
            int receivedClock = -1;
            while ((response = in.readLine()) != null) {
                System.out.println("Response from server: " + response);
                if (response.startsWith("Lamport-Clock:")) {
                    receivedClock = Integer.parseInt(response.split(":")[1].trim());
                }
            }

            // Update the Lamport clock based on the received clock
            if (receivedClock != -1) {
                clock.update(receivedClock);
                System.out.println("Lamport clock updated after PUT: " + clock.getClock());
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to connect to AggregationServer at " + server + ":" + port);
        }
    }
}