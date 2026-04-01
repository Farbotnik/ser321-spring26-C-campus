package taskone;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import taskone.proto.Request;
import taskone.proto.Response;
import taskone.proto.TaskProto;
import taskone.proto.TaskList;

/**
 * Task Management Client.
 * Provides a menu-based interface to interact with the task server.
 */
public class Client {
    private static Socket socket;

    private static InputStream inStream; // For proto
    private static OutputStream outStream; // For proto
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

            /////////////////////////////////////////////////////////////////////////////
                        // Welcome Proto
            /////////////////////////////////////////////////////////////////////////////
            Response response = Response.parseDelimitedFrom(inStream);
            System.out.println(response.getMessage());
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
    private static void addTask() throws IOException {
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

        // Create and send request
        Request request = Request.newBuilder()
                .setType(Request.RequestType.ADD)
                .setDescription(description)
                .setCategory(category)
                .build();
        request.writeDelimitedTo(outStream);

        // Read response
        Response response = Response.parseDelimitedFrom(inStream);

        if (response != null) {
            if (response.getType() == Response.ResponseType.SUCCESS) {
                TaskProto task = response.getTask();
                System.out.println("Task added successfully!");
                System.out.println("  ID: " + task.getId());
                System.out.println("  Description: " + task.getDescription());
                System.out.println("  Category: " + task.getCategory());
            } else {
                System.out.println("Error: " + response.getMessage());
            }
        }
    }

    /**
     * List tasks with filter options.
     */
    private static void listAllTasks() throws IOException {
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

        // Create and send request
        Request request = Request.newBuilder()
                .setType(Request.RequestType.LIST)
                .setFilter(filter)
                .build();
        request.writeDelimitedTo(outStream);

        // Read response
        Response response = Response.parseDelimitedFrom(inStream);

        // handle response
        if (response != null) {
            if (response.getType() == Response.ResponseType.SUCCESS) {
                TaskList taskList =response.getTaskList();
                int count = taskList.getCount();

                System.out.println("\n" + filter.toUpperCase() + " TASKS (" + count + "):");
                System.out.println("─────────────────────────────────────────────────");

                if (count == 0) {
                    System.out.println("No tasks found.");
                } else {
                    for (TaskProto task : taskList.getTasksList()) {
                        System.out.println(formatTask(task));
                    }
                }
            } else {
                System.out.println("Error: " + response.getMessage());
            }
        }
    }

    /**
     * Mark a task as finished.
     */
    private static void finishTask() throws IOException {
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
        Request request = Request.newBuilder()
                .setType(Request.RequestType.FINISH)
                .setId(id)
                .build();
        request.writeDelimitedTo(outStream);

        // Send request and get response
        Response response = Response.parseDelimitedFrom(inStream);
        if (response != null) {
            if (response.getType() == Response.ResponseType.SUCCESS) {
                System.out.println(response.getMessage());
            } else {
                System.out.println("Error: " + response.getMessage());
            }
        }
    }

    /**
     * Quit the application.
     */
    private static void quit() throws IOException {
        System.out.println("\n--- Quitting ---");

        // Create and send request
        Request request = Request.newBuilder()
                .setType(Request.RequestType.QUIT)
                .build();
        request.writeDelimitedTo(outStream);

        // Read response
        Response response= Response.parseDelimitedFrom(inStream);
        if (response != null && response.getType() == Response.ResponseType.SUCCESS) {
            System.out.println(response.getMessage());
        }
    }

    /**
     * Format a task for display.
     */
    private static String formatTask(TaskProto task) {
        String status;
        if (task.getFinished()) {
            status = "[DONE]";
        } else {
            status = "[PENDING]";
        }
        String categoryTag;
        switch (task.getCategory()) {
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

        return String.format("%s #%d %s %s", status, task.getId(), categoryTag, task.getDescription());
    }

    /**
     * Clean up resources.
     */
    private static void cleanup() {
        try {
            if (scanner != null) {
                scanner.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }
}
