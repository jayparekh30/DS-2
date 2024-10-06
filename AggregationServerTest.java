import static org.junit.Assert.*;
import java.io.*;
import java.net.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AggregationServerTest {
    private static final int TEST_PORT = 4567;
    private Thread serverThread;

    @Before
    public void setUp() throws Exception {
        serverThread = new Thread(() -> {
            try {
                AggregationServer.main(new String[]{String.valueOf(TEST_PORT)});  // Start the server on a specific port
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        Thread.sleep(500);  // Small delay to ensure the server starts
    }

    @After
    public void tearDown() throws Exception {
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();  // Ensure the thread terminates
        }
    }

    // Test valid PUT request to store weather data
    @Test
    public void testValidPutRequest() throws Exception {
        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Sending a valid PUT request
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

        socket.close();
    }

    // Test invalid PUT request with no data
    @Test
    public void testInvalidPutRequest() throws Exception {
        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Sending a PUT request with no content
        writer.println("PUT /weather.json HTTP/1.1");
        writer.println("Host: localhost");
        writer.println("Content-Length: 0");
        writer.println();
        
        // Read response
        String responseLine = reader.readLine();
        assertEquals("HTTP/1.1 400 Bad Request", responseLine);

        socket.close();
    }

   // Test valid GET request to retrieve stored weather data
    @Test
    public void testValidGetRequest() throws Exception {
        // First, send a PUT request to store weather data
        testValidPutRequest();

        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Sending a GET request
        writer.println("GET /weather.json HTTP/1.1");
        writer.println("Host: localhost");
        writer.println();
        
        // Read response
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

    // Edge Case 1: Test for empty PUT request (invalid, no body content)
    @Test
    public void testPutRequestWithEmptyBody() throws Exception {
        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        writer.println("PUT /weather.json HTTP/1.1");
        writer.println("Host: localhost");
        writer.println("Content-Length: 0");
        writer.println();

        String responseLine = reader.readLine();
        assertEquals("HTTP/1.1 400 Bad Request", responseLine);  // Should return bad request
        socket.close();
    }

    // Edge Case 2: Test sending the same PUT request twice (should overwrite the data)
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

        // Second PUT request (same data)
        Socket socket2 = new Socket("localhost", TEST_PORT);
        PrintWriter writer2 = new PrintWriter(socket2.getOutputStream(), true);
        BufferedReader reader2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));

        writer2.println("PUT /weather.json HTTP/1.1");
        writer2.println("Host: localhost");
        writer2.println("Content-Type: application/json");
        writer2.println("Content-Length: " + jsonBody.length());
        writer2.println();
        writer2.println(jsonBody);

        String responseLine2 = reader2.readLine();
        assertEquals("HTTP/1.1 201 Created", responseLine2);  // Data should be overwritten successfully
        socket2.close();
    }

    // Integration Test: Simulate multiple clients performing PUT and GET operations concurrently
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
                assertTrue(isDataFound);  // Verify data was stored and can be retrieved
                getSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Run client tasks concurrently
        for (int i = 0; i < 5; i++) {
            executor.submit(clientTask);
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(500);
        }
    }
}
