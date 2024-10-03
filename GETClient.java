// import java.io.*;
// import java.net.*;

// public class GETClient {

//     public static void main(String[] args) {
//         if (args.length < 2) {
//             System.out.println("Usage: java GETClient <server:port>");
//             return;
//         }

//         String serverAddress = args[0];

//         try (Socket socket = new Socket(serverAddress.split(":")[0], Integer.parseInt(serverAddress.split(":")[1]));
//              PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//              BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

//             // Send GET request
//             out.println("GET /weather.json HTTP/1.1");
//             out.println("Host: " + serverAddress);
//             out.println();  // Blank line to end headers

//             // Read and print the server response
//             String response;
//             while ((response = in.readLine()) != null) {
//                 System.out.println("Response from server: " + response);
//             }

//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }
// }

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

        try (Socket socket = new Socket(server, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send the GET request
            out.println("GET /weather.json HTTP/1.1");
            out.println(); // End of headers

            // Read the response from the server
            String responseLine;
            while ((responseLine = in.readLine()) != null) {
                System.out.println(responseLine);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
