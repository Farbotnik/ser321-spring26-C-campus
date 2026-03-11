import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Hangman Game Server - Farbotnik
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
    static int hintsUsed = 0;
    static boolean inGame = false;

    // Leaderboard - list of game results (you can change this any way you want)
    static Map<String, Map<String, Object>> leaderboard = new HashMap<>();

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
                        Object obj = in.readObject();
                        if (!(obj instanceof String)) {
                            continue;
                        }
                        s = (String) obj;
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
                        // TODO: check game not started yet and name set
                      /// include the other types
                    } else if (type.equals("start")) {
                        res = handleStart(req);
                    } else if (type.equals("guess")) {
                        res = handleGuess(req);
                    } else if (type.equals("state")) {
                        res = handleState(req);
                    } else if (type.equals("hint")) {
                        res = handleHint(req);
                    } else if (type.equals("guessed")) {
                        res = handleGuessed(req);
                    } else if (type.equals("giveup")) {
                        res = handleGiveUp(req);
                    } else if (type.equals("leaderboard")) {
                        res = handleLeaderboard(req);
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
     * Set player name
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
     * Handles start JSON
     */
    static JSONObject handleStart(JSONObject req) {
        System.out.println("Start request: " + req.toString());
        JSONObject res = new JSONObject();
        // check if game already running
        if (inGame) {
            res.put("type", "start");
            res.put("ok", false);
            res.put("message", "A game is in progress");
            return res;
        }

        // picks a random word
        Random rand = new Random();
        secretWord = WORDS[rand.nextInt(WORDS.length)];
        usedLetters = new HashSet<>();
        // new game state
        misses = 0;
        points = 0;
        inGame = true;
        System.out.println("Secret word: " + secretWord);
        String hiddenWord = buildHiddenWord();

        res.put("type", "start");
        res.put("ok", true);
        res.put("message", GAME_STAGES[0]);
        res.put("hiddenWord", hiddenWord);
        res.put("wordLength", secretWord.length());
        res.put("lives", 6);
        res.put("misses", 0);
        res.put("points", 0);
        return res;
    }

    /**
     * Helper for word logic
     * @return
     */
    static String buildHiddenWord() {
        String result = "";
        for (int i = 0; i < secretWord.length(); i++) {
            if (i > 0) result += " ";
            char c = secretWord.charAt(i);
            if (usedLetters.contains(c)) {
                result += c;
            } else {
                result += "_";
            }
        }
        return result;
    }

    /**
     * Allows the user to get hints during the game
     * @param req hint request
     * @return the letter hint
     */
    static JSONObject handleHint(JSONObject req) {
        System.out.println("Hint request: " + req.toString());
        JSONObject res = new JSONObject();

        if (!inGame) {
            res.put("type", "hint");
            res.put("ok", false);
            res.put("message", "No active game");
            return res;
        }

        // gets all unused letters
        List<Character> unrevealed = new ArrayList<>();
        for (char c : secretWord.toCharArray()) {
            if (!usedLetters.contains(c)) {
                unrevealed.add(c);
            }
        }

        // sends a random to user
        Random rand = new Random();
        char hintLetter = unrevealed.get(rand.nextInt(unrevealed.size()));
        usedLetters.add(hintLetter);
        hintsUsed++;
        points -= 8;

        res.put("type", "hint");
        res.put("ok", true);
        res.put("message", GAME_STAGES[misses]);
        res.put("hiddenWord", buildHiddenWord());
        res.put("hintLetter", String.valueOf(hintLetter));
        res.put("lives", 6 - misses);
        res.put("misses", misses);
        res.put("points", points);
        return res;
    }

    /**
     * Handles leaderboard
     */
    static JSONObject handleLeaderboard(JSONObject req) {
        System.out.println("Leaderboard request: " + req.toString());
        JSONObject res = new JSONObject();
        // init
        if (leaderboard.isEmpty()) {
            res.put("type", "leaderboard");
            res.put("ok", true);
            res.put("entries", new JSONArray());
            res.put("message", "No games played yet");
            return res;
        }

        // sort by best score descending
        List<Map.Entry<String, Map<String, Object>>> entries = new ArrayList<>(leaderboard.entrySet());
        entries.sort((a, b) -> (int) b.getValue().get("bestScore") - (int) a.getValue().get("bestScore"));

        // top 10 only
        JSONArray arr = new JSONArray();
        for (int i = 0; i < Math.min(10, entries.size()); i++) {
            String name = entries.get(i).getKey();
            Map<String, Object> stats = entries.get(i).getValue();

            int gamesPlayed = (int) stats.get("gamesPlayed");
            int gamesWon = (int) stats.get("gamesWon");
            int totalPoints = (int) stats.get("totalPoints");
            int bestScore = (int) stats.get("bestScore");

            double avgScore = (double) totalPoints / gamesPlayed;
            double winPct = ((double) gamesWon / gamesPlayed) * 100;

            JSONObject entry = new JSONObject();
            entry.put("rank", i + 1);
            entry.put("name", name);
            entry.put("bestScore", bestScore);
            entry.put("avgScore", Math.round(avgScore * 10.0) / 10.0);
            entry.put("winPct", Math.round(winPct * 10.0) / 10.0);
            entry.put("gamesPlayed", gamesPlayed);
            entry.put("gamesWon", gamesWon);
            arr.put(entry);
        }

        res.put("type", "leaderboard");
        res.put("ok", true);
        res.put("message", "Top " + arr.length() + " players");
        res.put("entries", arr);
        return res;
    }

    /**
     * Does the logic for a guess on the word
     * @param req the guessed letter or word
     * @return response to handleLetterGuess() or handleWordGuess()
     */
    static JSONObject handleGuess(JSONObject req) {
        System.out.println("Guess request: " + req.toString());
        JSONObject res = new JSONObject();

        // checks for an active game
        if (!inGame) {
            res.put("type", "guess");
            res.put("ok", false);
            res.put("message", "No active game");
            return res;
        }

        // check if the letter field exists
        if (req.has("letter")) {
            return handleLetterGuess(req, res);
        } else if (req.has("word")) {
            return handleWordGuess(req, res);
        } else {
            res.put("type", "guess");
            res.put("ok", false);
            res.put("message", "No letter or word given");
            return res;
        }
    }

    /**
     * Single Letter guessing and checking
     * @param req guess from client
     * @param res response built so far from handleGuess()
     * @return the response to client
     */
    static JSONObject handleLetterGuess(JSONObject req, JSONObject res) {
        String input = req.getString("letter").toLowerCase().trim();
        if (input.length() != 1 || !Character.isLetter(input.charAt(0))) {
            res.put("type", "guess");
            res.put("ok", false);
            res.put("message", "Must be a single letter");
            return res;
        }

        char letter = input.charAt(0);

        // check the already guessed list
        if (usedLetters.contains(letter)) {
            res.put("type", "guess");
            res.put("ok", false);
            res.put("message", "Letter '" + letter + "' was already guessed");
            return res;
        }

        // add to used letters
        usedLetters.add(letter);

        // check if its in the word
        boolean correct = secretWord.contains(String.valueOf(letter));
        if (correct) {
            // makes sure we can add 5pts per right guess
            int count = 0;
            for (char c : secretWord.toCharArray()) {
                if (c == letter) count++;
            }
            points += 5 * count;
        } else {
            points -= 1;
            misses++;
        }

        String hiddenWord = buildHiddenWord();
        boolean gameOver = false;
        String result = "";

        // check for a win or loss
        if (!hiddenWord.contains("_")) {
            gameOver = true;
            result = "win";
            inGame = false;
            points += 20;
            if (hintsUsed == 0) {
                points += 10;
            }
            updateLeaderboard(true);
        }// loss
        else if (misses == 6) {
            gameOver = true;
            result = "loss";
            updateLeaderboard(false);
            inGame = false;
        }

        res.put("type", "guess");
        res.put("ok", true);
        res.put("correct", correct);
        res.put("message", GAME_STAGES[misses]);
        res.put("hiddenWord", hiddenWord);
        res.put("wordLength", secretWord.length());
        res.put("lives", 6 - misses);
        res.put("misses", misses);
        res.put("points", points);
        res.put("gameOver", gameOver);

        if (gameOver) {
            res.put("result", result);
            res.put("solution", secretWord);
        }

        return res;
    }

    /**
     * Does all the checking for word guesses
     * @param req guess from client
     * @param res response built so far from handleGuess()
     * @return the response to client
     */
    static JSONObject handleWordGuess(JSONObject req, JSONObject res) {
        String input = req.getString("word").toLowerCase().trim();

        // Validate only letters
        for (char c : input.toCharArray()) {
            if (!Character.isLetter(c)) {
                res.put("type", "guess");
                res.put("ok", false);
                res.put("message", "Must contain only letters");
                return res;
            }
        }

        boolean correct = input.equals(secretWord);
        if (correct) {
            Set<Character> alreadyRevealed = new HashSet<>(usedLetters);
            usedLetters.clear();
            for (char c : secretWord.toCharArray()) {
                usedLetters.add(c);
            }
            for (char c : secretWord.toCharArray()) {
                if (!alreadyRevealed.contains(c)) {
                    points += 5;
                }
            }
            points += 20;
            if (hintsUsed == 0) {
                points += 10;
            }
        } else {
            misses += 2;
            points -= 2;
            if (misses > 6) {
                misses = 6;
            }
        }

        String hiddenWord = buildHiddenWord();
        boolean gameOver = false;
        String result = "";
        // checks win conditions
        if (correct) {
            gameOver = true;
            result = "win";
            inGame = false;
            updateLeaderboard(true);
        } else if (misses >= 6) {
            gameOver = true;
            result = "loss";
            updateLeaderboard(false);
            inGame = false;
        }

        res.put("type", "guess");
        res.put("ok", true);
        res.put("correct", correct);
        res.put("message", GAME_STAGES[misses]);
        res.put("hiddenWord", hiddenWord);
        res.put("wordLength", secretWord.length());
        res.put("lives", 6 - misses);
        res.put("misses", misses);
        res.put("points", points);
        res.put("gameOver", gameOver);

        if (gameOver) {
            res.put("result", result);
            res.put("solution", secretWord);
        }

        return res;
    }

    /**
     * Handles the messagine when user checks state.
     * @param req client request
     * @return game state response
     */
    static JSONObject handleState(JSONObject req) {
        System.out.println("State request: " + req.toString());
        JSONObject res = new JSONObject();

        if (!inGame) {
            res.put("type", "state");
            res.put("ok", false);
            res.put("message", "No active game");
            return res;
        }

        res.put("type", "state");
        res.put("ok", true);
        res.put("message", GAME_STAGES[misses]);
        res.put("hiddenWord", buildHiddenWord());
        res.put("misses", misses);
        res.put("lives", 6 - misses);
        res.put("points", points);
        return res;
    }

    /**
     * Handles when the User wants to quit
     * @param req
     * @return
     */
    static JSONObject handleGiveUp(JSONObject req) {
        System.out.println("GiveUp request: " + req.toString());
        JSONObject res = new JSONObject();

        if (!inGame) {
            res.put("type", "giveup");
            res.put("ok", false);
            res.put("message", "No active game");
            return res;
        }

        String word = secretWord;
        String art =
            "                 __,__\n" +
                    "        .--.  .-\"     \"-.  .--.\n" +
                    "       / .. \\/  .-. .-.  \\/ .. \\\n" +
                    "      | |  '|  /   Y   \\  |'  | |\n" +
                    "      | \\   \\  \\ 0 | 0 /  /   / |\n" +
                    "       \\ '- ,\\.-\"`` ``\"-./, -' /\n" +
                    "        `'-' /_   ^ ^   _\\ '-'\n" +
                    "        .--'|  \\._   _./  |'--. \n" +
                    "      /`    \\   \\ `~` /   /    `\\\n" +
                    "     /       '._ '---' _.'       \\\n" +
                    "    /           '~---~'   |       \\\n" +
                    "   /        _.             \\       \\\n" +
                    "  /   .'-./`/        .'~'-.|\\       \\\n" +
                    " /   /    `\\:       /      `\\'.      \\\n" +
                    "/   |       ;      |         '.`;    /\n" +
                    "\\   \\       ;      \\           \\/   /\n" +
                    " '.  \\      ;       \\       \\   `  /\n" +
                    "   '._'.     \\       '.      |   ;/_\n" +
                    "jgs  /__>     '.       \\_ _ _/   ,  '--.\n" +
                    "   .'   '.   .-~~~~~-. /     |--'`~~-.  \\\n" +
                    "  // / .---'/  .-~~-._/ / / /---..__.'  /\n" +
                    " ((_(_/    /  /      (_(_(_(---.__    .'\n" +
                    "           | |     _              `~~`\n" +
                    "           | |     \\\\'.\n" +
                    "            \\ '....' |\n" +
                    "             '.,___.'";
        inGame = false;
        secretWord = null;
        usedLetters = new HashSet<>();
        misses = 0;
        points = 0;
        hintsUsed = 0;

        res.put("type", "giveup");
        res.put("ok", true);
        res.put("message", "You gave.... The word is: " + word);
        res.put("solution", word);
        res.put("doom", art);
        return res;
    }

    /**
     * Lets user see whats been guessed
     * @param req guessed request from client
     * @return the letters guessed sent back to client
     */
    static JSONObject handleGuessed(JSONObject req) {
        System.out.println("Guessed request: " + req.toString());
        JSONObject res = new JSONObject();
        // error handling
        if (!inGame) {
            res.put("type", "guessed");
            res.put("ok", false);
            res.put("message", "No active game");
            return res;
        }
        // array of guesses
        JSONArray guessedArray = new JSONArray();
        for (char c : usedLetters) {
            guessedArray.put(String.valueOf(c));
        }
        res.put("type", "guessed");
        res.put("ok", true);
        res.put("guessedLetters", guessedArray);
        return res;
    }

    /**
     * Does the storing of leaderboard
     * @param won
     */
    static void updateLeaderboard(boolean won) {
        Map<String, Object> entry;

        if (leaderboard.containsKey(playerName)) {
            entry = leaderboard.get(playerName);
        } else {
            entry = new HashMap<>();
            entry.put("gamesPlayed", 0);
            entry.put("gamesWon", 0);
            entry.put("totalPoints", 0);
            entry.put("bestScore", 0);
        }
        // all the data to store
        int gamesPlayed = (int) entry.get("gamesPlayed") + 1;
        int gamesWon = (int) entry.get("gamesWon") + (won ? 1 : 0);
        int totalPoints = (int) entry.get("totalPoints") + points;
        int bestScore = Math.max((int) entry.get("bestScore"), points);
        entry.put("gamesPlayed", gamesPlayed);
        entry.put("gamesWon", gamesWon);
        entry.put("totalPoints", totalPoints);
        entry.put("bestScore", bestScore);

        leaderboard.put(playerName, entry);
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
        hintsUsed = 0;
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
