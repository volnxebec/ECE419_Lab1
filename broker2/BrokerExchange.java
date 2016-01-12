import java.io.*;
import java.net.*;

public class BrokerExchange {

  public static void main(String[] args) throws IOException,
    ClassNotFoundException {

    Socket brokerSocket = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;

    try {
      String hostname = "localhost";
      int port = 4444;

      if(args.length == 2 ) {
        hostname = args[0];
        port = Integer.parseInt(args[1]);
      } else {
        System.err.println("ERROR: Invalid arguments!");
        System.exit(-1);
      }
      brokerSocket = new Socket(hostname, port);

      out = new ObjectOutputStream(brokerSocket.getOutputStream());
      in = new ObjectInputStream(brokerSocket.getInputStream());
    }
    catch (UnknownHostException e) {
      System.err.println("ERROR: Don't know where to connect!!");
      System.exit(1);
    }
    catch (IOException e) {
      System.err.println("ERROR: Couldn't get I/O for the connection.");
      System.exit(1);
    }

    BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
    String userInput;

    System.out.print("CONSOLE>");
    while ((userInput = stdIn.readLine()) != null
      && userInput.toLowerCase().indexOf("x") == -1) {
      
      /* make a new request packet */
      BrokerPacket packetToServer = new BrokerPacket();
      
      String[] userInputList = userInput.split(" ");

      if (userInputList[0].equals("add")) {
        packetToServer.type = BrokerPacket.EXCHANGE_ADD;
      }
      else if (userInputList[0].equals("update")) {
        packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
        packetToServer.quote = Long.parseLong(userInputList[2]);
      }
      else if (userInputList[0].equals("remove")) {
        packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;
      }

      packetToServer.symbol = userInputList[1];
      out.writeObject(packetToServer);

      /* print server reply */
      BrokerPacket packetFromServer;
      packetFromServer = (BrokerPacket) in.readObject();

      if (packetFromServer.type == BrokerPacket.EXCHANGE_REPLY) {
        System.out.println(packetFromServer.exchange);       
      }
      else if (packetFromServer.type == BrokerPacket.ERROR_INVALID_SYMBOL) {
        System.out.println(userInputList[1]+" invalid.");
      }
      else if (packetFromServer.type == BrokerPacket.ERROR_OUT_OF_RANGE) {
        System.out.println(userInputList[1]+" out of range.");
      }
      else if (packetFromServer.type == BrokerPacket.ERROR_SYMBOL_EXISTS) {
        System.out.println(userInputList[1]+" exists.");
      }

      /* re-print console prompt */
      System.out.print("CONSOLE>");
    }

    /* tell server that i'm quitting */
    BrokerPacket packetToServer = new BrokerPacket();
    packetToServer.type = BrokerPacket.BROKER_BYE;
    out.writeObject(packetToServer);

    out.close();
    in.close();
    stdIn.close();
    brokerSocket.close();
  }
}
































