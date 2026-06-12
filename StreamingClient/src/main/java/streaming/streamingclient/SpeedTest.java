/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package streaming.streamingclient;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import java.util.logging.Logger;

/**
 *
 * @author Nevena
 */
public class SpeedTest {

    private static final Logger logger = Logger.getLogger(SpeedTest.class.getName());

    private final SpeedTestSocket socket;
    private float downloadSpeed = 0;
    private float percentage = 0;

    public SpeedTest() {
        socket = new SpeedTestSocket();

        socket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(SpeedTestReport report) {
                socket.closeSocket();
                percentage = 100;
                downloadSpeed = report.getTransferRateBit().floatValue() / (1024 * 1024);
                logger.info("Speed test complete: " + downloadSpeed + " Mbps");
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                percentage = percent;
            }

            @Override
            public void onError(SpeedTestError error, String message) {
                logger.warning("Speed test error: " + message);
                percentage = 100;   // unblock any waiting loop
            }
        });
    }

    // Starts a fixed-duration download test (5 seconds) against the given URL
    public void testSpeed(String url) {
        socket.startFixedDownload(url, 5000);
    }

    // Returns the measured download speed in Mbps (0 if not yet complete)
    public float getSpeed() {
        return downloadSpeed;
    }

    // Returns the current progress percentage (0–100)
    public float getPercent() {
        return percentage;
    }
}