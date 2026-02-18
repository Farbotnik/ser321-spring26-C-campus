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
    [Your protocol design here]
}
```

**Success Response:**
```json
{
    [Your protocol design here]
}
```

**Error Response(s):**
```json
{
    [Document all possible errors]
}
```

## Error Handling Strategy

[Explain your approach to error handling:]

**Server-side validation:**
- [What validations does your server perform?]
  <Your answer>

- [How do you handle missing fields?]
  <Your answer>

- [How do you handle invalid data types?]
  <Your answer>

- [How do you handle game state errors?]
  <Your answer>

---

## Robustness

[Explain how you ensured robustness:]

**Server robustness:**
- [How does server handle invalid input without crashing?]
- <Your answer>


**Client robustness:**
- [How does client handle unexpected responses?]
- <Your answer>

- [What happens if server is unavailable?]
- <Your answer>

---

## Assumptions (if applicable)

[List any assumptions you made about the protocol or game rules]

1. [Assumption 1]
2. [Assumption 2]
3. [etc.]

---

## Known Issues

[List any known bugs or limitations]

1. [Issue 1]
2. [Issue 2]

---
