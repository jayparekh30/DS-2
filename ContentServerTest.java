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
    serverThread = new Thread(() -> {
        try {
            mockServerSocket = new ServerSocket(TEST_PORT);
            while (!mockServerSocket.isClosed()) {
                try {
                    Socket clientSocket = mockServerSocket.accept();
                    handleClient(clientSocket);
                } catch (IOException e) {
                    if (!mockServerSocket.isClosed()) {
                        // Only print errors if the server socket wasn't intentionally closed
                        System.err.println("Server error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    });
    serverThread.start();
}

    private static void handleClient(Socket clientSocket) throws IOException {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

        String requestLine = in.readLine();
        if (requestLine != null && requestLine.startsWith("PUT")) {
            out.println("HTTP/1.1 201 Created");
            out.println("Lamport-Clock: 123");
            out.println();
        }
    } finally {
        clientSocket.close();
    }
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

    
    // Test invalid file path
    @Test(expected = IOException.class)
    public void testConvertFileToLinkedHashMapWithInvalidFile() throws Exception {
        // Provide an invalid file path and expect IOException
        ContentServer.convertFileToLinkedHashMap("invalid_path.txt");
    }
}