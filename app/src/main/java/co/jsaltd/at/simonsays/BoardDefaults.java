package co.jsaltd.at.simonsays;

import android.os.Build;

import com.google.android.things.pio.PeripheralManager;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public final class BoardDefaults {

    private static final String DEVICE_EDISON_ARDUINO = "edison_arduino";
    private static final String DEVICE_EDISON = "edison";
    private static final String DEVICE_JOULE = "joule";
    private static final String DEVICE_VVDN = "imx6ul_iopb";
    private static final String DEVICE_PICO = "imx6ul_pico";
    private static final String DEVICE_RPI3 = "rpi3";
    private static final String DEVICE_IMX7D = "imx7d_pico";
    private static String sBoardVariant = "";

    public static String getPwmPin() {
        switch (getBoardVariant()) {
            case DEVICE_EDISON_ARDUINO:
                return "IO3";
            case DEVICE_EDISON:
                return "GP13";
            case DEVICE_JOULE:
                return "PWM_0";
            case DEVICE_RPI3:
                return "PWM1";
            case DEVICE_PICO:
                return "PWM7";
            case DEVICE_VVDN:
                return "PWM3";
            case DEVICE_IMX7D:
                return "PWM2";
            default:
                throw new UnsupportedOperationException("Unknown device: " + Build.DEVICE);
        }
    }

    public static String getI2cBus() {
        switch (Build.DEVICE) {
            case DEVICE_RPI3:
                return "I2C1";
            case DEVICE_IMX7D:
                return "I2C1";
            default:
                throw new IllegalArgumentException("Unsupported device: " + Build.DEVICE);
        }
    }

    public static String getSpiBus() {
        switch (Build.DEVICE) {
            case DEVICE_RPI3:
                return "SPI0.0";
            case DEVICE_IMX7D:
                return "SPI3.1";
            default:
                throw new IllegalArgumentException("Unsupported device: " + Build.DEVICE);
        }
    }

    private static String getBoardVariant() {
        if (!sBoardVariant.isEmpty()) {
            return sBoardVariant;
        }
        sBoardVariant = Build.DEVICE;
        // For the edison check the pin prefix
        // to always return Edison Breakout pin name when applicable.
        if (sBoardVariant.equals(DEVICE_EDISON)) {
            PeripheralManager pioService = PeripheralManager.getInstance();
            List<String> gpioList = pioService.getGpioList();
            if (gpioList.size() != 0) {
                String pin = gpioList.get(0);
                if (pin.startsWith("IO")) {
                    sBoardVariant = DEVICE_EDISON_ARDUINO;
                }
            }
        }
        return sBoardVariant;
    }}
