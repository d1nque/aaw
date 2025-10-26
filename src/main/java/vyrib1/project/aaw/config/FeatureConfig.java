package vyrib1.project.aaw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "features")
public class FeatureConfig {

    private GpioButtonsConfig gpioButtons = new GpioButtonsConfig();
    private LrfConfig lrf = new LrfConfig();
    private CameraConfig camera = new CameraConfig();
    private LoggerConfig logger = new LoggerConfig();

    public GpioButtonsConfig getGpioButtons() {
        return gpioButtons;
    }

    public void setGpioButtons(GpioButtonsConfig gpioButtons) {
        this.gpioButtons = gpioButtons;
    }

    public LrfConfig getLrf() {
        return lrf;
    }

    public void setLrf(LrfConfig lrf) {
        this.lrf = lrf;
    }

    public CameraConfig getCamera() {
        return camera;
    }

    public void setCamera(CameraConfig camera) {
        this.camera = camera;
    }

    public LoggerConfig getLogger() {
        return logger;
    }

    public void setLogger(LoggerConfig logger) {
        this.logger = logger;
    }

    public static class GpioButtonsConfig {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class LrfConfig {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class CameraConfig {
        private boolean dayCameraEnabled = true;
        private boolean thermalCameraEnabled = false;

        public boolean isDayCameraEnabled() {
            return dayCameraEnabled;
        }

        public void setDayCameraEnabled(boolean dayCameraEnabled) {
            this.dayCameraEnabled = dayCameraEnabled;
        }

        public boolean isThermalCameraEnabled() {
            return thermalCameraEnabled;
        }

        public void setThermalCameraEnabled(boolean thermalCameraEnabled) {
            this.thermalCameraEnabled = thermalCameraEnabled;
        }
    }

    public static class LoggerConfig {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

