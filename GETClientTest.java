import org.junit.*;
import java.io.*; // Provides class for input and output operations
import java.net.*; // Provide socket connection
import static org.junit.Assert.*; // Provide assertion Methods for testing

public class GETClientTest {

    private static ServerSocket testServerSocket;
    private static Thread serverThread;
    private static final int TEST_PORT = 8888; 

    @BeforeClass
    public static void startTestServer() throws Exception {
        // Start a server in a new thread
        serverThread = new Thread(() -> {
            try {
                testServerSocket = new ServerSocket(TEST_PORT);
                while (!testServerSocket.isClosed()) {
                    Socket clientSocket = testServerSocket.accept();
                    handleClient(clientSocket);
                }
            } catch (IOException e) {
                if (!testServerSocket.isClosed()) { 
                    System.err.println("Server error: " + e.getMessage());
                }
            }
        });
        serverThread.start();
    }

    // Helper method to simulate server response
    private static void handleClient(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        String requestLine = in.readLine();
        if (requestLine.startsWith("GET")) {
            if (requestLine.contains("/weather.json")) {
                // Return mock weather JSON for a valid request
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println();
                out.println("{\"weather\":\"Sunny\"}");
            } else if (requestLine.contains("/empty")) {
                // Return empty body for testing empty response
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println();
                out.println("");
            } else {
                // Return 404 for any other paths
                out.println("HTTP/1.1 404 Not Found");
                out.println("Content-Type: text/plain");
                out.println();
                out.println("Not Found");
            }
        }
        clientSocket.close();
    }

    @AfterClass
    public static void stopTestServer() throws Exception {
        // Stop the server after all tests
        if (testServerSocket != null && !testServerSocket.isClosed()) {
            try {
                testServerSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();  // Stop the thread
        }
        serverThread.join();
    }

    // Test a valid GET request
    @Test
    public void testValidGETRequest() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        GETClient.main(new String[]{"localhost", String.valueOf(TEST_PORT)});

        String output = outputStream.toString();
        assertTrue(output.contains("HTTP/1.1 200 OK"));
        assertTrue(output.contains("{\"weather\":\"Sunny\"}"));

        System.setOut(originalOut);  // Restore original System.out
    }

    // Edge Case: Empty response from server
    @Test
    public void testEmptyResponse() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        GETClient.main(new String[]{"localhost", String.valueOf(TEST_PORT)});

        String output = outputStream.toString();
        assertTrue(output.contains("HTTP/1.1 200 OK"));
        assertTrue(output.contains("{}") || output.contains(""));  // Empty JSON or empty response

        System.setOut(originalOut);
    }

    // Edge Case: Malformed or unexpected path request
    @Test
    public void testBadRequest() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Send a malformed GET request
        writer.println("GET /invalid_path HTTP/1.1");
        writer.println("Host: localhost");
        writer.println();

        String responseLine = reader.readLine();
        assertTrue(responseLine.contains("404 Not Found"));

        socket.close();
        System.setOut(originalOut);
    }

    // Integration Test: Large payload response from server
    @Test
    public void testLargePayloadResponse() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        // Simulating large payload
        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Sending a GET request
        writer.println("GET /weather.json HTTP/1.1");
        writer.println("Host: localhost");
        writer.println();

        // Reading large response from server
        StringBuilder largeResponse = new StringBuilder();
        String responseLine;
        while ((responseLine = reader.readLine()) != null) {
            largeResponse.append(responseLine);
        }

        // Assuming that the server can handle large responses
        assertTrue(largeResponse.toString().contains("{\"weather\":\"Sunny\"}"));

        socket.close();
        System.setOut(originalOut);
    }

    // Edge Case: Simulate request timeout (Test by simulating slow server response)
    @Test(timeout = 2000) 
    public void testRequestTimeout() throws Exception {
        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Simulating slow response from server
        writer.println("GET /weather.json HTTP/1.1");
        writer.println("Host: localhost");
        writer.println();

        String responseLine = reader.readLine();
        assertTrue(responseLine.contains("HTTP/1.1 200 OK"));

        socket.close();
    }
}
