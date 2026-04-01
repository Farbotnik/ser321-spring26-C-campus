package taskone;

import java.io.*;
import java.net.Socket;
import java.util.List;
import taskone.proto.Request;
import taskone.proto.Response;
import taskone.proto.TaskProto;

/**
 * Performer class handles client requests using JSON protocol.
 * This version uses JSON for serialization.
 */
public class Performer {
    private final Socket clientSocket;
    private final TaskList taskList;

    private InputStream inStream; // For proto
    private OutputStream outStream; // For proto

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
            /////////////////////////////////////////////////////////////////////////////
                        // Welcome Proto
            /////////////////////////////////////////////////////////////////////////////
            Response.Builder protoResp = Response.newBuilder().setType(Response.ResponseType.SUCCESS).setMessage("Connected to Proto Task Management Server");
            protoResp.build().writeDelimitedTo(outStream);
            /////////////////////////////////////////////////////////////////////////////
                        // End Welcome Proto
            /////////////////////////////////////////////////////////////////////////////

            // Process requests
            while (true) {
                Request request = Request.parseDelimitedFrom(inStream);
                if (request == null) break;

                System.out.println(request.getType());
                Response response;

                switch (request.getType()) {
                    case ADD:
                        response = handleAdd(request);
                        break;
                    case LIST:
                        response = handleList(request);
                        break;
                    case FINISH:
                        response = handleFinish(request);
                        break;
                    case QUIT:
                        response = handleQuit();
                        break;
                    default:
                        response = Response.newBuilder().setType(Response.ResponseType.ERROR).setMessage("Unknown request type").build();
                }

                response.writeDelimitedTo(outStream);

                if (request.getType() == Request.RequestType.QUIT) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private Response handleAdd(Request request) {
        String description = request.getDescription();
        String category =request.getCategory();
        Task task = taskList.addTask(description, category);

        TaskProto taskProto = TaskProto.newBuilder()
                .setId(task.getId())
                .setDescription(task.getDescription())
                .setCategory(task.getCategory())
                .setAssignee(task.getAssignee())
                .setFinished(task.isFinished())
                .build();

        return Response.newBuilder()
                .setType(Response.ResponseType.SUCCESS)
                .setTask(taskProto)
                .build();
    }

    private Response handleList(Request request) {
        String filter;
        if (request.getFilter().isEmpty()) {
            filter = "all";
        } else {
            filter = request.getFilter();
        }

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
                return Response.newBuilder()
                        .setType(Response.ResponseType.ERROR)
                        .setMessage("Invalid filter value. Must be 'all', 'pending', or 'finished'")
                        .build();
        }

        taskone.proto.TaskList.Builder taskListBuilder = taskone.proto.TaskList.newBuilder();
        for (Task task : tasks) {
            taskListBuilder.addTasks(TaskProto.newBuilder()
                    .setId(task.getId())
                    .setDescription(task.getDescription())
                    .setCategory(task.getCategory())
                    .setAssignee(task.getAssignee())
                    .setFinished(task.isFinished())
                    .build());
        }
        taskListBuilder.setCount(tasks.size());

        return Response.newBuilder()
                .setType(Response.ResponseType.SUCCESS)
                .setTaskList(taskListBuilder.build())
                .build();
    }

    private Response handleFinish(Request request) {
        int id = request.getId();
        boolean success = taskList.finishTask(id);

        if (success) {
            return Response.newBuilder()
                    .setType(Response.ResponseType.SUCCESS)
                    .setMessage("Task #" + id + " marked as finished")
                    .build();
        } else {
            return Response.newBuilder()
                    .setType(Response.ResponseType.ERROR)
                    .setMessage("Task not found with ID: " + id)
                    .build();
        }
    }

    private Response handleQuit() {
        return Response.newBuilder()
                .setType(Response.ResponseType.SUCCESS)
                .setMessage("Goodbye!")
                .build();
    }
}
