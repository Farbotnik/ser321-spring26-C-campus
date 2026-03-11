import org.json.JSONArray;
import org.json.JSONObject;

import java.net.*;
import java.io.*;
import java.util.Scanner;

/**
 * Hangman Game Client - Farbotnik
 */
public class HangmanClient {
    static Socket sock;
    static ObjectOutputStream oos;
    static ObjectInputStream in;

    static Scanner scanner = new Scanner(System.in);
    static boolean inGame = false;
    static boolean hasName = false;
    static String playerName = "";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            sock = new Socket(host, port);
            oos = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());

            System.out.println("---------------------------------------");
            System.out.println("|     WELCOME TO HANGMAN GAME!        |");
            System.out.println("---------------------------------------");
            System.out.println();

            boolean running = true;
            while (running) {
                if (!hasName) {
                    running = showInitialMenu();
                } else if (!inGame) {
                    running = showMainMenu();
                } else {
                    running = showGameMenu();
                }
                System.out.println();
            }

            closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initial menu - before name is set
     */
    static boolean showInitialMenu() {
        System.out.println("---------------------------------------");
        System.out.println("  1. Set Your Name");
        System.out.println("  2. Quit");
        System.out.println("---------------------------------------");
        System.out.print("Enter choice: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                setName();
                return true;
            case "2":
                quit();
                return false;
            default:
                System.out.println("Invalid choice. Please try again.");
                return true;
        }
    }

    /**
     * Main menu - after name set, no active game
     */
    static boolean showMainMenu() {
        System.out.println("---------------------------------------");
        System.out.println("MAIN MENU:");
        System.out.println("  1. Start New Game");
        System.out.println("  2. View Leaderboard");
        System.out.println("  3. Quit");
        System.out.println("---------------------------------------");
        System.out.print("Enter choice: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                startGame();
                return true;
            case "2":
                showLeaderboard();
                return true;
            case "3":
                quit();
                return false;
            default:
                System.out.println("Invalid choice. Please try again.");
                return true;
        }
    }

    /**
     * Game menu - during active game
     * Natural input: just type letter/word to guess
     * Commands: 1, 2, 3, 4, 0 for special actions
     */
    static boolean showGameMenu() {
        System.out.println("\n---------------------------------------");
        System.out.println("Type a letter or word to guess");
        System.out.println("Or choose:");
        System.out.println("  1 - Show game state");
        System.out.println("  2 - See guessed letters");
        System.out.println("  3 - Get a hint (-8 points)");
        System.out.println("  4 - Give up (return to main menu)");
        System.out.println("  0 - Quit game");
        System.out.println("---------------------------------------");
        System.out.print("Your input: ");
        String input = scanner.nextLine().trim();

        // Handle special commands
        if (input.equals("1")) {
            JSONObject request = new JSONObject();
            request.put("type", "state");
            JSONObject response = sendRequest(request);
            if (response != null) {
                if (response.getBoolean("ok")) {
                    System.out.println(response.getString("message")); // hangman art
                    System.out.println("Word: " + response.getString("hiddenWord"));
                    System.out.println("Misses: " + response.getInt("misses"));
                    System.out.println("Lives:" + response.getInt("lives"));
                    System.out.println("Points:" + response.getInt("points"));
                } else {
                    System.out.println("✗ Error: " + response.getString("message"));
                }
            }
            return true;
        } else if (input.equals("2")) {
            JSONObject request = new JSONObject();
            request.put("type", "guessed");
            JSONObject response = sendRequest(request);
            if (response != null) {
                if (response.getBoolean("ok")) {
                    JSONArray letters = response.getJSONArray("guessedLetters");
                    if (letters.length() == 0) {
                        System.out.println("No letters guessed yet.");
                    } else {
                        String display = "";
                        for (int i = 0; i < letters.length(); i++) {
                            if (i > 0) display += ", ";
                            display += letters.getString(i);
                        }
                        System.out.println("Guessed letters: " + display);
                    }
                } else {
                    System.out.println("✗ Error: " + response.getString("message"));
                }
            }
            return true;
        } else if (input.equals("3")) {
            JSONObject request = new JSONObject();
            request.put("type", "hint");
            JSONObject response = sendRequest(request);
            if (response != null) {
                if (response.getBoolean("ok")) {
                    System.out.println(response.getString("message")); // hangman art
                    System.out.println("Hint: the word contains '" + response.getString("hintLetter") + "'");
                    System.out.println("Word: " + response.getString("hiddenWord"));
                    System.out.println("Lives: " + response.getInt("lives") + "  Misses: " + response.getInt("misses") + "  Points: " + response.getInt("points"));
                } else {
                    System.out.println("✗ Error: " + response.getString("message"));
                }
            }
            return true;
        } else if (input.equals("4")) {
            giveUp();
            return false;
        } else if (input.equals("0")) {
            quit();
            return false;
        }

        if (input.isEmpty()) {
            System.out.println("Please enter a letter, word, or command.");
            return true;
        }

        // Single character = letter guess, multiple = word guess
        if (input.length() == 1) {
            // happy case 1 word guess
            JSONObject request = new JSONObject();
            request.put("type", "guess");
            request.put("letter", input);
            JSONObject response = sendRequest(request);
            if (response != null) {
                if (response.getBoolean("ok")) {
                    System.out.println(response.getString("message")); // hangman art
                    System.out.println("Word: " + response.getString("hiddenWord"));
                    System.out.println("Lives: " + response.getInt("lives") + "  Misses: " + response.getInt("misses") + "  Points: " + response.getInt("points"));
                    if (response.getBoolean("correct")) {
                        System.out.println("Good guess!");
                    } else {
                        System.out.println("Wrong guess!");
                    }
                    if (response.getBoolean("gameOver")) {
                        try {
                            System.out.println("Solution: " + response.getString("solution"));
                            if (response.getString("result").equals("win")) {
                                System.out.println("You win! Final score: " + response.getInt("points"));
                            } else {
                                System.out.println("Game over! The word was: " + response.getString("solution"));
                            }
                        } catch (Exception e) {
                            System.out.println("Game over!");
                        }
                        inGame = false;
                    }
                } else {
                    System.out.println("✗ Error: " + response.getString("message"));
                }
            }
        } else {
            // handles word inputs
            JSONObject request = new JSONObject();
            request.put("type", "guess");
            request.put("word", input);

            JSONObject response = sendRequest(request);
            if (response != null) {
                if (response.getBoolean("ok")) {
                    System.out.println(response.getString("message")); // hangman art
                    System.out.println("Word: " + response.getString("hiddenWord"));
                    System.out.println("Lives: " + response.getInt("lives") + "  Misses: " + response.getInt("misses") + "  Points: " + response.getInt("points"));

                    if (response.getBoolean("correct")) {
                        System.out.println("Correct! You guessed the word!");
                    } else {
                        System.out.println("Wrong word! -2");
                    }
                    if (response.getBoolean("gameOver")) {
                        try {
                            System.out.println("Solution: " + response.getString("solution"));
                            if (response.getString("result").equals("win")) {
                                System.out.println("You win! Final score: " + response.getInt("points"));
                            } else {
                                System.out.println("Game over! The word was: " + response.getString("solution"));
                            }
                        } catch (Exception e) {
                            System.out.println("Game over!");
                        }
                        inGame = false;
                    }
                } else {
                    System.out.println("✗ Error: " + response.getString("message"));
                }
            }
            return true;
        }
        return true;
    }

    /**
     * IMPORTANT: This should send a request to the server to end the game!
     * Just setting inGame = false locally creates a state mismatch
     * where the client thinks the game is over but the server still has it active.
     *
     * Proper implementation should:
     * - Confirm with user
     * - Send "give up" or "end game" (or similar) request to server
     * - Server ends game, does not add to leaderboard
     * - Server responds confirming game ended
     * - Client sets inGame = false
     */
    static void giveUp() {
        System.out.print("\nAre you sure you want to give up? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("yes") || confirm.equals("y")) {
            System.out.println("\nYou gave up! Returning to main menu...\n");
            JSONObject request = new JSONObject();
            request.put("type", "giveup");
            JSONObject response = sendRequest(request);
            if (response != null) {
                if (response.getBoolean("ok")) {
                    System.out.println(response.getString("doom")); //cat
                    System.out.println(response.getString("message"));
                    inGame = false;
                } else {
                    System.out.println("✗ Error: " + response.getString("message"));
                }
            }
        } else {
            System.out.println("\nContinuing game...");
        }
    }

    /**
     * Set player name
     */
    static void setName() {
        System.out.print("\nEnter your name: ");
        String name = scanner.nextLine().trim();

        // Create request according to YOUR protocol design
        JSONObject request = new JSONObject();
        request.put("type", "name");
        request.put("name", name);

        // Send request and get response
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                hasName = true;
                playerName = name;
                System.out.println("\n" + response.getString("message"));
                System.out.println();
            } else {
                System.out.println("✗ Error: " + response.getString("message"));
            }
        }
    }

    /**
     * Start game
     * Should send a start request to the server and handle the response
     */
    static void startGame() {
        System.out.println("\n[Start game - send start request to server]");
        JSONObject request = new JSONObject();
        request.put("type", "start");
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                inGame = true;
                System.out.println(response.getString("message"));   // hangman art
                System.out.println("Word: " + response.getString("hiddenWord")
                        + "  (" + response.getInt("wordLength") + " letters)");
                System.out.println("Lives: " + response.getInt("lives"));
                System.out.println("Points: " + response.getInt("points"));
                System.out.println();
            } else {
                System.out.println("✗ Error: " + response.getString("message"));
            }
        }
        
    }

    /**
     * Should send a leaderboard request to the server and handle the response
     */
    static void showLeaderboard() {
        JSONObject request = new JSONObject();
        request.put("type", "leaderboard");
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                JSONArray entries = response.getJSONArray("entries");
                if (entries.length() == 0) {
                    System.out.println("No games played yet.");
                } else {
                    System.out.println("Rank | Name         | Best | Avg  | Win% | Played");
                    System.out.println("--------------------------------------------------");
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject entry = entries.getJSONObject(i);
                        System.out.println(entry.getInt("rank") + " | " +
                                entry.getString("name") + " | " +
                                entry.getInt("bestScore") + " | " +
                                entry.getDouble("avgScore") + " | " +
                                entry.getDouble("winPct") + "% | " +
                                entry.getInt("gamesPlayed")
                        );
                    }
                }
                System.out.println("---------------------------------------");
            } else {
                System.out.println("✗ Error: " + response.getString("message"));
            }
        }
        
    }

    /**
     * Quit game
     */
    static boolean quit() {
        JSONObject request = new JSONObject();
        request.put("type", "quit");

        JSONObject response = sendRequest(request);
        if (response != null && response.getBoolean("ok")) {
            System.out.println("\n" + response.getString("message"));
            System.out.println("Thanks for playing!");
        }
        return false; // Stop the main loop
    }

    /**
     * Helper: Send request and receive response
     * This handles the basic communication pattern
     */
    static JSONObject sendRequest(JSONObject request) {
        try {
            String req = request.toString();
            oos.writeObject(req);
            oos.flush();

            String res = (String) in.readObject();
            return new JSONObject(res);
        } catch (Exception e) {
            System.out.println("Error communicating with server: " + e.getMessage());
            return null;
        }
    }

    /**
     * Close connection
     */
    static void closeConnection() {
        try {
            if (oos != null) oos.close();
            if (in != null) in.close();
            if (sock != null) sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
