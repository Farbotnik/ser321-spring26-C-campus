package taskone;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Multi-threaded Task Management Server. Creates a new thread per client
 */
public class ThreadedServer {
    private static final int DEFAULT_PORT = 8888;
    private static TaskList taskList = new TaskList();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port: " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }

        System.out.println("Task Management Server starting on port " + port);
        System.out.println("Mode: Multi-threaded (one thread per client)");
        System.out.println("Waiting for clients...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket;
                // Accept connection (blocks until client connects)
                try {
                    
                    clientSocket = serverSocket.accept();
                } catch (IOException e) {
                    System.err.println("Failed to accept client connection: " + e.getMessage());
                    continue;
                }

                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                Thread clientThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Handle client request (BLOCKS - single-threaded)
                            Performer performer = new Performer(clientSocket, taskList);
                            performer.doPerform();
                        } catch (Exception e) {
                            System.err.println("Unexpected error handling client: " + e.getMessage());
                        } finally {
                            try {
                                // Close connection
                                if (!clientSocket.isClosed()) {
                                    clientSocket.close();
                                }
                            } catch (IOException e) {
                                System.err.println("Error closing client socket: " + e.getMessage());
                            }
                            System.out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
                        }
                    }
                });

                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
