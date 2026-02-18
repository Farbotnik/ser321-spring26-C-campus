##### Author: Instructor team SE, ASU Polytechnic, CIDSE, SE

##### Purpose
This is starter code for Assignment 3, Task 1.

**What's Provided:**
- Two working example services: `echo`, `add`
- One working but undocumented service: `calculatemany` (server-side only)
- One buggy service: `stringconcatenation` (has bugs to find and fix)
- Two services to implement: `currency`, `playlist` (you implement ONE)

**Your Tasks:**
- Part A: Document the `calculatemany` protocol by reading the server code
- Part B: Find and fix 4 bugs in the `stringconcatenation` service
- Part C: Implement ONE of the two services (`currency` or `playlist`) following the protocols below
- Part D: Deploy to AWS and test with peers

**How to Run:**
Default port and host:
  gradle Client
  gradle Server

Changing the port:
  gradle Server -Pport=9000
  gradle Client -Pport=9000

Changing the host if needed:
  gradle Client -Phost=localhost -Pport=9000

Running things without Gradle being so verbose with default values
 gradle Server --console=plain -q
 gradle Client --console=plain -q

Running with Gradle wrapper:
 Any combination from above use
 ./gradlew Server

**How to Test:**
There are two tests classes given, work on the ServerTest class when you write your test cases.
The ServerTest class assumes a server is running on 8888 and will establish a connection and make different requests.

The Testing class is only calling server methods without establishen a connection. This is just to show another way how to test things. It does not need a server running. I prefer the first way but to test a server without having to also take network issues into account and only test the methods in it, this is a good option.


*Use gradle or ./gradlew up to you*

Run all tests, assumes that server runs on 8888:
  gradle test

Run only the ServerTest class, assumes server runs on 8888
  gradle serverTest


Run only the Testing class, this calls methods from SockServer directly, server does not need to run
  gradle simpleTest

## Protocol: ##


### Important Protocol Notes
Do not change any given protocol specifications in this README (field names, types, response formats, error messages, etc.).
You must add the missing protocol documentation for `calculatemany` in the section above.
Choose ONE of the two services (`currency` or `playlist`) to implement exactly as specified.

### Echo: ###

Request:

    {
        "type" : "echo", -- type of request
        "data" : <String>  -- String to be echoed
    }

General response:

    {
        "type" : "echo", -- echoes the initial response
        "ok" : <bool>, -- true or false depending on request
        "echo" : <String>,  -- echoed String if ok true
        "message" : <String>,  -- error message if ok false
    }

Success response:

    {
        "type" : "echo",
        "ok" : true,
        "echo" : <String> -- the echoed string (server prefixes with "Here is your echo: ")
    }

Error response:

    {
        "type" : "echo",
        "ok" : false,
        "message" : <String> -- what went wrong
    }

### Add: ###
Request:

    {
        "type" : "add",
        "num1" : <String>, -- first number -- String needs to be an int number e.g. "3"
        "num2" : <String> -- second number -- String needs to be an int number e.g. "4"
    }

General response

    {
        "type" : "add", -- echoes the initial request
        "ok" : <bool>, -- true or false depending on request
        "result" : <int>,  -- result if ok true
        "message" : <String>,  -- error message if ok false
    }

Success response:

    {
        "type" : "add",
        "ok" : true,
        "result" : <int> -- the result of add
    }

Error response:

    {
        "type" : "add",
        "ok" : false,
        "message" : <String> - error message about what went wrong
    }


### CalculateMany: ###
**TODO - Part A:** You need to document this protocol by:
1. Reading the server implementation in SockServer.java
2. Testing the service with various inputs (add the service to the client or write Unit Tests to test)
3. Write the complete protocol specification here (follow the format of echo and add above)
4. Document ALL possible error cases (hint: there are at least 4 different error cases)

YOUR PROTOCOL DOCUMENTATION GOES HERE

### StringConcatenation: ###
This service will concatenate two strings provided by the client. The client will send a request to the server with two strings to be concatenated.
The server will concatenate the strings and send back the result to the client.

**NOTE - Part B:** This service has 4 bugs! The implementation (both client and server) does NOT match the protocol below. Find and fix all bugs in the code, then document them in `bug_report.md`.

Request:

    {
        "type" : "stringconcatenation",
        "string1" : <String>, -- first string
        "string2" : <String> -- second string
    }

General response:

    {
        "type" : "stringconcatenation",
        "ok" : <bool>, -- true or false depending on request
        "result" : <String>,  -- concatenated string if ok true
        "message" : <String>  -- error message if ok false
    }

Success response:

    {
        "type" : "stringconcatenation",
        "ok" : true,
        "result" : <String> -- concatenated string
    }

