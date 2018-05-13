package co.jsaltd.at.simonsays;

import com.google.android.things.pio.Gpio;

import java.io.IOException;

/**
 * Created by julius on 13/07/17.
 */

public class UpdateLEDRunnable implements Runnable {

	private final Gpio mLed;
	private final boolean mValue;

	UpdateLEDRunnable(Gpio led, boolean value) {
		mLed = led;
		mValue = value;
	}

	@Override public void run() {
		updateLED(mLed, mValue);
	}


	protected static void updateLED(Gpio led, boolean value) {
		try {
			led.setValue(value);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
