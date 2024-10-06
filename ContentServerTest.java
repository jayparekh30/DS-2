import org.junit.*;
import org.junit.rules.TemporaryFolder;
// import org.mockito.Mockito;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;
// import static org.mockito.Mockito.*;

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
                System.err.println("Server error: " + e.getMessage());
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

    // Test the sendPutRequest method
    @Test
    public void testSendPutRequest() throws Exception {
        // Mock weather data to be sent
        LinkedHashMap<String, String> weatherData = new LinkedHashMap<>();
        weatherData.put("temperature", "25");
        weatherData.put("humidity", "60");

        // Simulate sending the request to the mock server
        ContentServer.sendPutRequest("localhost", TEST_PORT, weatherData);

        // If the server responds with Lamport-Clock 123, it means the communication succeeded.
        // To validate, you can add output verification if required.
        // We're not asserting specific outputs in this case, just ensuring no exceptions occur.
    }

    // Test the main method with valid inputs
    @Test
    public void testMainWithValidInputs() throws Exception {
        // Create a temp file to simulate the weather data file
        File tempFile = tempFolder.newFile("weather_data.txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("temperature: 25\n");
            writer.write("humidity: 60\n");
        }

        // Simulate command-line arguments to pass to the main method
        String[] args = {"localhost", String.valueOf(TEST_PORT), tempFile.getAbsolutePath()};
        ContentServer.main(args);

        // This test ensures that the main method executes successfully.
        // You can add more assertions by capturing the System.out if needed.
    }

    // Test invalid file path
    @Test(expected = IOException.class)
    public void testConvertFileToLinkedHashMapWithInvalidFile() throws Exception {
        // Provide an invalid file path and expect IOException
        ContentServer.convertFileToLinkedHashMap("invalid_path.txt");
    }
}