Error response:

    {
        "type" : "stringconcatenation",
        "ok" : false,
        "message" : <String> -- error message about what went wrong
    }


### Currency: ###
**TODO - Part C (Option 1):** You need to implement this service following the protocol specification below.

This service converts amounts between currencies using fixed exchange rates.
Supported currencies: USD, EUR, GBP.
The client sends an amount and the source/target currencies, and the server performs the conversion.

Request:

    {
        "type" : "currency",
        "amount" : <Number>, -- numeric value to convert (can be decimal) e.g. 100.50
        "from" : <String>, -- source currency: "USD", "EUR", or "GBP"
        "to" : <String> -- target currency: "USD", "EUR", or "GBP"
    }

General response:

    {
        "type" : "currency",
        "ok" : <bool>, -- true or false depending on request
        "from" : <String>, -- source currency
        "to" : <String>, -- target currency
        "amount" : <Number>, -- original amount
        "result" : <Number>, -- converted amount if ok true (rounded to 2 decimals)
        "rate" : <Number>, -- exchange rate used if ok true
        "message" : <String> -- error message if ok false
    }

Success response:

    {
        "type" : "currency",
        "ok" : true,
        "from" : "USD",
        "to" : "EUR",
        "amount" : 100.0,
        "result" : 92.0,
        "rate" : 0.92
    }

Error response (invalid currency):

    {
        "ok" : false,
        "message" : "Field 'from' must be one of: USD, EUR, GBP"
    }

Error response (negative amount):

    {
        "ok" : false,
        "message" : "Field 'amount' cannot be negative"
    }

**Fixed Exchange Rates (relative to USD):**
- USD = 1.0
- EUR = 0.92
- GBP = 0.79


### Playlist: ###
**TODO - Part C (Option 2):** You need to implement this service following the protocol specification below.

This service manages a playlist of songs. The client can add songs, remove songs, list the playlist contents, and clear the playlist. The playlist persists for the duration of the client connection.

**Action: add**

Request:

    {
        "type" : "playlist",
        "action" : "add",
        "song" : <String>, -- song title
        "artist" : <String> -- artist name
    }

Success response:

    {
        "type" : "playlist",
        "ok" : true,
        "action" : "add",
        "songCount" : <Number>, -- total songs in playlist
        "message" : <String> -- confirmation message e.g. "Added 'Song Title' by Artist to playlist"
    }

**Action: remove**

Request:

    {
        "type" : "playlist",
        "action" : "remove",
        "song" : <String> -- song title to remove
    }

Success response:

    {
        "type" : "playlist",
        "ok" : true,
        "action" : "remove",
        "songCount" : <Number>, -- remaining songs in playlist
        "message" : <String> -- confirmation message e.g. "Removed 'Song Title' from playlist"
    }

Error response (song not found):

    {
        "ok" : false,
        "message" : "Song 'Song Title' not found in playlist"
    }

**Action: list**

Request:

    {
        "type" : "playlist",
        "action" : "list"
    }

Success response:

    {
        "type" : "playlist",
        "ok" : true,
        "action" : "list",
        "songs" : [
            {
                "song" : <String>,
                "artist" : <String>
            }
        ], -- array of songs in playlist (empty array if playlist is empty)
        "songCount" : <Number> -- total songs
    }

**Action: clear**

Request:

    {
        "type" : "playlist",
        "action" : "clear"
    }

Success response:

    {
        "type" : "playlist",
        "ok" : true,
        "action" : "clear",
        "message" : "Playlist cleared"
    }

**Error responses:**

Invalid action:

    {
        "ok" : false,
        "message" : "Invalid action '<action>'. Valid actions: add, remove, list, clear"
    }

Empty song or artist (for add):

    {
        "ok" : false,
        "message" : "Field 'song' cannot be empty"
    }


### General error responses: ###
These are used for all requests.

Error response: When a required field "key" is not in request

    {
        "ok" : false
        "message" : "Field <key> does not exist in request"
    }

Error response: When a required field "key" is not of correct "type" (int, string, double)

    {
        "ok" : false
        "message" : "Field <key> needs to be of type: <type>"
    }

Error response: When the "type" is not supported, so an unsupported request

    {
        "ok" : false
        "message" : "Type <type> is not supported."
    }


Error response: When the request is not valid JSON

    {
        "ok" : false
        "message" : "req not JSON"
    }

---

## Task 1.2: Mystery Service Discovery ##

A mystery service is included in this starter code (provided as a JAR file). Your task is to:
1. Discover the complete protocol through systematic testing
2. Document your discovery process and protocol in `discovery_and_protocol.md`

The service type is `"analyzer"` - it is already integrated into the server. All other details must be discovered through testing.

See PDF and `discovery_and_protocol_TEMPLATE.md` for the required documentation format.

---

