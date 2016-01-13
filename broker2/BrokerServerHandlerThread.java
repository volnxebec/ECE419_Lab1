import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class BrokerServerHandlerThread extends Thread {

  private Socket socket = null;
  //private volatile ConcurrentHashMap<String, Integer> hm = new ConcurrentHashMap<String, Integer>();
  private ConcurrentHashMap<String, Long> hm;

  public BrokerServerHandlerThread(Socket socket, ConcurrentHashMap<String, Long> hm) {
    super("BrokerServerHandlerThread");
    this.socket = socket;
    this.hm = hm;
    System.out.println("Created new Thread to handle client");
  }

  public void run() {
    boolean gotXPacket = false;

    try {
      /* stream to read from client */
      ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
      BrokerPacket packetFromClient;

      /* stream to write back to client */
      ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

      while (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
        /* create a packet to send reply back to client */
        BrokerPacket packetToClient = new BrokerPacket();
        packetToClient.type = BrokerPacket.BROKER_QUOTE;

        /* process message */
        if (packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
          try {
            long symbolExist = hm.get(packetFromClient.symbol);
            packetToClient.quote = symbolExist;
          }
          catch (NullPointerException e) {
            packetToClient.quote = (long) 0;
          }
          System.out.println("From Client: " + packetFromClient.symbol);
          toClient.writeObject(packetToClient);
          continue;
        }

        //Exchange messages
        if (packetFromClient.type == BrokerPacket.EXCHANGE_ADD) {
          //Check if the symbol is already there
          try {
            long symbolExist = hm.get(packetFromClient.symbol);
            packetToClient = new BrokerPacket();
            packetToClient.type = BrokerPacket.ERROR_SYMBOL_EXISTS;
          }
          catch (NullPointerException e) {
            //Added to hashMap...
            hm.put(packetFromClient.symbol, (long) 0);
            packetToClient = new BrokerPacket();
            packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
            packetToClient.exchange = "added.";
          }
          System.out.println("From Exchange: Add " + packetFromClient.symbol);
          toClient.writeObject(packetToClient);
          continue;
        }
        else if (packetFromClient.type == BrokerPacket.EXCHANGE_UPDATE) {
          try {
            long symbolExist = hm.get(packetFromClient.symbol);

            if (packetFromClient.quote > 300 || packetFromClient.quote < 0) {
              packetToClient = new BrokerPacket();
              packetToClient.type = BrokerPacket.ERROR_OUT_OF_RANGE;
            }
            else {
              hm.put(packetFromClient.symbol, packetFromClient.quote);
              packetToClient = new BrokerPacket();
              packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
              packetToClient.exchange = "updated to "+packetFromClient.quote+".";
            }
          }
          catch (NullPointerException e) {
            packetToClient = new BrokerPacket();
            packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
          }
          System.out.println("From Exchange: Update "+packetFromClient.symbol+" to "+packetFromClient.quote);
          toClient.writeObject(packetToClient);
          continue;
        }
        else if (packetFromClient.type == BrokerPacket.EXCHANGE_REMOVE) {
          try {
            long symbolExist = hm.get(packetFromClient.symbol);

            hm.remove(packetFromClient.symbol);
            packetToClient = new BrokerPacket();
            packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
            packetToClient.exchange = "removed.";
          }
          catch (NullPointerException e) {
            packetToClient = new BrokerPacket();
            packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
          }
          System.out.println("From Exchange: Remove " + packetFromClient.symbol);
          toClient.writeObject(packetToClient);
          continue;
        }

        //Bye and error processing...
        if (packetFromClient.type == BrokerPacket.BROKER_NULL ||
            packetFromClient.type == BrokerPacket.BROKER_BYE) {
          gotXPacket = true;
          packetToClient = new BrokerPacket();
          packetToClient.type = BrokerPacket.BROKER_BYE;
          toClient.writeObject(packetToClient); 
          break;
        }

        /* if code comes here, there is an error in the packet */
        System.err.println("ERROR: Unknown BROKER_* packet!!");
        System.exit(-1);
      }

      /* cleanup when client exits */
      fromClient.close();
      toClient.close();
      socket.close();
    }
    catch (IOException e) {
      if(!gotXPacket)
        e.printStackTrace();
    }
    catch (ClassNotFoundException e) {
      if(!gotXPacket)
        e.printStackTrace();
    }
  }
}
