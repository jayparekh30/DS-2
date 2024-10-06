import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import static org.junit.Assert.*;

public class ContentServerTest {

    private static ServerSocket mockServerSocket;
    private static Thread serverThread;
    private static final int TEST_PORT = 8888;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();  // Creates temp files and folders for testing

    @BeforeClass
    public static void startMockServer() throws Exception {
        // Start a mock server in a separate thread
        serverThread = new Thread(() -> {
            try {
                mockServerSocket = new ServerSocket(TEST_PORT);
                while (!mockServerSocket.isClosed()) {
                    Socket clientSocket = mockServerSocket.accept();
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


    @AfterClass
    public static void stopMockServer() throws Exception {
        if (mockServerSocket != null && !mockServerSocket.isClosed()) {
            mockServerSocket.close();
        }
        serverThread.join();  // Ensure the thread finishes
    }

    private static void handleClient(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

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
        assertTrue(result.isEmpty());  // Invalid format should result in empty map
    }


    // Edge Case: Failed connection to the server (integration test)
    @Test
    public void testSendPutRequestWithFailedConnection() {
        LinkedHashMap<String, String> weatherData = new LinkedHashMap<>();
        weatherData.put("temperature", "25");
        weatherData.put("humidity", "60");

        ContentServer.sendPutRequest("localhost", 9999, weatherData);  // Port 9999 should not be running
        System.out.println("Test 'testSendPutRequestWithFailedConnection' passed.");
    }
}