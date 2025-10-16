package vyrib1.project.aaw.services.impl;

import com.fazecast.jSerialComm.SerialPort;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vyrib1.project.aaw.services.LrfService;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static vyrib1.project.aaw.data.Constants.Lrf.CMD_READ_RESULT;
import static vyrib1.project.aaw.data.Constants.Lrf.DEVICE_ADDRESS;

@Service
public class LrfServiceImpl implements LrfService {

    private static final Logger logger = LoggerFactory.getLogger(LrfServiceImpl.class);

    private SerialPort port; // Remove static - causes issues in Spring
    private final AtomicReference<Double> distanceMeters = new AtomicReference<>(-1.0);
    private final AtomicReference<Double> angleDegrees = new AtomicReference<>(0.0);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private Thread readerThread;

    @Override
    public void startLrf() {
        if (running.get()) {
            logger.warn("LRF service already running");
            return;
        }

        String portName = "/dev/serial0";
        port = SerialPort.getCommPort(portName);

        // Configure port for LRF3K1LS
        port.setComPortParameters(19200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 500, 500); // Increased timeout
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

        if (!port.openPort()) {
            logger.error("Failed to open port: {}", portName);
            connected.set(false);
            return;
        }

        logger.info("Port opened: {}", portName);
        connected.set(true);
        running.set(true);

        // Clear any existing data in buffers
        port.flushIOBuffers();

        readerThread = Thread.ofVirtual().name("LRF-Reader").start(() -> {
            try {
                while (running.get() && connected.get()) {
                    try {
                        sendCommand(CMD_READ_RESULT);
                        Thread.sleep(100); // 10Hz update rate
                    } catch (Exception e) {
                        logger.error("Error reading from LRF", e);
                        // Try to reconnect after error
                        Thread.sleep(1000);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("LRF reader thread interrupted");
            } finally {
                cleanup();
            }
        });
    }

    private void sendCommand(byte command) {
        if (!connected.get() || port == null) {
            return;
        }

        try {
            // Create command frame for LRF3K1LS
            byte[] frame = new byte[3];
            frame[0] = DEVICE_ADDRESS;
            frame[1] = command;
            frame[2] = calculateChecksum(frame[0], frame[1]); // Proper checksum calculation

            // Send command
            int written = port.writeBytes(frame, frame.length);
            if (written != frame.length) {
                logger.warn("Failed to write complete command frame");
                return;
            }

            // Wait a bit for device to process
            Thread.sleep(10);

            // Read response
            byte[] buffer = new byte[32]; // Increased buffer size
            int numRead = port.readBytes(buffer, buffer.length);

            if (numRead > 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Received {} bytes: {}", numRead, bytesToHex(buffer, numRead));
                }
                parseResponse(buffer, numRead);
            } else {
                logger.debug("No data received from LRF");
            }

        } catch (Exception e) {
            logger.error("Error sending command to LRF", e);
        }
    }

    private byte calculateChecksum(byte addr, byte cmd) {
        // LRF3K1LS checksum calculation
        int sum = (addr & 0xFF) + (cmd & 0xFF);
        return (byte) ((256 - (sum & 0xFF)) & 0xFF);
    }

    private void parseResponse(byte[] data, int len) {
        if (len < 5) {
            logger.debug("Response too short: {} bytes", len);
            return;
        }

        // Check if this is a valid response
        if (data[0] != DEVICE_ADDRESS) {
            logger.debug("Invalid device address in response: 0x{}", String.format("%02X", data[0]));
            return;
        }

        byte respType = data[1];

        if (respType == 0x01 && len >= 6) { // RSP_DATA response
            parseDistanceData(data);
        } else if (respType == 0x02) { // RSP_ACK
            logger.debug("Received ACK response");
        } else if (respType == 0x03) { // RSP_NACK
            logger.warn("Received NACK response");
        } else {
            logger.debug("Unknown response type: 0x{}", String.format("%02X", respType));
        }
    }

    private void parseDistanceData(byte[] data) {
        byte flags = data[2];
        boolean distInvalid = (flags & 0x80) != 0;
        boolean angleInvalid = (flags & 0x40) != 0;
        boolean highRes = (flags & 0x20) != 0;
        boolean yard = (flags & 0x10) != 0;

        if (!distInvalid && data.length >= 6) {
            int distRaw = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
            double dist = highRes ? distRaw / 10.0 : distRaw / 2.0;
            double distanceInMeters = yard ? dist * 0.9144 : dist;

            double angleDegrees = Double.NaN;
            if (!angleInvalid) {
                byte angleByte = data[5];
                // angleByte – two’s complement signed byte => in Java it’s already signed
                angleDegrees = angleByte;  // in degrees
            }

            // Тепер можна зберегти обидва: distance & angle
            distanceMeters.set(distanceInMeters);
            this.angleDegrees.set(angleDegrees);
            // якщо є змінна atomic для кута, set її теж

            logger.debug("Distance = {} m, Angle = {}° (flags = 0x{})",
                    distanceInMeters, angleDegrees, String.format("%02X", flags));
        } else {
            // Обробка недійсного результату
            distanceMeters.set(-1.0);
            logger.debug("Invalid measurement (flags: 0x{})", String.format("%02X", flags));
        }
    }


    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }

    @Override
    public double getDistanceMeters() {
        return distanceMeters.get();
    }

    @Override
    public double getAngleDegrees() {
        return angleDegrees.get();
    }

    public boolean isConnected() {
        return connected.get();
    }

    public boolean isRunning() {
        return running.get();
    }

    @PreDestroy
    public void stopLrf() {
        logger.info("Stopping LRF service");
        running.set(false);

        if (readerThread != null) {
            readerThread.interrupt();
            try {
                readerThread.join(2000); // Wait up to 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        cleanup();
    }

    private void cleanup() {
        connected.set(false);
        if (port != null && port.isOpen()) {
            port.closePort();
            logger.info("Serial port closed");
        }
    }
}
