package vyrib1.project.aaw.services.impl;

import jakarta.annotation.PreDestroy;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vyrib1.project.aaw.config.FeatureConfig;
import vyrib1.project.aaw.data.BallisticConstants;
import vyrib1.project.aaw.data.domain.Coordinates;
import vyrib1.project.aaw.data.domain.GpioButtons;
import vyrib1.project.aaw.services.BallisticCalculatorService;
import vyrib1.project.aaw.services.CameraService;
import vyrib1.project.aaw.services.CoordinatesPersistenceService;
import vyrib1.project.aaw.services.GuiService;
import vyrib1.project.aaw.services.LrfService;

import java.awt.event.KeyEvent;

/**
 * Main GUI service that coordinates camera, LRF, and GUI display.
 */
@Service
public class GuiServiceImpl implements GuiService {

    private static final Logger logger = LoggerFactory.getLogger(GuiServiceImpl.class);

    // GUI Constants
    private static final int DEFAULT_GUI_WIDTH = 720;
    private static final int DEFAULT_GUI_HEIGHT = 576;
    private static final int CROSSHAIR_MOVE_STEP = 5;
    //private static final int FRAME_DELAY_MS = 33; // ~30 FPS
    private static final int FRAME_DELAY_MS = 15; // ~30 FPS
    private static final int GUI_INIT_DELAY_MS = 2000;
    private static final int GUI_READY_DELAY_MS = 500;
    private static final int GPIO_POLL_INTERVAL_MS = 100;
    private static final int FRAME_WAIT_DELAY_MS = 30;
    private static final double MAX_VALID_RANGE_METERS = 3000.0;

    private final CameraService cameraService;
    private final LrfService lrfService;
    private final FeatureConfig featureConfig;
    private final SwingGuiServiceImpl swingGuiService;
    private final BallisticCalculatorService ballisticCalculatorService;
    private final CoordinatesPersistenceService coordinatesPersistenceService;

    private GpioButtons gpioButtons;
    private Coordinates currentCoordinates;

    public GuiServiceImpl(CameraService cameraService, LrfService lrfService,
                         FeatureConfig featureConfig, SwingGuiServiceImpl swingGuiService,
                         BallisticCalculatorService ballisticCalculatorService,
                         CoordinatesPersistenceService coordinatesPersistenceService) {
        this.cameraService = cameraService;
        this.lrfService = lrfService;
        this.featureConfig = featureConfig;
        this.swingGuiService = swingGuiService;
        this.ballisticCalculatorService = ballisticCalculatorService;
        this.coordinatesPersistenceService = coordinatesPersistenceService;
    }

    @Override
    public void startGui() {
        try {
            initializeLrfService();
            initializeGpioButtons();
            
            logger.info("Starting GUI...");
            Thread.sleep(GUI_INIT_DELAY_MS);
            
            currentCoordinates = coordinatesPersistenceService.loadCoordinates();
            
            // Create Swing GUI window (fullscreen mode)
            swingGuiService.createAndShowGUI("Camera Feed", DEFAULT_GUI_WIDTH, DEFAULT_GUI_HEIGHT, true);
            
            // Wait for GUI to initialize
            Thread.sleep(GUI_READY_DELAY_MS);
            
            startGuiDisplayLoop();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("GUI startup interrupted", e);
        } catch (Exception e) {
            logger.error("Error starting GUI", e);
        }
    }

    /**
     * Initializes LRF service if enabled in configuration.
     */
    private void initializeLrfService() {
        if (featureConfig.getLrf().isEnabled()) {
            logger.info("Starting LRF service...");
            lrfService.startLrf();
        } else {
            logger.info("LRF service disabled in config");
        }
    }

    /**
     * Initializes GPIO buttons if enabled in configuration.
     */
    private void initializeGpioButtons() {
        if (featureConfig.getGpioButtons().isEnabled()) {
            logger.info("Initializing GPIO buttons...");
            gpioButtons = new GpioButtons();
            startReadingGpioButtons();
        } else {
            logger.info("GPIO buttons disabled in config");
        }
    }

    /**
     * Starts the main GUI display loop in a separate thread.
     */
    private void startGuiDisplayLoop() {
        Thread guiThread = new Thread(() -> {
            logger.info("GUI display thread started");
            try {
                while (swingGuiService.isRunning()) {
                    processFrame();
                }
            } catch (Exception e) {
                logger.error("Error in GUI display thread", e);
            } finally {
                cleanup();
            }
        });

        guiThread.setDaemon(false);
        guiThread.setName("GUI-Display-Thread");
        guiThread.start();
        logger.info("GUI display thread launched");
    }

