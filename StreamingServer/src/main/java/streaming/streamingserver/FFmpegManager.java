/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package streaming.streamingserver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Nevena
 */
public class FFmpegManager {

    private static final Logger logger = Logger.getLogger(FFmpegManager.class.getName());

    public static final String[] FORMATS = { "mkv", "mp4", "avi" };
    public static final String[] RESOLUTIONS = { "240p", "360p", "480p", "720p", "1080p" };

    private final String videosDir;
    private final String ffmpegBin;
 
    public FFmpegManager(String videosDir, String ffmpegBin) {
        this.videosDir = videosDir;
        this.ffmpegBin = ffmpegBin;
    }
 
    
    /*** Helpers ***/
 
    // Returns the ffmpeg scale filter string for given resolution
    private String getScale(String resolution) {
        switch (resolution) {
            case "240p":  return "426:240";
            case "360p":  return "640:360";
            case "480p":  return "854:480";
            case "720p":  return "1280:720";
            case "1080p":  return "1920:1080";
            default:    return "640:360";
        }
    }
 
    // Returns min Mbps needed for given resolution
    public double getBitrate(String resolution) {
        switch (resolution) {
            case "240p":  return 0.5;
            case "360p":  return 1.0;
            case "480p":  return 2.5;
            case "720p":  return 5.0;
            case "1080p":  return 8.0;
            default:    return 0.5;
        }
    }
 
    // Returns the index of a resolution in RESOLUTIONS (-1 if not found)
    public int getResolutionIndex(String resolution) {
        return arrayIndexOf(RESOLUTIONS, resolution);
    }
 
    // Returns true if the given format is in supported formats array
    public boolean isValidFormat(String format) {
        return arrayContains(FORMATS, format);
    }
 
    // Returns true if given resolution is in resolutions array
    public boolean isValidResolution(String resolution) {
        return arrayContains(RESOLUTIONS, resolution);
    }


     /* Scans the videos/ directory for each movie found
       generates every missing (resolution, format) pair
       up to the highest resolution already on disk (never upscales)
       Returns the full list of available VideoFile objects after generation */
    public List<VideoFile> scanAndGenerate() {
        File dir = new File(videosDir);
        if (!dir.exists()) {
            dir.mkdirs();
            logger.info("Created videos/ folder at: " + dir.getAbsolutePath());
        }
 
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            logger.warning("No video files found in " + dir.getAbsolutePath());
            logger.warning("Add a file named e.g. Forrest_Gump-720p.mkv and restart.");
            return new ArrayList<VideoFile>();
        }
 
