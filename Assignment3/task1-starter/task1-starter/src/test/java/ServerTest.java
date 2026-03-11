import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;


public class ServerTest {

    Socket sock;
    OutputStream out;
    ObjectOutputStream os;

    DataInputStream in;


    // Establishing a connection to the server, make sure you start the server on localhost and 8888
    @org.junit.Before
    public void setUp() throws Exception {
        // Establish connection to server and create in/out streams
        sock = new Socket("localhost", 8888); // connect to host and socket

        // get output channel
        out = sock.getOutputStream();

        // create an object output writer (Java only)
        os = new ObjectOutputStream(out);

        // setup input stream
        in = new DataInputStream(sock.getInputStream());
    }

    @org.junit.After
    public void close() throws Exception {
        if (out != null)  out.close();
        if (sock != null) sock.close();
    }

    @Test
    public void addRequest() throws IOException {
        // create a correct req for server
        JSONObject req = new JSONObject();
        req.put("type", "add");
        req.put("num1", "1");
        req.put("num2", "2");

        // write the whole message
        os.writeObject(req.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        String i = (String) in.readUTF();
        // assuming I get correct JSON back
        JSONObject res = new JSONObject(i);

        // test response
        assertTrue(res.getBoolean("ok"));
        assertEquals("add", res.getString("type"));
        assertEquals(3, res.getInt("result"));

        // Wrong request to server num2 missing
        JSONObject req2 = new JSONObject();
        req2.put("type", "add");
        req2.put("num1", "1");
        // write the whole message
        os.writeObject(req2.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        i = (String) in.readUTF();
        // assuming I get correct JSON back
        res = new JSONObject(i);

        System.out.println(res);

        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("Field num2 does not exist in request", res.getString("message"));

        // Wrong request to server num1 missing
        JSONObject req3 = new JSONObject();
        req3.put("type", "add");
        req3.put("num2", "1");
        // write the whole message
        os.writeObject(req3.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        i = (String) in.readUTF();
        // assuming I get correct JSON back
        res = new JSONObject(i);

        System.out.println(res);

        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("Field num1 does not exist in request", res.getString("message"));

        // Wrong request to server num1 num2 missing
        JSONObject req4 = new JSONObject();
        req4.put("type", "add");
        // write the whole message
        os.writeObject(req4.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        i = (String) in.readUTF();
        // assuming I get correct JSON back
        res = new JSONObject(i);

        System.out.println(res);

        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("Field num1 does not exist in request", res.getString("message"));

        // Wrong request to server num2 missing
        JSONObject req5 = new JSONObject();
        req5.put("type", "add");
        req5.put("num1", "hello");
        req5.put("num2", "2");
        // write the whole message
        os.writeObject(req5.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        i = (String) in.readUTF();
        // assuming I get correct JSON back
        res = new JSONObject(i);

        System.out.println(res);

        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("Field num1/num2 needs to be of type: int", res.getString("message"));
    }

    @Test
    public void echoRequest() throws IOException {
        // valid request with data
        JSONObject req1 = new JSONObject();
        req1.put("type", "echo");
        req1.put("data", "gimme this back!");
        // write the whole message
        os.writeObject(req1.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();
        String i = (String) in.readUTF();
        // assuming I get correct JSON back
        JSONObject res = new JSONObject(i);
        // test response
        assertTrue(res.getBoolean("ok"));
        assertEquals("echo", res.getString("type"));
        assertEquals("Here is your echo: gimme this back!", res.getString("echo"));

        // Invalid request - no data sent
        JSONObject req2 = new JSONObject();
        req2.put("type", "echo");
        // write the whole message
        os.writeObject(req2.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();
        i = (String) in.readUTF();
        // assuming I get correct JSON back
        res = new JSONObject(i);
        System.out.println(res);
        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("Field data does not exist in request", res.getString("message"));
    }
    @Test
    public void stringConcatWithInt() throws IOException {
        String string1 = "hello";
        String string2 = "5";
        JSONObject req1 = new JSONObject();
        req1.put("type", "stringconcatenation");
        req1.put("string1", string1);
        req1.put("string2", string2);
        os.writeObject(req1.toString());
        os.flush();
        String i = (String) in.readUTF();

        JSONObject res = new JSONObject(i);
        assertTrue(res.getBoolean("ok"));
        assertEquals("stringconcatenation", res.getString("type"));
        assertEquals("hello5", res.getString("result"));
    }

    // tests happy case
    @Test
    public void currencySuccess() throws IOException {
        JSONObject req1 = new JSONObject();
        req1.put("type", "currency");
        req1.put("amount", 100.0);
        req1.put("from", "USD");
        req1.put("to", "EUR");
        os.writeObject(req1.toString());
        os.flush();

        String i = (String) in.readUTF();
        JSONObject res = new JSONObject(i);
        assertTrue(res.getBoolean("ok"));
        assertEquals("currency", res.getString("type"));
        assertEquals("USD", res.getString("from"));
        assertEquals("EUR", res.getString("to"));
        assertEquals(100.0, res.getDouble("amount"), 0.001);
        assertEquals(92.0, res.getDouble("result"), 0.001);
        assertEquals(0.92, res.getDouble("rate"), 0.001);
    }
    // tests negative
    @Test
    public void currencyError() throws IOException {
        // Error case: negative amount
        JSONObject req1 = new JSONObject();
        req1.put("type", "currency");
        req1.put("amount", -50.0);
        req1.put("from", "USD");
        req1.put("to", "GBP");
        os.writeObject(req1.toString());
        os.flush();

        String i = (String) in.readUTF();
        JSONObject res = new JSONObject(i);
        System.out.println(res);

        assertFalse(res.getBoolean("ok"));
        assertEquals("Field 'amount' cannot be negative", res.getString("message"));
    }

    @Test
    public void notJSON() throws IOException {
        // create a correct req for server
        os.writeObject("a");

        String i = (String) in.readUTF();
        // assuming I get correct JSON back
        JSONObject res = new JSONObject(i);

        // test response
        assertFalse(res.getBoolean("ok"));
        assertEquals("req not JSON", res.getString("message"));

        // calling the other test to make sure server continues to work and the "continue" does what it is supposed to do
        addRequest();
    }

    @Test
    public void analyzer1() throws Exception {
        JSONObject req = new JSONObject();
        req.put("type", "analyzer");
        req.put("action", "search");
        req.put("text", "abcdefss");
        req.put("find", "");
        os.writeObject(req.toString());
        os.flush();

        String i = (String) in.readUTF();
        JSONObject res = new JSONObject(i);
        System.out.println(res);
    }
    @Test
    public void analyzerWordCount() throws Exception {
        JSONObject req = new JSONObject();
        req.put("type", "analyzer");
        req.put("action", "wordcount");
        req.put("text", "abcdef ss");
        os.writeObject(req.toString());
        os.flush();

        String i = (String) in.readUTF();
        JSONObject res = new JSONObject(i);
        System.out.println(res);

        assertTrue(res.getBoolean("ok"));
        assertEquals("analyzer", res.getString("type"));
        assertEquals("wordcount", res.getString("action"));
        assertEquals(2, res.getInt("count"));
    }

    @Test
    public void analyzerCharCount() throws Exception {
        JSONObject req = new JSONObject();
        req.put("type", "analyzer");
        req.put("action", "charcount");
        req.put("text", "abcdef ss");
        os.writeObject(req.toString());
        os.flush();
        String i = (String) in.readUTF();
        JSONObject res = new JSONObject(i);
        System.out.println(res);

        assertTrue(res.getBoolean("ok"));
        assertEquals("analyzer", res.getString("type"));
        assertEquals("charcount", res.getString("action"));
        assertEquals(9, res.getInt("count"));
    }

    @Test
    public void analyzerSearch() throws Exception {
        JSONObject req = new JSONObject();
        req.put("type", "analyzer");
        req.put("action", "search");
        req.put("text", "abcdef ss");
        req.put("find", "f");
        os.writeObject(req.toString());
        os.flush();
        String i = (String) in.readUTF();
        JSONObject res = new JSONObject(i);
        System.out.println(res);

        assertTrue(res.getBoolean("ok"));
        assertEquals("analyzer", res.getString("type"));
        assertEquals("search", res.getString("action"));
        assertEquals("f", res.getString("find"));
        assertTrue(res.getBoolean("found"));
        assertEquals(1, res.getInt("count"));
        assertEquals(5, res.getJSONArray("positions").getInt(0));
    }

}