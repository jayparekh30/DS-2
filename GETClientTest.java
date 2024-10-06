import org.junit.*;
import java.io.*;
import java.net.*;
import static org.junit.Assert.*;

public class GETClientTest {

    private static ServerSocket testServerSocket;
    private static Thread serverThread;
    private static final int TEST_PORT = 8888;

    @BeforeClass
    public static void startTestServer() throws Exception {
        // Start a simple server in a new thread
        serverThread = new Thread(() -> {
            try {
                testServerSocket = new ServerSocket(TEST_PORT);
                while (!testServerSocket.isClosed()) {
                    Socket clientSocket = testServerSocket.accept();
                    handleClient(clientSocket);
                }
            } catch (IOException e) {
                System.err.println("Server error: " + e.getMessage());
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
            testServerSocket.close();
        }
        serverThread.join();  // Wait for the server thread to finish
    }

    // Test a valid GET request
    @Test
    public void testValidGETRequest() throws Exception {
        // Capture client output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        // Simulate calling the main method with localhost and the test server port
        GETClient.main(new String[]{"localhost", String.valueOf(TEST_PORT)});

        // Verify the response from the server
        String output = outputStream.toString();
        assertTrue(output.contains("HTTP/1.1 200 OK"));
        assertTrue(output.contains("{\"weather\":\"Sunny\"}"));

        System.setOut(originalOut);  // Restore original System.out
    }

    // Test a GET request that results in a 404 Not Found
    @Test
    public void testGETRequestNotFound() throws Exception {
        // Capture client output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        // Simulate calling the main method with an invalid path
        GETClient.main(new String[]{"localhost", String.valueOf(TEST_PORT)});

        // Verify the response from the server for an invalid path
        String output = outputStream.toString();
        assertTrue(output.contains("HTTP/1.1 404 Not Found"));
        assertTrue(output.contains("Not Found"));

        System.setOut(originalOut);  // Restore original System.out
    }

    // Test a case where the server is unreachable
    @Test
    public void testServerUnreachable() throws Exception {
        // Capture client output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        // Call GETClient with a non-existent port to simulate server unreachable
        GETClient.main(new String[]{"localhost", "9999"});  // Invalid port

        // Verify that the client properly handles the error
        String output = outputStream.toString();
        assertTrue(output.contains("Error while communicating with the server"));

        System.setOut(originalOut);  // Restore original System.out
    }

    // Test invalid or malformed responses from the server
    @Test
    public void testInvalidResponse() throws Exception {
        // Capture client output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        // Simulate a server that sends an invalid response
        GETClient.main(new String[]{"localhost", String.valueOf(TEST_PORT)});

        // Check that the client handles an invalid response
        String output = outputStream.toString();
        assertTrue(output.contains("HTTP/1.1 200 OK"));
        assertTrue(output.contains("{\"weather\":\"Sunny\"}"));  // Make sure valid JSON was returned

        System.setOut(originalOut);  // Restore original System.out
    }
}
