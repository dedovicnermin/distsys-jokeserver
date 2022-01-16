package tech.nermindedovic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.nonNull;


class ClientWorker implements Runnable {

    private static final String RESPONSE_FORMAT = "%s %s : %s";

    final String clientId;
    final String username;
    final String serverName;
    final int serverPort;

    public ClientWorker(final String username, final String clientId, final String serverName, final int serverPort) {
        System.out.println("CLIENT WORKER BEGIN");
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
                toJokeServer.println(clientId);
                toJokeServer.flush();

                final String response = fromJokeServer.readLine();          // wait until joke/proverb arrives
                System.out.println(response);
                System.out.println(formatServerResponse(response));

        } catch (IOException e) {
            System.out.println("CATCH JOKE CLIENT WORKER");
            e.printStackTrace();
        }

    }

    private String formatServerResponse(final String responseKey) {
        final boolean isJokeKey = responseKey.contains("J");
        String value;
        if (isJokeKey) {
            value = JokeClient.JOKE_MAP.getOrDefault(responseKey, "Could not find key=" + responseKey);
        } else {
            // must be a proverb key ?
            value = JokeClient.PROVERB_MAP.getOrDefault(responseKey, "Could not find key=" + responseKey);
        }
        return String.format(RESPONSE_FORMAT, responseKey, username, value);
    }
}

public class JokeClient {

    // sonar lint plugin intellij suggested I use protected. Has something to do with concurrentHashMap decicion?
    // each client will have a copy of the jokes and proverbs locally.
    // so long as the server is formatting keys correctly (ie. J[A-Z] & P[A-Z] we should be good to handle issues client-side (ie default message on unmatched) until
    // client side has matching data to tango with servers keys

    protected static final Map<String, String> JOKE_MAP = createFormattedJokeMap();
    protected static final Map<String, String> PROVERB_MAP = createFormattedProverbMap();

    public static void main (String[] args) {
        final String serverName = (args.length < 1) ? "localhost" : args[0];
        final int primaryServerPort = 4545;

        System.out.println("Nermin Dedovic's Joker Client, 1.8.\n");
        System.out.println("Using server: " + serverName + ", Port: " + primaryServerPort);

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
                        nonNull(potentialQuit) && potentialQuit.equals("quit")
                ) {
                    continueFlag = false;
                    continue;
                }
                new ClientWorker(username, uniqueClientID, serverName, primaryServerPort).run();
            }
        } catch (IOException x) {
            System.out.println("CATCH JOKE CLIENT");
            x.printStackTrace ();
        }
    }


    /**
     * Server will be sending back single key belonging to jokes or proverbs.
     * Client code retrieves key selected by server code tracking client positions.
     * @return Map
     */
    public static Map<String, String> createFormattedJokeMap() {
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
    public static Map<String, String> createFormattedProverbMap() {
        final Map<String, String> proverbMap = new HashMap<>();
        proverbMap.put("PA", "In a closed mouth, flies do not enter.");
        proverbMap.put("PB", "A father is a banker provided by nature");
        proverbMap.put("PC", "A turtle travels only when it sticks its neck out. ");
        proverbMap.put("PD", "Wealth is like hair in the nose: it hurts to be separated whether from a little or from a lot.");
        return proverbMap;
    }

}