    /**
     * Processes a single frame: gets camera frame, calculates ballistic aim point,
     * and updates the GUI display.
     */
    private void processFrame() throws InterruptedException {
        Mat frame = cameraService.getDayFrame();
        if (frame == null || frame.empty()) {
            Thread.sleep(FRAME_WAIT_DELAY_MS);
            return;
        }

        int frameWidth = frame.cols();
        int frameHeight = frame.rows();

        double rangeFromLrf = lrfService.getDistanceMeters();
        Point aimPoint = calculateAimPoint(frameWidth, frameHeight, rangeFromLrf);

        // Update range if invalid
        if (rangeFromLrf <= 0 || rangeFromLrf >= MAX_VALID_RANGE_METERS) {
            rangeFromLrf = BallisticConstants.TARGET_DISTANCE;
        }

        String distanceText = rangeFromLrf + "m";
        String angleText = lrfService.getAngleDegrees() + "*";
        String speedText = BallisticConstants.TARGET_SPEED_KMH + "km/h";

        swingGuiService.updateFrame(frame, currentCoordinates.x(), currentCoordinates.y(),
                distanceText, angleText, speedText, (int) aimPoint.x, (int) aimPoint.y);

        Thread.sleep(FRAME_DELAY_MS);
    }

    /**
     * Calculates the ballistic aim point based on LRF distance or default.
     */
    private Point calculateAimPoint(int frameWidth, int frameHeight, double rangeFromLrf) {
        if (rangeFromLrf > 0 && rangeFromLrf < MAX_VALID_RANGE_METERS) {
            return ballisticCalculatorService.calculateAimPoint(
                    currentCoordinates.x(), currentCoordinates.y(),
                    frameWidth, frameHeight, rangeFromLrf
            );
        } else {
            return ballisticCalculatorService.calculateAimPoint(
                    currentCoordinates.x(), currentCoordinates.y(),
                    frameWidth, frameHeight
            );
        }
    }

    /**
     * Starts background thread for reading GPIO button inputs.
     */
    private void startReadingGpioButtons() {
        Thread.ofVirtual().name("GPIO-Button-Reader").start(() -> {
            logger.info("Started reading GPIO buttons");
            while (swingGuiService.isRunning()) {
                try {
                    int key = getGpioButtonStatus();
                    if (key != 0) {
                        handleGpioButtonPress(key);
                        coordinatesPersistenceService.saveCoordinates(currentCoordinates);
                        logger.debug("GPIO Button: {} - Position: {}", KeyEvent.getKeyText(key), currentCoordinates);
                    }
                    Thread.sleep(GPIO_POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.info("GPIO button reader interrupted");
                    break;
                } catch (Exception e) {
                    logger.error("Error reading GPIO buttons", e);
                }
            }
            logger.info("Stopped reading GPIO buttons");
        });
    }

    /**
     * Handles GPIO button press by updating crosshair coordinates.
     */
    private void handleGpioButtonPress(int key) {
        int newX = currentCoordinates.x();
        int newY = currentCoordinates.y();

        switch (key) {
            case KeyEvent.VK_RIGHT -> newX += CROSSHAIR_MOVE_STEP;
            case KeyEvent.VK_LEFT -> newX -= CROSSHAIR_MOVE_STEP;
            case KeyEvent.VK_UP -> newY -= CROSSHAIR_MOVE_STEP;
            case KeyEvent.VK_DOWN -> newY += CROSSHAIR_MOVE_STEP;
            case KeyEvent.VK_SPACE -> logger.info("Center button pressed");
            default -> logger.warn("Unknown key code: {}", key);
        }

        // Ensure coordinates stay non-negative
        if (newX >= 0 && newY >= 0) {
            currentCoordinates = new Coordinates(newX, newY);
        }
    }

    /**
     * Gets the currently pressed GPIO button status.
     */
    private int getGpioButtonStatus() {
        if (gpioButtons == null) {
            return 0;
        }

        if (gpioButtons.getCenterBtn().isActive()) {
            return KeyEvent.VK_SPACE;
        } else if (gpioButtons.getUpBtn().isActive()) {
            return KeyEvent.VK_UP;
        } else if (gpioButtons.getDownBtn().isActive()) {
            return KeyEvent.VK_DOWN;
        } else if (gpioButtons.getLeftBtn().isActive()) {
            return KeyEvent.VK_LEFT;
        } else if (gpioButtons.getRightBtn().isActive()) {
            return KeyEvent.VK_RIGHT;
        }

        return 0;
    }

    /**
     * Cleanup method for releasing resources.
     */
    @PreDestroy
    public void cleanup() {
        logger.info("Cleaning up GUI service resources");
        
        if (gpioButtons != null) {
            gpioButtons.close();
            logger.info("GPIO buttons closed");
        }
        
        swingGuiService.close();
        logger.info("GUI service cleanup completed");
    }
}
