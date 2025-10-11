package vyrib1.project.aaw.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.springframework.stereotype.Service;
import vyrib1.project.aaw.services.CameraService;
import vyrib1.project.aaw.services.GuiService;
import vyrib1.project.aaw.services.LrfService;

import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX;
import static org.opencv.imgproc.Imgproc.LINE_AA;
import static org.opencv.imgproc.Imgproc.putText;

@Service
@RequiredArgsConstructor
public class GuiServiceImpl implements GuiService {

    private final CameraService cameraService;
    private final LrfService lrfService;
    private double lastDistance = 0.0;

    static {
        try {
            // Try to load OpenCV native library
            nu.pattern.OpenCV.loadShared();
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
        Scalar color = new Scalar(0, 0, 255); // Red color in BGR
        int x = 10;
        int y = cameraService.getDayFrame().rows() - 10;

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

                    // Update y position in case frame size changed
                    int currentY = displayFrame.rows() - 10;

                    // Add distance text to the frame
                    String distanceText = lrfService.getDistanceMeters() + "m";
                    putText(displayFrame, distanceText,
                            new Point(x, currentY), font, fontScale, color, thickness, LINE_AA, false);

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

        // Optional: Join the thread if you want to wait for it to complete
        // guiThread.join();
    }
}
