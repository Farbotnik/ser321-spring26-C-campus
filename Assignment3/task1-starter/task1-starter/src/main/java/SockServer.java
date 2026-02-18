import mysteryservice.MysteryService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.net.*;
import java.io.*;
import java.util.*;

/**
 * A class to demonstrate a simple client-server connection using sockets.
 *
 */
public class SockServer {
  static Socket sock;
  static DataOutputStream os;
  static ObjectInputStream in;

  static int port = 8888;


  public static void main (String args[]) {

    if (args.length != 1) {
      System.out.println("Expected arguments: <port(int)>");
      System.exit(1);
    }

    try {
      port = Integer.parseInt(args[0]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      //open socket
      ServerSocket serv = new ServerSocket(port);
      System.out.println("Server ready for connections");

      /**
       * Simple loop accepting one client and calling handling one request.
       *
       */


      while (true){
        System.out.println("Server waiting for a connection");
        sock = serv.accept(); // blocking wait
        System.out.println("Client connected");

        // setup the object reading channel
        in = new ObjectInputStream(sock.getInputStream());

        // get output channel
        OutputStream out = sock.getOutputStream();

        // create an object output writer (Java only)
        os = new DataOutputStream(out);

        boolean connected = true;
        while (connected) {
          String s = "";
          try {
            s = (String) in.readObject(); // attempt to read string in from client
          } catch (Exception e) { // catch rough disconnect
            System.out.println("Client disconnect");
            connected = false;
            continue;
          }

          JSONObject res = isValid(s);

          if (res.has("ok")) {
            writeOut(res);
            continue;
          }

          JSONObject req = new JSONObject(s);

          res = testField(req, "type");
          if (!res.getBoolean("ok")) { // no "type" header provided
            res = noType(req);
            writeOut(res);
            continue;
          }
          // check which request it is (could also be a switch statement)
          if (req.getString("type").equals("echo")) {
            res = echo(req);
          } else if (req.getString("type").equals("add")) {
            res = add(req);
          } else if (req.getString("type").equals("calculatemany")) {
            res = calculatemany(req);
          } else if (req.getString("type").equals("stringconcatenation")) {
            res = concat(req);
          } else if (req.getString("type").equals("analyzer")) {
            // Mystery service - discover the protocol
            res = MysteryService.processRequest(req);
          } else {
            res = wrongType(req);
          }
          writeOut(res);
        }
        // if we are here - client has disconnected so close connection to socket
        overandout();
      }
    } catch(Exception e) {
      e.printStackTrace();
      overandout(); // close connection to socket upon error
    }
  }


  /**
   * Checks if a specific field exists
   *
   */
  static JSONObject testField(JSONObject req, String key){
    JSONObject res = new JSONObject();

    // field does not exist
    if (!req.has(key)){
      res.put("ok", false);
      res.put("message", "Field " + key + " does not exist in request");
      return res;
    }
    return res.put("ok", true);
  }

  // handles the simple echo request
  static JSONObject echo(JSONObject req){
    System.out.println("Echo request: " + req.toString());
    JSONObject res = testField(req, "data");
    if (res.getBoolean("ok")) {
      if (!req.get("data").getClass().getName().equals("java.lang.String")){
        res.put("ok", false);
        res.put("message", "Field data needs to be of type: String");
        return res;
      }

      res.put("type", "echo");
      res.put("echo", "Here is your echo: " + req.getString("data"));
    }
    return res;
  }

  // handles the simple add request with two numbers
  static JSONObject add(JSONObject req){
    System.out.println("Add request: " + req.toString());
    JSONObject res1 = testField(req, "num1");
    if (!res1.getBoolean("ok")) {
      return res1;
    }

    JSONObject res2 = testField(req, "num2");
    if (!res2.getBoolean("ok")) {
      return res2;
    }

    JSONObject res = new JSONObject();
    res.put("ok", true);
    res.put("type", "add");
    try {
      res.put("result", req.getInt("num1") + req.getInt("num2"));
    } catch (org.json.JSONException e){
      res.put("ok", false);
      res.put("message", "Field num1/num2 needs to be of type: int");
    }
    return res;
  }

  
  // handles string concatenation
  static JSONObject concat(JSONObject req) {
    System.out.println("Concatenation request: " + req.toString());
    JSONObject res = testField(req, "string1");
    if (!res.getBoolean("ok")) {
      return res;
    }

    res = new JSONObject();
    res.put("ok", true);
    res.put("type", "concat");

    String str1 = req.getString("string1");
    String str2 = req.getString("string2");
    res.put("combined", str1 + str2);

    return res;
  }

  // handles the calculatemany request with multiple operations
  static JSONObject calculatemany(JSONObject req){
    System.out.println("Calculate many request: " + req.toString());
    JSONObject res = new JSONObject();

    // Check for numList field
    JSONObject numListCheck = testField(req, "numList");
    if (!numListCheck.getBoolean("ok")) {
      return numListCheck;
    }

    // Check for operation field
    JSONObject opCheck = testField(req, "operation");
    if (!opCheck.getBoolean("ok")) {
      return opCheck;
    }

    JSONArray array = req.getJSONArray("numList");
    String operation = req.getString("operation").toLowerCase();

    // Check for empty array
    if (array.length() == 0){
      res.put("ok", false);
      res.put("message", "Array 'numList' cannot be empty");
      return res;
    }

    // Validate operation
    if (!operation.equals("add") && !operation.equals("multiply") && !operation.equals("average")) {
      res.put("ok", false);
      res.put("message", "Invalid operation '" + req.getString("operation") + "'. Valid operations: add, multiply, average");
      return res;
    }

    // Extract integers from array
    int[] numbers = new int[array.length()];
    for (int i = 0; i < array.length(); i++){
      try {
        numbers[i] = array.getInt(i);
      } catch (org.json.JSONException e){
        res.put("ok", false);
        res.put("message", "Values in numList must be integers");
        return res;
      }
    }

    // Perform the operation
    res.put("ok", true);
    res.put("type", "calculatemany");
    res.put("operation", operation);
    res.put("count", numbers.length);

    // Note: No overflow handling - extreme values may cause unexpected results
    if (operation.equals("add")) {
      int sum = 0;
      for (int num : numbers) {
        sum += num;
      }
      res.put("sum", sum);
    } else if (operation.equals("multiply")) {
      int product = 1;
      for (int num : numbers) {
        product *= num;
      }
      res.put("product", product);
    } else if (operation.equals("average")) {
      double sum = 0;
      for (int num : numbers) {
        sum += num;
      }
      double average = sum / numbers.length;
      // Round to 2 decimal places
      average = Math.round(average * 100.0) / 100.0;
      res.put("average", average);
    }

    return res;
  }


//  SOME GENERAL ERROR MESSAGES

  // creates the error message for wrong type
  static JSONObject wrongType(JSONObject req){
    System.out.println("Wrong type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "Type " + req.getString("type") + " is not supported.");
    return res;
  }

  // creates the error message for no given type
  static JSONObject noType(JSONObject req){
    System.out.println("No type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "No request type was given.");
    return res;
  }

  // From: https://www.baeldung.com/java-validate-json-string
  public static JSONObject isValid(String json) {
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      try {
        new JSONArray(json);
      } catch (JSONException ne) {
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "req not JSON");
        return res;
      }
    }
    return new JSONObject();
  }


// SENDING OUT REPSONSES, CLOSING CONNECTION
  // sends the response and closes the connection between client and server.
  static void overandout() {
    try {
      os.close();
      in.close();
      sock.close();
    } catch(Exception e) {e.printStackTrace();}

  }

  // sends the response and closes the connection between client and server.
  static void writeOut(JSONObject res) {
    try {
      os.writeUTF(res.toString());
      // make sure it wrote and doesn't get cached in a buffer
      os.flush();

    } catch(Exception e) {e.printStackTrace();}

  }
}