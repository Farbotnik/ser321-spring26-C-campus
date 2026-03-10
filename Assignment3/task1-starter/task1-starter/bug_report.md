# StringConcatenation Debugging Exercise

## Overview
The stringconcatenation service is implemented in both client and server, but has **4 bugs** that prevent it from working correctly according to the protocol specification.

The Correct Protocol is in the README.md

---

## The 4 Bugs

### Bug #1:  concat
**Location:** `SockServer.java`, line 184

**The Problem:** type in the concat() method puts "concat" instead of stringconcatenation
```
res.put("type", "concat");
```

**The Fix:**
```Solution
res.put("type", "stringconcatenation");
```

**Why it matters:**
Its important to make sure the server and client both follow the protocol. The type being wrong makes it impossible for the client to get the corrct repsonse here.


**How did you find this:**
Wrote a test case for a happy case client request and received errors indicating that type was concat instead of stringconcatenation.

### Bug #2:  result
**Location:** `SockServer.java`, line 188

**The Problem:** The server uses "combined" as a field instead of "result"
```
res.put("combined", str1 + str2);
```

**The Fix:**
```
res.put("result", str1 + str2);
```

**Why it matters:**
In its current state the concatenated data sent back to the client will be in a field the client is not looking in. Goes against protocol.


**How did you find this:**
After rerunning my test method I was able to recognize that there was an error indicating that JSONObject["result"] not found, and tracked it down to the server.

### Bug #3:  str1
**Location:** `SockClient.java`, line 74

**The Problem:** In the client request one of the field in the JSON object are str1 instead of string1
```
json.put("str1", str1);
```

**The Fix:**
```
json.put("string1", str1);
```

**Why it matters:**
Putting in the wrong field when making a request does against the protocol and means the server is missing an argument for its response.

**How did you find this:**
When reviewing the README.md and the protocols and the lines of codes in client it stood out as different than the established protocol.


### Bug #4:  int
**Location:** `SockClient.java`, line 98

**The Problem:** In the client the got response section tries to print "result" as an int instead of a String.
```
System.out.println(res.getInt("result"));
```

**The Fix:**
```
System.out.println(res.getString("result"));
```

**Why it matters:**
This bug will crash the client as the response will be a string not an int.
**How did you find this:**
I ran the program after fixing all the previous bugs and came across this error: org.json.JSONException: JSONObject["result"] is not an int. I then traced it down to the culprit.