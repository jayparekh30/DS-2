import org.junit.*; // Import JUnit assertions for testing
import org.junit.rules.TemporaryFolder; // Provides Temporary folders used in testing
import java.io.*; // Provides class for input and output operations
import java.net.ServerSocket; // To create Mocke server
import java.net.Socket; // To simulate client connection
import java.util.LinkedHashMap; // To store weather data in key-value pair
import static org.junit.Assert.*; // To validate tests result

public class ContentServerTest {

    // It simulate AggregationServer
    private static ServerSocket mockServerSocket;
    // Thread to run mock server
    private static Thread serverThread;
    // PORT - 8888 (for mock server)
    private static final int TEST_PORT = 8888;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();  // Creates temp files for testing

    // Start the mock server before running the test cases
    @BeforeClass
    public static void startMockServer() throws Exception {
        // Start mock server in a new thread
        serverThread = new Thread(() -> {
            try {
                // Open socket connection
                mockServerSocket = new ServerSocket(TEST_PORT);
                while (!mockServerSocket.isClosed()) {
                    Socket clientSocket = mockServerSocket.accept(); // Accept client request
                    handleClient(clientSocket);
                }
            } catch (IOException e) {
                if (!mockServerSocket.isClosed()) {
                    System.err.println("Server error: " + e.getMessage());
                }
            }
        });
        serverThread.start();
    }

    // Stop the mock server after testing all cases
    @AfterClass
    public static void stopMockServer() throws Exception {
        if (mockServerSocket != null && !mockServerSocket.isClosed()) {
            mockServerSocket.close(); // Close the server
        }
        serverThread.join();  // Ensure the thread finished before test cases
    }

    // To handle client requests send to the mock server
    private static void handleClient(Socket clientSocket) throws IOException {
        // Reading client request
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        // Response back to the client
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        // Read first line
        String requestLine = in.readLine();
        if (requestLine.startsWith("PUT")) {
            out.println("HTTP/1.1 201 Created");
            out.println("Lamport-Clock: 123");
            out.println();
        }

        clientSocket.close();
    }

    // Test for convertFileToLinkedHashMap()
    @Test
    public void testConvertFileToLinkedHashMap() throws Exception {
        File tempFile = tempFolder.newFile("weather_data.txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("temperature: 25\n");
            writer.write("humidity: 60\n");
        }

        LinkedHashMap<String, String> result = ContentServer.convertFileToLinkedHashMap(tempFile.getAbsolutePath());

        assertNotNull(result);
        assertEquals("25", result.get("temperature"));
        assertEquals("60", result.get("humidity"));
    }

    // Edge Case: Empty weather data file
    @Test
    public void testConvertFileToLinkedHashMapWithEmptyFile() throws Exception {
        File emptyFile = tempFolder.newFile("empty_weather_data.txt");
        LinkedHashMap<String, String> result = ContentServer.convertFileToLinkedHashMap(emptyFile.getAbsolutePath());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Edge Case: Invalid file path
    @Test(expected = IOException.class)
    public void testConvertFileToLinkedHashMapWithInvalidFile() throws Exception {
        ContentServer.convertFileToLinkedHashMap("invalid_path.txt");
    }

    // Edge Case: File with incorrect format (missing ':' delimiter)
    @Test
    public void testConvertFileToLinkedHashMapWithMalformedFile() throws Exception {
        File tempFile = tempFolder.newFile("malformed_weather_data.txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("temperature 25\n");
        }

        LinkedHashMap<String, String> result = ContentServer.convertFileToLinkedHashMap(tempFile.getAbsolutePath());
        assertNotNull(result);
        assertTrue(result.isEmpty()); 
    }


    // Edge Case: Failed connection to server (integration test)
    @Test
    public void testSendPutRequestWithFailedConnection() {
        LinkedHashMap<String, String> weatherData = new LinkedHashMap<>();
        weatherData.put("temperature", "25");
        weatherData.put("humidity", "60");

        ContentServer.sendPutRequest("localhost", 9999, weatherData);  // Port 9999 should not be running
        System.out.println("Test 'testSendPutRequestWithFailedConnection' passed.");
    }
}