package auction;

import buffers.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Auction Game Server - Players compete against bot opponents.
 * Each player plays independently against 3 bots.
 */
public class AuctionServer {
    private static final int DEFAULT_PORT = 8889;
    private static final String SCORES_FILE = "scores.txt";

    private static final int initialGold = 150;

    // Shared leaderboard
    private static LeaderboardManager leaderboard;

    // Track connected player names (to prevent duplicates)
    private static Set<String> activePlayerNames = new HashSet<>();

    // Grading mode flag
    private static boolean gradingMode = false;

    // Bot opponent name pool
    private static final String[] BOT_NAMES = {
            "Alaric", "Brynn", "Cedric", "Daphne",
            "Elara", "Finn", "Gwen", "Hugo",
            "Isolde", "Jasper"
    };
    private static Random botNameRandom = new Random();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--grading")) {
                gradingMode = true;
                System.out.println("Running in grading mode (deterministic results)");
            } else {
                try {
                    port = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + args[i]);
                }
            }
        }

        // Initialize leaderboard
        leaderboard = new LeaderboardManager(SCORES_FILE);
        System.out.println("Leaderboard loaded with " + leaderboard.size() + " scores");


        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Auction Server started on port " + port);
            System.out.println("Waiting for connections...");

            int clientId = 0;
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientId++;
                    final int id = clientId;
                    System.out.println("Client " + id + " connected from " +
                            clientSocket.getInetAddress().getHostAddress());

                    processConnection(clientSocket, id);
                } catch (IOException e) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Handle a client connection (runs in thread pool).
     */
    private static void processConnection(Socket clientSocket, int clientId) {
        String playerName = null;
        PlayerGameState gameState = null;

        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {

            System.out.println("[Client " + clientId + "] Handler started");

            // Send initial welcome
            sendWelcome(out, "Welcome to the Auction Game! Please set your name.");

            // Read and process requests
            Request request;
            while ((request = Request.parseDelimitedFrom(in)) != null) {
                Request.RequestType type = request.getType();
                System.out.println("[Client " + clientId + "] Received: " + type);

                Response response = null;

                switch (type) {
                    case REGISTER:
                        String[] result = handleRegister(request, playerName);
                        playerName = result[0];
                        String message = result[1];
                        if (playerName != null) {
                            response = buildWelcome("Welcome, " + playerName + "! You have " + initialGold + " gold. " +
                                    "Type 'join' to start playing against bot opponents!");
                        } else {
                            response = buildError(message);
                        }
                        break;
                    case QUIT:
                        response = handleQuit(gameState);
                        if (response != null) {
                            response.writeDelimitedTo(out);
                        }
                        return; // Exit handler

                    default:
                        response = buildError("Unknown request type");
                }

                if (response != null) {
                    response.writeDelimitedTo(out);
                }
            }

            System.out.println("[Client " + clientId + "] Disconnected");

        } catch (IOException e) {
            System.err.println("[Client " + clientId + "] Error: " + e.getMessage());
        } finally {
            // Cleanup
            if (playerName != null) {
                activePlayerNames.remove(playerName);
                System.out.println("[Client " + clientId + "] Removed player: " + playerName);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Handle REGISTER request - set player name.
     * Returns [playerName, errorMessage] - playerName is null if error.
     */
    private static String[] handleRegister(Request request, String currentName) {
        String name = request.getName().trim();

        if (name.isEmpty()) {
            return new String[]{null, "Name cannot be empty"};
        }

        if (activePlayerNames.contains(name)) {
            return new String[]{null, "Name already taken. Please choose another."};
        }

        // Add new name
        activePlayerNames.add(name);
        return new String[]{name, null};
    }

    /**
     * Handle QUIT request.
     */
    private static Response handleQuit(PlayerGameState gameState) {
        String message = "Thanks for playing!";
        if (gameState != null) {
            message += " Final score: " + gameState.getPlayerScore() + ".";
        }
        message += " Goodbye!";

        return Response.newBuilder()
                .setType(Response.ResponseType.FAREWELL)
                .setOk(true)
                .setMessage(message)
                .build();
    }

    /**
     * Helper: send welcome response.
     */
    private static void sendWelcome(OutputStream out, String message) throws IOException {
        buildWelcome(message).writeDelimitedTo(out);
    }

    /**
     * Helper: build welcome response.
     */
    private static Response buildWelcome(String message) {
        return Response.newBuilder()
                .setType(Response.ResponseType.WELCOME)
                .setOk(true)
                .setMessage(message)
                .build();
    }

    /**
     * Helper: build error response.
     */
    private static Response buildError(String message) {
        return Response.newBuilder()
                .setType(Response.ResponseType.ERROR)
                .setOk(false)
                .setMessage(message)
                .build();
    }

    /**
     * Helper: convert Item to protobuf AuctionItem.
     * Includes reserve_price calculated as 50% of min_value.
     */
    private static AuctionItem itemToProto(Item item) {
        return AuctionItem.newBuilder()
                .setId(item.getId())
                .setName(item.getName())
                .setCategory(item.getCategory())
                .setMinValue(item.getMinValue())
                .setMaxValue(item.getMaxValue())
                .setReservePrice(item.getMinValue() / 2)
                .build();
    }

    /**
     * Helper: get random bot name.
     */
    private static String getRandomBotName() {
        return BOT_NAMES[botNameRandom.nextInt(BOT_NAMES.length)];
    }

    /**
     * Inner class to track player game state.
     */
    private static class PlayerGameState {
        private String playerName;
        private int gold;
        private List<Item> inventory;
        private List<Item> items;
        private int currentItemIndex;
        private BotOpponent bot1;
        private BotOpponent bot2;
        private BotOpponent bot3;

        public PlayerGameState(String playerName, boolean gradingMode) {
            this.playerName = playerName;
            this.gold = initialGold;
            this.inventory = new ArrayList<>();

            // Load items
            this.items = ItemLoader.loadItems(gradingMode);
            this.currentItemIndex = 0;

            // Create 3 bot opponents with unique names
            Set<String> usedNames = new HashSet<>();
            this.bot1 = createUniqueBot(usedNames, gradingMode);
            this.bot2 = createUniqueBot(usedNames, gradingMode);
            this.bot3 = createUniqueBot(usedNames, gradingMode);
        }

        private BotOpponent createUniqueBot(Set<String> usedNames, boolean gradingMode) {
            String name;
            do {
                name = getRandomBotName();
            } while (usedNames.contains(name));
            usedNames.add(name);
            return new BotOpponent(name, gradingMode);
        }

        /**
         * Validate a bid.
         * Returns null if valid, error message if invalid.
         * bid_amount of -1 means skip (treated as bid of 0).
         * Bids > 0 must meet the reserve price.
         */
        public String validateBid(int itemId, int bidAmount) {
            Item currentItem = getCurrentItem();

            if (currentItem.getId() != itemId) {
                return "Invalid item ID. Current item is #" + currentItem.getId();
            }

            // -1 means skip
            if (bidAmount == -1) {
                return null; // Valid skip
            }

            if (bidAmount < 0) {
                return "Bid cannot be negative (use -1 to skip)";
            }

            if (bidAmount > gold) {
                return "Insufficient gold. You have " + gold + " gold.";
            }

            // Check reserve price (bids > 0 must meet reserve)
            int reservePrice = currentItem.getMinValue() / 2;
            if (bidAmount > 0 && bidAmount < reservePrice) {
                return "Bid must meet reserve price of " + reservePrice + " gold.";
            }

            return null; // Valid
        }

        public void awardItemToPlayer(Item item, int bidAmount) {
            inventory.add(item);
            gold -= bidAmount;
        }

        public boolean moveToNextItem() {
            currentItemIndex++;
            return currentItemIndex < items.size();
        }

        public Item getCurrentItem() {
            return items.get(currentItemIndex);
        }

        public int getInventoryValue() {
            int total = 0;
            for (Item item : inventory) {
                total += item.getActualValue();
            }
            return total;
        }

        public int getPlayerScore() {
            return gold + getInventoryValue();
        }

        public List<String> getItemNames() {
            List<String> names = new ArrayList<>();
            for (Item item : inventory) {
                names.add(item.getName());
            }
            return names;
        }

        // Getters
        public String getPlayerName() { return playerName; }
        public int getGold() { return gold; }
        public List<Item> getInventory() { return new ArrayList<>(inventory); }
        public BotOpponent getBot1() { return bot1; }
        public BotOpponent getBot2() { return bot2; }
        public BotOpponent getBot3() { return bot3; }
    }
}
