package tech.nermindedovic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

class Worker extends Thread {
    final Socket socket;
    Worker (Socket s) {
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
            handleRemoteAddressRequest(in, out);
            // close socket, thread completed its work and streamed its response.
            // worker can no longer put socket to use.
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    /**
     * This method will block until connected client sends an address for
     * server to query. Upon receiving an address, query it and respond to client
     * with results.
     *
     * @param in buffer responsible for handling data from incoming request
     * @param out entity responsible for formatting / streaming response
     */
    static void handleRemoteAddressRequest(final BufferedReader in, final PrintStream out) {
        try {
            final String address = in.readLine();           // will throw if issues with socket
            System.out.println("Looking up " + address);
            printRemoteAddress(address, out);
        }
        catch (IOException x) {
            System.out.println("Server read error");
            x.printStackTrace ();
        }
    }

    static void printRemoteAddress (final String address, final PrintStream out) {
        try {
            out.println("Looking up " + address + "...");
            // use library to query address
            InetAddress machine = InetAddress.getByName (address);
            out.println("Host name : " + machine.getHostName ());
            out.println("Host IP : " + toText (machine.getAddress ()));
        } catch(UnknownHostException e) {
            out.println ("Failed in attempt to look up " + address);
        }
    }


    // deserialize address to String
    static String toText (final byte[] ip) {
        final StringBuffer result = new StringBuffer ();
        for (int i = 0; i < ip.length; ++ i) {
            if (i > 0) result.append (".");
            result.append (0xff & ip[i]);
        }
        return result.toString ();
    }
}

public class InetServer {
    public static void main(String[] a) throws IOException {
        final int max_buffered_conn = 6;             /* limit to number of requests that can be handled at once */
        final int port = 1565;

        // allow discovery of server so that clients can connect & communicate.
        // close resource once we force server to shut down and not have dangling process
        try (final ServerSocket serverSocket = new ServerSocket(port, max_buffered_conn)) {
            System.out.println("Nermin Dedovic's Inet server 0.0.1 starting up, listening at port 1565.\n");
            while (true) {
                /* block and listen for clients attempting to connect to socket. Once a connection arrives,
                   accept and create socket to be handeled/closed by worker thread */

                final Socket sock = serverSocket.accept();
                new Worker(sock).start(); // spawn worker thread to handle request
            }
        }
    }
}
