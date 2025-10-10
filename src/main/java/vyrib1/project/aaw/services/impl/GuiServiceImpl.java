package vyrib1.project.aaw.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.springframework.stereotype.Service;
import vyrib1.project.aaw.services.CameraService;
import vyrib1.project.aaw.services.GuiService;
import vyrib1.project.aaw.services.LrfService;

import javax.swing.JFrame;

import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX;
import static org.opencv.imgproc.Imgproc.LINE_AA;
import static org.opencv.imgproc.Imgproc.putText;

@Service
@RequiredArgsConstructor
public class GuiServiceImpl implements GuiService {

    private final CameraService cameraService;

    private final LrfService lrfService;

    private double lastDistance = 0.0;

    @Override
    @SneakyThrows
    public void startGui() {
        Thread.sleep(2000);
        CanvasFrame canvas = new CanvasFrame("Camera Feed");
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

        // Додавання тексту в лівому нижньому куті
        int font = FONT_HERSHEY_SIMPLEX;
        double fontScale = 1.0;
        int thickness = 2;
        Scalar color = new Scalar(0, 0, 255, 0);
        int x = 10;
        int y = cameraService.getDayFrame().rows() - 10;

        Thread.startVirtualThread(() -> {
            Frame frame;
            while (true) {
                frame = converter.convert(cameraService.getDayFrame());
                canvas.showImage(frame);
                putText(cameraService.getDayFrame(), lrfService.getDistanceMeters() + "m", new Point(x, y), font, fontScale, color, thickness, LINE_AA, false);
                //if(lastDistance != lrfService.getDistanceMeters()){
                //lastDistance = lrfService.getDistanceMeters();
                //}
            }
        });
    }

}
