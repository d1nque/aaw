package vyrib1.project.aaw.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import vyrib1.project.aaw.data.domain.GpioButtons;
import vyrib1.project.aaw.services.CameraService;
import vyrib1.project.aaw.services.GuiService;
import vyrib1.project.aaw.services.LrfService;

import java.awt.event.KeyEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static org.opencv.imgproc.Imgproc.*;

@Service
@RequiredArgsConstructor
public class GuiServiceImpl implements GuiService {

    private final CameraService cameraService;
    private final LrfService lrfService;

    private GpioButtons gpioButtons;

    private int x = 0;
    private int y = 0;
    private double lastDistance = 0.0;

    static {
        try {
            // Try to load OpenCV native library
            nu.pattern.OpenCV.loadLocally();
            System.out.println("OpenCV loaded successfully");
        } catch (Exception e) {
            try {
                System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                System.out.println("OpenCV system library loaded");
            } catch (Exception ex) {
                System.err.println("Failed to load OpenCV: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Override
    @SneakyThrows
    public void startGui() {
        System.out.printf("Starting LRF service...%n");
        lrfService.startLrf();

        System.out.println("Initializing GPIO buttons...");
        gpioButtons = new GpioButtons();
        startReadingGpioButtons();

        System.out.println("Starting GUI...");
        Thread.sleep(2000);

        // Check if camera service is working
        Mat testFrame = cameraService.getDayFrame();
        if (testFrame == null || testFrame.empty()) {
            System.err.println("Camera frame is null or empty!");
            return;
        }
        System.out.println("Camera frame size: " + testFrame.size());

        // Text parameters
        int font = FONT_HERSHEY_SIMPLEX;
        double fontScale = 1.0;
        int thickness = 2;
        Scalar textColor = new Scalar(0, 0, 255); // Red color in BGR
        Scalar crosshairColor = new Scalar(0, 0, 255); // Red color for crosshair
        int crosshairThickness = 2;
        int crosshairLength = 35; // Length of crosshair lines

        x = cameraService.getDayFrame().cols() / 2;
        y = cameraService.getDayFrame().rows() / 2;

        HighGui.namedWindow("Camera Feed", HighGui.WINDOW_NORMAL);
        // Use regular thread instead of virtual thread for GUI operations
        Thread guiThread = new Thread(() -> {
            System.out.println("GUI thread started");
            try {
                while (true) {
                    Mat frame = cameraService.getDayFrame();
                    if (frame == null || frame.empty()) {
                        System.err.println("Received empty frame, skipping...");
                        Thread.sleep(30);
                        continue;
                    }

                    // Clone the frame to avoid modifying the original
                    Mat displayFrame = frame.clone();

                    // Draw crosshair - horizontal line
                    Imgproc.line(displayFrame,
                            new Point(x - crosshairLength, y),
                            new Point(x + crosshairLength, y),
                            crosshairColor, crosshairThickness, LINE_AA);

                    // Draw crosshair - vertical line
                    Imgproc.line(displayFrame,
                            new Point(x, y - crosshairLength),
                            new Point(x, y + crosshairLength),
                            crosshairColor, crosshairThickness, LINE_AA);

                    int currentY = displayFrame.rows() - 10;

                    // Add distance text to the frame
                    String distanceText = lrfService.getDistanceMeters() + "m";
                    putText(displayFrame, distanceText,
                            new Point(10, currentY), font, fontScale, textColor, thickness, LINE_AA, false);

                    //Add angle text to the frame
                    String angleText = lrfService.getAngleDegrees() + "*";
                    putText(displayFrame, angleText,
                            new Point(150, currentY), font, fontScale, textColor, thickness, LINE_AA, false);

                    // Display the frame using OpenCV's imshow
                    HighGui.imshow("Camera Feed", displayFrame);

                    // Wait for key press (30ms delay) - this is necessary for imshow to work
                    int key = HighGui.waitKey(30);

                    // Break on ESC key or 'q'
                    if (key == 27 || key == 'q' || key == 'Q') {
                        System.out.println("Exit key pressed, closing GUI");
                        break;
                    }

                    // Release the cloned frame
                    displayFrame.release();
                }
            } catch (Exception e) {
                System.err.println("Error in GUI thread: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Clean up
                System.out.println("Cleaning up GUI resources");
                HighGui.destroyAllWindows();
            }
        });

        guiThread.setDaemon(false);
        guiThread.setName("GUI-Display-Thread");
        guiThread.start();
        System.out.println("GUI thread launched");
    }

    private void startReadingGpioButtons() {
        Thread.startVirtualThread(() -> {
            System.out.println("Started reading GPIO buttons");
            while (true) {
                try {
                    int key = getGpioButtonStatus();
                    if (key != 0) {
                        handleGpioButtonPress(key);
                        saveCoordinatesToFile();
                        System.out.println("GPIO Button Pressed: " + KeyEvent.getKeyText(key));
                        System.out.println("Current Position: x=" + x + ", y=" + y);
                    }
                    Thread.sleep(100);
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error reading GPIO buttons: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleGpioButtonPress(int key) {
        switch (key) {
            case KeyEvent.VK_RIGHT:
                x += 5;
                break;
            case KeyEvent.VK_LEFT:
                x -= 5;
                break;
            case KeyEvent.VK_UP:
                y -= 5;
                break;
            case KeyEvent.VK_DOWN:
                y += 5;
                break;
            case KeyEvent.VK_SPACE:
                System.out.println("Center button pressed");
                break;
        }
    }

    private int getGpioButtonStatus() throws IOException {
        int result = 0;

        if (gpioButtons.centerBtn.isActive()) {
            result = KeyEvent.VK_SPACE;
        } else if (gpioButtons.upBtn.isActive()) {
            result = KeyEvent.VK_UP;
        } else if (gpioButtons.downBtn.isActive()) {
            result = KeyEvent.VK_DOWN;
        } else if (gpioButtons.leftBtn.isActive()) {
            result = KeyEvent.VK_LEFT;
        } else if (gpioButtons.rightBtn.isActive()) {
            result = KeyEvent.VK_RIGHT;
        }

        return result;
    }

    private void saveCoordinatesToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("coordinates.txt", false))) {
            writer.printf("x=%d, y=%d%n", x, y);
            System.out.printf("Saved to file: x=%d, y=%d%n", x, y);
        } catch (IOException e) {
            System.err.println("Error writing coordinates to file: " + e.getMessage());
        }
    }

}
