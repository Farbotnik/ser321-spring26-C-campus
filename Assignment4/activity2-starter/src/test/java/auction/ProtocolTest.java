package auction;

import buffers.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.Socket;

/**
 * Protocol compliance tests for AuctionServer.
 *
 * These tests verify that the protocol is followed.
 *
 * USAGE:
 * 1. Start your server: gradle runServer --args="--grading"
 * 2. Run tests: gradle test
 *
 * Tests use grading mode for deterministic results.
**/

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProtocolTest {

    private static final String HOST = "localhost";
    private static final int PORT = 8889;
    private static final int TIMEOUT = 5000; // 5 second timeout

    private Socket socket;
    private InputStream in;
    private OutputStream out;

    /**
     * Connect to server before each test.
     */
    @BeforeEach
    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        socket.setSoTimeout(TIMEOUT);
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    /**
     * Disconnect after each test.
     */
    @AfterEach
    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Test 1: Initial Connection
     * Server should send WELCOME message on connect.
     */
    @Test
    @Order(1)
    public void testInitialWelcome() throws IOException {
        Response welcome = Response.parseDelimitedFrom(in);

        assertNotNull(welcome, "Server should send welcome message on connect");
        assertEquals(Response.ResponseType.WELCOME, welcome.getType(),
            "Initial message should be WELCOME");
        assertTrue(welcome.getOk(), "Welcome should have ok=true");
        assertTrue(welcome.getMessage().toLowerCase().contains("welcome"),
            "Welcome message should contain 'welcome'");
    }

    /**
     * Test 2: REGISTER Request - Valid Name
     * Server should accept valid name and send WELCOME with gold amount.
     */
    @Test
    @Order(2)
    public void testRegisterRequestValid() throws IOException {
        // Read initial welcome
        Response.parseDelimitedFrom(in);

        // Send REGISTER request
        Request registerRequest = Request.newBuilder()
            .setType(Request.RequestType.REGISTER)
            .setName("TestPlayer")
            .build();
        registerRequest.writeDelimitedTo(out);

        // Read response
        Response response = Response.parseDelimitedFrom(in);

        assertNotNull(response, "Server should respond to REGISTER request");
        assertEquals(Response.ResponseType.WELCOME, response.getType(),
            "Response should be WELCOME");
        assertTrue(response.getOk(), "REGISTER request should succeed");
        assertTrue(response.getMessage().contains("TestPlayer"),
            "Response should include player name");
        assertTrue(response.getMessage().contains("150") || response.getMessage().contains("gold"),
            "Response should mention starting gold");
    }

    /**
     * Test 3: REGISTER Request - Empty Name
     * Server should reject empty names with ERROR.
     */
    @Test
    @Order(3)
    public void testRegisterRequestEmpty() throws IOException {
        Response.parseDelimitedFrom(in); // Initial welcome

        Request registerRequest = Request.newBuilder()
            .setType(Request.RequestType.REGISTER)
            .setName("")
            .build();
        registerRequest.writeDelimitedTo(out);

        Response response = Response.parseDelimitedFrom(in);

        assertNotNull(response);
        assertEquals(Response.ResponseType.ERROR, response.getType(),
            "Empty name should return ERROR");
        assertFalse(response.getOk(), "Error response should have ok=false");
        assertTrue(response.getMessage().toLowerCase().contains("empty"),
            "Error should mention empty name");
    }

    /**
     * Test 4: QUIT Request
     * Server should send FAREWELL response.
     */
    @Test
    @Order(4)
    public void testQuitRequest() throws IOException {
        Response.parseDelimitedFrom(in);

        Request quitRequest = Request.newBuilder()
            .setType(Request.RequestType.QUIT)
            .build();
        quitRequest.writeDelimitedTo(out);

        Response response = Response.parseDelimitedFrom(in);

        assertEquals(Response.ResponseType.FAREWELL, response.getType());
        assertTrue(response.getOk());
        assertTrue(response.getMessage().toLowerCase().contains("goodbye") ||
                   response.getMessage().toLowerCase().contains("bye"),
            "FAREWELL message should be friendly");
    }

    // ====================================================================
    // ADD YOUR TESTS BELOW (at least 3 more tests required)
    // ====================================================================

    /**
     * Test 5: JOIN Request
     * After registering, JOIN should return GAME_JOINED with first item and player gold.
     */
    @Test
    @Order(5)
    public void testJoinStartsGame() throws IOException {
        Response.parseDelimitedFrom(in); // initial welcome
        sendRegister("JoinTester");

        sendJoin();
        Response response = Response.parseDelimitedFrom(in);

        assertNotNull(response, "Server should respond to JOIN");
        assertEquals(Response.ResponseType.GAME_JOINED, response.getType(),
            "JOIN should return GAME_JOINED");
        assertTrue(response.getOk(), "GAME_JOINED should have ok=true");
        assertTrue(response.hasNextItem(), "GAME_JOINED should include first item");
        assertTrue(response.hasPlayerStatus(), "GAME_JOINED should include player_status");
        assertEquals(150, response.getPlayerStatus().getGoldRemaining(),
            "Player should start with 150 gold");
        assertTrue(response.getNextItem().getReservePrice() > 0,
            "First item should have a reserve price");
    }

    /**
     * Test 6: BID with -1 (skip) — bots still compete, BID_RESULT returned
     */
    @Test
    @Order(6)
    public void testSkipBid() throws IOException {
        Response.parseDelimitedFrom(in); // initial welcome
        sendRegister("SkipTester");

        sendJoin();
        Response joined = Response.parseDelimitedFrom(in);
        int firstItemId = joined.getNextItem().getId();

        // Send skip bid (-1)
        Request skipBid = Request.newBuilder()
            .setType(Request.RequestType.BID)
            .setItemId(firstItemId)
            .setBidAmount(-1)
            .build();
        skipBid.writeDelimitedTo(out);

        Response response = Response.parseDelimitedFrom(in);

        assertNotNull(response, "Server should respond to skip bid");
        assertEquals(Response.ResponseType.BID_RESULT, response.getType(),
            "Skip bid should return BID_RESULT");
        assertTrue(response.getOk(), "BID_RESULT should have ok=true");
        assertTrue(response.hasResult(), "BID_RESULT should include auction result");
        assertEquals(4, response.getResult().getAllBidsCount(),
            "All 4 participants (player + 3 bots) should have bids listed");
        // Player skipped so bots compete — winner should not be the player
        // (player bid treated as 0, which is below any reserve price)
        assertTrue(response.hasPlayerStatus(), "BID_RESULT should include updated player_status");
    }

    /**
     * Test 7: Reserve price enforced — bid above 0 but below reserve returns ERROR
     */
    @Test
    @Order(7)
    public void testReservePriceEnforced() throws IOException {
        Response.parseDelimitedFrom(in); // initial welcome
        sendRegister("ReserveTester");

        sendJoin();
        Response joined = Response.parseDelimitedFrom(in);
        int itemId = joined.getNextItem().getId();
        int reserve = joined.getNextItem().getReservePrice();

        // Only attempt if reserve > 1 so we can bid below it
        assertTrue(reserve > 1, "Reserve price should be > 1 for this test to be meaningful");

        Request lowBid = Request.newBuilder()
            .setType(Request.RequestType.BID)
            .setItemId(itemId)
            .setBidAmount(1) // guaranteed below reserve
            .build();
        lowBid.writeDelimitedTo(out);

        Response response = Response.parseDelimitedFrom(in);

        assertEquals(Response.ResponseType.ERROR, response.getType(),
            "Bid below reserve price should return ERROR");
        assertFalse(response.getOk(), "Error response should have ok=false");
        assertTrue(response.getMessage().toLowerCase().contains("reserve"),
            "Error should mention reserve price");
    }



    // Helper methods if you want to use them

    private void sendRegister(String name) throws IOException {
        Request registerRequest = Request.newBuilder()
            .setType(Request.RequestType.REGISTER)
            .setName(name)
            .build();
        registerRequest.writeDelimitedTo(out);
        Response.parseDelimitedFrom(in); // Consume response
    }

    private void sendJoin() throws IOException {
        Request joinRequest = Request.newBuilder()
            .setType(Request.RequestType.JOIN)
            .build();
        joinRequest.writeDelimitedTo(out);
    }
}
