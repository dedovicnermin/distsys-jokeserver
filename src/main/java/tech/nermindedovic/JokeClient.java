package tech.nermindedovic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.*;

import static java.util.Objects.nonNull;

/*-------------------------------------------------------------------------------
1. Nermin Dedovic | Jan. 16 2022
2. Java 11
3. > javac JokeClient.java
4. > java JokeClient
   > java JokeClient secondary
5. Running this process without command line arguments assumes user wants to
   start application with default ports for both jokeServer and adminServer
   listening socket connections.
   If two arguments are passed, process is started using secondary ports. Does not
   leverage data passed, as length of args only is important.
   File can be run standalone, does not require extra config.
 */
class ClientWorker implements Runnable {

    private static final String DEFAULT_RESPONSE_FORMAT = "%s %s : %s";
    private static final String SECONDARY_RESPONSE_FORMAT = "<S2> %s %s : %s";


    private final String clientId;
    private final String username;
    private final String serverName;
    private final int serverPort;

    public ClientWorker(final String username, final String clientId, final String serverName, final int serverPort) {
        this.clientId = clientId;
        this.username = username;
        this.serverName = serverName;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try (final Socket socket = new Socket(serverName, serverPort);
            final BufferedReader fromJokeServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final PrintStream toJokeServer = new PrintStream(socket.getOutputStream()))
        {
            toJokeServer.println(clientId);         // server needs UUID to identify clients respectively
            toJokeServer.flush();

            final String data = fromJokeServer.readLine();          // wait until joke/proverb arrives
            final String response = handleResponse(new ServerResponseHandler(data), serverPort);        // turns JN | PN (where N can be any number >= 0) into J[A-D] | P[A-D]
            System.out.println(response);
        } catch (IOException e) {
            System.out.println("CATCH JOKE CLIENT WORKER");
            e.printStackTrace();
        }

    }


    private String handleResponse(final ServerResponseHandler handler, final int serverPort) {
        final String key = handler.convertToClientSideKey();                // key will look like JX | PX where X is [A-D]
        if (handler.isCurrModeJoke()) {                                     // key is for joke map
            if (serverPort == JokeClient.PRIMARY_SERVER_PORT) return String.format(DEFAULT_RESPONSE_FORMAT, key, username, JokeClient.getJokeMap().get(key));
            else return String.format(SECONDARY_RESPONSE_FORMAT, key, username, JokeClient.getJokeMap().get(key));
        }
        if (serverPort == JokeClient.PRIMARY_SERVER_PORT) return String.format(DEFAULT_RESPONSE_FORMAT, key, username, JokeClient.getProverbMap().get(key));      // key is for proverb map
        return String.format(SECONDARY_RESPONSE_FORMAT, key, username, JokeClient.getProverbMap().get(key));
    }

}

/**
 * The component responsible for taking server data and enriching it to format
 * JokeClient can leverage / query
 */
class ServerResponseHandler {
    private final String mode;
    private final boolean isCurrModeJoke;
    private final int serverPosition;

    public ServerResponseHandler(String response) {
        final String[] keySplit = response.split(":");
        this.mode = keySplit[0];
        this.isCurrModeJoke = mode.equals("J");
        this.serverPosition = Integer.parseInt(keySplit[1]);
    }

    public boolean isCurrModeJoke() {
        return isCurrModeJoke;
    }

    // enriched so that P0 -> PA, J1 -> JB
    // JUNIT5 tests
    public String convertToClientSideKey() {
        final int clientSidePosition = getClientAppropriatePosition();
        return mode + (char) (clientSidePosition + 'A');
    }


    /**
     * server is keeping track of how many times a joke/proverb has been requested by client. Position can be
     * outside the version of joke/proverb data on client side, so ensure retrieving joke/proverb by key will be
     * within bounds.
     *
     * Will shuffle joke/proverbs depending on position given
     *
     * @return position (within range of joke/proverb map)
     */
    private int getClientAppropriatePosition() {
        int clientPosition;
        if (isCurrModeJoke) {
            clientPosition = serverPosition % JokeClient.getJokeMap().size();
            if (serverPosition != 0 && clientPosition == 0) {
                final Map<String, String> shuffledMap = JokeClient.shuffleMap(JokeClient.getJokeMap());
                JokeClient.setJokeMap(shuffledMap);
            }
        } else {
            clientPosition = serverPosition % JokeClient.getProverbMap().size();
            if (serverPosition != 0 && clientPosition == 0) {
                final Map<String, String> shuffledMap = JokeClient.shuffleMap(JokeClient.getProverbMap());
                JokeClient.setProverbMap(shuffledMap);
            }
        }
        return clientPosition;
    }

}

