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
                // Suppress 'socket closed' error if server is shutting down intentionally
                if (!mockServerSocket.isClosed()) {
                    System.err.println("Server error: " + e.getMessage());
                }
            }
        });
        serverThread.start();
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

    @AfterClass
    public static void stopMockServer() throws Exception {
        if (mockServerSocket != null && !mockServerSocket.isClosed()) {
            mockServerSocket.close();
        }
        serverThread.join();  // Ensure the thread finishes
    }

    // Test for convertFileToLinkedHashMap()
    @Test
    public void testConvertFileToLinkedHashMap() throws Exception {
        // Create a temp file to simulate the weather data
        File tempFile = tempFolder.newFile("weather_data.txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("temperature: 25\n");
            writer.write("humidity: 60\n");
        }

        // Test the conversion
        LinkedHashMap<String, String> result = ContentServer.convertFileToLinkedHashMap(tempFile.getAbsolutePath());

        assertNotNull(result);
        assertEquals("25", result.get("temperature"));
        assertEquals("60", result.get("humidity"));
    }

    // Test for an empty weather data file
    @Test
    public void testConvertFileToLinkedHashMapWithEmptyFile() throws Exception {
        File emptyFile = tempFolder.newFile("empty_weather_data.txt");

        LinkedHashMap<String, String> result = ContentServer.convertFileToLinkedHashMap(emptyFile.getAbsolutePath());

        // Expect the map to be empty
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Test invalid file path
    @Test(expected = IOException.class)
    public void testConvertFileToLinkedHashMapWithInvalidFile() throws Exception {
        // Provide an invalid file path and expect IOException
        ContentServer.convertFileToLinkedHashMap("invalid_path.txt");
    }

    // Test for a file with incorrect format (missing ':' delimiter)
    @Test
    public void testConvertFileToLinkedHashMapWithMalformedFile() throws Exception {
        // Create a malformed temp file
        File tempFile = tempFolder.newFile("malformed_weather_data.txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("temperature 25\n");  // Incorrect format (missing ':')
        }

        LinkedHashMap<String, String> result = ContentServer.convertFileToLinkedHashMap(tempFile.getAbsolutePath());

        // Expect the map to be empty since the format is incorrect
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Test failed connection to a server (integration test)
    @Test
    public void testSendPutRequestWithFailedConnection() {
        LinkedHashMap<String, String> weatherData = new LinkedHashMap<>();
        weatherData.put("temperature", "25");
        weatherData.put("humidity", "60");

        // Attempt to send data to an unreachable server
        ContentServer.sendPutRequest("localhost", 9999, weatherData);  // Port 9999 should not be running
        System.out.println("Test 'testSendPutRequestWithFailedConnection' passed.");
    }
}