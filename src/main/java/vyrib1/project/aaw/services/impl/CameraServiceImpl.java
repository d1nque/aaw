package vyrib1.project.aaw.services.impl;

import jakarta.annotation.PreDestroy;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vyrib1.project.aaw.config.FeatureConfig;
import vyrib1.project.aaw.services.CameraService;

import java.util.concurrent.atomic.AtomicBoolean;

import static vyrib1.project.aaw.data.Constants.OpenCV.DAY_CAMERA_INDEX;


@Service
public class CameraServiceImpl implements CameraService {

    private static final Logger logger = LoggerFactory.getLogger(CameraServiceImpl.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private VideoCapture dayCamera;
    private Mat dayFrame;
    private Thread cameraReaderThread;
    private final boolean cameraEnabled;

    //TODO after thermal camera implementation
    //private VideoCapture thermalCamera;
    //private Mat thermalFrame = new Mat();

    public CameraServiceImpl(FeatureConfig featureConfig) {
        this.cameraEnabled = featureConfig.getCamera().isDayCameraEnabled();

        try {
            loadOpenCV();

            dayFrame = new Mat();

            if (cameraEnabled) {
                // Асинхронна ініціалізація камери для швидкого запуску Spring Boot
                logger.info("Starting camera initialization in background...");
                Thread.startVirtualThread(this::initializeCamera);
            } else {
                logger.info("Day camera running in MOCK mode - no hardware initialization (disabled in config)");
                // Create empty mock frame with fixed size (1280x960)
                dayFrame = new Mat(960, 1280, org.opencv.core.CvType.CV_8UC3);
            }
        } catch (Exception e) {
            logger.error("Error initializing camera service: {}", e.getMessage(), e);
        }
    }

    private void initializeCamera() {
        try {
            long startTime = System.currentTimeMillis();
            logger.info("Initializing camera hardware...");

            // Спочатку спробуємо MSMF (Windows Media Foundation) - найшвидший для Windows
            // CAP_MSMF = 1400 (Windows), CAP_V4L2 = 200 (Linux)
            int backend = getOptimalBackend();
            logger.info("Trying camera backend: {}", getBackendName(backend));

            dayCamera = new VideoCapture(DAY_CAMERA_INDEX, backend);

            // Якщо не вдалося з оптимальним backend, спробуємо auto-detect
            if (!dayCamera.isOpened()) {
                logger.warn("Failed with {}, trying auto-detect...", getBackendName(backend));
                dayCamera = new VideoCapture(DAY_CAMERA_INDEX);
            }

            if (!dayCamera.isOpened()) {
                throw new RuntimeException("Failed to open camera");
            }

            Thread.sleep(100);
            setDayCameraProperties();
            Thread.sleep(100);

            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("Camera initialized successfully in {}ms", elapsed);
            
            running.set(true);
            startReadingCamera();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Camera initialization interrupted", e);
        } catch (Exception e) {
            logger.error("Error initializing camera hardware: {}", e.getMessage(), e);
        }
    }

    /**
     * Визначити оптимальний backend для поточної ОС
     * Windows: DSHOW (700) - DirectShow, часто швидший за MSMF
     * Linux: V4L2 (200) - нативний
     */
    private int getOptimalBackend() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return 700;   // CAP_DSHOW - DirectShow (швидший за MSMF для багатьох камер)
        } else if (os.contains("linux")) {
            return 200;   // CAP_V4L2 - Video4Linux2
        } else {
            return 0;     // CAP_ANY - auto-detect для інших ОС
        }
    }

    private String getBackendName(int backend) {
        return switch (backend) {
            case 1400 -> "MSMF (Windows Media Foundation)";
            case 700 -> "DSHOW (DirectShow)";
            case 200 -> "V4L2 (Video4Linux2)";
            default -> "AUTO (all backends)";
        };
    }

    private void startReadingCamera() {
        cameraReaderThread = Thread.ofVirtual().name("Camera-Reader").start(() -> {
            logger.info("Started reading day camera");
            while (running.get()) {
                if (dayCamera != null && dayCamera.isOpened()) {
                    dayCamera.read(dayFrame);
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            logger.info("Stopped reading day camera");
        });
    }

    private void setDayCameraProperties() {
        dayCamera.set(Videoio.CAP_PROP_FOURCC, VideoWriter.fourcc('M', 'J', 'P', 'G'));
        dayCamera.set(Videoio.CAP_PROP_FRAME_WIDTH, 1280);
        dayCamera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 960);
        dayCamera.set(Videoio.CAP_PROP_FPS, 30);
    }

    @Override
    public Mat getDayFrame() {
        return dayFrame;
    }

    /**
     * Cleanup method called when Spring context is destroyed.
     * Stops camera reading thread and releases resources.
     */
    @PreDestroy
    public void cleanup() {
        logger.info("Cleaning up camera service");
        running.set(false);
        
        if (cameraReaderThread != null) {
            try {
                cameraReaderThread.join(2000); // Wait up to 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for camera reader thread to stop");
            }
        }
        
        if (dayCamera != null && dayCamera.isOpened()) {
            dayCamera.release();
            logger.info("Day camera released");
        }
        
        if (dayFrame != null) {
            dayFrame.release();
            logger.info("Day frame released");
        }
        
        logger.info("Camera service cleanup completed");
    }

    private static void loadOpenCV() {
        try {
            nu.pattern.OpenCV.loadLocally();
            LoggerFactory.getLogger(CameraServiceImpl.class).info("OpenCV loaded successfully via openpnp");
        } catch (Exception e) {
            LoggerFactory.getLogger(CameraServiceImpl.class).error("Failed to load OpenCV: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot initialize OpenCV", e);
        }
    }
}
