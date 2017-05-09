package co.jsaltd.at.simonsays;

import android.util.Log;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.pio.Gpio;

import java.io.IOException;

/**
 * Handle standard hardware access/release tasks
 */

class ATHelper {

	private static final String TAG = ATHelper.class.getName();

	public static void releaseButton(Button button) {
		if (button != null) {
			try {
				button.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing button", e);
			}
		}
	}

	public static void releaseLed(Gpio led) {
		if(led != null) {
			try {
				led.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing colorred LED", e);
			}
		}
	}

	public static void releaseDisplay(AlphanumericDisplay display) {
		// close display
		if (display != null) {
			try {
				display.clear();
				display.setEnabled(false);
				display.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing display", e);
			} finally {
				display = null;
			}
		}

	}

	public static void releaseSpeaker(Speaker speaker) {
		// close speaker
		if (speaker != null) {
			try {
				speaker.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing speaker", e);
			} finally {
				speaker = null;
			}
		}
	}


	public static void releaseLEDStrip(Apa102 ledstrip) {
		if (ledstrip != null) {
			try {
				ledstrip.write(new int[7]);
				ledstrip.setBrightness(0);
				ledstrip.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing LED strip", e);
			}
		}

	}
}
