package vyrib1.project.aaw.services.impl;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.springframework.stereotype.Service;
import vyrib1.project.aaw.services.CameraService;
import vyrib1.project.aaw.services.GuiService;
import vyrib1.project.aaw.services.LrfService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.opencv.imgcodecs.Imgcodecs.imencode;
import static org.opencv.imgproc.Imgproc.putText;

@Service
@RequiredArgsConstructor
public class GuiServiceImpl implements GuiService {

    private final CameraService cameraService;

    private final LrfService lrfService;

    private Stage stage;
    private ImageView imageView;
    private Text overlayText;

    @Override
    @SneakyThrows
    public void startGui() {
        // Викликати JavaFX Application, наприклад через Platform.runLater або у стартовій точці JavaFX
        Platform.startup(() -> {  // якщо JavaFX ще не стартовано
            initStage();
            runLoop();
        });
    }

    private void initStage() {
        stage = new Stage();
        imageView = new ImageView();
        overlayText = new Text();
        overlayText.setStyle("-fx-fill: red; -fx-font-size: 24px;");

        Pane pane = new Pane();
        pane.getChildren().addAll(imageView, overlayText);

        Scene scene = new Scene(pane);
        stage.setScene(scene);
        stage.setTitle("Camera Feed");

        // Розтягти сцену на весь екран:
        stage.setFullScreen(true);
        // або:
        // stage.setMaximized(true);

        // Підлаштувати ImageView під розміри сцени
        imageView.setPreserveRatio(true);  // зберігати пропорції, якщо треба
        imageView.fitWidthProperty().bind(scene.widthProperty());
        imageView.fitHeightProperty().bind(scene.heightProperty());

        // Позиція тексту — лівий нижній кут:
        overlayText.layoutXProperty().set(10);
        overlayText.layoutYProperty().bind(scene.heightProperty().subtract(10));

        stage.show();
    }

    private void runLoop() {
        new Thread(() -> {
            while (true) {
                Mat mat = cameraService.getDayFrame();
                if (mat == null || mat.empty()) {
                    continue;
                }

                double dist = lrfService.getDistanceMeters();
                String text = String.format("%.2f m", dist);

                Image fxImage = matToImage(mat);

                Platform.runLater(() -> {
                    imageView.setImage(fxImage);
                    overlayText.setFont(Font.font("Helvetica", 36));
                    overlayText.setText(text);
                });

                try {
                    Thread.sleep(33); // приблизно ~30 fps
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private Image matToImage(Mat mat) {
        var buf = new org.opencv.core.MatOfByte();
        imencode(".jpg", mat, buf);
        byte[] byteArray = buf.toArray();
        try (var in = new ByteArrayInputStream(byteArray)) {
            BufferedImage bimg = ImageIO.read(in);
            WritableImage fxImg = SwingFXUtils.toFXImage(bimg, null);
            return fxImg;
        } catch (Exception e) {
            return null;
        }
    }

}
