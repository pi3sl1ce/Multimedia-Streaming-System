/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package streaming.streamingclient;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Nevena
 */
public class StatsLogger {
 
    private static final String LOG_FILE = "client_stats.log";
    private static final String CSV_HEADER = "timestamp,filename,protocol,speed_mbps,duration_sec";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
 

    /*** Session state (set at stream start, written at stream end) ***/
 
    private String filename;
    private String protocol;
    private double speedMbps;
    private long startMs  = -1;   // -1 means streamStarted() was never called
 
    
    /*** API 
    
    /* Call this just before SELECT is sent to the server
       Records the context that will be written when the stream finishes */
    public void streamStarted(String filename, String protocol, double speedMbps) {
        this.filename = filename;
        this.protocol = protocol;
        this.speedMbps = speedMbps;
        this.startMs = System.currentTimeMillis();
    }
 
    /* Call this after the server sends DONE (or an error)
     Appends one line to client_stats.log
     Does nothing if streamStarted() was never called */
    public void streamFinished() {
        if (startMs < 0) {
            // streamStarted() was never called so nothing to log
            return;
        }
        long durationSec = (System.currentTimeMillis() - startMs) / 1000;
        String timestamp = SDF.format(new Date());
        String line = timestamp + "," + filename + "," + protocol
                      + "," + speedMbps + "," + durationSec;
        appendToFile(line);
        startMs = -1;   // reset so a second accidental call is also a no-op
    }
 
    
    /*** Helperts ***/
 
    private void appendToFile(String line) {
        File f = new File(LOG_FILE);
        boolean needsHeader = !f.exists() || f.length() == 0;
        try (PrintWriter pw = new PrintWriter(new FileWriter(f, true))) {
            if (needsHeader) pw.println(CSV_HEADER);
            pw.println(line);
        } catch (IOException e) {
            System.err.println("[StatsLogger] Could not write to " + LOG_FILE
                               + ": " + e.getMessage());
        }
    }
}