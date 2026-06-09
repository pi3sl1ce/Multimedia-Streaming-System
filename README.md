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
---
