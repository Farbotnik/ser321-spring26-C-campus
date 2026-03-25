# Activity 2: Auction Game vs Bot Opponents

## Overview

In this activity, you will implement a multi-threaded auction game server where players compete against bot opponents to win fantasy items. Players bid on 5 items, trying to maximize their final score (remaining gold + value of items won).

You will learn about:
- **Thread pools** - Managing multiple concurrent clients
- **Protocol Buffers** - Efficient binary serialization
- **Basic synchronization** - Protecting shared data (leaderboard)

## Starter Code

### What's Provided

1. **AuctionServer.java** - Server skeleton with examples (YOU COMPLETE THIS)
2. **AuctionClient.java** - Complete menu-driven client
3. **Item.java** - Item data structure
4. **ItemLoader.java** - Loads items from file
5. **BotOpponent.java** - Bot bidding logic (you don't modify)
6. **LeaderboardManager.java** - Leaderboard persistence (make thread-safe)
7. **auction.proto** - Protocol Buffers definition
8. **items.txt** - Fantasy items for auction
9. **PROTO_PROTOCOL.md** - Complete protocol specification
10. **build.gradle** - Build configuration
11. **ProtocolTest.java** - 4 starter tests

### Running the Starter Code

**Build without running tests:**
```bash
gradle build -x test
```

**Start the server:**
```bash
gradle runServer
# Or with custom port:
gradle runServer --args="9000"
```

**Start the client (in a new terminal):**
```bash
gradle runClient
# Or with custom host/port:
gradle runClient -Phost=localhost -Pport=9000
```

## Game Mechanics
See given PDF.

## Protocol Details

See `PROTO_PROTOCOL.md` for complete specification.

### Request Types
- REGISTER - Set player name
- JOIN - Start game against bots
- BID - Submit bid on current item (-1 to skip)
- LEADERBOARD - Query top scores
- QUIT - Disconnect

### Response Types
- WELCOME - Greeting
- GAME_JOINED - Game started with first item
- BID_RESULT - Auction complete, show winner
- GAME_OVER - Final scores + leaderboard rank
- LEADERBOARD_RESPONSE - Top 10 scores
- ERROR - Validation error
- FAREWELL - Disconnect acknowledged

## Testing

### Protocol Tests
Start your server in grading mode and run the provided tests:
```bash
# Terminal 1: Start server
gradle runServer --args="--grading"

# Terminal 2: Run tests
gradle test
```

### Manual Testing

**Single Player Test:**
1. Start server: `gradle runServer`
2. Start client: `gradle runClient`
3. Register name, join game, bid on all items
4. Try skipping (-1) and bidding below reserve price
5. See final score and leaderboard position

**Multiple Concurrent Players Test:**
1. Start server
2. Start 3 clients in separate terminals
3. Each registers unique name and joins
4. All play independent games simultaneously
5. Check leaderboard to see all scores

**Leaderboard Persistence Test:**
1. Play a game and note your score
2. Stop server (Ctrl+C)
3. Restart server
4. Query leaderboard - your score should still be there

## Example Game Session

```
Server: Welcome to the Auction Game! Please set your name.
Client: Warrior
Server: Welcome, Warrior! You have 150 gold. Type 'join' to start playing!

Client: join
Server: Game started! You're playing against Alaric, Brynn, and Cedric.
        Item #1: Sword of Flames (weapon, 30-50 gold, reserve: 15)
Client: 40

Server: Auction complete!
        Winner: Warrior (bid: 40)
        All Bids: Warrior=40, Alaric=35, Brynn=32, Cedric=28
        Actual Value: 42 gold
        Item #2: Magic Shield (armor, 25-45 gold, reserve: 12)
Client: -1  (skip)

Server: Auction complete!
        Winner: Alaric (bid: 30)
        ...

... continues for all items ...

Server: Game over! Final results:
        Warrior: 165 total (70 gold + 95 items)
        Alaric: 110 total (40 gold + 70 items)
        Brynn: 105 total (55 gold + 50 items)
        Cedric: 100 total (60 gold + 40 items)
        Winner: Warrior
        Your Leaderboard Position: #3
```

## Tips
- Use ExecutorService for the thread pool - don't create threads manually
- Follow the protocol specification exactly
- Test with multiple concurrent clients
- Make sure leaderboard persists across server restarts (uses `scores.txt`)
