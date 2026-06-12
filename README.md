# Multimedia-Streaming-System
Adaptive video streaming system with Client-Server architecture, Java Sockets, and FFmpeg

**Μάθημα:** Πολυμέσα & Πολυμεσικές Επικοινωνίες   
**Τμήμα:** Μηχανικών Πληροφορικής και Υπολογιστών     
**Πανεπιστήμιο:** Πανεπιστήμιο Δυτικής Αττικής    

---

# Overview
This project implements a complete video streaming pipeline based on the Client–Server model:
- Video processing using FFMPEG
- Network communication via Java Sockets
- Streaming using multiple protocols (TCP / UDP / RTP)
- Adaptive quality based on client bandwidth
- GUI-based client interface

The system is designed in a modular way and can scale from a simple streaming server to a distributed system with load balancing.

---

# Architecture
The system consists of three main components:

### Streaming Server
- Scans `/videos` directory
- Generates missing formats/resolutions using FFMPEG
- Maintains video metadata (title, resolution, format)
- Streams video to clients
- Supports multiple clients (multithreading)

### Streaming Client
- Performs network speed test
- Requests compatible videos from server
- Allows user to select:
  - Video
  - Resolution
  - Format
  - Protocol (TCP / UDP / RTP)
  - Plays video stream via FFmpeg/FFplay

### Load Balancer
- Distributes clients across multiple servers
- Improves scalability and performance

## Project Structure

```
Multimedia-Streaming-System/
├── README.md
├── StreamingClient/
│   ├── pom.xml
│   ├── streaming.keystore
│   ├── client_stats.log            // generated at runtime
│   └── src/.../streamingclient/
│       ├── StreamingClient.java    // main, controller, background tasks
│       ├── Dashboard.java          // Swing GUI
│       ├── ServerConnection.java   // SSL socket, ffplay/ffmpeg launchers
│       ├── SpeedTest.java          // JSpeedTest wrapper (5s download test)
│       └── StatsLogger.java        // CSV session logger
├── StreamingLoadBalancer/
│   ├── pom.xml
│   └── src/.../streamingloadbalancer/
│       └── StreamingLoadBalancer.java 
└── StreamingServer/
    ├── pom.xml
    ├── streaming.keystore
    ├── server.log               // generated at runtime 
    ├── videos/                   
    └── src/.../streamingserver/
        ├── StreamingServer.java  // main, SSL ServerSocket, thread-per-client
        ├── ClientHandler.java    // HELLO/SELECT protocol, ffmpeg streaming
        ├── FFmpegManager.java    // transcoding, format/resolution helpers
        └── VideoFile.java       // model: name, resolution, format, path
```                     

---

## Setup
 
### 1. Generate the SSL keystore
 
Run this once from the project root. Copy the generated `streaming.keystore` to both `StreamingServer/` and `StreamingClient/`.
 
```bash
keytool -genkeypair -alias streaming \
        -keyalg RSA -keysize 2048 -validity 365 \
        -keystore streaming.keystore \
        -storepass streaming123 -keypass streaming123 \
        -dname "CN=localhost, OU=Lab, O=University, L=Athens, ST=Attica, C=GR"
```
 
### 2. Add source video files
 
Place at least one video file in `StreamingServer/videos/` using the naming convention:
 
```
MovieName-ResolutionP.format
```
 
Examples:
```
videos/
├── Yellowjackets-720p.mkv
└── Star_Wars_Episode_V-480p.mp4
```
 
Supported formats: `mkv`, `mp4`, `avi`  
Supported resolutions: `240p`, `360p`, `480p`, `720p`, `1080p`
 
The server will auto-generate all lower resolutions and missing formats on startup (it never upscales).

---

## Running
 
Start the components in this order:
 
```bash
# 1. Load Balancer (optional, skip for single-server setup)
java -jar StreamingLoadBalancer/target/StreamingLoadBalancer.jar
 
# 2. Server (one or more instances)
java -jar StreamingServer/target/StreamingServer.jar
 
# 3. Client
java -jar StreamingClient/target/StreamingClient.jar

(or right-click)
```
 
Then in the GUI:
1. Click **1) Connect**
2. Click **2) Speed Test** and wait ~5 seconds
3. Choose a format and click **3) Get Files**
4. Select a file from the list, choose a protocol (or leave Auto), and click **4) Start Streaming**  
(Optional: Tick **Record stream to file** before streaming to save the stream as `<MovieName>_recorded.mp4`).

<p align="center">
  <img width="585" height="475" alt="DashboardGUI" src="https://github.com/user-attachments/assets/9745f4d4-355f-4179-bdea-26c16cb05fbd" />
</p>
 
 ---
