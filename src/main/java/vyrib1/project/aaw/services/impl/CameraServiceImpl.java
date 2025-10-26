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
    private Mat dayFrame = new Mat();

    //TODO after thermal camera implementation
    //private VideoCapture thermalCamera;
    //private Mat thermalFrame = new Mat();

    @SneakyThrows
    public CameraServiceImpl(FeatureConfig featureConfig) {
        this.featureConfig = featureConfig;
        
        if (featureConfig.getCamera().isDayCameraEnabled()) {
            dayCamera = new VideoCapture(DAY_CAMERA_INDEX);
            Thread.sleep(500);
            setDayCameraProperties();
            Thread.sleep(1000);
            startReadingCamera();
        } else {
            System.out.println("Day camera running in MOCK mode - no hardware initialization (disabled in config)");
            // Create empty mock frame with fixed size (1280x960)
            dayFrame = new Mat(960, 1280, org.opencv.core.CvType.CV_8UC3);
        }
        //TODO after thermal camera implementation
        //if (featureConfig.getCamera().isThermalCameraEnabled()) {
        //    this.thermalCamera = new VideoCapture(2);
        //    Thread.sleep(500);
        //    setThermalCameraProperties();
        //    Thread.sleep(1000);
        //    startReadingThermalCamera();
        //}
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
}
