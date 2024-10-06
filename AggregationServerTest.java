import static org.junit.Assert.*; // Provides assertion methods of J Unit Testing
import java.io.*; // Provides class for input output operations
import java.net.*; // Provides classes for socket networking
import org.junit.After; // Method for cleanup after test cases
import org.junit.Before;
import org.junit.Test; // Import test methods
import java.util.concurrent.ExecutorService; // To manage multiple threads
import java.util.concurrent.Executors; // To create thread pools

public class AggregationServerTest {
    private static final int TEST_PORT = 4567; // server port - 4567
    private Thread serverThread; // Thread used to run AggregationServer

    @Before
    public void setUp() throws Exception {
        // Run AggregationServer in new thread
        serverThread = new Thread(() -> {
            try {
                AggregationServer.main(new String[]{String.valueOf(TEST_PORT)});  // Start server on specific port
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        Thread.sleep(500);  //Small delay to check server starts
    }

    @After
    public void tearDown() throws Exception {
        // If server thread is running then stop it
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
    }

    // Test valid PUT request to store weather data
    @Test
    public void testValidPutRequest() throws Exception {
        // Start socket connection
        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true); // Send a request
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Read a request

        // Sending valid PUT request
        String jsonBody = "{\"id\": \"IDS60901\", \"name\": \"Adelaide (West Terrace / ngayirdapira)\", \"state\": \"SA\", \"air_temp\": \"20.5\"}";
        writer.println("PUT /weather.json HTTP/1.1");
        writer.println("Host: localhost");
        writer.println("Content-Type: application/json");
        writer.println("Content-Length: " + jsonBody.length());
        writer.println();
        writer.println(jsonBody);

        // Reading response
        String responseLine = reader.readLine();
        assertEquals("HTTP/1.1 201 Created", responseLine);

        socket.close(); // Connection closed
    }

    // Test invalid PUT request (empty data)
    @Test
    public void testInvalidPutRequest() throws Exception {
        // Open socket connection
        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Sending a PUT request without content
        writer.println("PUT /weather.json HTTP/1.1");
        writer.println("Host: localhost");
        writer.println("Content-Length: 0");
        writer.println();
        
        // Reading response
        String responseLine = reader.readLine();
        assertEquals("HTTP/1.1 400 Bad Request", responseLine);

        socket.close();
    }

   // Test valid GET request to get weather data
    @Test
    public void testValidGetRequest() throws Exception {
        // send a PUT request to store weather data
        testValidPutRequest();

        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Sending a GET request
        writer.println("GET /weather.json HTTP/1.1");
        writer.println("Host: localhost");
        writer.println();
        
        // Reading response
        String responseLine;
        boolean isDataFound = false;
        while ((responseLine = reader.readLine()) != null) {
            if (responseLine.contains("\"id\": \"IDS60901\"")) {
                isDataFound = true;
                break;
            }
        }

        assertTrue(isDataFound);
        socket.close();
    }

    // Edge Case : Test for empty PUT request (invalid, no content)
    @Test
    public void testPutRequestWithEmptyBody() throws Exception {
        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Send PUT request with no body
        writer.println("PUT /weather.json HTTP/1.1");
        writer.println("Host: localhost");
        writer.println("Content-Length: 0");
        writer.println();

        // Reading response
        String responseLine = reader.readLine();
        assertEquals("HTTP/1.1 400 Bad Request", responseLine);  // Return bad request
        socket.close(); // Close the socket
    }

    // Edge Case : Test sending the same PUT request two times
    @Test
    public void testDuplicatePutRequest() throws Exception {
        String jsonBody = "{\"id\": \"IDS60904\", \"name\": \"Brisbane\", \"state\": \"QLD\", \"air_temp\": \"25.5\"}";

        // First PUT request
        Socket socket1 = new Socket("localhost", TEST_PORT);
        PrintWriter writer1 = new PrintWriter(socket1.getOutputStream(), true);
        BufferedReader reader1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));

        writer1.println("PUT /weather.json HTTP/1.1");
        writer1.println("Host: localhost");
        writer1.println("Content-Type: application/json");
        writer1.println("Content-Length: " + jsonBody.length());
        writer1.println();
        writer1.println(jsonBody);

        String responseLine1 = reader1.readLine();
        assertEquals("HTTP/1.1 201 Created", responseLine1);
        socket1.close();

        // Second PUT request(same data)
        Socket socket2 = new Socket("localhost", TEST_PORT);
        PrintWriter writer2 = new PrintWriter(socket2.getOutputStream(), true);
        BufferedReader reader2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));

        writer2.println("PUT /weather.json HTTP/1.1");
        writer2.println("Host: localhost");
        writer2.println("Content-Type: application/json");
        writer2.println("Content-Length: " + jsonBody.length());
        writer2.println();
        // Send JSON data
        writer2.println(jsonBody);

        String responseLine2 = reader2.readLine();
        assertEquals("HTTP/1.1 201 Created", responseLine2);  // Overwrite data
        socket2.close();
    }

    // Integration Test: Simulate multiple clients (PUT and GET) operations concurrently
    @Test
    public void testConcurrentClients() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5);  // Simulate 5 clients

        Runnable clientTask = () -> {
            try {
                // Each client sends a unique PUT request
                String clientData = "{\"id\": \"IDS6090" + Thread.currentThread().getId() + "\", \"name\": \"ClientCity\", \"state\": \"ST\", \"air_temp\": \"30.0\"}";
                Socket socket = new Socket("localhost", TEST_PORT);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                writer.println("PUT /weather.json HTTP/1.1");
                writer.println("Host: localhost");
                writer.println("Content-Type: application/json");
                writer.println("Content-Length: " + clientData.length());
                writer.println();
                writer.println(clientData);

                String responseLine = reader.readLine();
                assertEquals("HTTP/1.1 201 Created", responseLine);  // Verify successful PUT request
                socket.close();

                // Immediately after, send a GET request
                Socket getSocket = new Socket("localhost", TEST_PORT);
                PrintWriter getWriter = new PrintWriter(getSocket.getOutputStream(), true);
                BufferedReader getReader = new BufferedReader(new InputStreamReader(getSocket.getInputStream()));

                getWriter.println("GET /weather.json HTTP/1.1");
                getWriter.println("Host: localhost");
                getWriter.println();

                boolean isDataFound = false;
                String response;
                while ((response = getReader.readLine()) != null) {
                    if (response.contains("\"id\": \"IDS6090" + Thread.currentThread().getId() + "\"")) {
                        isDataFound = true;
                        break;
                    }
                }
                assertTrue(isDataFound);  // Verify data was stored
                getSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Run 5 client tasks concurrently
        for (int i = 0; i < 5; i++) {
            executor.submit(clientTask);
        }

        executor.shutdown(); // Close the exexutor after all tasks
        while (!executor.isTerminated()) {
            Thread.sleep(500); // Wait for all task
        }
    }
}
