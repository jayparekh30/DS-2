import static org.junit.Assert.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AggregationServerTest {
    private static final int TEST_PORT = 4567;
    private ServerSocket serverSocket;
    private Thread serverThread;

    @Before
    public void setUp() throws Exception {
        // Start the AggregationServer in a separate thread
        serverThread = new Thread(() -> {
            String[] args = {String.valueOf(TEST_PORT)};
            AggregationServer.main(args);
        });
        serverThread.start();

        // Give the server some time to start
        Thread.sleep(1000);
    }

    @After
    public void tearDown() throws Exception {
        // Close the serverSocket to terminate the server
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
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

    // Test GET request when no data is available
    @Test
    public void testEmptyGetRequest() throws Exception {
        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Sending a GET request with no stored data
        writer.println("GET /weather.json HTTP/1.1");
        writer.println("Host: localhost");
        writer.println();
        
        // Read response
        String responseLine = reader.readLine();
        assertEquals("HTTP/1.1 200 OK", responseLine);

        boolean isDataFound = false;
        while ((responseLine = reader.readLine()) != null) {
            if (responseLine.contains("{")) {
                isDataFound = true;
                break;
            }
        }

        assertFalse(isDataFound);
        socket.close();
    }
  boolean isDataFound = false;

}
