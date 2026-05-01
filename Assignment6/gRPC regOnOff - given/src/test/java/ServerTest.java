import com.google.protobuf.Empty;
import example.grpcclient.Client;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONArray;
import org.json.JSONObject;
import service.*;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Server unit tests for Assignment 6.
 *
 * IMPORTANT: These tests require the server to be running BEFORE you run them.
 *
 * To run these tests:
 * 1. First, start the server in one terminal: gradle runNode
 * 2. Then, in another terminal, run: gradle test
 *
 * The tests connect to localhost:8000 (the default port for runNode).
 * Make sure your server is running on this port before running tests.
 *
 * TODO for students:
 * This file contains example tests for the Echo and Joke services.
 * You need to add your own tests for:
 * - Converter service (happy path and error cases)
 * - Library service (happy path, error cases, and persistence testing)
 *
 * Your tests should follow the same pattern as the examples below.
 */
public class ServerTest {

    ManagedChannel channel;
    private EchoGrpc.EchoBlockingStub blockingStub;
    private JokeGrpc.JokeBlockingStub blockingStub2;
    private ConverterGrpc.ConverterBlockingStub converterStub;
    private LibraryGrpc.LibraryBlockingStub libraryStub;


    @org.junit.Before
    public void setUp() throws Exception {
        // assuming default port and localhost for our testing, make sure Node runs on this port
        channel = ManagedChannelBuilder.forTarget("localhost:8000").usePlaintext().build();

        blockingStub = EchoGrpc.newBlockingStub(channel);
        blockingStub2 = JokeGrpc.newBlockingStub(channel);
        converterStub = ConverterGrpc.newBlockingStub(channel);
        libraryStub = LibraryGrpc.newBlockingStub(channel);
    }

