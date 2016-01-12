import java.net.*;
import java.io.*;
import java.util.*;

public class BrokerServerHandlerThread extends Thread {

  private Socket socket = null;
  private HashMap<String, Integer> hm = new HashMap<String, Integer>();

  public BrokerServerHandlerThread(Socket socket) {
    super("BrokerServerHandlerThread");
    this.socket = socket;
    System.out.println("Created new Thread to handle client");

    // Read the nasdaq quotes
    try {
      BufferedReader br = new BufferedReader(new FileReader("nasdaq"));
      for (String line; (line = br.readLine()) != null; ) {
        String[] tempLine = line.split(" ");
        hm.put(tempLine[0], Integer.parseInt(tempLine[1]));
      }
    }
    catch (IOException e) {
      System.err.println("ERROR: Could not open nasdaq quotes!");
      System.exit(-1);
    }
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
          packetToClient.quote = (long) hm.get(packetFromClient.symbol);
          System.out.println("From Client: " + packetFromClient.symbol);

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
        System.err.println("ERROR: Unknown ECHO_* packet!!");
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
