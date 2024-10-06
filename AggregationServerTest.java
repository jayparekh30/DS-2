import static org.junit.Assert.*;
import java.io.*;
import java.net.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AggregationServerTest {
    private static final int TEST_PORT = 4567;
    private ServerSocket serverSocket;
    private Thread serverThread;

     @Before
    public void setUp() throws Exception {
        serverThread = new Thread(() -> {
            try {
                AggregationServer.main(new String[]{"4567"});  // Start the server
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        Thread.sleep(500);  // Small delay to ensure the server starts
    }

    @After
    public void tearDown() throws Exception {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();  // Properly close the server socket
        }
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
  boolean isDataFound = false;

}
