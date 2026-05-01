package example.grpcclient;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import service.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.protobuf.Empty; // needed to use Empty


/**
 * Client that requests `parrot` method from the `EchoServer`.
 */
public class Client {
  private final EchoGrpc.EchoBlockingStub blockingStub;
  private final JokeGrpc.JokeBlockingStub blockingStub2;
  private final RegistryGrpc.RegistryBlockingStub blockingStub3;
  private final RegistryGrpc.RegistryBlockingStub blockingStub4;
  private final ConverterGrpc.ConverterBlockingStub blockingStub5;
  private final LibraryGrpc.LibraryBlockingStub blockingStub6;
  private final PokemonBattleGrpc.PokemonBattleBlockingStub blockingStub7;

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel, Channel regChannel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = RegistryGrpc.newBlockingStub(regChannel);
    blockingStub4 = RegistryGrpc.newBlockingStub(channel);
    blockingStub5 = ConverterGrpc.newBlockingStub(channel);
    blockingStub6 = LibraryGrpc.newBlockingStub(channel);
    blockingStub7 = PokemonBattleGrpc.newBlockingStub(channel);
  }

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = null;
    blockingStub4 = null;
    blockingStub5 = ConverterGrpc.newBlockingStub(channel);
    blockingStub6 = LibraryGrpc.newBlockingStub(channel);
    blockingStub7 = PokemonBattleGrpc.newBlockingStub(channel);
  }

  public void askServerToParrot(String message) {

    ClientRequest request = ClientRequest.newBuilder().setMessage(message).build();
    ServerResponse response;
    try {
      response = blockingStub.parrot(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    System.out.println("Received from server: " + response.getMessage());
  }

  public void askForJokes(int num) {
    JokeReq request = JokeReq.newBuilder().setNumber(num).build();
    JokeRes response;

    // just to show how to use the empty in the protobuf protocol
    Empty empt = Empty.newBuilder().build();

    try {
      response = blockingStub2.getJoke(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
    System.out.println("Your jokes: ");
    for (String joke : response.getJokeList()) {
      System.out.println("--- " + joke);
    }
  }

  public void askForConvert(double value, String fromUnit, String toUnit) {
    ConversionRequest request = ConversionRequest.newBuilder()
        .setValue(value)
        .setFromUnit(fromUnit)
        .setToUnit(toUnit)
        .build();
    ConversionResponse response;
    try {
      response = blockingStub5.convert(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
    if (response.getIsSuccess()) {
      System.out.println("Result: " + response.getResult());
    } else {
      System.out.println("Error: " + response.getError());
    }
  }

  public void setJoke(String joke) {
    JokeSetReq request = JokeSetReq.newBuilder().setJoke(joke).build();
    JokeSetRes response;

    try {
      response = blockingStub2.setJoke(request);
      System.out.println(response.getOk());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }


  private void printBooks(java.util.List<service.Book> bookList) {
    int i = 1;
    for (service.Book b : bookList) {
      System.out.println("  " + i + ". " + b.getTitle() + " by " + b.getAuthor() + " (ISBN: " + b.getIsbn() + ")");
      if (b.getIsBorrowed()) {
        System.out.println("     Borrowed by: " + b.getBorrowedBy());
      } else {
        System.out.println("     Status: Available");
      }
      i++;
    }
  }

  public void listBooks() {
    com.google.protobuf.Empty request = com.google.protobuf.Empty.newBuilder().build();
    BookListResponse response;
    try {
      response = blockingStub6.listBooks(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    if (response.getIsSuccess()) {
      System.out.println("\nBook List");
      System.out.println("========================");
      printBooks(response.getBooksList());
    } else {
      System.out.println("Error: " + response.getError());
    }
  }

  public void searchBooks(String query) {
    BookSearchRequest request = BookSearchRequest.newBuilder().setQuery(query).build();
    BookListResponse response;
    try {
      response = blockingStub6.searchBooks(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    if (response.getIsSuccess()) {
      System.out.println("\nSearch Results");
        System.out.println("========================");
      printBooks(response.getBooksList());
    } else {
      System.out.println("Error: " + response.getError());
    }
  }

  public void borrowBook(String isbn, String borrowerName) {
    BorrowRequest request = BorrowRequest.newBuilder().setIsbn(isbn).setBorrowerName(borrowerName).build();
    BorrowResponse response;
    try {
      response = blockingStub6.borrowBook(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    if (response.getIsSuccess()) {
      System.out.println(response.getMessage());
    } else {
      System.out.println("Error: " + response.getError());
    }
  }

  public void returnBook(String isbn) {
    ReturnRequest request = ReturnRequest.newBuilder().setIsbn(isbn).build();
    ReturnResponse response;
    try {
      response = blockingStub6.returnBook(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    if (response.getIsSuccess()) {
      System.out.println(response.getMessage());
    } else {
      System.out.println("Error: " + response.getError());
    }
  }

  public void getNodeServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub4.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub3.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServer(String name) {
    FindServerReq request = FindServerReq.newBuilder().setServiceName(name).build();
    SingleServerRes response;
    try {
      response = blockingStub3.findServer(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServers(String name) {
    FindServersReq request = FindServersReq.newBuilder().setServiceName(name).build();
    ServerListRes response;
    try {
      response = blockingStub3.findServers(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void listPokemon() {
    com.google.protobuf.Empty request = com.google.protobuf.Empty.newBuilder().build();
    service.PokemonListResponse response;
    try {
      response = blockingStub7.listPokemon(request);
    } catch (Exception e) {
      System.err.println("Server not responding: " + e.getMessage());
      return;
    }
    if (response.getIsSuccess()) {
      System.out.println("\nPokemon List");
      System.out.println("========================");
      int i = 1;
      for (service.Pokemon p : response.getPokemonsList()) {
        System.out.println("  " + i + ". " + p.getName() + " [" + p.getType() + "]");
        i++;
      }
    } else {
      System.out.println("Error: " + response.getError());
    }
  }

  public void createPokemon(String name, String typeStr) {
    service.PokemonType type;
    try {
      type = service.PokemonType.valueOf(typeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      System.out.println("Invalid type '" + typeStr + "'. Must be FIRE, WATER, or GRASS.");
      return;
    }
    if (type == service.PokemonType.UNKNOWN) {
      System.out.println("Type must be FIRE, WATER, or GRASS.");
      return;
    }
    CreatePokemonRequest request = CreatePokemonRequest.newBuilder()
        .setName(name).setType(type).build();
    PokemonResponse response;
    try {
      response = blockingStub7.createPokemon(request);
    } catch (Exception e) {
      System.err.println("Server not responding: " + e.getMessage());
      return;
    }
    if (response.getIsSuccess()) {
      service.Pokemon p = response.getPokemon();
      System.out.println("\nCreated Pokemon: " + p.getName() + " [" + p.getType() + "] (ID: " + p.getId() + ")");
    } else {
      System.out.println("Error: " + response.getError());
    }
  }

  public void battlePokemon(String name1, String name2) {
    BattlePokemonRequest request = BattlePokemonRequest.newBuilder()
        .setPokemon1Name(name1).setPokemon2Name(name2).build();
    BattleResult response;
    try {
      response = blockingStub7.battlePokemon(request);
    } catch (Exception e) {
      System.err.println("Server not responding: " + e.getMessage());
      return;
    }
    if (response.getIsSuccess()) {
      service.Pokemon p1 = response.getPokemon1();
      service.Pokemon p2 = response.getPokemon2();
      System.out.println("\nBattle Result");
      System.out.println("========================");
      System.out.println("  " + p1.getName() + " [" + p1.getType() + "]  vs  " + p2.getName() + " [" + p2.getType() + "]");
      System.out.println("  Winner: " + response.getWinnerName());
      System.out.println("  " + response.getResult());
    } else {
      System.out.println("Error: " + response.getError());
    }
  }

  public void getBattleHistory(int limit) {
    HistoryRequest request = HistoryRequest.newBuilder().setLimit(limit).build();
    BattleHistoryResponse response;
    try {
      response = blockingStub7.getBattleHistory(request);
    } catch (Exception e) {
      System.err.println("Server not responding: " + e.getMessage());
      return;
    }
    if (response.getIsSuccess()) {
      System.out.println("\nBattle History");
      System.out.println("========================");
      //
      int i = 1;
      for (BattleResult b : response.getBattlesList()) {
        System.out.println("  " + i + ". " + b.getPokemon1().getName() + " [" + b.getPokemon1().getType()
                + "]" + "  vs  " + b.getPokemon2().getName() + " [" + b.getPokemon2().getType() + "]"
                + "  ||  Winner: " + b.getWinnerName());
        i++;
      }
    } else {
      System.out.println("Error: " + response.getError());
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 6) {
      System.out
          .println("Expected arguments: <host(String)> <port(int)> <regHost(string)> <regPort(int)> <message(String)> <regOn(bool)>");
      System.exit(1);
    }
    int port = 9099;
    int regPort = 9003;
    String host = args[0];
    String regHost = args[2];
    String message = args[4];
    try {
      port = Integer.parseInt(args[1]);
      regPort = Integer.parseInt(args[3]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port] must be an integer");
      System.exit(2);
    }

    // Create a communication channel to the server (Node), known as a Channel. Channels
    // are thread-safe
    // and reusable. It is common to create channels at the beginning of your
    // application and reuse
    // them until the application shuts down.
    String target = host + ":" + port;
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS
        // to avoid
        // needing certificates.
        .usePlaintext().build();

    String regTarget = regHost + ":" + regPort;
    ManagedChannel regChannel = ManagedChannelBuilder.forTarget(regTarget).usePlaintext().build();
    try {

      // ##############################################################################
      // ## Assume we know the port here from the service node it is basically set through Gradle
      // here.
      // In your version you should first contact the registry to check which services
      // are available and what the port
      // etc is.

      /**
       * Your client should start off with
       * 1. contacting the Registry to check for the available services
       * 2. List the services in the terminal and the client can
       *    choose one (preferably through numbering)
       * 3. Based on what the client chooses
       *    the terminal should ask for input, eg. a new sentence, a sorting array or
       *    whatever the request needs
       * 4. The request should be sent to one of the
       *    available services (client should call the registry again and ask for a
       *    Server providing the chosen service) should send the request to this service and
       *    return the response in a good way to the client
       *
       * You should make sure your client does not crash in case the service node
       * crashes or went offline.
       */

      // Just doing some hard coded calls to the service node without using the
      // registry
      // create client
      Client client = new Client(channel, regChannel);
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

      boolean running = true;
      while (running) {
        System.out.println("\nAvailable Services");
          System.out.println("=============================");
        System.out.println("1. Converter - convert between units");
        System.out.println("2. Library - list all books");
        System.out.println("3. Library - search books by title or author");
        System.out.println("4. Library - borrow a book");
        System.out.println("5. Library - return a book");
        System.out.println("6. Pokemon - create a pokemon");
        System.out.println("7. Pokemon - battle two pokemon");
        System.out.println("8. Pokemon - view battle history");
        System.out.println("9. Pokemon - list all pokemon");
        System.out.println("0. Exit");
        System.out.println("Choose a service: ");

        String choice = reader.readLine();
        if (choice == null) {
          break;
        }
        choice = choice.trim();

        if (choice.equals("1")) {
          System.out.println("Supported units:");
          System.out.println("  Length: KILOMETER, MILE, YARD, FOOT");
          System.out.println("  Weight: KILOGRAM, POUND");
          System.out.println("  Temperature: CELSIUS, FAHRENHEIT");
          System.out.println("Enter value: ");
          String valStr = reader.readLine();
          System.out.println("Enter from unit: ");
          String fromUnit = reader.readLine();
          System.out.println("Enter to unit: ");
          String toUnit = reader.readLine();
          double convertVal = 0;
          try {
            if (valStr != null) {
              convertVal = Double.parseDouble(valStr.trim());
            }
          } catch (NumberFormatException e) {
            System.out.println("Invalid number, using 0.");
          }
          if (fromUnit == null) {
            fromUnit = "";
          }
          if (toUnit == null) {
            toUnit = "";
          }
          client.askForConvert(convertVal, fromUnit.trim(), toUnit.trim());

        } else if (choice.equals("2")) {
          client.listBooks();

        } else if (choice.equals("3")) {
          System.out.println("Enter search query (title or author): ");
          String query = reader.readLine();
          if (query == null) {
            query = "";
          }
          client.searchBooks(query.trim());

        } else if (choice.equals("4")) {
          System.out.println("Enter ISBN: ");
          String isbn = reader.readLine();
          System.out.println("Enter your name: ");
          String borrowerName = reader.readLine();
          if (isbn == null) {
            isbn = "";
          }
          if (borrowerName == null) {
            borrowerName = "";
          }
          client.borrowBook(isbn.trim(), borrowerName.trim());

        } else if (choice.equals("5")) {
          System.out.println("Enter ISBN: ");
          String returnIsbn = reader.readLine();
          if (returnIsbn == null) {
            returnIsbn = "";
          }
          client.returnBook(returnIsbn.trim());

        } else if (choice.equals("6")) {
          System.out.println("Enter Pokemon name: ");
          String pname = reader.readLine();
          System.out.println("Enter type (FIRE, WATER, GRASS): ");
          String ptype = reader.readLine();
          if (pname == null){
              pname = "";
          }
          if (ptype == null){
              ptype = "";
          }
          client.createPokemon(pname.trim(), ptype.trim());

        } else if (choice.equals("7")) {
          client.listPokemon();
          System.out.println("Enter name of first Pokemon: ");
          String bname1 = reader.readLine();
          System.out.println("Enter name of second Pokemon: ");
          String bname2 = reader.readLine();
          if (bname1 == null){
              bname1 = "";
          }
          if (bname2 == null){
              bname2 = "";
          }
          bname1 = bname1.trim();
          bname2 = bname2.trim();
          if (bname1.equalsIgnoreCase(bname2)) {
            System.out.println("A Pokemon cannot battle itself. Choose two different Pokemon.");
          } else {
            client.battlePokemon(bname1, bname2);
          }

        } else if (choice.equals("8")) {
          System.out.println("How many recent battles to show? (0 for all): ");
          String limitStr = reader.readLine();
          int limit = 0;
          try {
            if (limitStr != null) {
              limit = Integer.parseInt(limitStr.trim());
            }
            if (limit < 0) limit = 0;
          } catch (NumberFormatException e) {
            System.out.println("Invalid number, showing all.");
          }
          client.getBattleHistory(limit);

        } else if (choice.equals("9")) {
          client.listPokemon();

        } else if (choice.equals("0")) {
          running = false;

        } else {
          System.out.println("\nUnknown option, please try again.");
        }
      }

      // list all the services that are implemented on the node that this client is connected to

      System.out.println("Services on the connected node. (without registry)");
      client.getNodeServices(); // get all registered services

      // ############### Contacting the registry just so you see how it can be done

      if (args[5].equals("true")) {
        // Comment these last Service calls while in Activity 1 Task 1, they are not needed and wil throw issues without the Registry running
        // get thread's services
        client.getServices(); // get all registered services

        // get parrot
        client.findServer("services.Echo/parrot"); // get ONE server that provides the parrot service

        // get all setJoke
        client.findServers("services.Joke/setJoke"); // get ALL servers that provide the setJoke service

        // get getConversion
        client.findServer("services.Converter/getConversion"); // get ALL servers that provide Converter service

        // get getJoke
        client.findServer("services.Joke/getJoke"); // get ALL servers that provide the getJoke service

        // does not exist
        client.findServer("random"); // shows the output if the server does not find a given service
      }

    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent
      // leaking these
      // resources the channel should be shut down when it will no longer be used. If
      // it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      if (args[5].equals("true")) {
        regChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      }
    }
  }
}
