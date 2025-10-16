package vyrib1.project.aaw.data;

public class Constants {

    public static final class Lrf {
        public static final byte DEVICE_ADDRESS = 0x10;
        public static final byte CMD_READ_RESULT = (byte) 0x81;
    }

    public static final class GpioButtons {
        public static final int UP_BTN_PIN = 27;
        public static final int DOWN_BTN_PIN = 22;
        public static final int CENTER_BTN_PIN = 26;
        public static final int LEFT_BTN_PIN = 17;
        public static final int RIGHT_BTN_PIN = 16;
        public static final boolean USE_GPIO_BUTTONS = true;
    }


    public static final class OpenCV {
        public static final int DAY_CAMERA_INDEX = 0;
        //TODO after thermal camera implementation
        //public static final int THERMAL_CAMERA_INDEX = 2;
    }

}
