package tech.nermindedovic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import static java.util.Objects.nonNull;

public class JokeClientAdmin {
    static final String DEFAULT_SERVER = "localhost";
    static final int    DEFAULT_PORT   = 5050;

    public static void main(String[] args) throws IOException {
        final String serverName = (args.length < 1) ? DEFAULT_SERVER : args[0];

        System.out.println("Nermin Dedovic's Joker Client Admin, 1.0 \n");
        System.out.println("Using server: " + serverName + ", Port: " + DEFAULT_PORT);
        try (BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("JokeClientAdminWorker connected. Safe to press <Enter> or shutdown* (do quit instead)");      // shutdown not implemented
            while (true) {
                final String s = systemIn.readLine(); // wait for user to press enter
                if (nonNull(s) && s.equals("quit")) {
                    break;
                }
                new AdminWorker(serverName, DEFAULT_PORT).run();
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
                final PrintStream toJokeServer = new PrintStream(socket.getOutputStream())
        ) {
            System.out.println("ClientAdminWorker connected! Sending ping to change mode...");
            toJokeServer.println();
            toJokeServer.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
