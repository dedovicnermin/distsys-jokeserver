package tech.nermindedovic;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/*-------------------------------------------------------------------------------
1. Nermin Dedovic | Jan. 16 2022
2. Java 11
3. > javac JokeServer.java
4. > java JokeServer
   > java JokeServer secondary
5. Running this process without command line arguments assumes user wants to
   start application with default ports for both jokeServer and adminServer
   listening socket connections.
   If two arguments are passed, process is started using secondary ports. Does not
   leverage data passed, as length of args only is important.
   File can be run standalone, does not require extra config.
 */

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


    /**
     * a.  take client UUID and retrieve from server state. If not found, start a new record for given UUID string
     * b. create the key for client response
     *      i.  get current modje
     *      ii. get position for mode
     *      iii. craft and return key using (mode, position) - expected response M:PositionForM (where M is current server mode)
     * c. increment respective position (based on mode) for the client
     * d. persist updated client state into server state
     * e. send response to client
     *
     * @param in bufferedReader
     * @param out
     * @throws IOException
     */

    private void handleJokeServerRequest(final BufferedReader in,final PrintStream out) throws IOException {
        final String clientId = in.readLine();                                      // client pipes UUID:String through
        final JokeClientPositions clientPositions = JokeServer.getServerStateMap().getOrDefault(clientId, new JokeClientPositions(clientId));  // retrieve client state if key has been seen else start new slot for this client

        final boolean currModeIsJoke = JokeServer.isJokeMode();
        final String response = createResponseUsingCurrMode(clientPositions, currModeIsJoke);      // should be in format M(mode):N(position for respective mode)
        updateServerState(clientPositions, currModeIsJoke);

        //send response + done
        System.out.println("SENDING SERVER RESPONSE=" + response + " - to client=" + clientId);
        out.println(response);
        out.flush();
    }

    // computes the response (its a key) that client will leverage once response arrives
    private String createResponseUsingCurrMode(final JokeClientPositions clientPositions, final boolean currModeIsJoke) {
        // returning string does not include updated version.
        if (currModeIsJoke) {
            return "J:" + clientPositions.getJokePosition();
        } else {
            return "P:" + clientPositions.getProverbPosition();
        }
    }

    // increment position based on curr mode and persist updated positions for client
    private void updateServerState(final JokeClientPositions clientPositions, final boolean currModeIsJoke) {
        if (currModeIsJoke) clientPositions.incrementJokePosition();
        else { clientPositions.incrementProverbPosition(); }
        JokeServer.getServerStateMap().put(clientPositions.getId(), clientPositions);
    }


}


class JokeClientPositions {
    private final String id;
    private int jokePosition = 0;
    private int proverbPosition = 0;

    public JokeClientPositions(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public int getJokePosition() {
        return jokePosition;
    }

    public void incrementJokePosition() {
        jokePosition++;
    }

    public int getProverbPosition() {
        return proverbPosition;
    }

    public void incrementProverbPosition() {
        proverbPosition++;
    }
}


public class JokeServer {

    protected static final Map<String, JokeClientPositions> SERVER_STATE = new ConcurrentHashMap<>();
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

    public static Map<String, JokeClientPositions> getServerStateMap() {
        return SERVER_STATE;
    }

    public static void setServerIsUp(boolean updated) {
        serverIsUp.set(updated);
    }



    static final int JOKE_SERVER_DEFAULT_PORT = 4545;
    static final int JOKE_SERVER_SECONDARY_PORT = 4546;

    static final int ADMIN_SERVER_DEFAULT_PORT = 5050;
    static final int ADMIN_SERVER_SECONDARY_PORT = 5051;




    public static void main(String[] args) throws IOException {
        final int concurrent_resp_limit = 6;             /* limit to number of requests that can be handled at once */
        final int jokeServerPort = (args.length >= 1) ? JOKE_SERVER_SECONDARY_PORT : JOKE_SERVER_DEFAULT_PORT;                    // 'secondary' passed? use secondary port for both jokeServer and thread handling AdminClient requests
        final int adminServerPort = (args.length >= 1) ? ADMIN_SERVER_SECONDARY_PORT : ADMIN_SERVER_DEFAULT_PORT;


        final AdminLooper adminLooper = new AdminLooper(adminServerPort);
        final Thread adminServer = new Thread(adminLooper, "userThread:adminLooper");
        adminServer.start();


        System.out.println("Nermin Dedovic'c Joke server starting up at port=" + jokeServerPort);
        System.out.println(); //EMPTY LINE

        try (final ServerSocket serverSocket = new ServerSocket(jokeServerPort, concurrent_resp_limit)) {
            while (continueServer()) {
                final Socket connectedSocket = serverSocket.accept();
                new JokeServerWorker(connectedSocket).start();
            }
        }
    }


}

/*
Listens to socket connections at admin port.
Spawns worker threads to handle request and returns to serving others
Each instance should have its own AtomicBoolean
 */
class AdminLooper implements Runnable {
    public final AtomicBoolean adminControlSwitch;      // thread safe. get/set is an atomic action
    private final int port;

    public AdminLooper(int port) {
        adminControlSwitch = new AtomicBoolean(true);
        this.port = port;
    }


    @Override
    public void run() {
        int concurrent_limit = 6; // 6 max at the same exact instance, else throw away rest


        System.out.println("AdminLooper Thread listening to incoming connections on port="+port);
        try (final ServerSocket serverSocket = new ServerSocket(this.port, concurrent_limit)){
            while (adminControlSwitch.get()) {
                final Socket connection = serverSocket.accept();
                new JokeServerAdminWorker(connection, adminControlSwitch).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


class JokeServerAdminWorker extends Thread {
    final Socket socket;
    final AtomicBoolean controlSwitch;

    public JokeServerAdminWorker(Socket socket, final AtomicBoolean controlSwitch) {
        this.socket = socket;
        this.controlSwitch = controlSwitch;
    }

    // stop admin looper thread
    public void setAdminControlSwitch(final boolean updated) {
        controlSwitch.set(updated);
    }

    @Override
    public void run() {
        try (
                final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ){
            final String s = in.readLine();         // <Enter> should == null if they want to switch mode. Else, check for shut down, allowing admin client to shut down server and this thread

            if (Objects.nonNull(s) && s.equals("shutdown")) {
                setAdminControlSwitch(false);                       // shut down instance of looper thread on next iteration
                JokeServer.setServerIsUp(false);                    // shut down server of joke server
                return;
            }
            final boolean currModeIsJokeMode = JokeServer.isJokeMode();
            JokeServer.setJokeMode(!currModeIsJokeMode);        // opposite mode
            System.out.println("MODE SWITCH. Server is in mode " + (currModeIsJokeMode ? "PROVERB" : "JOKE"));     // if we went from J->P, first stmnt else : second stmnt.
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            this.interrupt();
        }
    }
}
