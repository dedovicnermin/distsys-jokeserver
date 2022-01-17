package tech.nermindedovic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import static java.util.Objects.nonNull;


/*-------------------------------------------------------------------------------
1. Nermin Dedovic | Jan. 16 2022
2. Java 11
3. > javac JokeClientAdmin.java
4. > java JokeClientAdmin
   > java JokeClientAdmin secondary
5. Running this process without command line arguments assumes user wants to
   start application with default ports for both jokeServer and adminServer
   listening socket connections.
   If two arguments are passed, process is started using secondary ports. Does not
   leverage data passed, as length of args only is important.
   File can be run standalone, does not require extra config.
 */
public class JokeClientAdmin {
    static final String DEFAULT_SERVER = "localhost";
    static final int    DEFAULT_PORT   = 5050;
    static final int    SECONDARY_PORT = 5051;

    public static void main(String[] args) throws IOException {
        final String serverName = DEFAULT_SERVER;
        final int port = (args.length >= 1) ? SECONDARY_PORT : DEFAULT_PORT;

        System.out.println("Running Nermin Dedovic's JokerClientAdmin");
        System.out.println("Using server: " + serverName + ", Port: " + port);
        System.out.println();               //empty line separator
        try (BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("JokeClientAdminWorker connected. Safe to press <Enter> or shutdown.");      // shutdown not implemented
            while (true) {
                final String s = systemIn.readLine(); // wait for user to press enter
                if (nonNull(s) && s.equals("shutdown")) {
                    break;
                }
                new AdminWorker(serverName, port).run();            // connect to socket with port set at startup
            }
        }
    }
}

class AdminWorker implements Runnable {

    private final String server;
    private final int port;

    public AdminWorker(String server, int port) {
        this.server = server;
        this.port = port;
    }

    @Override
    public void run() {
        try (
                final Socket socket = new Socket(server, port);
                final PrintStream toJokeServer = new PrintStream(socket.getOutputStream())      // reading from Server not necessary
        ) {
            System.out.println("ClientAdminWorker connected! Sending ping to change mode.");
            toJokeServer.println();     // PING
            toJokeServer.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
