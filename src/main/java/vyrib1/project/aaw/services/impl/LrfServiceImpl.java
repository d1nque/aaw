package vyrib1.project.aaw.services.impl;

import com.fazecast.jSerialComm.SerialPort;
import org.springframework.stereotype.Service;
import vyrib1.project.aaw.services.LrfService;

import static vyrib1.project.aaw.data.Constants.Lrf.CMD_READ_RESULT;
import static vyrib1.project.aaw.data.Constants.Lrf.DEVICE_ADDRESS;

@Service
public class LrfServiceImpl implements LrfService {

    private static SerialPort port;

    private double distanceMeters = -1.0;

    public LrfServiceImpl() {
        startLrf();
    }

    public void startLrf() {
        String portName = "/dev/serial0";
        port = SerialPort.getCommPort(portName);
        port.setComPortParameters(19200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 100);

        if (!port.openPort()) {
            System.err.println("Не вдалося відкрити порт: " + portName);
            return;
        }

        System.out.println("Порт відкрито: " + portName);
        System.out.println("Читання дистанції... (Ctrl+C для зупинки)");

        Thread.startVirtualThread(() -> {
            try {
                while (true) {
                    sendCommand(CMD_READ_RESULT);
                    Thread.sleep(100); // частота ~10 разів на секунду
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                port.closePort();
            }
        });
    }

    private void sendCommand(byte command) {
        byte[] frame = new byte[3];
        frame[0] = DEVICE_ADDRESS;
        frame[1] = command;
        frame[2] = (byte) ((0x100 - (command & 0xFF)) & 0xFF);
        port.writeBytes(frame, frame.length);

        byte[] buffer = new byte[16];
        int numRead = port.readBytes(buffer, buffer.length);
        if (numRead > 0) {
            parseResponse(buffer, numRead);
        }
    }

    private void parseResponse(byte[] data, int len) {
        if (len < 5) return;

        byte respType = data[1];
        if (respType == 0x01 && len >= 6) { // RSP_DATA
            byte b1 = data[2];
            boolean distInvalid = (b1 & 0x80) != 0;
            boolean highRes = (b1 & 0x20) != 0;
            boolean yard = (b1 & 0x10) != 0;

            if (!distInvalid) {
                int distRaw = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
                double dist = highRes ? distRaw / 10.0 : distRaw / 2.0;
                distanceMeters = yard ? dist * 0.9144 : dist;
                System.out.printf("Відстань: %.1f %s%n", dist, yard ? "ярдів" : "метрів");
            } else {
                System.out.println("Немає цілі / результат недійсний");
            }
        }
    }

    @Override
    public double getDistanceMeters() {
        return distanceMeters;
    }
}
