package tech.nermindedovic.distsysjokeserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;


public class JokeClient {

    public static void main (String[] args) {
        /* determine if process was invoked with a server name to run on or default to localhost */
        final String serverName = (args.length < 1) ? "localhost" : args[0];
        System.out.println("Nermin Dedovic's Inet Client, 1.8.\n");
        System.out.println("Using server: " + serverName + ", Port: 1565");

        // create stream to read input coming through CLI
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            String name;
            do {
                System.out.print("Enter a username or (quit) to end: ");
                System.out.flush ();            // flush stream of characters to System.out
                name = in.readLine ();          // block until invoker sends an endpoint over from CLI

                if (!name.contains("quit")) getRemoteAddress(name, serverName);
            } while (!name.contains("quit"));
            System.out.println ("Cancelled by user request.");
        } catch (IOException x) {
            x.printStackTrace ();
        }
    }

    static void getRemoteAddress (String name, String serverName){
        BufferedReader fromServer = null;
        PrintStream toServer = null;
        String textFromServer;


        /* Open a connection to server. Ensure port is the one that Server is currently running on
           and close connection when execution block complete or problems occur with socket resource */
        try (final Socket sock = new Socket(serverName, 1565)){
            System.out.println(sock.toString());
            // instantiate to capture and hold data sent from server till ready to be accessed
            fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            // instantiate to capture data we want to send to server and stream over once ready to be sent
            toServer = new PrintStream(sock.getOutputStream());

            // send passed endpoint to server:
            toServer.println(name);
            toServer.flush();

            // read server response which contains 2-3 line summary of endpoint depending on if successful
            for (int i = 1; i <= 3; i++){
                textFromServer = fromServer.readLine(); // block until server responds or socket has been closed from server (worker thread) side
                if (textFromServer != null) System.out.println(textFromServer);
            }
        } catch (IOException x) {
            System.out.println ("Socket error.");
            x.printStackTrace ();
        }
    }
}



