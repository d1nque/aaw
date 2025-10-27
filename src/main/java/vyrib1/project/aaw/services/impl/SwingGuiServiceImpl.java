package vyrib1.project.aaw.services.impl;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;
import vyrib1.project.aaw.data.BallisticConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static org.opencv.imgproc.Imgproc.LINE_AA;

/**
 * Swing-based GUI service for displaying camera feed with crosshair and text overlay.
 * Designed to work reliably on Raspberry Pi 5 with both X11 and Wayland.
 */
@Component
public class SwingGuiServiceImpl {

    private JFrame frame;
    private VideoPanel videoPanel;
    private volatile boolean running = false;

    /**
     * Creates and displays the main GUI window
     */
    public void createAndShowGUI(String windowTitle, int width, int height, boolean fullscreen) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame(windowTitle);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            videoPanel = new VideoPanel();
            frame.add(videoPanel);
            
            if (fullscreen) {
                // Full screen mode without decorations
                frame.setUndecorated(true);
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            } else {
                frame.setSize(width, height);
            }
            
            frame.setVisible(true);
            running = true;
            
            System.out.println("Swing GUI window created and displayed");
        });
    }

    /**
     * Updates the displayed frame with crosshair, aim circle and text overlay
     */
    public void updateFrame(Mat frame, int crosshairX, int crosshairY, 
                           String distanceText, String angleText,
                           int aimCircleX, int aimCircleY) {
        if (!running || videoPanel == null || frame == null || frame.empty()) {
            return;
        }

        try {
            // Clone frame to avoid modifying original
            Mat displayFrame = frame.clone();
            
            // Resize to fit panel
            Size targetSize = new Size(videoPanel.getWidth(), videoPanel.getHeight());
            if (targetSize.width > 0 && targetSize.height > 0) {
                Imgproc.resize(displayFrame, displayFrame, targetSize);
                
                // Scale crosshair coordinates proportionally
                double scaleX = targetSize.width / 720.0;
                double scaleY = targetSize.height / 576.0;
                int scaledCrosshairX = (int)(crosshairX * scaleX);
                int scaledCrosshairY = (int)(crosshairY * scaleY);
                
                // Scale aim circle coordinates proportionally
                int scaledAimX = (int)(aimCircleX * scaleX);
                int scaledAimY = (int)(aimCircleY * scaleY);
                
                // Draw crosshair
                drawCrosshair(displayFrame, scaledCrosshairX, scaledCrosshairY);
                
                // Draw aim circle (yellow circle showing ballistic lead point)
                drawAimCircle(displayFrame, scaledAimX, scaledAimY);
                
                // Draw text overlay
                drawTextOverlay(displayFrame, distanceText, angleText);
            }
            
            // Convert Mat to BufferedImage
            BufferedImage image = matToBufferedImage(displayFrame);
            
            // Update panel
            SwingUtilities.invokeLater(() -> {
                videoPanel.updateImage(image);
            });
            
            displayFrame.release();
        } catch (Exception e) {
            System.err.println("Error updating frame: " + e.getMessage());
        }
    }

    /**
     * Draws crosshair on the frame
     */
    private void drawCrosshair(Mat frame, int x, int y) {
        Scalar crosshairColor = new Scalar(0, 0, 255); // Red in BGR
        int thickness = 2;
        int length = 35;
        
        // Horizontal line
        Imgproc.line(frame,
                new Point(x - length, y),
                new Point(x + length, y),
                crosshairColor, thickness, LINE_AA);
        
        // Vertical line
        Imgproc.line(frame,
                new Point(x, y - length),
                new Point(x, y + length),
                crosshairColor, thickness, LINE_AA);
    }

    /**
     * Draws aim circle (ballistic lead indicator) on the frame
     */
    private void drawAimCircle(Mat frame, int x, int y) {
        // Import constants from BallisticConstants
        Scalar circleColor = vyrib1.project.aaw.data.BallisticConstants.AIM_CIRCLE_COLOR;
        int radius = vyrib1.project.aaw.data.BallisticConstants.AIM_CIRCLE_RADIUS;
        int thickness = vyrib1.project.aaw.data.BallisticConstants.AIM_CIRCLE_THICKNESS;
        
        // Draw circle at ballistic aim point
        Imgproc.circle(frame,
                new Point(x, y),
                radius,
                circleColor,
                thickness,
                LINE_AA);
    }

    /**
     * Draws text overlay (distance, angle, target speed)
     */
    private void drawTextOverlay(Mat frame, String distanceText, String angleText) {
        int font = Imgproc.FONT_HERSHEY_SIMPLEX;
        double fontScale = 1.0;
        int thickness = 2;
        Scalar textColor = new Scalar(0, 0, 255); // Red in BGR
        int yPosition = frame.rows() - 30;
        
        // Draw distance
        Imgproc.putText(frame, distanceText,
                new Point(10, yPosition), font, fontScale, textColor, thickness, LINE_AA, false);
        
        // Draw angle
        Imgproc.putText(frame, angleText,
                new Point(235, yPosition), font, fontScale, textColor, thickness, LINE_AA, false);

        // TODO Delete
        // Draw speed
        Imgproc.putText(frame, BallisticConstants.TARGET_SPEED_KMH + "km/h",
                new Point(365, yPosition), font, fontScale, textColor, thickness, LINE_AA, false);
    }

    /**
     * Converts OpenCV Mat to BufferedImage for Swing display
     */
    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] buffer = new byte[bufferSize];
        mat.get(0, 0, buffer);
        
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
        
        return image;
    }

    /**
     * Checks if GUI is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Closes the GUI window and releases resources
     */
    public void close() {
        running = false;
        if (frame != null) {
            SwingUtilities.invokeLater(() -> {
                frame.dispose();
                System.out.println("Swing GUI window closed");
            });
        }
    }

    /**
     * Custom JPanel for displaying video frames
     */
    private static class VideoPanel extends JPanel {
        private BufferedImage image;

        public VideoPanel() {
            setBackground(Color.BLACK);
        }

        public void updateImage(BufferedImage img) {
            this.image = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                // Draw image to fit panel while maintaining aspect ratio
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(image, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }
}

