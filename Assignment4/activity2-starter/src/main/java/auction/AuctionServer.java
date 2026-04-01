package auction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import buffers.AuctionItem;
import buffers.AuctionResult;
import buffers.GameResult;
import buffers.Leaderboard;
import buffers.PlayerBid;
import buffers.PlayerStatus;
import buffers.Request;
import buffers.Response;

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
    private static Set<String> activePlayerNames = Collections.synchronizedSet(new HashSet<>());

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


        ExecutorService threadPool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Auction Server started on port " + port);
            System.out.println("Waiting for connections...");

            int clientId = 0;
            for (;;) { // I saw a video where linus uses this I wanted to try it in a project.
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientId++;
                    System.out.println("Client " + clientId + " connected from " +
                            clientSocket.getInetAddress().getHostAddress());

                    threadPool.submit(new ClientHandler(clientSocket, port, clientId));
                } catch (IOException e) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * Worker thread that handles one client connection
     */
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final int port;
        private final int clientId;
        private PlayerGameState gameState;

        ClientHandler(Socket clientSocket, int port, int clientId) {
            this.clientSocket = clientSocket;
            this.port = port;
            this.clientId = clientId;
            this.gameState = null;
        }

        @Override
        public void run() {
            String playerName = null;

            try (InputStream in = clientSocket.getInputStream();
                 OutputStream out = clientSocket.getOutputStream()) {

                System.out.println("[Client " + clientId + "] Handler started on port " + port);

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
                        case BID:
                            if (gameState == null) {
                                response = buildError("You must join a game first");
                            } else {
                                String bidError = gameState.validateBid(request.getItemId(), request.getBidAmount());
                                if (bidError != null) {
                                    response = buildError(bidError);
                                } else {
                                    Response bidResult = handleBid(request, gameState);
                                    bidResult.writeDelimitedTo(out);
                                    if (!bidResult.hasNextItem()) {
                                        int rank = leaderboard.addScore(gameState.getPlayerName(), gameState.getPlayerScore());
                                        gameOverResponse(gameState, rank).writeDelimitedTo(out);
                                        gameState = null;
                                    }
                                    response = null;
                                }
                            }
                            break;

                        case JOIN:
                            if (playerName == null) {
                                response = buildError("Please set your name first");
                            } else if (gameState != null) {
                                response = buildError("You are already in a game");
                            } else {
                                gameState = new PlayerGameState(playerName, gradingMode);
                                response = handleJoin(gameState);
                            }
                            break;

                        case LEADERBOARD:
                            response = Response.newBuilder()
                                    .setType(Response.ResponseType.LEADERBOARD_RESPONSE)
                                    .setOk(true)
                                    .setMessage("Top 10 Scores:")
                                    .setLeaderboard(Leaderboard.newBuilder()
                                            .addAllEntries(leaderboard.getTopScores(10))
                                            .build())
                                    .build();
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
     * Handle JOIN request
     */
    private static Response handleJoin(PlayerGameState gameState) {
        BotOpponent b1 = gameState.getBot1();
        BotOpponent b2 = gameState.getBot2();
        BotOpponent b3 = gameState.getBot3();

        String msg = "Game started! You're playing against " +
                b1.getName() + ", " + b2.getName() + ", and " + b3.getName() + ". Current item:";
        PlayerStatus playerStatus = PlayerStatus.newBuilder()
                .setGoldRemaining(gameState.getGold())
                .build();

        return Response.newBuilder()
                .setType(Response.ResponseType.GAME_JOINED)
                .setOk(true)
                .setMessage(msg)
                .setPlayerStatus(playerStatus)
                .setNextItem(itemToProto(gameState.getCurrentItem()))
                .build();
    }

    /**
     * Handle BID request
     */
    private static Response handleBid(Request request, PlayerGameState gameState) {
        Item item = gameState.getCurrentItem();
        int reservePrice = item.getMinValue() / 2;
        int playerBid = request.getBidAmount();
        int effectiveBid;

        if (playerBid == -1) {
            effectiveBid = 0;
        } else {
            effectiveBid = playerBid;
        }

        BotOpponent b1 = gameState.getBot1();
        BotOpponent b2 = gameState.getBot2();
        BotOpponent b3 = gameState.getBot3();

        String[] names = { gameState.getPlayerName(), b1.getName(), b2.getName(), b3.getName() };
        int[] bids = { effectiveBid, b1.decideBid(item, reservePrice), b2.decideBid(item, reservePrice), b3.decideBid(item, reservePrice) };

        // highest bid >= reserve ties
        String winnerName = "(unsold)";
        int winningBid = 0;
        for (int i = 0; i < names.length; i++) {
            if (bids[i] < reservePrice) continue;
            if (bids[i] > winningBid) {
                winnerName = names[i];
                winningBid = bids[i];
            } else if (bids[i] == winningBid && names[i].compareTo(winnerName) < 0) {
                winnerName = names[i];
                winningBid = bids[i];
            }
        }
        if (winnerName.equals(gameState.getPlayerName())) {
            gameState.awardItemToPlayer(item, winningBid);
        } else if (winnerName.equals(b1.getName())) {
            b1.awardItem(item, bids[1]);
        } else if (winnerName.equals(b2.getName())) {
            b2.awardItem(item, bids[2]);
        } else if (winnerName.equals(b3.getName())) {
            b3.awardItem(item, bids[3]);
        } 
        List<PlayerBid> allBids = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            allBids.add(PlayerBid.newBuilder().setPlayerName(names[i]).setBidAmount(bids[i]).build());
        }

        AuctionResult result = AuctionResult.newBuilder()
                .setItem(itemToProto(item))
                .setActualValue(item.getActualValue())
                .setWinnerName(winnerName)
                .setWinningBid(winningBid)
                .addAllAllBids(allBids)
                .build();

        // bidding now
        boolean hasMore = gameState.moveToNextItem();
        String bidMessage;

        if (hasMore) {
            bidMessage = "Auction complete!";
        } else {
            bidMessage = "Auction complete! Calculating final scores...";
        }

        Response.Builder builder = Response.newBuilder()
                .setType(Response.ResponseType.BID_RESULT)
                .setOk(true)
                .setMessage(bidMessage)
                .setResult(result)
                .setPlayerStatus(PlayerStatus.newBuilder().setGoldRemaining(gameState.getGold()).build());

        if (hasMore) {
            builder.setNextItem(itemToProto(gameState.getCurrentItem()));
        }

        return builder.build();
    }

    /**
     * Makes GAME_OVER response
     */
    private static Response gameOverResponse(PlayerGameState gameState, int rank) {
        BotOpponent b1 = gameState.getBot1();
        BotOpponent b2 = gameState.getBot2();
        BotOpponent b3 = gameState.getBot3();

        PlayerStatus[] scores = {
            PlayerStatus.newBuilder()
                .setPlayerName(gameState.getPlayerName())
                .setGoldRemaining(gameState.getGold())
                .setItemsValue(gameState.getInventoryValue())
                .setTotalScore(gameState.getPlayerScore())
                .addAllItemsWon(gameState.getItemNames())
                .build(),
            PlayerStatus.newBuilder()
                .setPlayerName(b1.getName())
                .setGoldRemaining(b1.getGold())
                .setItemsValue(b1.getInventoryValue())
                .setTotalScore(b1.getTotalScore())
                .addAllItemsWon(b1.getItemNames())
                .build(),
            PlayerStatus.newBuilder()
                .setPlayerName(b2.getName())
                .setGoldRemaining(b2.getGold())
                .setItemsValue(b2.getInventoryValue())
                .setTotalScore(b2.getTotalScore())
                .addAllItemsWon(b2.getItemNames())
                .build(),
            PlayerStatus.newBuilder()
                .setPlayerName(b3.getName())
                .setGoldRemaining(b3.getGold())
                .setItemsValue(b3.getInventoryValue())
                .setTotalScore(b3.getTotalScore())
                .addAllItemsWon(b3.getItemNames())
                .build()
        };
        String gameWinner = scores[0].getPlayerName();
        int topScore = scores[0].getTotalScore();

        // finding winner
        for (PlayerStatus s : scores) {
            if (s.getTotalScore() > topScore) {
                gameWinner = s.getPlayerName();
                topScore = s.getTotalScore();
            } else if (s.getTotalScore() == topScore && s.getPlayerName().compareTo(gameWinner) < 0) {
                gameWinner = s.getPlayerName();
                topScore = s.getTotalScore();
            }
        }
        GameResult gameResult = GameResult.newBuilder()
                .addAllPlayerScores(java.util.Arrays.asList(scores))
                .setWinnerName(gameWinner)
                .setLeaderboardPosition(rank)
                .build();



        return Response.newBuilder()
                .setType(Response.ResponseType.GAME_OVER)
                .setOk(true)
                .setMessage("Game over! Final results:")
                .setGameResult(gameResult)
                .build();
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
