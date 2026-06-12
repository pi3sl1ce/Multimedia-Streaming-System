/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package streaming.streamingloadbalancer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Nevena
 */
public class StreamingLoadBalancer {

     private static final int LB_PORT = 7070;
    private static final Logger logger = Logger.getLogger(StreamingLoadBalancer.class.getName());
 
    // Registered server ports, protected by synchronized methods
    private final List<Integer> servers = new ArrayList<>();
    private int nextIndex = 0;
 
    /*** Main ***/
    public static void main(String[] args) {
        new StreamingLoadBalancer().run();
    }
 
    private void run() {
        logger.info("Load Balancer starting on port " + LB_PORT);
        try (ServerSocket ss = new ServerSocket(LB_PORT)) {
            while (true) {
                Socket conn = ss.accept();

                Thread thread = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            handleConnection(conn);
                        }
                    }, "lb-conn"
                );
                thread.start();
            }
        } catch (IOException e) {
            logger.severe("Load Balancer error: " + e.getMessage());
        }
    }
 
    private void handleConnection(Socket conn) {
        try (
            BufferedReader in  = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            PrintWriter    out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream()), true)
        ) {
            String line = in.readLine();
            if (line == null) return;
 
            if (line.startsWith("REGISTER ")) {
                // A server is announcing itself
                int port = Integer.parseInt(line.split("\\s+")[1]);
                registerServer(port);
                out.println("OK Registered port " + port);
                logger.info("Server registered on port " + port
                            + " | pool size: " + servers.size());
 
            } else if (line.equals("ASSIGN")) {
                // A client wants to know which server to connect to
                int port = assignServer();
                if (port < 0) {
                    out.println("ERROR No servers available");
                    logger.warning("Client requested assignment but no servers are registered");
                } else {
                    out.println("PORT " + port);
                    logger.info("Assigned client to server port " + port);
                }
 
            } else {
                out.println("ERROR Unknown command: " + line);
            }
 
        } catch (IOException | NumberFormatException e) {
            logger.warning("LB connection error: " + e.getMessage());
        } finally {
            try { conn.close(); } catch (IOException ignored) {}
        }
    }
 
    // Pool management (synchronized for thread safety) 
 
    private synchronized void registerServer(int port) {
        if (!servers.contains(port)) {
            servers.add(port);
        }
    }
 
    // Round-robin returns the next server port (-1 if none registered)
    private synchronized int assignServer() {
        if (servers.isEmpty()) return -1;
        int port = servers.get(nextIndex % servers.size());
        nextIndex = (nextIndex + 1) % servers.size();
        return port;
    }
}
 