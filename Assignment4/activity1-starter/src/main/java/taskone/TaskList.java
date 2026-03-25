package taskone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task list that manages tasks.
 *
 * NOTE: This class is NOT thread-safe yet. You need to make it thread safe
 * for Part B when implementing the threaded server.
 */
public class TaskList {
    private final List<Task> tasks;
    private final AtomicInteger nextId;

    public TaskList() {
        this.tasks = new ArrayList<>();
        this.nextId = new AtomicInteger(1);
    }

    /**
     * Add a new task to the list.
     * @param description Task description
     * @param category Task category (work, personal, school, other)
     * @return The created Task object
     */
    public Task addTask(String description, String category) {
        Task task = new Task(nextId.getAndIncrement(), description, category);
        tasks.add(task);
        return task;
    }

    /**
     * Get all tasks.
     * @return Copy of task list
     */
    public List<Task> listAllTasks() {
        return new ArrayList<>(tasks);
    }

    /**
     * Get pending (incomplete) tasks.
     * @return List of pending tasks
     */
    public List<Task> listPendingTasks() {
        List<Task> pending = new ArrayList<>();
        for (Task task : tasks) {
            if (!task.isFinished()) {
                pending.add(task);
            }
        }
        return pending;
    }

    /**
     * Get finished tasks.
     * @return List of finished tasks
     */
    public List<Task> listFinishedTasks() {
        List<Task> finished = new ArrayList<>();
        for (Task task : tasks) {
            if (task.isFinished()) {
                finished.add(task);
            }
        }
        return finished;
    }

    /**
     * Find a task by ID.
     * @param id Task ID
     * @return Task object or null if not found
     */
    public Task findTaskById(int id) {
        for (Task task : tasks) {
            if (task.getId() == id) {
                return task;
            }
        }
        return null;
    }

    /**
     * Mark a task as finished.
     * @param id Task ID
     * @return true if successful, false if task not found
     */
    public boolean finishTask(int id) {
        Task task = findTaskById(id);
        if (task != null) {
            task.setFinished(true);
            return true;
        }
        return false;
    }

    /**
     * Delegate a task to someone.
     * @param id Task ID
     * @param assignee Person to delegate to
     * @return true if successful, false if task not found
     */
    public boolean delegateTask(int id, String assignee) {
        Task task = findTaskById(id);
        if (task != null) {
            task.setAssignee(assignee);
            return true;
        }
        return false;
    }

    /**
     * Delete a task from the list.
     * @param id Task ID
     * @return true if successful, false if task not found
     */
    public boolean deleteTask(int id) {
        Task task = findTaskById(id);
        if (task != null) {
            tasks.remove(task);
            return true;
        }
        return false;
    }

    /**
     * Get count of tasks.
     * @return Number of tasks
     */
    public int getTaskCount() {
        return tasks.size();
    }
}
