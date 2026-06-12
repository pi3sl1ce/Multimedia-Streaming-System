/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package streaming.streamingserver;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author Nevena
 */
public class StreamingServer {
    
    private static final int PORT = 5000;
    private static final String VIDEOS_DIR = "videos";
    private static final String FFMPEG_BIN = "ffmpeg"; 
    
    static final Logger logger = Logger.getLogger(StreamingServer.class.getName());

    public static void main(String[] args) {
        
        setupLogger();
        
        System.out.println("Streaming Server starting on port " + PORT + "...");
        
        FFmpegManager ffmpeg = new FFmpegManager(VIDEOS_DIR, FFMPEG_BIN);
 
        logger.info("Scanning videos/ folder and generating missing files...");
        List<VideoFile> videoFiles = ffmpeg.scanAndGenerate();
 
        if (videoFiles.isEmpty()) {
            logger.warning("No video files found in videos/ folder!");
            logger.warning("Add at least one file, px videos/Forrest_Gump-720p.mkv");
            logger.warning("Server will still start but clients will receive an empty list.");
        } else {
            logger.info("Video library ready - " + videoFiles.size() + " files:");
            for (VideoFile vf : videoFiles) {
                logger.info("  " + vf.getDisplayName());
            }
        }
 
        logger.info("Waiting for clients...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            
             registerWithLoadBalancer(PORT);
 
            while (true) {
                
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
              
                ClientHandler handler = new ClientHandler(clientSocket, videoFiles, ffmpeg);
                Thread clientThread = new Thread(handler);
                clientThread.start();
 
            }
 
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
    
    private static void setupLogger() {
        try {
            FileHandler fh = new FileHandler("server.log", true); // append=true
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
        } catch (IOException e) {
            System.err.println("Could not create server.log: " + e.getMessage());
        }
    }

    private static void registerWithLoadBalancer(int port) {
        try (
            Socket lb = new Socket("localhost", 7070);
            PrintWriter lbOut = new PrintWriter(new OutputStreamWriter(lb.getOutputStream()), true);
            BufferedReader lbIn = new BufferedReader(new InputStreamReader(lb.getInputStream()))
        ) {
            lbOut.println("REGISTER " + port);
            String response = lbIn.readLine();
            logger.info("Load Balancer response: " + response);
        } catch (IOException e) {
            logger.warning("Could not reach Load Balancer (not running?): " + e.getMessage());
            logger.warning("Server will accept direct connections on port " + port);
        }
    }
}