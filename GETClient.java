import java.io.*; // Provides class for input and output operations
import java.net.*; // Provides socket connection

public class GETClient {

    public static void main(String[] args) {
        // Check if both server and port are provided as arguments
        if (args.length < 2) {
            System.out.println("Usage: java GETClient <server> <port>");
            return;
        }

        String server = args[0];
        int port = Integer.parseInt(args[1]);

        //Establish a connection to the server
        try {
            connectToServer(server, port); 
        } catch (IOException e) {
            System.err.println("Error while communicating with the server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Send a GET request and connect to server
    private static void connectToServer(String server, int port) throws IOException {
        // Open socket connection
        Socket socket = new Socket(server, port);  

        try (BufferedReader serverResponse = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter requestWriter = new PrintWriter(socket.getOutputStream(), true)) {

            // Send the GET request for weather data
            sendGetRequest(requestWriter);

            // Display the response from the server
            readServerResponse(serverResponse);
        } finally {
            socket.close();
        }
    }

    // Send GET request to the server in HTTP/1.1 format
    private static void sendGetRequest(PrintWriter requestWriter) {
        requestWriter.println("GET /weather.json HTTP/1.1");
        requestWriter.println("Host: localhost"); 
        requestWriter.println("Connection: close");  // Close connection after response
        requestWriter.println();  // End of header
    }

    // Read the response from the server
    private static void readServerResponse(BufferedReader serverResponse) throws IOException {
        String responseLine;

        // Iterate through the response from the server
        while ((responseLine = serverResponse.readLine()) != null) {
            System.out.println(responseLine);
        }
    }
}