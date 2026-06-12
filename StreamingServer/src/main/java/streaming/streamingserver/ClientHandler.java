/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package streaming.streamingserver;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 *
 * @author Nevena
 */
public class ClientHandler implements Runnable{
 
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private static final AtomicInteger nextPort = new AtomicInteger(6000);

    private final Socket socket;
    private final List<VideoFile> allFiles;  // full list from server (read-only)
    private final FFmpegManager   ffmpeg;
    
    public ClientHandler(Socket socket, List<VideoFile> allFiles, FFmpegManager ffmpeg) {
        this.socket = socket;
        this.allFiles = allFiles;
        this.ffmpeg = ffmpeg;
    }
 
   @Override
    public void run() {
 
        String clientAddr = socket.getRemoteSocketAddress().toString();
        logger.info("Handling client: " + clientAddr);
 
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
 
            out.println("Welcome to the Streaming Server!");
 
            String request;
 
            while ((request = in.readLine()) != null) {
 
                String[] parts = request.trim().split("\\s+");
                String command = parts[0].toUpperCase();
 
                switch (command) {
 
                    case "EXIT":
                        out.println("BYE");
                        logger.info("Client " + clientAddr + " disconnected.");
                        System.out.println("Client " + clientAddr + " disconnected.");
                        return;
 
                    case "HELLO":
                        if (parts.length != 3) {
                            out.println("ERROR Usage: HELLO <speedMbps> <format>");
                            break;
                        }
                        
                        double speed;
                        try {
                            speed = Double.parseDouble(parts[1]);
                        } catch (NumberFormatException e) {
                            out.println("ERROR speedMbps must be a number");
                            break;
                        }
 
                        // If client sends ".mkv" instead of "mkv" replace
                        String format = parts[2].toLowerCase().replaceFirst("^\\.", "");
 
                        System.out.println("Client " + clientAddr 
                                + " speed=" + speed + " Mbps, format=" + format);
                        logger.info("Client " + clientAddr
                                + " | speed=" + speed + " Mbps | format=" + format);
 
                        // Find the highest resolution this BW supports
                        String maxRes = maxResolutionForSpeed(speed);
                        int maxIdx = ffmpeg.getResolutionIndex(maxRes);
                        logger.info("Max resolution for " + speed + " Mbps = " + maxRes);
 
                        // Filter: matching format && resolution <= maxRes
                        List<VideoFile> filtered = new ArrayList<VideoFile>();
                        for (int i = 0; i < allFiles.size(); i++) {
                            VideoFile vf = allFiles.get(i);

                            // 1st check: format must match what the client asked for
                            boolean formatMatches = vf.getFormat().equalsIgnoreCase(format);

                            // 2nd check: resolution must be <= what the bandwidth supports
                            int resIdx = ffmpeg.getResolutionIndex(vf.getResolution());
                            boolean resolutionFits = resIdx <= maxIdx;

                            if (formatMatches && resolutionFits) {
                                filtered.add(vf);
                            }
                        }

                        logger.info("Sending " + filtered.size() + " files to " + clientAddr);
 
                        // Send LIST blah blah END  
                        out.println("LIST");
                        for (int i = 0; i < filtered.size(); i++) {
                            out.println(filtered.get(i).getDisplayName());
                        }
                        out.println("END");
                        break;
 
                         case "SELECT":
                            if (parts.length < 3) {
                                out.println("ERROR Usage: SELECT <filename> <protocol>");
                                 break;
                            }
                        
                            String filename = parts[1];
                            String protocol = parts[2].toUpperCase(); // For tcp, udp, rtp/udp, auto
                            System.out.println("Client selected: " + filename + " via " + protocol);
                            logger.info("Client " + clientAddr + " selected: " + filename + " via " + protocol);
 
                            VideoFile chosen = null;
                            for (int i = 0; i < allFiles.size(); i++) {
                            VideoFile vf = allFiles.get(i);
                            if (vf.getDisplayName().equals(filename)) {
                                chosen = vf;
                                break; // if it finds it doesn't need to look more
                            }
                        }
 
                        if (chosen == null) {
                            out.println("ERROR File not found: " + filename);
                            logger.warning("File not found: " + filename);
                            break;
                        }
 
                        // Resolve AUTO protocol based on resolution
                        if (protocol.equals("AUTO")) {
                            protocol = autoProtocol(chosen.getResolution());
                            logger.info("AUTO resolved to " + protocol + " for " + chosen.getResolution());
                        }
 
                        // Give this stream a unique port
                        int streamPort = nextPort.getAndIncrement();
 
                        logger.info("Streaming " + filename + " | protocol=" + protocol
                                    + " | port=" + streamPort);
 
                        /* Tell client which port to listen on before starting ffmpeg
                           Client launches ffplay after receiving this line */
                        out.println("STREAM " + streamPort + " " + protocol);
                        String ack = in.readLine();

                        if (!"READY".equals(ack))
                            logger.warning("Expected READY from client, got: " + ack);

                        Thread.sleep(1500); // give ffplay time to bind before ffmpeg starts sending

                        long streamStart = System.currentTimeMillis();

                        try {
                            Process streamProc = ffmpeg.startStreaming(chosen.getFilePath(), protocol, streamPort);
                            streamProc.waitFor(); // blocks until stream ends
                        } catch (Exception e) {
                            logger.warning("Streaming error: " + e.getMessage());
                            out.println("ERROR Streaming failed");
                            break;
                        }
 
                        // Log streaming stats 
                        long durationSec = (System.currentTimeMillis() - streamStart) / 1000;
                        logger.info("********** STREAM FINISHED **********");
                        logger.info("File:     " + filename);
                        logger.info("Protocol: " + protocol);
                        logger.info("Duration: " + durationSec + " seconds");
                        logger.info("Client:   " + clientAddr);
                        logger.info("*************************************");
 
                        out.println("DONE");
                        break;
 
                    default:
                        out.println("ERROR Unknown command: " + command);
                        logger.warning("Unknown command from " + clientAddr
                                       + ": " + command);
                }
            }
 
        } catch (IOException | InterruptedException e) {
            System.err.println("Error with client " + clientAddr + ": " + e.getMessage());
            logger.warning("IO error with " + clientAddr + ": " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            logger.info("Connection closed: " + clientAddr);
        }
    }
 
    
    /*** Helper functions ***/
 
    // Returns the highest resolution whose required bitrate is smaller or equal speedMbps.
    private String maxResolutionForSpeed(double speedMbps) {
        String best = "240p";
        for (int i = 0; i < FFmpegManager.RESOLUTIONS.length; i++) {
    String res = FFmpegManager.RESOLUTIONS[i];
    if (ffmpeg.getBitrate(res) <= speedMbps) {
                best = res;
            }
        }
        return best;
    }
 
    // Based on the resolutioln it autoselects a streaming protocol
    private String autoProtocol(String resolution) {
        switch (resolution) {
            case "720p":
            case "1080p":  return "RTP/UDP";
            case "360p":
            case "480p":  return "UDP";
            case "240p":
            default:    return "TCP";
        }
    }
}