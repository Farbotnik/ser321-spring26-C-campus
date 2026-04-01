package taskone;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import org.json.JSONObject;
import org.json.JSONArray;
import taskone.proto.Response;

/**
 * Task Management Client.
 * Provides a menu-based interface to interact with the task server.
 * You will need to edit this when you change it to proto
 */
public class Client {
    private static Socket socket;

    private static InputStream inStream; // For proto
    private static OutputStream outStream; // For proto
    private static BufferedReader in; // For JSON
    private static PrintWriter out; // For JSON
    private static Scanner scanner;

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8888;

        // Parse command line arguments
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: 8888");
            }
        }

        scanner = new Scanner(System.in);

        try {
            // Connect to server
            System.out.println("Trying to connect to Task Management Server at " + host + ":" + port);
            socket = new Socket(host, port);

            // Proto uses these streams.
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();

            // JSON uses these wrappers.
            in = new BufferedReader(new InputStreamReader(inStream));
            out = new PrintWriter(outStream, true);

            // Use either JSON or Proto from the examples below,
            // and make matching changes in Performer.java.

                /////////////////////////////////////////////////////////////////////////////
                            // Welcome JSON
                /////////////////////////////////////////////////////////////////////////////
            String welcomeMsg = in.readLine();
            if (welcomeMsg != null) {
                JSONObject welcomeMessage = new JSONObject(welcomeMsg);
                System.out.println(welcomeMessage);
                if (welcomeMessage.getBoolean("ok")) {
                    System.out.println(welcomeMessage.getString("data"));
                }
            }
                /////////////////////////////////////////////////////////////////////////////
                            // End Welcome JSON
                /////////////////////////////////////////////////////////////////////////////

                /////////////////////////////////////////////////////////////////////////////
                            // Welcome Proto
                /////////////////////////////////////////////////////////////////////////////
//            Response response = Response.parseDelimitedFrom(inStream);
//            System.out.println(response.getMessage());
                /////////////////////////////////////////////////////////////////////////////
                            // End Welcome Proto
                /////////////////////////////////////////////////////////////////////////////

            // Main menu loop
            boolean running = true;
            while (running) {
                displayMenu();
                int choice = getMenuChoice();

                switch (choice) {
                    case 1:
                        addTask();
                        break;
                    case 2:
                        listAllTasks();
                        break;
                    case 3:
                        finishTask();
                        break;
                    case 0:
                        quit();
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * Display the main menu.
     */
    private static void displayMenu() {
        System.out.println("\n========== Task Management Menu ==========");
        System.out.println("1. Add Task");
        System.out.println("2. List Tasks");
        System.out.println("3. Finish Task");
        System.out.println("0. Quit");
        System.out.println("==========================================");
        System.out.print("Enter your choice: ");
    }

    /**
     * Get user's menu choice.
     */
    private static int getMenuChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Add a new task.
     */
    private static void addTask() {
        System.out.println("\n--- Add Task ---");
        System.out.print("Enter task description: ");
        String description = scanner.nextLine().trim();

        if (description.isEmpty()) {
            System.out.println("Error: Description cannot be empty");
            return;
        }

        System.out.print("Enter category (work/personal/school/other): ");
        String category = scanner.nextLine().trim().toLowerCase();

        if (!category.equals("work") && !category.equals("personal") && !category.equals("school") && !category.equals("other")) {
            System.out.println("Error: Invalid category. Must be 'work', 'personal', 'school', or 'other'");
            return;
        }

        // Create request (convert this to Proto)
        JSONObject request = new JSONObject();
        request.put("type", "add");
        request.put("description", description);
        request.put("category", category);

        // Send request and get response
        JSONObject response = sendRequest(request); // Replace this with:
        // request.writeDelimitedTo(outStream); // where request is a Request proto
        // Response response = Response.parseDelimitedFrom(inStream); // Read response as Proto
        // Then parse the Proto response.


        if (response != null) {
            if (response.getBoolean("ok")) {
                JSONObject taskData = response.getJSONObject("data");
                System.out.println("Task added successfully!");
                System.out.println("  ID: " + taskData.getInt("id"));
                System.out.println("  Description: " + taskData.getString("description"));
                System.out.println("  Category: " + taskData.getString("category"));
            } else {
                JSONObject error = response.getJSONObject("data");
                System.out.println("Error: " + error.getString("error"));
            }
        }
    }

    /**
     * List tasks with filter options.
     */
    private static void listAllTasks() {
        System.out.println("\n--- List Tasks ---");
        System.out.println("1. All tasks");
        System.out.println("2. Pending tasks");
        System.out.println("3. Finished tasks");
        System.out.print("Enter your choice: ");

        int choice = getMenuChoice();
        String filter;

        switch (choice) {
            case 1:
                filter = "all";
                break;
            case 2:
                filter = "pending";
                break;
            case 3:
                filter = "finished";
                break;
            default:
                System.out.println("Invalid choice");
                return;
        }

        // Create request in JSON (convert this to Proto)
        JSONObject request = new JSONObject();
        request.put("type", "list");
        request.put("filter", filter);

        // Send request and get response
        JSONObject response = sendRequest(request); // Same conversion approach as in addTask().

        // handle response
        if (response != null) {
            if (response.getBoolean("ok")) {
                JSONObject data = response.getJSONObject("data");
                JSONArray tasks = data.getJSONArray("tasks");
                int count = data.getInt("count");

                System.out.println("\n" + filter.toUpperCase() + " TASKS (" + count + "):");
                System.out.println("─────────────────────────────────────────────────");

                if (count == 0) {
                    System.out.println("No tasks found.");
                } else {
                    for (int i = 0; i < tasks.length(); i++) {
                        JSONObject task = tasks.getJSONObject(i);
                        System.out.println(formatTask(task));
                    }
                }
            } else {
                JSONObject error = response.getJSONObject("data");
                System.out.println("Error: " + error.getString("error"));
            }
        }
    }

    /**
     * Mark a task as finished.
     */
    private static void finishTask() {
        System.out.println("\n--- Finish Task ---");
        System.out.print("Enter task ID to finish: ");

        int id;
        try {
            id = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid task ID");
            return;
        }

        // Create request
        JSONObject request = new JSONObject();
        request.put("type", "finish");
        request.put("id", id);

        // Send request and get response
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                JSONObject data = response.getJSONObject("data");
                System.out.println(data.getString("message"));
            } else {
                JSONObject error = response.getJSONObject("data");
                System.out.println("Error: " + error.getString("error"));
            }
        }
    }

    /**
     * Quit the application.
     */
    private static void quit() {
        System.out.println("\n--- Quitting ---");

        // Create request
        JSONObject request = new JSONObject();
        request.put("type", "quit");

        // Send request and get response
        JSONObject response = sendRequest(request);
        if (response != null && response.getBoolean("ok")) {
            JSONObject data = response.getJSONObject("data");
            System.out.println(data.getString("message"));
        }
    }

    /**
     * Send a request to the server and receive response.
     */
    private static JSONObject sendRequest(JSONObject request) {
        try {
            // Send request
            out.println(request.toString());

            // Receive response
            String responseLine = in.readLine();
            if (responseLine != null) {
                return new JSONObject(responseLine);
            } else {
                System.out.println("Error: No response from server");
                return null;
            }
        } catch (IOException e) {
            System.out.println("Error communicating with server: " + e.getMessage());
            return null;
        }
    }

    /**
     * Format a task for display.
     */
    private static String formatTask(JSONObject task) {
        int id = task.getInt("id");
        String description = task.getString("description");
        String category = task.getString("category");
        boolean finished = task.getBoolean("finished");

        String status = finished ? "[DONE]" : "[PENDING]";
        String categoryTag;
        switch (category) {
            case "work":
                categoryTag = "[WORK]";
                break;
            case "personal":
                categoryTag = "[PERSONAL]";
                break;
            case "school":
                categoryTag = "[SCHOOL]";
                break;
            default:
                categoryTag = "[OTHER]";
                break;
        }

        return String.format("%s #%d %s %s",
                status, id, categoryTag, description);
    }

    /**
     * Clean up resources.
     */
    private static void cleanup() {
        try {
            if (scanner != null) {
                scanner.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }
}
