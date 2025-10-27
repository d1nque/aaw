package vyrib1.project.aaw.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "features")
public class FeatureConfig {

    private GpioButtonsConfig gpioButtons = new GpioButtonsConfig();
    private LrfConfig lrf = new LrfConfig();
    private CameraConfig camera = new CameraConfig();
    private LoggerConfig logger = new LoggerConfig();

    @Data
    public static class GpioButtonsConfig {
        private boolean enabled = true;
    }

    @Data
    public static class LrfConfig {
        private boolean enabled = true;
    }

    @Data
    public static class CameraConfig {
        private boolean dayCameraEnabled = true;
        private boolean thermalCameraEnabled = false;
    }

    @Data
    public static class LoggerConfig {
        private boolean enabled = true;
    }
}

