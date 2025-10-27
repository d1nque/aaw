package vyrib1.project.aaw.services.impl;

import lombok.SneakyThrows;
import org.opencv.core.Mat;
import org.springframework.stereotype.Service;
import vyrib1.project.aaw.config.FeatureConfig;
import vyrib1.project.aaw.data.domain.GpioButtons;
import vyrib1.project.aaw.services.CameraService;
import vyrib1.project.aaw.services.GuiService;
import vyrib1.project.aaw.services.LrfService;

import java.awt.event.KeyEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class GuiServiceImpl implements GuiService {

    /* Last found position: x=325, y=320 */

    private final CameraService cameraService;
    private final LrfService lrfService;
    private final FeatureConfig featureConfig;
    private final SwingGuiServiceImpl swingGuiService;

    private GpioButtons gpioButtons;

    private int x = 640;
    private int y = 480;

    public GuiServiceImpl(CameraService cameraService, LrfService lrfService, FeatureConfig featureConfig, SwingGuiServiceImpl swingGuiService) {
        this.cameraService = cameraService;
        this.lrfService = lrfService;
        this.featureConfig = featureConfig;
        this.swingGuiService = swingGuiService;
    }

    @Override
    @SneakyThrows
    public void startGui() {
        if (featureConfig.getLrf().isEnabled()) {
            System.out.printf("Starting LRF service...%n");
            lrfService.startLrf();
        } else {
            System.out.println("LRF service disabled in config");
        }

        if (featureConfig.getGpioButtons().isEnabled()) {
            System.out.println("Initializing GPIO buttons...");
            gpioButtons = new GpioButtons();
            startReadingGpioButtons();
        } else {
            System.out.println("GPIO buttons disabled in config");
        }

        System.out.println("Starting GUI...");
        Thread.sleep(2000);

        loadCoordinatesFromFiles();

        // Create Swing GUI window (fullscreen mode)
        swingGuiService.createAndShowGUI("Camera Feed", 720, 576, true);

        // Wait for GUI to initialize
        Thread.sleep(500);

        // Use regular thread for GUI operations
        Thread guiThread = new Thread(() -> {
            System.out.println("GUI thread started");
            try {
                while (swingGuiService.isRunning()) {
                    Mat frame = cameraService.getDayFrame();
                    if (frame == null || frame.empty()) {
                        //System.err.println("Received empty frame, skipping...");
                        Thread.sleep(30);
                        continue;
                    }

                    // Prepare text overlay
                    String distanceText = lrfService.getDistanceMeters() + "m";
                    String angleText = lrfService.getAngleDegrees() + "*";

                    // Update Swing GUI with frame, crosshair and text
                    swingGuiService.updateFrame(frame, x, y, distanceText, angleText);

                    // Small delay for frame rate control (~30 FPS)
                    Thread.sleep(33);
                }
            } catch (Exception e) {
                System.err.println("Error in GUI thread: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Clean up
                System.out.println("Cleaning up GUI resources");
                swingGuiService.close();
            }
        });

        guiThread.setDaemon(false);
        guiThread.setName("GUI-Display-Thread");
        guiThread.start();
        System.out.println("GUI thread launched");
    }

    private void loadCoordinatesFromFiles() {
        Path xPath = Paths.get("x.txt");
        Path yPath = Paths.get("y.txt");

        try {
            if (Files.exists(xPath)) {
                String xContent = Files.readString(xPath).trim();
                this.x = Integer.parseInt(xContent);
            } else {
                System.out.println("x.txt not found, using default x=0");
                this.x = 0;
            }
        } catch (Exception e) {
            System.out.println("Error reading x.txt, using default x=0" + e.getMessage());
            this.x = 0;
        }

        try {
            if (Files.exists(yPath)) {
                String yContent = Files.readString(yPath).trim();
                this.y = Integer.parseInt(yContent);
            } else {
                System.out.println("y.txt not found, using default y=0");
                this.y = 0;
            }
        } catch (Exception e) {
            System.out.println("Error reading y.txt, using default y=0" + e.getMessage());
            this.y = 0;
        }

        System.out.println("Loaded coordinates: x={" + this.x + "}, y={" + this.y + "}");
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
        if (gpioButtons == null) {
            return 0;
        }

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
        try (PrintWriter writer = new PrintWriter(new FileWriter("x.txt", false))) {
            writer.printf(String.valueOf(x));
            System.out.printf("Saved x: " + x);
        } catch (IOException e) {
            System.err.println("Error writing x coordinates to file: " + e.getMessage());
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter("y.txt", false))) {
            writer.printf(String.valueOf(y));
            System.out.printf("Saved y: " + y);
        } catch (IOException e) {
            System.err.println("Error writing y coordinates to file: " + e.getMessage());
        }
    }

}
