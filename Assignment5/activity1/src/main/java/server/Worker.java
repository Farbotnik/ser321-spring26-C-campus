package server;
import java.net.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import buffers.OperationProtos.*;

class Worker implements Runnable {
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int lastResult = 0;
    boolean sentResponse = false;
    String workerId = "?";

    public Worker(Socket s, String id) {
        clientSocket = s;
        workerId = id;
    }

    // Converts proto back to operator symbol
    static String typeToSymbol(Type type) {
        switch (type) {
            case ADDITION:
                return "+";
            case SUBTRACTION:
                return "-";
            case MULTIPLICATION:
                return "*";
            case DIVISION:
                return "/";
            case MODULAR:
                return "%";
            default:
                return "?";
        }
    }

    public static void main(String args[]) throws Exception {
        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <id(String)>");
            System.exit(1);
        }

        int port = 9099;
        String id = args[1];

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        System.out.println(id + " connecting to leader at localhost:" + port);

        ServerSocket serv = null;

        try {
            serv = new ServerSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

        while (serv.isBound() && !serv.isClosed()) {
            Socket clientSocket = serv.accept();
            System.out.println("Connected successfully!");

            Worker newWorker = new Worker(clientSocket, id);
            Thread newThread = new Thread(newWorker);
            newThread.start();
        }
    }

    public void run() {
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();

            while (true) {
                Request req = Request.parseDelimitedFrom(in);

                if (req == null) {
                    System.out.println("Leader disconnected");
                    break;
                }

                if (req.getType() == Type.CONSENSUS) {
                    int consensusValue = req.getNum1();
                    int agreedCount = req.getNum2();
                    int total = Integer.parseInt(req.getMessage());

                    System.out.println("Consensus announced: " + consensusValue + " (" + agreedCount + "/" + total + " workers agreed)");

                    if (sentResponse && lastResult == consensusValue) {
                        System.out.println("You voted with the majority!");
                    }

                    sentResponse = false;
                    System.out.println("Waiting for next task...");
                    continue;
                }

                System.out.println(workerId + " Task received: " + req.getNum1() + " " + typeToSymbol(req.getType()) + " " + req.getNum2());

                int result = 0;
                switch (req.getType()) {
                    case ADDITION:
                        result = req.getNum1() + req.getNum2();
                        break;
                    case SUBTRACTION:
                        result = req.getNum1() - req.getNum2();
                        break;
                    case MULTIPLICATION:
                        result = req.getNum1() * req.getNum2();
                        break;
                    case DIVISION:
                        result = req.getNum1() / req.getNum2();
                        break;
                    case MODULAR:
                        result = req.getNum1() % req.getNum2();
                        break;
                    default:
                        result = 0;
                        break;
                }

                System.out.println("Enter your result: " + result);

                Response.Builder b = Response.newBuilder();
                b.setResult(result);
                b.build().writeDelimitedTo(out);

                lastResult = result;
                sentResponse = true;
                System.out.println("Result submitted to leader.");
            }

        } catch (Exception ex) {
            System.out.println("Leader disconnected");
        }
    }
}
