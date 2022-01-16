package tech.nermindedovic;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static java.util.Optional.ofNullable;


class JokeServerWorker extends Thread {
    final Socket socket;
    JokeServerWorker (Socket s) {
        this.socket = s;
    }

    @Override
    public void run() {
        // Get I/O streams in/out from the socket to achieve bi-directional comm.
        // flush/close created resources once complete
        try (
                final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final PrintStream out = new PrintStream(socket.getOutputStream())
        ) {
            handleJokeServerRequest(in, out);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleJokeServerRequest(final BufferedReader in,final PrintStream out) throws IOException {
        final String clientId = in.readLine();                                      // client pipes UUID:String through
        final boolean isNewClient = !JokeServer.CLIENT_POSITIONS.containsKey(clientId); // this was introduced after seeing  0 % 4 == 4. Needs to be distinction.
        final Map<String, Integer> clientPositions = JokeServer.CLIENT_POSITIONS.getOrDefault(clientId, defaultMap());
        final String response = retrieveKeyForClientResponse(clientPositions, isNewClient);      // JX | PX - where X is [A|B|C|D]

        JokeServer.CLIENT_POSITIONS.put(clientId, clientPositions);     // update server state. updated after retrieval.

        //send response + done
        System.out.println("SENDING SERVER RESPONSE=" + response + " - to client=" + clientId);
        out.println(response);
        out.flush();
    }

    // in order to get an outcome of --> JX || PX (where X is A|B|C|D)
    // we must get current mode --> J | P
    // and the position for client in current mode --> 0-N
    // We can send client the response needed for them to query --> JX | PX (Key) : Joke | Proverb (Value)
    private static String retrieveKeyForClientResponse(final Map<String, Integer> clientPositions, final boolean newClient) {
        final String modeKey = retrieveKeyBasedOnMode();
        final Integer index = retrieveIndexBasedOnPositionValue(clientPositions.get(modeKey), newClient);
        updateClientPosition(clientPositions, modeKey, index+1);
        return modeKey + JokeServer.INDEX_TO_LETTER.get(index);
    }

    // randomizing the key for joke / proverb retrieve after seeing all jokes is (imo) more cost-effective than shuffling collection
    private static Integer retrieveIndexBasedOnPositionValue(final int index, final boolean newClient) {
        if (!newClient && index % JokeServer.INDEX_TO_LETTER.size() == 0) {
            System.out.println("TIME TO RANDOMIZE");
            return JokeServer.getRandomIndex();
        }
        return index;
    }


    private static void updateClientPosition(final Map<String, Integer> clientPositions, final String key, final int updatedIndex) {
        clientPositions.put(key, updatedIndex);
    }


    private static String retrieveKeyBasedOnMode() {
        return JokeServer.isJokeMode() ? "J" : "P";
    }

    private static HashMap<String, Integer> defaultMap() {
        final HashMap<String, Integer> defaultMap = new HashMap<>();
        defaultMap.put("J", 0);
        defaultMap.put("P", 0);
        return defaultMap;
    }
}



public class JokeServer {

    protected static final Map<String, Map<String, Integer>> CLIENT_POSITIONS = new ConcurrentHashMap<>();
    protected static final List<String> INDEX_TO_LETTER = List.of("A", "B", "C", "D");
    private static final Random RANDOM = new Random();
    protected static AtomicBoolean jokeMode = new AtomicBoolean(true);
    protected static AtomicBoolean serverIsUp = new AtomicBoolean(true);

    public static boolean continueServer() {
        return serverIsUp.get();
    }

    // best practice
    public static boolean isJokeMode() {
        return jokeMode.get();
    }

    // best practice
    public static void setJokeMode(boolean updated) {
        jokeMode.set(updated);
    }

    public static Integer getRandomIndex() {
        return RANDOM.nextInt(INDEX_TO_LETTER.size());
    }



    public static void main(String[] a) throws IOException {
        final int concurrent_resp_limit = 6;             /* limit to number of requests that can be handled at once */
        final int port = 4545;

        final AdminLooper adminLooper = new AdminLooper();
        final Thread adminServer = new Thread(adminLooper, "userThread:adminLooper");
        adminServer.start();


        System.out.println("Nermin Dedovic'c Joke server starting up at port 4545.");
        try (final ServerSocket serverSocket = new ServerSocket(port, concurrent_resp_limit)) {
            while (continueServer()) {
                final Socket connectedSocket = serverSocket.accept();
                new JokeServerWorker(connectedSocket).start();
            }
        }
    }


}



// TODO : implement server side listener that will flip the mode switch
class AdminLooper implements Runnable {
    public static boolean adminControlSwitch = true;


    @Override
    public void run() {
        System.out.println("In the admin looper thread"); //not me
        int q_len = 6;
        int port = 5050;


        try (final ServerSocket serverSocket = new ServerSocket(port, q_len)){

            while (adminControlSwitch) {
                final Socket connection = serverSocket.accept();
                new JokeServerAdminWorker(connection).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


class JokeServerAdminWorker extends Thread {
    final Socket socket;

    public JokeServerAdminWorker(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final PrintWriter out = new PrintWriter(socket.getOutputStream())
        ){
            final String s = in.readLine();         // <Enter> should == null. Have not implemented shutdown functionality yet
            final boolean currentMode = JokeServer.isJokeMode();
            JokeServer.setJokeMode(!currentMode);        // opposite mode
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
