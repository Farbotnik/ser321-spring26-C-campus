import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Hangman Game Server - Student Starter Code
 *
 * Your task: Design the protocol and implement the game logic.
 *
 * What's provided:
 * - Resource loading (game stages, word list)
 * - Name handling as a complete example
 * - Basic server structure and routing
 *
 * What you need to implement:
 * - Complete protocol design (document in README.md)
 * - All game logic handlers (stubs provided below)
 */
public class HangmanServer {
    static Socket sock;
    static ObjectOutputStream os;
    static ObjectInputStream in;
    static int port = 8888;

    // Game state for current player - YOU WILL NEED THESE
    static String playerName = null;
    static String secretWord = null;
    static Set<Character> usedLetters = new HashSet<>();
    static int misses = 0;
    static int points = 0;
    static boolean inGame = false;

    // Leaderboard - list of game results (you can change this any way you want)
    static List<Map<String, Object>> leaderboard = new ArrayList<>();

    // Game ASCII art - 7 stages (0-6 misses allowed)
    // Loaded from resources/game_stages.txt
    static String[] GAME_STAGES = new String[7];

    // Word list - loaded from resource file
    static String[] WORDS;

    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Expected arguments: <port(int)>");
            System.exit(1);
        }

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("Port must be an integer");
            System.exit(2);
        }

        // Load game resources
        loadGameStages();
        loadWords();

        try {
            ServerSocket serv = new ServerSocket(port);
            System.out.println("Hangman Server ready for connections on port " + port);

            while (true) {
                System.out.println("Server waiting for a connection");
                sock = serv.accept();
                System.out.println("Client connected");

                // Setup streams
                in = new ObjectInputStream(sock.getInputStream());
                OutputStream out = sock.getOutputStream();
                os = new ObjectOutputStream(out);

                // Initialize game state for new connection
                initGame();

                boolean connected = true;
                while (connected) {
                    String s = "";
                    try {
                        s = (String) in.readObject();
                    } catch (Exception e) {
                        System.out.println("Client disconnect");
                        connected = false;
                        continue;
                    }

                    JSONObject res = isValid(s);
                    if (res.has("ok")) {
                        sendResponse(res);
                        continue;
                    }

                    JSONObject req = new JSONObject(s);
                    res = testField(req, "type");
                    if (!res.getBoolean("ok")) {
                        res = noType(req);
                        sendResponse(res);
                        continue;
                    }

                    // Route to appropriate handler
                    String type = req.getString("type");
                    if (type.equals("name")) {
                        res = handleName(req);
                      /// include the other types
                    } else if (type.equals("quit")) {
                        res = handleQuit(req);
                        sendResponse(res);
                        connected = false;
                        continue;
                    } else {
                        res = wrongType(req);
                    }
                    sendResponse(res);
                }
                closeConnection();
            }
        } catch (Exception e) {
            e.printStackTrace();
            closeConnection();
        }
    }

    /**
     * EXAMPLE IMPLEMENTATION: Set player name
     * This is provided as a complete example of request handling.
     * Use this as a reference for implementing other handlers.
     */
    static JSONObject handleName(JSONObject req) {
        System.out.println("Name request: " + req.toString());
        JSONObject res = testField(req, "name");
        if (!res.getBoolean("ok")) {
            return res;
        }

        String name = req.getString("name");
        if (name == null || name.trim().isEmpty()) {
            res = new JSONObject();
            res.put("ok", false);
            res.put("message", "Name cannot be empty");
            return res;
        }

        playerName = name.trim();
        res = new JSONObject();
        res.put("ok", true);
        res.put("type", "name");
        res.put("message", "Welcome " + playerName + "! Ready to play Hangman?");
        return res;
    }

    /**
     * Quit handler
     */
    static JSONObject handleQuit(JSONObject req) {
        System.out.println("Quit request: " + req.toString());
        JSONObject res = new JSONObject();

        res.put("ok", true);
        res.put("type", "quit");
        res.put("message", "Goodbye " + (playerName != null ? playerName : "player") + "!");

        return res;
    }

    /**
     * Helper: Initialize game state for new connection
     */
    static void initGame() {
        playerName = null;
        secretWord = null;
        usedLetters = new HashSet<>();
        misses = 0;
        points = 0;
        inGame = false;
    }

    /**
     * Helper: Check if field exists in request
     */
    static JSONObject testField(JSONObject req, String key) {
        JSONObject res = new JSONObject();
        if (!req.has(key)) {
            res.put("ok", false);
            res.put("message", "Field '" + key + "' does not exist in request");
            return res;
        }
        return res.put("ok", true);
    }

    /**
     * Helper: Validate JSON
     */
    static JSONObject isValid(String json) {
        try {
            new JSONObject(json);
        } catch (JSONException e) {
            try {
                new JSONArray(json);
            } catch (JSONException ne) {
                JSONObject res = new JSONObject();
                res.put("ok", false);
                res.put("message", "Request is not valid JSON");
                return res;
            }
        }
        return new JSONObject();
    }

    /**
     * Error: no type field
     */
    static JSONObject noType(JSONObject req) {
        System.out.println("No type request: " + req.toString());
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "No request type was given");
        return res;
    }

    /**
     * Error: wrong type
     */
    static JSONObject wrongType(JSONObject req) {
        System.out.println("Wrong type request: " + req.toString());
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "Type '" + req.getString("type") + "' is not supported");
        return res;
    }

    /**
     * Load game ASCII art stages from resource file
     */
    static void loadGameStages() {
        try {
            InputStream is = HangmanServer.class.getResourceAsStream("/game_stages.txt");
            if (is == null) {
                System.err.println("Error: game_stages.txt not found in resources");
                System.exit(1);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder currentStage = new StringBuilder();
            int stageIndex = 0;

            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    GAME_STAGES[stageIndex++] = "\n" + currentStage.toString();
                    currentStage = new StringBuilder();
                } else if (!line.startsWith("STAGE")) {
                    currentStage.append(line).append("\n");
                }
            }
            // Add final stage
            if (currentStage.length() > 0 && stageIndex < 7) {
                GAME_STAGES[stageIndex] = "\n" + currentStage.toString();
            }
            reader.close();
            System.out.println("Loaded " + (stageIndex + 1) + " game stages");
        } catch (Exception e) {
            System.err.println("Error loading game stages: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Load word list from resource file
     */
    static void loadWords() {
        try {
            WORDS = loadWordList("/words.txt");
            System.out.println("Loaded " + WORDS.length + " words");
        } catch (Exception e) {
            System.err.println("Error loading word list: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Helper: Load a single word list from file
     */
    static String[] loadWordList(String filename) throws IOException {
        InputStream is = HangmanServer.class.getResourceAsStream(filename);
        if (is == null) {
            throw new IOException("Word list file not found: " + filename);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        List<String> words = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                words.add(line.toLowerCase());
            }
        }
        reader.close();

        return words.toArray(new String[0]);
    }

    /**
     * Write response to client
     */
    static void sendResponse(JSONObject res) {
        try {
            os.writeObject(res.toString());
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Close connection
     */
    static void closeConnection() {
        try {
            if (os != null) os.close();
            if (in != null) in.close();
            if (sock != null) sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
