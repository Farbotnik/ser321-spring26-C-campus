package client;

import java.net.*;
import java.io.*;
import java.util.Scanner;

import buffers.OperationProtos.*;

class Leader {

    // operator symbol to proto Type
    static Type parseType(String op) {
        switch (op) {
            case "+":
                return Type.ADDITION;
            case "-":
                return Type.SUBTRACTION;
            case "*":
                return Type.MULTIPLICATION;
            case "/":
                return Type.DIVISION;
            case "%":
                return Type.MODULAR;
            default:
                return Type.NONE;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Expected arguments: <host(String)> <port(int)> <workers(int)>");
            System.exit(1);
        }

        String host = args[0];
        int basePort = 9099;
        int numWorkers = 1;

        try {
            basePort = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        try {
            numWorkers = Integer.parseInt(args[2]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Workers] must be integer");
            System.exit(2);
        }

        System.out.println("Leader starting, connecting to " + numWorkers + " workers from " + host + ":" + basePort);
        System.out.println("Waiting for workers to connect... (need at least " + numWorkers + ")");

        Socket[] sockets = new Socket[numWorkers];
        Thread[] threads = new Thread[numWorkers];

        // Spawn one thread per worker to connect
        for (int i = 0; i < numWorkers; i++) {
            final int port = basePort + i;
            final int id = i + 1;
            final int idx = i;

            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sockets[idx] = new Socket(host, port);
                        System.out.println("Worker" + id + " connected from " + host + ":" + port);
                    } catch (Exception e) {
                        System.err.println("Worker" + id + " failed to connect: " + e.getMessage());
                    }
                }
            });

            threads[i].start();
        }

        // Wait for all connection threads to finish
        for (int i = 0; i < numWorkers; i++) {
            threads[i].join();
        }

        System.out.println("All " + numWorkers + " connected. Starting consensus rounds...\n");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Please enter an arithmetic task (or 'quit'): ");
            String input = scanner.nextLine().trim();

            if (input.equals("quit")) {
                break;
            }

            String[] parts = input.split(" ");
            if (parts.length != 3) {
                System.out.println("Invalid format. Use: number operator number (e.g. 3 + 5)");
                continue;
            }

            int num1;
            int num2;
            try {
                num1 = Integer.parseInt(parts[0]);
                num2 = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid numbers. Use: number operator number (e.g. 3 + 5)");
                continue;
            }
            Type type = parseType(parts[1]);

            Request req = Request.newBuilder()
                .setNum1(num1)
                .setType(type)
                .setNum2(num2)
                .build();

            // Send request to all workers
            for (int i = 0; i < numWorkers; i++) {
                if (sockets[i] != null) {
                    Request taggedReq = req.toBuilder().setMessage(String.valueOf(i + 1)).build();
                    taggedReq.writeDelimitedTo(sockets[i].getOutputStream());
                }
            }

            // Collect responses from all workers
            int[] results = new int[numWorkers];
            boolean[] responded = new boolean[numWorkers];
            Thread[] responseThreads = new Thread[numWorkers];

            for (int i = 0; i < numWorkers; i++) {
                final int idx = i;
                final int id = i + 1;

                responseThreads[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Response res = Response.parseDelimitedFrom(sockets[idx].getInputStream());
                            results[idx] = res.getResult();
                            responded[idx] = true;
                            System.out.println("Received from Worker" + id + ": " + res.getResult());
                        } catch (Exception e) {
                            System.err.println("Failed to receive from Worker" + id + ": " + e.getMessage());
                        }
                    }
                });

                responseThreads[i].start();
            }

            // Wait up to 20 seconds total for responses
            long deadline = System.currentTimeMillis() + 20000;
            for (int i = 0; i < numWorkers; i++) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining > 0) {
                    responseThreads[i].join(remaining);
                }
            }

            // Consensus is based on if a value only has votes from more than half of all workers
            int majority = numWorkers / 2 + 1;
            int consensus = -1;
            int agreedCount = 0;
            for (int i = 0; i < numWorkers; i++) {
                if (!responded[i]) {
                    continue;
                }
                int count = 0;
                for (int j = 0; j < numWorkers; j++) {
                    if (responded[j] && results[j] == results[i]) {
                        count++;
                    }
                }
                if (count >= majority) {
                    consensus = results[i];
                    agreedCount = count;
                    break;
                }
            }

            // In case of a tie
            if (consensus == -1) {
                int popularityCount = 0;
                boolean tied = false;
                for (int i = 0; i < numWorkers; i++) {
                    if (!responded[i]) {
                        continue;
                    }
                    int count = 0;
                    for (int j = 0; j < numWorkers; j++) {
                        if (responded[j] && results[j] == results[i]) {
                            count++;
                        }
                    }
                    if (count > popularityCount) {
                        popularityCount = count;
                        consensus = results[i];
                        agreedCount = count;
                        tied = false;
                    } else if (count == popularityCount && results[i] != consensus) {
                        tied = true;
                    }
                }

                if (tied) {
                    // Makes the vote distribution array
                    StringBuilder dist = new StringBuilder();
                    boolean[] counted = new boolean[numWorkers];
                    for (int i = 0; i < numWorkers; i++) {
                        if (!responded[i] || counted[i]) {
                            continue;
                        }
                        int count = 0;
                        for (int j = 0; j < numWorkers; j++) {
                            if (responded[j] && results[j] == results[i]) {
                                count++;
                                counted[j] = true;
                            }
                        }
                        if (dist.length() > 0) {
                            dist.append(", ");
                        }
                        dist.append(results[i]).append(" (").append(count).append(" votes)");
                    }
                    System.out.println("No consensus reached. \nVote distribution: " + dist);
                    continue;
                }
            }

            System.out.println("Consensus: " + consensus + " (" + agreedCount + "/" + numWorkers + " workers agreed)");
            System.out.println("Announcing consensus to all workers...");

            Request consensusReq = Request.newBuilder()
                .setType(Type.CONSENSUS)
                .setNum1(consensus)
                .setNum2(agreedCount)
                .setMessage(String.valueOf(numWorkers))
                .build();

            for (int i = 0; i < numWorkers; i++) {
                if (sockets[i] != null) {
                    consensusReq.writeDelimitedTo(sockets[i].getOutputStream());
                }
            }
        }

        // Close all sockets on quit
        for (int i = 0; i < numWorkers; i++) {
            if (sockets[i] != null) {
                sockets[i].close();
            }
        }

        scanner.close();
    }
}

