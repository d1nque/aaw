package vyrib1.project.aaw.services.impl;

import lombok.SneakyThrows;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
import org.springframework.stereotype.Service;
import vyrib1.project.aaw.config.FeatureConfig;
import vyrib1.project.aaw.services.CameraService;

import static vyrib1.project.aaw.data.Constants.OpenCV.DAY_CAMERA_INDEX;


@Service
public class CameraServiceImpl implements CameraService {

    private final FeatureConfig featureConfig;
    private VideoCapture dayCamera;
    private Mat dayFrame;

    //TODO after thermal camera implementation
    //private VideoCapture thermalCamera;
    //private Mat thermalFrame = new Mat();

    public CameraServiceImpl(FeatureConfig featureConfig) {
        this.featureConfig = featureConfig;

        try {
            loadOpenCV();

            dayFrame = new Mat();

            if (featureConfig.getCamera().isDayCameraEnabled()) {
                // Асинхронна ініціалізація камери для швидкого запуску Spring Boot
                System.out.println("Starting camera initialization in background...");
                Thread.startVirtualThread(this::initializeCamera);
            } else {
                System.out.println("Day camera running in MOCK mode - no hardware initialization (disabled in config)");
                // Create empty mock frame with fixed size (1280x960)
                dayFrame = new Mat(960, 1280, org.opencv.core.CvType.CV_8UC3);
            }
        } catch (Exception e) {
            System.out.println("Error initializing camera service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SneakyThrows
    private void initializeCamera() {
        try {
            long startTime = System.currentTimeMillis();
            System.out.println("Initializing camera hardware...");

            // Спочатку спробуємо MSMF (Windows Media Foundation) - найшвидший для Windows
            // CAP_MSMF = 1400 (Windows), CAP_V4L2 = 200 (Linux)
            int backend = getOptimalBackend();
            System.out.println("Trying camera backend: " + getBackendName(backend));

            dayCamera = new VideoCapture(DAY_CAMERA_INDEX, backend);

            // Якщо не вдалося з оптимальним backend, спробуємо auto-detect
            if (!dayCamera.isOpened()) {
                System.out.println("Failed with " + getBackendName(backend) + ", trying auto-detect...");
                dayCamera = new VideoCapture(DAY_CAMERA_INDEX);
            }

            if (!dayCamera.isOpened()) {
                throw new RuntimeException("Failed to open camera");
            }

            Thread.sleep(100);
            setDayCameraProperties();
            Thread.sleep(100);

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("Camera initialized successfully in " + elapsed + "ms");
            startReadingCamera();
        } catch (Exception e) {
            System.out.println("Error initializing camera hardware: " + e.getMessage());
            e.printStackTrace();
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
        Thread.startVirtualThread(() -> {
            System.out.println("Started reading day camera");
            while (true) {
                dayCamera.read(dayFrame);
            }
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

    private void loadOpenCV() {
        try {
            nu.pattern.OpenCV.loadLocally();
            System.out.println("OpenCV loaded successfully via openpnp");
        } catch (Exception e) {
            System.err.println("Failed to load OpenCV: " + e.getMessage());
            throw new RuntimeException("Cannot initialize OpenCV", e);
        }
    }
}
