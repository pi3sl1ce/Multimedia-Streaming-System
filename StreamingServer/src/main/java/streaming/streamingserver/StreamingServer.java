/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package streaming.streamingserver;

import java.io.*;
import java.net.*;

/**
 *
 * @author Nevena
 */
public class StreamingServer {
    
    private static final int PORT = 5000;

    public static void main(String[] args) {
        
        System.out.println("Streaming Server starting on port " + PORT + "...");
 
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
 
            while (true) {
                
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
              
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread clientThread = new Thread(handler);
                clientThread.start();
 
            }
 
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
