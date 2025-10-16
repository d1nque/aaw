package vyrib1.project.aaw.data.domain;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import vyrib1.project.aaw.data.Constants;

public class GpioButtons {

    public final DigitalInputDevice upBtn;
    public final DigitalInputDevice downBtn;
    public final DigitalInputDevice centerBtn;
    public final DigitalInputDevice leftBtn;
    public final DigitalInputDevice rightBtn;

    public GpioButtons() {
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
    }

}