        // Collect all unique movie names
        ArrayList<String> movieNames = new ArrayList<String>();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].isFile()) continue;
            String name = files[i].getName();
            int dash = name.lastIndexOf('-');
            int dot = name.lastIndexOf('.');
            if (dash < 0 || dot < 0 || dot <= dash) continue;
 
            String res = name.substring(dash + 1, dot);
            String format = name.substring(dot + 1).toLowerCase();
            if (!isValidResolution(res) || !isValidFormat(format)) continue;
 
            String movie = name.substring(0, dash);
            if (!movieNames.contains(movie)) {
                movieNames.add(movie);
            }
        }
 
        // For each movie, find the highest resolution on disk and generate missing files
        for (int m = 0; m < movieNames.size(); m++) {
            String movie = movieNames.get(m);
            String highestRes = null;
            File sourceFile = null;
            int highestIdx = -1;
 
            // Search from highest resolution to downwards
            for (int ri = RESOLUTIONS.length - 1; ri >= 0; ri--) {
                for (int fi = 0; fi < FORMATS.length; fi++) {
                    File candidate = new File(videosDir,
                            movie + "-" + RESOLUTIONS[ri] + "." + FORMATS[fi]);
                    if (candidate.exists()) {
                        highestRes = RESOLUTIONS[ri];
                        highestIdx = ri;
                        sourceFile = candidate;
                        break;
                    }
                }
                if (highestRes != null) break;
            }
 
            if (highestRes == null) continue;
 
            logger.info("Movie: " + movie
                    + "  |  highest: " + highestRes
                    + "  |  source: "  + sourceFile.getName());
 
            // Generate every missing (resolution, format) pair up to highestIdx
            for (int ri = 0; ri <= highestIdx; ri++) {
                for (int fi = 0; fi < FORMATS.length; fi++) {
                    File outFile = new File(videosDir,
                            movie + "-" + RESOLUTIONS[ri] + "." + FORMATS[fi]);
                    if (!outFile.exists()) {
                        convertWithFFmpeg(sourceFile, outFile, RESOLUTIONS[ri]);
                    }
                }
            }
        }
 
        return buildVideoList(dir);
    }
 
 
    /* Launchea FFmpeg as a streaming sender
     Returns the Process so ClientHandler can waitFor() it */
    public Process startStreaming(String filePath, String protocol, int port)
            throws Exception {
 
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(ffmpegBin);
        cmd.add("-re");
        cmd.add("-i");
        cmd.add(filePath);
 
        switch (protocol.toUpperCase()) {
            case "UDP":
                cmd.add("-f");  cmd.add("mpegts");
                cmd.add("udp://127.0.0.1:" + port + "?listen");
                break;
            case "RTP/UDP":
                cmd.add("-c:v");    cmd.add("copy");
                cmd.add("-c:a");    cmd.add("aac");       
                cmd.add("-f");  cmd.add("rtp_mpegts"); 
                cmd.add("rtp://127.0.0.1:" + port);
                break;
            case "TCP":
            default:
                cmd.add("-f");  cmd.add("mpegts");
                cmd.add("tcp://127.0.0.1:" + port + "?listen");
                break;
        }
 
        logger.info("FFmpeg stream cmd: " + cmd.toString());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        return pb.start();
    }
 
    /*** Helpers ***/
 
    // Returns true if arr contains value (case-sensitive)
    private boolean arrayContains(String[] arr, String value) {
        return arrayIndexOf(arr, value) >= 0;
    }
 
    /* Returns the index of value in arr (-1 if not found)
       Used by arrayContains() and getResolutionIndex() */
    private int arrayIndexOf(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }
 
    // Runs FFmpeg to convert source to output at given resolution
    private void convertWithFFmpeg(File source, File output, String resolution) {
        logger.info("  Generating: " + output.getName() + "  from: " + source.getName());
 
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(ffmpegBin);
        cmd.add("-y");
        cmd.add("-i");  cmd.add(source.getAbsolutePath());
        cmd.add("-vf"); cmd.add("scale=" + getScale(resolution));
        cmd.add("-c:v");    cmd.add("libx264");
        cmd.add("-c:a");    cmd.add("aac");
        cmd.add("-preset");     cmd.add("fast");
        cmd.add(output.getAbsolutePath());
 
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.inheritIO();
            int exit = pb.start().waitFor();
            if (exit == 0) {
                logger.info("  Created: " + output.getName());
            } else {
                logger.warning("  FFmpeg exit=" + exit + " for " + output.getName());
            }
        } catch (Exception e) {
            logger.severe("FFmpeg convert error: " + e.getMessage());
        }
    }
 
    // Scans the directory and returns a list of all valid VideoFile objects
    private List<VideoFile> buildVideoList(File dir) {
        ArrayList<VideoFile> list = new ArrayList<VideoFile>();
        File[] files = dir.listFiles();
        if (files == null) return list;
 
        for (int i = 0; i < files.length; i++) {
            if (!files[i].isFile()) continue;
            String name = files[i].getName();
            int dash = name.lastIndexOf('-');
            int dot = name.lastIndexOf('.');
            if (dash < 0 || dot < 0 || dot <= dash) continue;
 
            String movie = name.substring(0, dash);
            String res = name.substring(dash + 1, dot);
            String format = name.substring(dot + 1).toLowerCase();
            if (!isValidResolution(res) || !isValidFormat(format)) continue;
 
            list.add(new VideoFile(movie, res, format, files[i].getAbsolutePath()));
        }
        return list;
    }
}