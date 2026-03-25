package taskone;

import java.io.*;
import java.net.Socket;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;
import taskone.proto.Response;

/**
 * Performer class handles client requests using JSON protocol.
 * This version uses JSON for serialization.
 */
public class Performer {
    private final Socket clientSocket;
    private final TaskList taskList;

    private InputStream inStream; // For proto
    private OutputStream outStream; // For proto

    private BufferedReader in; // For JSON
    private PrintWriter out; // For JSON

    public Performer(Socket clientSocket, TaskList taskList) {
        this.clientSocket = clientSocket;
        this.taskList = taskList;
    }

    /**
     * Main method to process client requests.
     * Reads requests, processes them, and sends responses.
     */
    public void doPerform() {
        try {
            inStream = clientSocket.getInputStream();
            outStream = clientSocket.getOutputStream();
            in = new BufferedReader(new InputStreamReader(inStream));
            out = new PrintWriter(outStream, true);

            // Use either JSON or Proto from the examples below,
            // and make matching changes in Client.java.

                /////////////////////////////////////////////////////////////////////////////
                            // Welcome JSON
                /////////////////////////////////////////////////////////////////////////////
            // Send welcome message. You can keep this as JSON.
            JSONObject welcomeMessage = JsonUtils.createSuccessResponse("connect", "Connected to Task Management Server");
            out.println(welcomeMessage);
                /////////////////////////////////////////////////////////////////////////////
                            // End Welcome JSON
                /////////////////////////////////////////////////////////////////////////////



                /////////////////////////////////////////////////////////////////////////////
                            // Welcome Proto
                /////////////////////////////////////////////////////////////////////////////
//            Response.Builder protoResp = Response.newBuilder().setType(Response.ResponseType.SUCCESS).setMessage("Connected to Proto Task Management Server");
//            protoResp.build().writeDelimitedTo(outStream);
                /////////////////////////////////////////////////////////////////////////////
                            // End Welcome Proto
                /////////////////////////////////////////////////////////////////////////////




            // Process requests
            String request; // would need to be changed to proto
            while (true) {
                request = in.readLine(); // Read as String JSON, not Proto yet.
                // We intentionally skip error handling here to focus on Proto conversion.
                // This may fail if the request is malformed or missing expected fields, which is ok this time.

                // Once you start changing things more and more to proto, the JSON parts might not work anymore.
                // That is fine, just do not call these requests until converted.
                // Start with the "add" request.

                System.out.println(request);
                JSONObject requestJSON = new JSONObject(request);
                String type = requestJSON.getString("type");

                JSONObject responseJSON;
                System.out.println(type);
                // Change all the following requests/responses to JSON here and in Client.java
                switch (type) {
                    case "add":
                        responseJSON = handleAdd(requestJSON); // would need to be changed to return ProtoRes and get ProtReq
                        break;
                    case "list":
                        responseJSON = handleList(requestJSON);
                        break;
                    case "finish":
                        responseJSON = handleFinish(requestJSON);
                        break;
                    case "quit":
                        responseJSON = handleQuit();
                        break;
                    default:
                        responseJSON = JsonUtils.createErrorResponse(type, "Unknown request type: " + type);
                }

                out.println(responseJSON.toString()); // send json

                // If quit, break the loop
                if (responseJSON.has("type") && responseJSON.getString("type").equals("quit")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private JSONObject handleAdd(JSONObject request) { // will need to change to not use JSON anymore - or make new method
        // Validation is intentionally removed so students can focus on Proto conversion.
        // These comments show what production-style validation would look like. You can also delete all these
        // if they annoy you since it makes it harder to read it

//        // Validate required fields
//        if (!request.has("description")) {
//            return JsonUtils.createErrorResponse("add", "Missing 'description' field");
//        }
//        if (!request.has("category")) {
//            return JsonUtils.createErrorResponse("add", "Missing 'category' field");
//        }

        String description = request.getString("description");
        String category = request.getString("category");

//        // Validate description not empty
//        if (description.trim().isEmpty()) {
//            return JsonUtils.createErrorResponse("add", "Description cannot be empty");
//        }
//
//        // Validate category value
//        if (!category.equals("work") && !category.equals("personal") && !category.equals("school") && !category.equals("other")) {
//            return JsonUtils.createErrorResponse("add", "Invalid category value. Must be 'work', 'personal', 'school', or 'other'");
//        }

        // Add task
        Task task = taskList.addTask(description, category); // Assume valid input for this starter version.

        // Return success response with created task
        return JsonUtils.createSuccessResponse("add", JsonUtils.taskToJson(task));
    }


    private JSONObject handleList(JSONObject request) {
        // Get filter (defaults to "all")
        String filter = request.optString("filter", "all");

        List<Task> tasks;
        switch (filter) {
            case "all":
                tasks = taskList.listAllTasks();
                break;
            case "pending":
                tasks = taskList.listPendingTasks();
                break;
            case "finished":
                tasks = taskList.listFinishedTasks();
                break;
            default:
                return JsonUtils.createErrorResponse("list", "Invalid filter value. Must be 'all', 'pending', or 'finished'"); // Keep similar error semantics in Proto.
        }

        // Convert tasks to JSON array
        JSONArray taskArray = new JSONArray();
        for (Task task : tasks) {
            taskArray.put(JsonUtils.taskToJson(task));
        }

        // Create response data
        JSONObject data = new JSONObject();
        data.put("tasks", taskArray);
        data.put("count", tasks.size());

        return JsonUtils.createSuccessResponse("list", data);
    }

    private JSONObject handleFinish(JSONObject request) {
        // Validation intentionally skipped.

        int id = request.getInt("id");
        // Mark task as finished
        boolean success = taskList.finishTask(id);

        if (success) {
            JSONObject data = new JSONObject();
            data.put("message", "Task #" + id + " marked as finished");
            return JsonUtils.createSuccessResponse("finish", data);
        } else {
            return JsonUtils.createErrorResponse("finish", "Task not found with ID: " + id);
        }
    }

    private JSONObject handleQuit() {
        JSONObject data = new JSONObject();
        data.put("message", "Goodbye!");
        return JsonUtils.createSuccessResponse("quit", data);
    }
}
