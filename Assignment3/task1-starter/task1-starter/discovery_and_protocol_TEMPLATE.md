# Task 1.2: Mystery Service Discovery and Protocol Documentation

**Your Name:** Neil Farbotnik
**How I tested:** Unit Tests in ServerTest.java

---

## Part 1: Discovery Log

Document at least 8 test attempts showing your systematic investigation.

### Attempt 1
**Request Sent:**
```json
{
  "type": "analyzer"
}
```

**Response Received:**
```json
{
  "ok":false,
  "message":"Field 'action' does not exist in request. Hint: what action do you want to perform?"
}
```

**What I Learned:**
I may need an action field since I was sent an error response.

---

### Attempt 2
**Request Sent:**
```json
{
  "type": "analyzer",
  "action": " "
}
```

**Response Received:**
```json
{
  "ok":false,
  "message":"Field 'text' does not exist in request"
}
```
**What I Learned:**
I may need an text field to make it work.

---
### Attempt 3
**Request Sent:**
```json
{
  "type": "analyzer",
  "action": " ", 
  "text": "abcdef"
}
```

**Response Received:**
```json
{
  "ok":false,
  "message":"Action ' ' not supported. Valid actions: wordcount, charcount, search"
}
```
**What I Learned:**
I have 3 options for the actions :  wordcount, charcount, search

---

### Attempt 4
**Request Sent:**
```json
{
  "type": "analyzer",
  "action": "wordcount",
  "text": "abcdef ss"
}
```

**Response Received:**
```json
{
  "count":2,
  "action":"wordcount",
  "type":"analyzer",
  "ok":true
}
```

**What I Learned:**
action: wordcount counts words in my text field and is a success response

---


### Attempt 5
**Request Sent:**
```json
{
  "type": "analyzer",
  "action": "charcount",
  "text": "abcdef ss"
}
```

**Response Received:**
```json
{
  "count":9,
  "action":"charcount",
  "type":"analyzer",
  "ok":true
}
```
**What I Learned:**
action: charcount counts all the chars in my text and is a success response

---
### Attempt 6
**Request Sent:**
```json
{
  "type": "analyzer",
  "action": "search",
  "text": "abcdef ss"
}
```
**Response Received:**
```json
{
  "ok":false,
  "message":"Field 'find' does not exist in request"
}
```
**What I Learned:**
action: search relies on the find field. this is also an error response.

---
### Attempt 7
**Request Sent:**
```json
{
  "type": "analyzer",
  "action": "search",
  "text": "abcdef ss",
  "find": "f"
}
```

**Response Received:**
```json
{
  "found":true,
  "find":"f",
  "count":1,
  "action":"search",
  "positions":[5],
  "type":"analyzer",
  "ok":true
}
```
**What I Learned:**
This is a success response and it tells me where in the string is the char I place in find field

---
### Attempt 8
**Request Sent:**
```json
{
  "type": "analyzer",
  "action": "search",
  "text": "abcdef ss",
  "find": ""
}
```

**Response Received:**
```json
{
  "ok":false,
  "message":"Field 'find' cannot be empty"
}
```

**What I Learned:**
find cannot be empty. Failed response.

---

[Continue for at least 8 attempts - show your progression from initial testing to complete understanding]

---

## Part 2: Complete Protocol Specification

Follow the same format as Task 1.1 README protocols.

### Analyzer

This analyzes a text string. You can count words, count characters,
or search for a substring in text.

Request:

    {
        "type": "analyzer",
        "action" : <String>, -- actions: "wordcount", "charcount", or "search"
        "text" : <String>, -- the text to analyze
        "find" : <String> -- substring to search for
    }

Success response - action: "wordcount":

    {
        "type" : "analyzer",
        "ok" : true,
        "action" : "wordcount",
        "count" : <int> -- number of words in text
    }

Success response - action: "charcount":

    {
        "type" : "analyzer",
        "ok" : true,
        "action" : "charcount",
        "count" : <int> -- number of characters in text
    }

Success response - action: "search":

    {
        "type" : "analyzer",
        "ok" : true,
        "action" : "search",
        "find" : <String>,
        "found" : <bool>, -- true if find exists in text
        "count" : <int>, -- number of times it appears in text
        "positions" : <Array> -- where it was found in the string
    }

Error response:

    {
        "ok" : false,
        "message" : "Field 'action' does not exist in request. Hint: what action do you want to perform?"
    }

Error response - missing text field:

    {
        "ok" : false,
        "message" : "Field 'text' does not exist in request"
    }

Error response -  not supported action:

    {
        "ok" : false,
        "message" : "Action '<value>' not supported. Valid actions: wordcount, charcount, search"
    }

Error response - missing find field (search only):

    {
        "ok" : false,
        "message" : "Field 'find' does not exist in request"
    }

Error response - empty find field (search only):

    {
        "ok" : false,
        "message" : "Field 'find' cannot be empty"
    }

---

## Part 3: Summary

**Total Operations Discovered:**
wordcount, charcount search
**How I approached discovery:**
I started by sending just the type field and added fields one at a time based on the error messages I received. 
Each error response told me exactly what the next required field should be.
**Most challenging part:**
The search operation was the most complex because it required an extra field called find on top of the initial request. 
Figuring out that find could not be empty was only discovered by testing an empty string.