    @org.junit.After
    public void close() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);

    }


    @Test
    public void parrot() {
        // success case
        ClientRequest request = ClientRequest.newBuilder().setMessage("test").build();
        ServerResponse response = blockingStub.parrot(request);
        assertTrue(response.getIsSuccess());
        assertEquals("test", response.getMessage());

        // error cases
        request = ClientRequest.newBuilder().build();
        response = blockingStub.parrot(request);
        assertFalse(response.getIsSuccess());
        assertEquals("No message provided", response.getError());

        request = ClientRequest.newBuilder().setMessage("").build();
        response = blockingStub.parrot(request);
        assertFalse(response.getIsSuccess());
        assertEquals("No message provided", response.getError());
    }

    // For this test the server needs to be started fresh AND the list of jokes needs to be the initial list
    @Test
    public void joke() {
        // getting first joke
        JokeReq request = JokeReq.newBuilder().setNumber(1).build();
        JokeRes response = blockingStub2.getJoke(request);
        assertEquals(1, response.getJokeCount());
        assertEquals("Did you hear the rumor about butter? Well, I'm not going to spread it!", response.getJoke(0));

        // getting next 2 jokes
        request = JokeReq.newBuilder().setNumber(2).build();
        response = blockingStub2.getJoke(request);
        assertEquals(2, response.getJokeCount());
        assertEquals("What do you call someone with no body and no nose? Nobody knows.", response.getJoke(0));
        assertEquals("I don't trust stairs. They're always up to something.", response.getJoke(1));

        // getting 2 more but only one more on server
        request = JokeReq.newBuilder().setNumber(2).build();
        response = blockingStub2.getJoke(request);
        assertEquals(2, response.getJokeCount());
        assertEquals("How do you get a squirrel to like you? Act like a nut.", response.getJoke(0));
        assertEquals("I am out of jokes...", response.getJoke(1));

        // trying to get more jokes but out of jokes
        request = JokeReq.newBuilder().setNumber(2).build();
        response = blockingStub2.getJoke(request);
        assertEquals(1, response.getJokeCount());
        assertEquals("I am out of jokes...", response.getJoke(0));

        // trying to add joke without joke field
        JokeSetReq req2 = JokeSetReq.newBuilder().build();
        JokeSetRes res2 = blockingStub2.setJoke(req2);
        assertFalse(res2.getOk());

        // trying to add empty joke
        req2 = JokeSetReq.newBuilder().setJoke("").build();
        res2 = blockingStub2.setJoke(req2);
        assertFalse(res2.getOk());

        // adding a new joke (well word)
        req2 = JokeSetReq.newBuilder().setJoke("whoop").build();
        res2 = blockingStub2.setJoke(req2);
        assertTrue(res2.getOk());

        // should have the new "joke" now and return it
        request = JokeReq.newBuilder().setNumber(1).build();
        response = blockingStub2.getJoke(request);
        assertEquals(1, response.getJokeCount());
        assertEquals("whoop", response.getJoke(0));
    }

    @Test
    public void converter() {
        // happy path - length: 1 km to miles
        ConversionRequest request = ConversionRequest.newBuilder()
            .setValue(1.0).setFromUnit("KILOMETER").setToUnit("MILE").build();
        ConversionResponse response = converterStub.convert(request);
        assertTrue(response.getIsSuccess());
        assertEquals(0.6214, response.getResult(), 0.001);

        // happy path - weight: 1 kg to pounds
        request = ConversionRequest.newBuilder()
            .setValue(1.0).setFromUnit("KILOGRAM").setToUnit("POUND").build();
        response = converterStub.convert(request);
        assertTrue(response.getIsSuccess());
        assertEquals(2.20462, response.getResult(), 0.001);

        // happy path - temperature: 100 C to F
        request = ConversionRequest.newBuilder()
            .setValue(100.0).setFromUnit("CELSIUS").setToUnit("FAHRENHEIT").build();
        response = converterStub.convert(request);
        assertTrue(response.getIsSuccess());
        assertEquals(212.0, response.getResult(), 0.001);

        // error - missing from_unit
        request = ConversionRequest.newBuilder()
            .setValue(1.0).setToUnit("MILE").build();
        response = converterStub.convert(request);
        assertFalse(response.getIsSuccess());
        assertEquals("from_unit is required", response.getError());

        // error - unsupported unit
        request = ConversionRequest.newBuilder()
            .setValue(1.0).setFromUnit("FURLONG").setToUnit("MILE").build();
        response = converterStub.convert(request);
        assertFalse(response.getIsSuccess());
        assertEquals("unsupported unit: FURLONG", response.getError());

        // error - cross-category conversion (length to weight)
        request = ConversionRequest.newBuilder()
            .setValue(1.0).setFromUnit("KILOMETER").setToUnit("KILOGRAM").build();
        response = converterStub.convert(request);
        assertFalse(response.getIsSuccess());
        assertEquals("units do not match - cannot convert KILOMETER to KILOGRAM", response.getError());

        // error - below absolute zero
        request = ConversionRequest.newBuilder()
            .setValue(-300.0).setFromUnit("CELSIUS").setToUnit("FAHRENHEIT").build();
        response = converterStub.convert(request);
        assertFalse(response.getIsSuccess());
        assertEquals("temperature below absolute zero (−273.15°C or −459.67°F)", response.getError());
    }

    @Test
    public void library() throws Exception {
        // happy path - list books
        BookListResponse listRes = libraryStub.listBooks(Empty.newBuilder().build());
        assertTrue(listRes.getIsSuccess());
        assertTrue(listRes.getBooksCount() > 0);

        // happy path - search by author
        BookListResponse searchRes = libraryStub.searchBooks(
            BookSearchRequest.newBuilder().setQuery("orwell").build());
        assertTrue(searchRes.getIsSuccess());
        assertEquals("1984", searchRes.getBooks(0).getTitle());

        // happy path - borrow then return
        String isbn = "978-0743273565"; // The Great Gatsby
        libraryStub.returnBook(ReturnRequest.newBuilder().setIsbn(isbn).build()); // ensure clean state
        BorrowResponse borrowRes = libraryStub.borrowBook(
            BorrowRequest.newBuilder().setIsbn(isbn).setBorrowerName("TestUser").build());
        assertTrue(borrowRes.getIsSuccess());
        assertTrue(borrowRes.getMessage().contains("Borrowed"));

        ReturnResponse returnRes = libraryStub.returnBook(
            ReturnRequest.newBuilder().setIsbn(isbn).build());
        assertTrue(returnRes.getIsSuccess());
        assertTrue(returnRes.getMessage().contains("Returned"));

        // error - empty search query
        BookListResponse badSearch = libraryStub.searchBooks(
            BookSearchRequest.newBuilder().setQuery("").build());
        assertFalse(badSearch.getIsSuccess());
        assertEquals("missing field", badSearch.getError());

        // error - borrow with invalid ISBN
        BorrowResponse notFound = libraryStub.borrowBook(
            BorrowRequest.newBuilder().setIsbn("000-0000000000").setBorrowerName("TestUser").build());
        assertFalse(notFound.getIsSuccess());
        assertEquals("book not found", notFound.getError());

        // error - borrow already borrowed book
        libraryStub.returnBook(ReturnRequest.newBuilder().setIsbn(isbn).build());
        libraryStub.borrowBook(BorrowRequest.newBuilder().setIsbn(isbn).setBorrowerName("UserA").build());
        BorrowResponse alreadyBorrowed = libraryStub.borrowBook(
            BorrowRequest.newBuilder().setIsbn(isbn).setBorrowerName("UserB").build());
        assertFalse(alreadyBorrowed.getIsSuccess());
        assertEquals("book is already borrowed", alreadyBorrowed.getError());
        libraryStub.returnBook(ReturnRequest.newBuilder().setIsbn(isbn).build()); // cleanup
    }

    // Two-phase persistence test:
    //   Run 1: borrows a book and saves it to library_data.json, then prints instructions.
    //   Restart the server: gradle runNode
    //   Run 2: the book is still borrowed (loaded from library_data.json), proving persistence.
    @Test
    public void libraryPersistence() throws Exception {
        String isbn = "978-1503280786"; // Moby-Dick
        String borrower = "PersistenceUser";

        // find the current state of Moby-Dick
        BookListResponse listRes = libraryStub.listBooks(Empty.newBuilder().build());
        Book mobyDick = null;
        for (Book b : listRes.getBooksList()) {
            if (b.getIsbn().equals(isbn)) {
                mobyDick = b;
                break;
            }
        }
        assertNotNull("Moby-Dick should be in the library", mobyDick);

        if (!mobyDick.getIsBorrowed()) {
            // Phase 1: book is available — borrow it so state is saved to library_data.json
            BorrowResponse res = libraryStub.borrowBook(
                BorrowRequest.newBuilder().setIsbn(isbn).setBorrowerName(borrower).build());
            assertTrue("borrow should succeed", res.getIsSuccess());

            // confirm the borrow was flushed to disk
            String content = new String(Files.readAllBytes(Paths.get("library_data.json")));
            JSONArray arr = new JSONArray(content);
            boolean found = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject book = arr.getJSONObject(i);
                if (book.optString("isbn", "").equals(isbn)) {
                    assertTrue(book.optBoolean("is_borrowed", false));
                    assertEquals(borrower, book.optString("borrowed_by", ""));
                    found = true;
                    break;
                }
            }
            assertTrue("borrow state must be written to library_data.json", found);
            System.out.println("PERSISTENCE PHASE 1: data saved. Stop the server, run 'gradle runNode', then run 'gradle test' again.");
        } else {
            // Phase 2: after server restart the book is still borrowed — persistence confirmed
            assertEquals("borrower name must survive server restart", borrower, mobyDick.getBorrowedBy());
            System.out.println("PERSISTENCE PHASE 2: data survived server restart!");

            // cleanup so Phase 1 can run again next time
            ReturnResponse ret = libraryStub.returnBook(ReturnRequest.newBuilder().setIsbn(isbn).build());
            assertTrue(ret.getIsSuccess());
        }
    }

}
