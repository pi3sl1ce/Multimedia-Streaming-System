/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package streaming.streamingclient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;


/**
 *
 * @author Nevena
 */
public class StreamingClient {
 
    private final Dashboard gui;
    private final ServerConnection server;
    private final StatsLogger stats;
 
    // Written on speedtest-thread, read on fetch/stream-threads
    private volatile double lastSpeedMbps = 10.0;  // default so all resolutions show without a test
 
    public StreamingClient() {
        gui = new Dashboard();
        server = new ServerConnection();
        stats = new StatsLogger();
        wireButtons();
    }
 
    
    /*** Buttons from dashboard ***/
 
    private void wireButtons() {
 
        gui.connectBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });
 
        gui.speedTestBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runSpeedTest();
            }
        });
 
        gui.fetchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fetchFileList();
            }
        });
 
        gui.streamBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startStream();
            }
        });
    }
 
    /*** Actions funcs ***/
 
    private void connectToServer() {
        gui.connectBtn.setEnabled(false);
        gui.log("Connecting (via Load Balancer if available)...");
        new Thread(new ConnectTask()).start();
    }
 
    private void runSpeedTest() {
        gui.speedTestBtn.setEnabled(false);
        gui.setSpeedText("Speed: measuring...");
        gui.log("Running 5-second speed test...");
        new Thread(new SpeedTestTask()).start();
    }
 
    private void fetchFileList() {
        gui.fetchBtn.setEnabled(false);
        gui.log("Fetching file list (format=" + gui.getSelectedFormat()
                + ", speed=" + lastSpeedMbps + " Mbps)...");
        new Thread(new FetchTask()).start();
    }
 
    private void startStream() {
        String filename = gui.getSelectedFile();
        if (filename == null) {
            gui.log("Please select a file from the list first.");
            return;
        }
        gui.streamBtn.setEnabled(false);
        gui.log("Requesting stream: " + filename
                + " via " + gui.getSelectedProtocol()
                + (gui.isRecordingEnabled() ? " [recording ON]" : "") + "...");
        new Thread(new StreamTask(filename)).start();
    }
 
    
    /*** Daemon tasks ***/
 
    private class ConnectTask implements Runnable {
        @Override
        public void run() {
            try {
                String welcome = server.connect();
                gui.log("Connected - " + welcome);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        gui.speedTestBtn.setEnabled(true);
                        gui.fetchBtn.setEnabled(true);
                    }
                });
            } catch (Exception ex) {
                gui.log("ERROR: " + ex.getMessage());
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        gui.connectBtn.setEnabled(true);
                    }
                });
            }
        }
    }
 
    private class SpeedTestTask implements Runnable {
        @Override
        public void run() {
            try {
                double mbps   = server.runSpeedTest();
                lastSpeedMbps = mbps;
                gui.setSpeedText("Speed: " + mbps + " Mbps");
                gui.log("Speed test done: " + mbps + " Mbps");
            } catch (Exception ex) {
                gui.log("ERROR: Speed test failed - " + ex.getMessage());
                gui.setSpeedText("Speed: error");
            } finally {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        gui.speedTestBtn.setEnabled(true);
                    }
                });
            }
        }
    }
 
    private class FetchTask implements Runnable {
        @Override
        public void run() {
            try {
                String format = gui.getSelectedFormat();
                List<String> files = server.sendHello(lastSpeedMbps, format);
                gui.setFileList(files);
                gui.log("Server returned " + files.size() + " file(s).");
            } catch (Exception ex) {
                gui.log("ERROR: " + ex.getMessage());
            } finally {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        gui.fetchBtn.setEnabled(true);
                    }
                });
            }
        }
    }
 
    private class StreamTask implements Runnable {
        private final String filename;
 
        StreamTask(String filename) {
            this.filename = filename;
        }
 
        @Override
        public void run() {
            try {
                String protocol = gui.getSelectedProtocol();
                boolean recording = gui.isRecordingEnabled();
 
                ServerConnection.StreamInfo info = server.sendSelect(filename, protocol);
                gui.log("Streaming on port " + info.port + " using " + info.protocol + ".");
 
                stats.streamStarted(filename, info.protocol, lastSpeedMbps);
 
                if (recording) {
                    String outFile = buildRecordingFilename(filename);
                    server.launchFfplayWithRecording(info.protocol, info.port, outFile);
                    gui.log("Recording to: " + outFile);
                } else 
                    server.launchFfplay(info.protocol, info.port);
 
                server.sendReady();  // sends to server that ffplay is up, start streaming
                gui.log("Player launched. Waiting for stream to finish...");
                server.waitForDone();
                stats.streamFinished();
                gui.log("Stream finished. Stats saved to client_stats.log.");
 
            } catch (Exception ex) {
                gui.log("ERROR: " + ex.getMessage());
            } finally {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        gui.streamBtn.setEnabled(true);
                    }
                });
            }
        }
    }
 
    /*** Helpers ***/
 
    // From the initial filename it adds for the recording _recorded.mp4 at the end
    private String buildRecordingFilename(String filename) {
        int dot = filename.lastIndexOf('.');
        String base = (dot >= 0) ? filename.substring(0, dot) : filename;
        return base + "_recorded.mp4";
    }
 
    /*** Main ***/
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                StreamingClient client = new StreamingClient();
                client.gui.setVisible(true);
            }
        });
    }
}