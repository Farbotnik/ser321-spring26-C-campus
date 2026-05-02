# Neil Farbotnik
#### Screencast: https://app.screencast.com/qtq5ZWeLASIFK
## Run things locally without registry

First Terminal
```bash
gradle runNode 
```

Second Terminal
```bash
gradle runClient --console=plain -q
```

## Connecting to AWS
```bash
gradle runClient -Phost=3.145.176.74 -Pport=8000
```

## About 
Three services were implemented on top of the starter code:
- **Converter** - converts units of length, weight, and temperature
- **Library** - manages a bunch of books 
- **Pokemon** - allows creating pokemon, battling two Pokemon against each other and viewing battle history.

## Pokemon Service
This service implements a simple Pokemon battle system using a rock paper scissors style type 
mechanic (FIRE, WATER, GRASS). Users can create Pokemon, battle them against each other, and get stored data from the server.
The battle logic (based on their type):
- FIRE > GRASS
- GRASS > WATER 
- WATER > FIRE 

### Requirements covered
- [x] Service allows at least 2 different requests:
    - CreatePokemon
    - BattlePokemon
    - GetBattleHistory
    - ListPokemon

- [x] Each request needs at least 1 input
    - CreatePokemon(name, type)
    - BattlePokemon(attackerName, defenderName)
    - GetBattleHistory(limit)
    - ListPokemon(filterType)

- [x] Response returns different data for different requests:
    - CreatePokemon = success/failure message
    - BattlePokemon = winner, loser, and battle summary
    - GetBattleHistory = list of past battles
    - ListPokemon = list of stored Pokemon

- [x] Response returns a repeated field:
    - GetBattleHistory returns a repeated list of battle records
    - ListPokemon returns a repeated list of Pokemon

- [x] Data is held persistent on the server:
    - Pokemon data is stored in pokemon_data.json
    - Battle results are stored in battle_history.json
    - Data remains available across multiple requests and server restarts

### Additional Features
- Prevents duplicate Pokemon names
- Prevents self-battles
- GetBattleHistory(limit) allows partial or full retrieval (0 returns all records)

## How to use the program
Once the client is running. Enter the number for the service you want and follow the prompts.

```
1. Converter - convert between units
2. Library - list all books
3. Library - search books by title or author
4. Library - borrow a book
5. Library - return a book
6. Pokemon - create a pokemon
7. Pokemon - battle two pokemon
8. Pokemon - view battle history
9. Pokemon - list all pokemon
0. Exit
```

#### Converter enter a number (value), then a from-unit, then a to-unit. Supported units:
- Length: `KILOMETER`, `MILE`, `YARD`, `FOOT`
- Weight: `KILOGRAM`, `POUND`
- Temperature: `CELSIUS`, `FAHRENHEIT`

#### Library books are identified by ISBN. 
Borrowing requires your name and the book's ISBN. Returning requires only the ISBN.

#### Pokemon
- When creating, enter a name and FIRE,WATER, or GRASS. 
- When battling, the list of existing Pokemon is shown first so you know who to pick. A Pokemon cannot battle itself. 
- Type advantages: Fire beats Grass, Grass beats Water, Water beats Fire.

### gradle runNode
Will run a node with services. The starter code includes Echo and Joke services as examples. You will need to implement and add the Converter and Library services.

For the Library service: A books.txt file is provided with initial book data (format: title|author|isbn, one per line). Your server should load this on first run and create library_data.json for persistence.

The node registers itself on the Registry. You can change the host and port the node runs on and this will register accordingly with the Registry

### gradle runClient
Will run a client which will call the services from the node, it talks to the node directly not through the registry. At the end the client does some calls to the Registry to pull the services, this will be needed later.


### gradle test
Runs the test cases.

IMPORTANT: Tests expect the server to be running first!
First run in one terminal:
```bash
gradle runNode
```
Then in second terminal:
```bash
gradle test
```
The tests connect to localhost:8000 by default.
