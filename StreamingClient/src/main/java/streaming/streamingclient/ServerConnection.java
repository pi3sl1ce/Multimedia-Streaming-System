/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package streaming.streamingclient;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Nevena
 */
public class ServerConnection {
 
    private static final Logger logger = Logger.getLogger(ServerConnection.class.getName());
 
    private static final String HOST = "localhost";
    private static final int SERVER_PORT = 5000;
    private static final int LB_PORT = 7070;
 
    private static final String SPEED_TEST_URL = "http://speedtest.tele2.net/10MB.zip";
 
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
 
    
    /*** Result type for SELECT ***/
 
    // Carries the port and the protocol the server resolved (important for AUTO)
    public static class StreamInfo {
        public final int port;
        public final String protocol;   // "TCP", "UDP", or "RTP/UDP"
 
        StreamInfo(int port, String protocol) {
            this.port = port;
            this.protocol = protocol;
        }
    }
 
    /*** Connect ***/
 
    /* Asks the Load Balancer for a server port, then opens the TCP connection
       Falls back to SERVER_PORT if the LB is not running
       Returns the welcome message sent by the server */
    public String connect() throws IOException {
        int targetPort = resolveServerPort();
        socket = new Socket(HOST, targetPort);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return in.readLine();   // "Welcome to the Streaming Server!"
    }
 
    // Returns true if the socket is open and connected
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
 
    
    /*** Speed test ***/
 
    /* Runs a 5-second download speed test using the JSpeedTest library
      Polls every 500 ms; throws IOException if it times out after 15 s */
    public double runSpeedTest() throws IOException, InterruptedException {
        SpeedTest st = new SpeedTest();
        logger.info("Starting speed test against: " + SPEED_TEST_URL);
        st.testSpeed(SPEED_TEST_URL);
 
        int waited = 0;
        while (st.getPercent() < 100) {
            Thread.sleep(500);
            waited += 500;
            if (waited > 15000) {
                throw new IOException("Speed test timed out after 15 seconds");
            }
        }
 
        double mbps = Math.round(st.getSpeed() * 100.0) / 100.0;
        logger.info("Speed test result: " + mbps + " Mbps");
        return mbps;
    }
 
    
    /*** HELLO to file list ***/
 
    /* Sends "HELLO <speedMbps> <format>" to the server
       Reads the LIST ... END response and returns the filenames as a List */
    public List<String> sendHello(double speedMbps, String format) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to server");
        }
        out.println("HELLO " + speedMbps + " " + format);
 
        String first = in.readLine();
        if (first == null || !first.equals("LIST")) {
            throw new IOException("Unexpected response to HELLO: " + first);
        }
 
        List<String> files = new ArrayList<String>();
        String line;
        while ((line = in.readLine()) != null) {
            if ("END".equals(line)) break;
            files.add(line);
        }
        return files;
    }
 
    
    /*** SELECT to stream info ───────────────────────────────────────────────── ***/
    /* Sends "SELECT <filename> <protocol>" to the server
       The server replies "STREAM <port> <resolvedProtocol>"
       Returns a StreamInfo with the port and the actual protocol to use for ffplay */
    public StreamInfo sendSelect(String filename, String protocol) throws IOException {
        if (!isConnected()) {
            throw new IOException("Not connected to server");
        }
        out.println("SELECT " + filename + " " + protocol);
 
        String response = in.readLine();    // "STREAM <port> <protocol>"
        if (response == null || !response.startsWith("STREAM ")) {
            throw new IOException("Unexpected SELECT response: " + response);
        }
 
        String[] parts = response.split("\\s+");
        int port = Integer.parseInt(parts[1]);
        String resolvedProto = (parts.length >= 3) ? parts[2] : "TCP";
        return new StreamInfo(port, resolvedProto);
    }
 
    /* Sends READY to the server, signalling that ffplay has been launched
       and the server can start pushing the stream */
    public void sendReady() {
        out.println("READY");
    }
 
    /* Blocks until the server sends "DONE" (stream finished)
       Must be called from a background thread */
    public void waitForDone() throws IOException {
        in.readLine();   // "DONE"
    }
 
    
    /*** ffplay/ffmpeg launchers ***/
 
    /* Launches ffplay to receive and display the stream
       protocol must be the resolved value ("TCP", "UDP", or "RTP/UDP"), not "AUTO" */
    public void launchFfplay(String protocol, int port) throws IOException {
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add("ffplay");
        cmd.add("-loglevel");
        cmd.add("warning");
        addStreamInput(cmd, protocol, port);
        new ProcessBuilder(cmd).inheritIO().start();
    }
 
    /* Launches ffplay for viewing && a parallel ffmpeg process that savea
       the stream to outputFile (px "MovieName-720p_recorded.mp4") */
    public void launchFfplayWithRecording(String protocol, int port, String outputFile) 
            throws IOException {
                launchFfplay(protocol, port);
 
                ArrayList<String> cmd = new ArrayList<String>();
                cmd.add("ffmpeg");
                cmd.add("-y");
                cmd.add("-loglevel");
                cmd.add("warning");
                addStreamInput(cmd, protocol, port);
                cmd.add("-c");
                cmd.add("copy");   // no re-encoding, just save the raw stream
                cmd.add(outputFile);
 
                new ProcessBuilder(cmd).inheritIO().start();
            }
 
    
    /*** Disconnect ***/
 
    // Sends EXIT and closes the socket
    public void disconnect() {
        try {
            if (out != null) out.println("EXIT");
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
        }
    }
 
    /*** Helpers ***/
 
    /* Adds the correct -i <url> arguments to a command list based on protocol
       Used by launchFfplay and launchFfplayWithRecording to avoid duplicating 
       the protocol to URL switch */
    private void addStreamInput(ArrayList<String> cmd, String protocol, int port) {
        switch (protocol.toUpperCase()) {
            case "UDP":
                cmd.add("-i");
                cmd.add("udp://127.0.0.1:" + port);
                break;
            case "RTP/UDP":
                cmd.add("-i");
                cmd.add("rtp://127.0.0.1:" + port);
                break;
            case "TCP":
            default:
                cmd.add("-i");
                cmd.add("tcp://127.0.0.1:" + port);
                break;
        }
    }
 
    /* Asks the Load Balancer on LB_PORT for a server port assignment
       Returns the assigned port, or SERVER_PORT as a fallback if the LB
       is not running or returns an error */
    private int resolveServerPort() {
        try (
            Socket lb = new Socket(HOST, LB_PORT);
            PrintWriter lbOut = new PrintWriter(new OutputStreamWriter(lb.getOutputStream()), true);
            BufferedReader lbIn  = new BufferedReader(new InputStreamReader(lb.getInputStream()))
        ) {
            lbOut.println("ASSIGN");
            String response = lbIn.readLine();   // "PORT 5000" or "ERROR ..."
            if (response != null && response.startsWith("PORT ")) {
                int port = Integer.parseInt(response.split("\\s+")[1]);
                logger.info("Load Balancer assigned server port: " + port);
                return port;
            }
            logger.warning("Load Balancer returned unexpected response: " + response);
        } catch (IOException e) {
            logger.warning("Load Balancer not available, connecting directly to port "
                           + SERVER_PORT + ": " + e.getMessage());
        }
        return SERVER_PORT;
    }
}