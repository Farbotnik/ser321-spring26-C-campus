# Assignment 3 Task 2: Hangman Game Protocol

**Author:** [Your Name]
**Date:** [Date]

---

## How to Run
You can use Gradle to run things, running with ./gradlew is of course also an option
**Server:**
Default
```bash
gradle Server
```

With arguments
```bash
gradle Server -Pport=8888
```

**Client:**
Default but running more quietly on Gradle
```bash
gradle Client --console=plain -q
```

With arguments
```bash
gradle Client -Phost=localhost -Pport=8888
```

---

## Video Demonstration

**Link:** [Insert link to your 4-7 minute video demonstration here]

The video demonstrates:
- Starting server and client
- Complete game playthrough
- All implemented features

---

## Implemented Features Checklist

### Core Features (Required)
- [x] Set Player Name (provided as example)
- [ ] Start New Game
- [ ] Guess Letter
- [ ] Game State
- [ ] Win/Lose Detection
- [x] Graceful Quit

### Medium Features (Enhanced Gameplay)
- [ ] Hint feature
- [ ] Word Guessing
- [ ] Guessed Letters Command
- [ ] Give Up

### Advanced Features (Competition)
- [ ] Scoring System
- [ ] Leaderboard

**Note:** Mark [x] for completed features, [ ] for not implemented.

---

## Protocol Specification

### Overview
[Provide a brief overview of your protocol design - what patterns did you use, how does communication work, etc.]

---

### 1. Set Player Name

**Request:**
```json
{
    "type": "name",
    "name": "<string>"
}
```

**Success Response:**
```json
{
    "type": "name",
    "ok": true,
    "message": "Welcome <name>! ..."
}
```

**Error Response:**
```json
{
    "ok": false,
    "message": "Name cannot be empty"
}
```

---

### 2. Start New Game

**Request:**
```json
{
    "type": "start"
}
```

**Success Response:**
```json
{
    "type": "start",
    "ok": true,
    "message": "hangmanSting",
    "hiddenWord": "_____",
    "wordLength": 5,
    "lives": 6,
    "misses": 2,
    "points": 8
}
```

**Error Response(s):**
```json
{
    "type": "start",
    "ok": false,
    "message": "Game already in progress"
}

```

---

### 3. Guess Letter

**Request: Letter Guess**
```json
{
    "type": "guess",
    "letter": "a"
}
```
**Request: Word Guess**
```json
{
    "type": "guess",
    "letter": "array"
}
```

**Success Response: Correct**
```json
{
    "type": "guess",
    "ok": true, 
    "correct": true,
    "message": "hangmanString",
    "hiddenWord": "a__a_",
    "wordLength": 5,
    "lives": 6,
    "misses": 0,
    "points": 10
}
```
**Success Response: Wrong**
```json
{
  "type": "guess",
  "ok": true,
  "correct": false,
  "message": "hangmanString",
  "hiddenWord": "_____",
  "wordLength": 5,
  "lives": 5,
  "misses": 1,
  "points": -1
}
```
``
**Success Response: Wrong Word**
```json
{
  "type": "guess",
  "ok": true,
  "correct": false,
  "message": "hangmanString",
  "hiddenWord": "_____",
  "wordLength": 5,
  "lives": 4,
  "misses": 2,
  "points": -2
}
```

**Error Response(s): Guess non-letter**
```json
{
  "type": "guess", 
  "ok": false, 
  "message": "Invalid input"
}
```

**Error Response(s): Already guessed**
```json
{
  "type": "guess", 
  "ok": false, 
  "message": "Letter 'a' was already guessed"
}
```
**Error Response(s): No Game**
```json
{
    "type": "guess",
    "ok": false,
    "message ": "No active game"
}
```

---

### 4. Game State

**Request:**
```json
{
    "type": "state"
}
```

**Success Response:**
```json
{
    "type": "state",
    "ok": true,
    "message": "hangmanString",
    "hiddenWord": "a_a__",
    "lives": 6,
    "misses": 2,
    "points": 8
}
```

**Error Response(s): No Game**
```json
{
    "type": "state",
    "ok": false,
    "message ": "No active game"
}
```

---

### 5. Win/Loss Detection

**Win Response:**
```json
{
    "type": "guess",
    "ok": true,
    "correct": true,
    "gameOver": true,
    "result": "win",
    "message": "hangmanString",
    "hiddenWord": "array",
    "lives": 3,
    "misses": 3,
    "points": 22,
    "solution": "array"
}
```

**Lose Response:**
```json
{
    "type": "guess",
    "ok": true,
    "correct": false,
    "gameOver": true,
    "result": "loss",
    "message": "hangmanString",
    "hiddenWord": "a__ay",
    "lives": 0,
    "misses": 6,
    "points": 9,
    "solution": "array"
}
```

---

### 6. Hint Feature

**Request:**
```json
{
    "type": "hint"
}
```

**Success Response:**
```json
{
    "type": "hint",
    "ok": true,
    "message": "hangmanString",
    "hiddenWord": "a_a__",
    "hintLetter": "y",
    "lives": 6,
    "misses": 0, 
    "points": 2
}
```

**Error Response(s): No Game**
```json
{
    "type": "hint",
    "ok": false,
    "message ": "No active game"
}
```

---

### 7. Guessed Letters

**Request:**
```json
{
    "type": "guessed"
}
```

**Success Response:**
```json
{
    "type": "guessed",
    "ok": true,
    "guessedLetters": ["a","e","j","v"]
}
```

**Error Response(s): No Game**
```json
{
    "type": "guessed",
    "ok": false,
    "message ": "No active game"
}
```
---

### 8. Give Up

**Request:**
```json
{
    "type": "giveup"
}
```

**Success Response:**
```json
{
    "type": "giveup",
    "ok": true,
    "message": "You gave up!The word was: ",
    "solution": "array",
    "doom": "ASCIIART"
}
```

**Error Response(s): No Game**
```json
{
    "type": "giveup",
    "ok": false,
    "message ": "No active game"
}
```
---
## Error Handling Strategy

[Explain your approach to error handling:]

**Server-side validation:**
- [What validations does your server perform?]
- The server checks every request for a valid type field or required fields like letter or word before doing anything

- [How do you handle missing fields?]
- If a required field is missing, the server uses testField() to return ok: false with a message explaining which field is missing, without touching any game state

- [How do you handle invalid data types?]
- If the data is the wrong type, the server checks with Character.isLetter() and returns an error message before going.

- [How do you handle game state errors?]
- If someone sends a guess when no game is running, the handler checks inGame first and immediately returns an error.

---

## Robustness

[Explain how you ensured robustness:]

**Server robustness:**
- [How does server handle invalid input without crashing?]
- The server wraps readObject() in a try-catch so if a client sends corrupted data, the server just moves on to the next connection instead of crashing.


**Client robustness:**
- [How does client handle unexpected responses?]
- Every response is null-checked before reading any fields, so a bad response just prints an error message instead of crashing the client.

- [What happens if server is unavailable?]
- If the server goes down midgame, the catch block in sendRequest sets inGame = false so the client returns to the main menu cleanly.

---

## Assumptions (if applicable)

[List any assumptions you made about the protocol or game rules]

1. Same player name means same player for leaderboard tracking.

---

## Known Issues

[List any known bugs or limitations]

1. The leaderboard resets when the server restarts since it is stored in memory only.
2. Guesses dont lose lives

---
