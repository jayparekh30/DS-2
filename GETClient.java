

import java.io.*;
import java.net.*;

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
            connectToServer(server, port);  // Moved connection logic to a separate method for clarity
        } catch (IOException e) {
            System.err.println("Error while communicating with the server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Connect to the server and send a GET request
    private static void connectToServer(String server, int port) throws IOException {
        Socket socket = new Socket(server, port);  // Open socket connection

        try (BufferedReader serverResponse = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter requestWriter = new PrintWriter(socket.getOutputStream(), true)) {

            // Send the GET request for weather data
            sendGetRequest(requestWriter);

            // Read and display the response from the server
            readServerResponse(serverResponse);
        } finally {
            socket.close();  // Ensure socket is properly closed after communication
        }
    }

    // Send GET request to the server in HTTP/1.1 format
    private static void sendGetRequest(PrintWriter requestWriter) {
        requestWriter.println("GET /weather.json HTTP/1.1");
        requestWriter.println("Host: localhost");  // Adding Host header for HTTP/1.1 compliance
        requestWriter.println("Connection: close");  // Explicitly close connection after response
        requestWriter.println();  // Empty line to signal the end of the headers
    }

    // Read the response from the server and output it to the console
    private static void readServerResponse(BufferedReader serverResponse) throws IOException {
        String responseLine;

        // Iterate through the response from the server
        while ((responseLine = serverResponse.readLine()) != null) {
            System.out.println(responseLine);  // Print the received line
        }
    }
}