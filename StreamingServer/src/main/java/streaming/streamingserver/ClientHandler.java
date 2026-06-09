/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package streaming.streamingserver;

import java.io.*;
import java.net.*;

/**
 *
 * @author Nevena
 */
public class ClientHandler implements Runnable{
 
    private final Socket socket;
 
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }
 
   @Override
    public void run() {
 
        String clientAddr = socket.getRemoteSocketAddress().toString();
 
        try (
            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
 
            out.println("Welcome to the Streaming Server!");
 
            String request;
 
            while ((request = in.readLine()) != null) {
 
                String[] parts   = request.trim().split("\\s+");
                String   command = parts[0].toUpperCase();
 
                switch (command) {
 
                    case "EXIT":
                        out.println("BYE");
                        System.out.println("Client " + clientAddr + " disconnected.");
                        return;
 
                    case "HELLO":
                        if (parts.length != 3) {
                            out.println("ERROR Usage: HELLO <speedMbps> <format>");
                            break;
                        }
                        double speed  = Double.parseDouble(parts[1]);
                        String format = parts[2];
                        System.out.println("Client " + clientAddr
                                + " speed=" + speed + " Mbps, format=" + format);
 
                        // fake files for starter
                        out.println("LIST");
                        out.println("Forrest_Gump-480p." + format);
                        out.println("Forrest_Gump-360p." + format);
                        out.println("Forrest_Gump-240p." + format);
                        out.println("END");
                        break;
 
                    case "SELECT":
                        if (parts.length < 3) {
                            out.println("ERROR Usage: SELECT <filename> <protocol>");
                            break;
                        }
                        String filename = parts[1];
                        String protocol = parts[2];
                        System.out.println("Client selected: " + filename + " via " + protocol);
 
                        out.println("STREAM 6000");
                        System.out.println("(Stage 1: no actual stream, just acknowledged)");
 
                        out.println("DONE");
                        break;
 
                    default:
                        out.println("ERROR Unknown command: " + command);
                }
            }
 
        } catch (IOException e) {
            System.err.println("Error with client " + clientAddr + ": " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
