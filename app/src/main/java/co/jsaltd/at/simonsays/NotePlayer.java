package co.jsaltd.at.simonsays;

import android.util.Log;

import com.google.android.things.contrib.driver.pwmspeaker.Speaker;

import java.io.IOException;

/**
 * Plays sounds on a speaker with a frequency
 */

public class NotePlayer implements Runnable {

	private final double mFrequency;
	private Speaker mSpeaker;

	NotePlayer(double frequency, Speaker speaker) {
		mSpeaker = speaker;
		mFrequency = frequency;
	}

	@Override public void run() {
		if (mSpeaker == null) return;

		try {
			mSpeaker.stop();
			if(mFrequency != 0) mSpeaker.play(mFrequency);
		} catch (IOException e) {
			Log.e(NotePlayer.class.getName(), "Error playing speaker", e);
		}
	}
}