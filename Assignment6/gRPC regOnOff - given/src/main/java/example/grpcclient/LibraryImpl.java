package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;
import com.google.protobuf.Empty;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LibraryImpl extends LibraryGrpc.LibraryImplBase {

    private static final String DATA_FILE = "library_data.json";
    private static final String BOOKS_FILE = "books.json";
    private List<JSONObject> books = new ArrayList<>();

    public LibraryImpl() {
        super();
        loadBooks();
    }

    private void loadBooks() {
        File dataFile = new File(DATA_FILE);
        File booksFile = new File(BOOKS_FILE);
        File toLoad;
        if (dataFile.exists()) {
            toLoad = dataFile;
        } else {
            toLoad = booksFile;
        }
        try {
            String content = new String(Files.readAllBytes(toLoad.toPath()));
            // parse JSON array into list
            JSONArray arr = new JSONArray(content);
            for (int i = 0; i < arr.length(); i++) {
                books.add(arr.getJSONObject(i));
            }
            System.out.println("Loaded " + books.size() + " books from " + toLoad.getName());
        } catch (Exception e) {
            System.out.println("Could not load books: " + e.getMessage());
        }
    }

    private void saveBooks() {
        try {
            // list back to JSON
            JSONArray arr = new JSONArray();
            for (JSONObject book : books) {
                arr.put(book);
            }
            Files.write(Paths.get(DATA_FILE), arr.toString(2).getBytes());
        } catch (Exception e) {
            System.out.println("Could not save books: " + e.getMessage());
        }
    }

    // map JSON fields to proto Book
    private Book jsonToBook(JSONObject obj) {
        return Book.newBuilder()
            .setTitle(obj.optString("title", ""))
            .setAuthor(obj.optString("author", ""))
            .setIsbn(obj.optString("isbn", ""))
            .setIsBorrowed(obj.optBoolean("is_borrowed", false))
            .setBorrowedBy(obj.optString("borrowed_by", ""))
            .build();
    }

    @Override
    public void listBooks(Empty req, StreamObserver<BookListResponse> responseObserver) {
        System.out.println("Received: listBooks");
        BookListResponse.Builder response = BookListResponse.newBuilder();
        // check if library empty
        if (books.isEmpty()) {
            response.setIsSuccess(false).setError("no books in library yet");
        } else {
            response.setIsSuccess(true);
            for (JSONObject book : books) {
                response.addBooks(jsonToBook(book));
            }
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void searchBooks(BookSearchRequest req, StreamObserver<BookListResponse> responseObserver) {
        System.out.println("Received: searchBooks - " + req.getQuery());
        BookListResponse.Builder response = BookListResponse.newBuilder();
        // check for empty query
        if (req.getQuery().isEmpty()) {
            response.setIsSuccess(false).setError("missing field");
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
            return;
        }
        if (books.isEmpty()) {
            response.setIsSuccess(false).setError("no books in library yet");
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
            return;
        }
        // match title or author
        String query = req.getQuery().toLowerCase();
        List<Book> matched = new ArrayList<>();
        for (JSONObject book : books) {
            String title = book.optString("title", "").toLowerCase();
            String author = book.optString("author", "").toLowerCase();
            if (title.contains(query) || author.contains(query)) {
                matched.add(jsonToBook(book));
            }
        }
        if (matched.isEmpty()) {
            response.setIsSuccess(false).setError("no books found matching query");
        } else {
            response.setIsSuccess(true);
            for (Book b : matched) {
                response.addBooks(b);
            }
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void borrowBook(BorrowRequest req, StreamObserver<BorrowResponse> responseObserver) {
        System.out.println("Received: borrowBook - isbn=" + req.getIsbn() + " borrower=" + req.getBorrowerName());
        BorrowResponse.Builder response = BorrowResponse.newBuilder();
        // validate required fields
        if (req.getIsbn().isEmpty()) {
            response.setIsSuccess(false).setError("missing field");
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
            return;
        }
        if (req.getBorrowerName().isEmpty()) {
            response.setIsSuccess(false).setError("borrower name is required");
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
            return;
        }
        // find book by ISBN
        for (JSONObject book : books) {
            if (book.optString("isbn", "").equals(req.getIsbn())) {
                if (book.optBoolean("is_borrowed", false)) {
                    response.setIsSuccess(false).setError("book is already borrowed");
                } else {
                    // mark borrowed
                    book.put("is_borrowed", true);
                    book.put("borrowed_by", req.getBorrowerName());
                    saveBooks();
                    response.setIsSuccess(true)
                        .setMessage("Borrowed: " + book.optString("title", ""));
                }
                responseObserver.onNext(response.build());
                responseObserver.onCompleted();
                return;
            }
        }
        response.setIsSuccess(false).setError("book not found");
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void returnBook(ReturnRequest req, StreamObserver<ReturnResponse> responseObserver) {
        System.out.println("Received: returnBook - isbn=" + req.getIsbn());
        ReturnResponse.Builder response = ReturnResponse.newBuilder();
        // check for missing ISBN
        if (req.getIsbn().isEmpty()) {
            response.setIsSuccess(false).setError("missing field");
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
            return;
        }
        // find book by ISBN
        for (JSONObject book : books) {
            if (book.optString("isbn", "").equals(req.getIsbn())) {
                if (!book.optBoolean("is_borrowed", false)) {
                    response.setIsSuccess(false).setError("book is not borrowed");
                } else {
                    // clear borrow fields
                    book.put("is_borrowed", false);
                    book.put("borrowed_by", "");
                    saveBooks();
                    response.setIsSuccess(true).setMessage("Returned: " + book.optString("title", ""));
                }
                responseObserver.onNext(response.build());
                responseObserver.onCompleted();
                return;
            }
        }
        response.setIsSuccess(false).setError("book not found");
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}
