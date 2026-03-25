# Activity 1: Task Management System with Threading

## Overview

In this activity, you will work with a **Task Management System** that allows multiple clients to manage shared tasks. The system supports:
- Adding tasks with categories
- Listing tasks (all, pending, or finished)
- Marking tasks as finished


You will learn about:
- **Protocol Buffers (Protobuf)** - An efficient binary serialization format
- **Multi-threading** - Handling multiple clients simultaneously
- **Thread safety** - Protecting shared data from race conditions

## Starter Code

### What's Provided

1. **Server.java** - Single-threaded server
2. **Client.java** - Complete client with menu system JSON basis
3. **Performer.java** - Handles all client requests using JSON
4. **Task.java** - Task data structure
5. **TaskList.java** - Shared task collection (not thread-safe yet)
6. **JsonUtils.java** - JSON utility methods
7. **task.proto** - Protobuf protocol definition (STARTER - incomplete)
8. **build.gradle** - Build configuration

### Running the Starter Code

The starter code uses JSON protocol and is fully functional for the implemented operations.
In `Client` and `Performer`, there is a welcome-message section where you can switch
between JSON and Proto by commenting/uncommenting. That should be your first step.
Use it as the example pattern for step-by-step conversion.

**Generate the java files for Proto:**
```bash
gradle generateProto
```

**Start the server:**

```bash
gradle runServer
# Or with custom port:
gradle runServer -Pport=9000
```

**Start the client (in a new terminal):**
```bash
gradle runClient
# Or with custom host/port:
gradle runClient -Phost=localhost -Pport=9000
```

Try the operations:
1. Add a task
2. List tasks
3. Finish a task
4. Quit
5. Try running multiple clients - only one works at a time (single-threaded!)

## Current Protocol (JSON)

The server currently uses a JSON-based protocol:

### Request Format
```json
{
  "type": "add|list|finish|quit",
  ... other fields depending on type
}
```

### Add Task
**Request:**
```json
{
  "type": "add",
  "description": "Implement Protobuf protocol",
  "category": "school"
}
```

**Success Response:**
```json
{
  "ok": true,
  "type": "add",
  "data": {
    "id": 1,
    "description": "Implement Protobuf protocol",
    "category": "school",
    "finished": false
  }
}
```

**Error Response:**
```json
{
  "ok": false,
  "type": "add",
  "data": {
    "error": "Missing 'description' field"
  }
}
```

### List Tasks
**Request:**
```json
{
  "type": "list",
  "filter": "all"
}
```
- `filter` can be: "all", "pending", or "finished" (optional, defaults to "all")

**Success Response:**
```json
{
  "ok": true,
  "type": "list",
  "data": {
    "tasks": [
      {
        "id": 1,
        "description": "Implement Protobuf protocol",
        "category": "school",
        "finished": false
      },
      ...
    ],
    "count": 5
  }
}
```

### Finish Task
**Request:**
```json
{
  "type": "finish",
  "id": 1
}
```

**Success Response:**
```json
{
  "ok": true,
  "type": "finish",
  "data": {
    "message": "Task #1 marked as finished"
  }
}
```

### Quit
**Request:**
```json
{
  "type": "quit"
}
```

**Success Response:**
```json
{
  "ok": true,
  "type": "quit",
  "data": {
    "message": "Goodbye!"
  }
}
```
