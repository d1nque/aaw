package vyrib1.project.aaw.data.domain;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vyrib1.project.aaw.data.Constants;

/**
 * Wrapper for GPIO button devices with automatic resource management.
 * Implements AutoCloseable to ensure proper cleanup of GPIO resources.
 */
public class GpioButtons implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(GpioButtons.class);

    @Getter
    private final DigitalInputDevice upBtn;
    @Getter
    private final DigitalInputDevice downBtn;
    @Getter
    private final DigitalInputDevice centerBtn;
    @Getter
    private final DigitalInputDevice leftBtn;
    @Getter
    private final DigitalInputDevice rightBtn;

    public GpioButtons() {
        logger.info("Initializing GPIO buttons");
        upBtn = new DigitalInputDevice(
                Constants.GpioButtons.UP_BTN_PIN,
                GpioPullUpDown.PULL_UP,
                GpioEventTrigger.BOTH
        );
        downBtn = new DigitalInputDevice(
                Constants.GpioButtons.DOWN_BTN_PIN,
                GpioPullUpDown.PULL_UP,
                GpioEventTrigger.BOTH
        );
        centerBtn = new DigitalInputDevice(
                Constants.GpioButtons.CENTER_BTN_PIN,
                GpioPullUpDown.PULL_UP,
                GpioEventTrigger.BOTH
        );
        leftBtn = new DigitalInputDevice(
                Constants.GpioButtons.LEFT_BTN_PIN,
                GpioPullUpDown.PULL_UP,
                GpioEventTrigger.BOTH
        );
        rightBtn = new DigitalInputDevice(
                Constants.GpioButtons.RIGHT_BTN_PIN,
                GpioPullUpDown.PULL_UP,
                GpioEventTrigger.BOTH
        );
        logger.info("GPIO buttons initialized successfully");
    }

    /**
     * Closes all GPIO button resources.
     * This method is automatically called when using try-with-resources.
     */
    @Override
    public void close() {
        logger.info("Closing GPIO button resources");
        try {
            if (upBtn != null) upBtn.close();
            if (downBtn != null) downBtn.close();
            if (centerBtn != null) centerBtn.close();
            if (leftBtn != null) leftBtn.close();
            if (rightBtn != null) rightBtn.close();
            logger.info("GPIO button resources closed successfully");
        } catch (Exception e) {
            logger.error("Error closing GPIO button resources", e);
        }
    }

}
