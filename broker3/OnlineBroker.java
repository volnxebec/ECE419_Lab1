import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class OnlineBroker {
  public static void main(String[] args) throws IOException {
    ServerSocket serverSocket = null;
    ConcurrentHashMap<String, Long> hm = new ConcurrentHashMap<String, Long>();
    boolean listening = true;

    try {
      if (args.length == 1) {
        serverSocket = new ServerSocket(Integer.parseInt(args[0]));
      }
      else {
        System.err.println("ERROR: Invalid arguments!");
        System.exit(-1);
      }
    } 
    catch (IOException e) {
      System.err.println("ERROR: Could not listen on port!");
      System.exit(-1);
    }

    try {
      BufferedReader br = new BufferedReader(new FileReader("nasdaq"));
      for (String line; (line = br.readLine()) != null; ) {
        String[] tempLine = line.split(" ");
        hm.put(tempLine[0], Long.parseLong(tempLine[1]));
      }
    }
    catch (IOException e) {
      System.err.println("ERROR: Could not open nasdaq quotes!");
      System.exit(-1);
    }
      
    while (listening) {
      new BrokerServerHandlerThread(serverSocket.accept(), hm).start();
    }

    System.err.println("End of an Era");

    serverSocket.close();

    //Write new changes to file...
    FileWriter fw = new FileWriter("nasdaq");
    for (ConcurrentHashMap.Entry<String, Long> entry : hm.entrySet()) {
      fw.write(entry.getKey() + " " + entry.getValue());
    }
    fw.close();
  }
}
