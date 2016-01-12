import java.net.*;
import java.io.*;
import java.util.*;

public class BrokerServerHandlerThread extends Thread {

  private Socket socket = null;
  private ArrayList<String> stockMarket = new ArrayList<String>();

  public BrokerServerHandlerThread(Socket socket) {
    super("BrokerServerHandlerThread");
    this.socket = socket;
    System.out.println("Created new Thread to handle client");

    // Read the nasdaq quotes
    try (BufferedReader br = new BufferedReader(new FileReader(nasdaq))) {
      for (String line; (line = br.readLine()) != null; ) {
        
      }
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


        }
      }

    }

  }
}
