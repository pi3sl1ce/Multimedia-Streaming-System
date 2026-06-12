/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package streaming.streamingserver;

/**
 *
 * @author Nevena
 */
public class VideoFile {
    
    
    private final String movieName; // px "Yellowjackets"
    private final String resolution;    // px "720p"
    private final String format;    // px "mkv"
    private final String filePath;  // full path on disk
 
    public VideoFile(String movieName, String resolution, String format, String filePath) {
        this.movieName = movieName;
        this.resolution = resolution;
        this.format = format;
        this.filePath = filePath;
    }
 
    public String getMovieName()  { 
        return movieName;  
    }
    public String getResolution() { 
        return resolution; 
    }
    public String getFormat()     { 
        return format;     
    }
    public String getFilePath()   { 
        return filePath;   
    }
 
    
    // The display name client sees, px "Yellowjackets-720p.mkv"
    public String getDisplayName() {
        return movieName + "-" + resolution + "." + format;
    }
 
    @Override
    public String toString() {
        return getDisplayName();
    }   
}
