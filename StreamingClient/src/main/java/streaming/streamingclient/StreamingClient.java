/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package streaming.streamingclient;

import java.io.*;
import java.net.*;

/**
 *
 * @author Nevena
 */
public class StreamingClient {

    private static final String HOST = "localhost";
    private static final int    PORT = 5000;
    
    public static void main(String[] args) {

        try (
            Socket socket = new Socket(HOST, PORT);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader stdIn  = new BufferedReader(new InputStreamReader(System.in))
            ) {
 
            System.out.println("Connected to server.");
            System.out.println(in.readLine()); 
 
            String userInput;
 
            while ((userInput = stdIn.readLine()) != null) {
 
                out.println(userInput);
 
                String command = userInput.trim().split("\\s+")[0].toUpperCase();
 
                if ("HELLO".equalsIgnoreCase(command)) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                        if ("END".equals(line)) break;
                    }
              
                } else {
                    System.out.println(in.readLine());
 
                    if ("SELECT".equalsIgnoreCase(command)) {
                        System.out.println(in.readLine()); 
                    }
                }
 
                if ("EXIT".equalsIgnoreCase(command)) break;
            }
 
        } catch (IOException e) {
            System.err.println("Cannot connect to server: " + e.getMessage());
        }
    }
}