public class JokeClient {


    // each client will have a copy of the jokes and proverbs locally.
    // so long as the server is formatting keys correctly (ie. J[A-Z] & P[A-Z] we should be good to handle issues client-side (ie default message on unmatched) until
    // client side has matching data to tango with servers keys

    static Map<String, String> jokeMap = createJokeMap();
    static Map<String, String> proverbMap = createProverbMap();

    static Map<String, String> getJokeMap() {
        return jokeMap;
    }

    static Map<String, String> getProverbMap() {
        return proverbMap;
    }

    static void setJokeMap(Map<String, String> jokeMap) {
        JokeClient.jokeMap = jokeMap;
    }

    static void setProverbMap(Map<String, String> proverbMap) {
        JokeClient.proverbMap = proverbMap;
    }

    static Map<String, String> shuffleMap(final Map<String, String> oldMap) {
        final ArrayList<String> keys = new ArrayList<>(oldMap.keySet());
        final ArrayList<String> values = new ArrayList<>(oldMap.values());
        Collections.shuffle(keys);
        Collections.shuffle(values);

        Map<String, String> shuffledMap = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            shuffledMap.put(keys.get(i), values.get(i));
        }
        return shuffledMap;
    }


    static final int PRIMARY_SERVER_PORT = 4545;            // JokeClient default port
    static final int SECONDARY_SERVER_PORT = 4546;          // JokeClient secondary port if any command line arguments are present on startup


    public static void main (String[] args) {

        final String serverName = "localhost";
        final int port = (args.length >= 1) ? SECONDARY_SERVER_PORT : PRIMARY_SERVER_PORT;

        System.out.println("Running Nermin Dedovic's JokeClient!");
        System.out.println("Using server: " + serverName + ", Port: " + port);

        final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        boolean continueFlag = true;
        try {
            System.out.print("Enter a username:");
            System.out.flush ();
            String username = in.readLine ();
            final String uniqueClientID = UUID.randomUUID().toString();
            System.out.println("READY TO GO (OR ENTER quit) " + username);
            while (continueFlag) {
                final String potentialQuit = in.readLine();                     // to invoke a response : entering anything besides quit
                System.out.println(potentialQuit);
                if (
                        nonNull(potentialQuit) && potentialQuit.equals("quit")  // client wants to exit loop
                ) {
                    continueFlag = false;
                    continue;   // don't complete block of logic, back to condition check
                }
                new ClientWorker(username, uniqueClientID, serverName, port).run();     // give username passed and UUID generated to handle req's
            }
        } catch (IOException e) {
            System.out.println("Error from JokeClient main thread = " + e.getMessage());
        }
    }


    /**
     * Clients each start with this copy, potentially being shuffled in the future
     * Server will be sending back single key belonging to jokes or proverbs.
     * Client code retrieves key selected by server code tracking client positions.
     * @return Map
     */
    public static Map<String, String> createJokeMap() {
        final Map<String, String> jokeMap = new HashMap<>();
        jokeMap.put("JA", "What do you call a pig that does karate? ... A pork chop!");
        jokeMap.put("JB", "Why did the scarecrow win an award? ... Because he was outstanding in his field!");
        jokeMap.put("JC", "Atheism is a non-prophet organization!");
        jokeMap.put("JD", "Why do you never see elephants hiding in trees? ... Because they're so good at it!");
        return jokeMap;
    }

    /**
     * Server will be sending back single key belonging to proverbs or jokes.
     * Client code retrieves key selected by server code tracking client positions.
     * @return Map
     */
    public static Map<String, String> createProverbMap() {
        final Map<String, String> proverbMap = new HashMap<>();
        proverbMap.put("PA", "In a closed mouth, flies do not enter.");
        proverbMap.put("PB", "A father is a banker provided by nature");
        proverbMap.put("PC", "A turtle travels only when it sticks its neck out. ");
        proverbMap.put("PD", "Wealth is like hair in the nose: it hurts to be separated whether from a little or from a lot.");
        return proverbMap;
    }

}
