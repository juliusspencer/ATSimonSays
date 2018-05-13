package co.jsaltd.at.simonsays;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.util.Log;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.Gpio;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

	private static final String TAG = MainActivity.class.getSimpleName();

	private static final long TEXT_INCREMENT_DELAY_IN_MILLIS = 150; // how quickly to 'move' text
	private static final long PLAYBACK_DELAY_IN_MILLIS = 500; // how quickly to switch between LEDs
	private static final long NOTE_DURATION_IN_MILLIS = 150; // how long to play a sound on the buzzer
	private static final int DISPLAY_LENGTH = 4; // number of characters that can be displayed at once

	// hardware
	private AlphanumericDisplay mDisplay;
	private Speaker mSpeaker;
	private Button mButtonA;
	private Button mButtonB;
	private Button mButtonC;
	private Gpio mRedLED;
	private Gpio mGreenLED;
	private Gpio mBlueLED;
	private MessageDisplayer mDisplayRunnable;

	// state
	@IntDef({State.IDLE, State.LISTENING, State.PLAYING, State.START, State.END})
	@Retention(RetentionPolicy.SOURCE) protected @interface State {
		int IDLE = 0;
		int LISTENING = 1;
		int PLAYING = 2;
		int START = 3;
		int END = 4;
	}

	private @State int mState = State.IDLE;

	private Handler mHandler = new Handler();

	// messaging
	private String mMessage = "";
	private int mCurrentMessageStartIndex = 0;

	// sequence
	Random mRandomGenerator = new Random();
	private final List<Integer> mSequence = new ArrayList<>();
	private int mCurrentSequencePlaybackIndex = 0;

	// listening
	private int mListeningIndex = 0;
	// score
	private int mScore = 0;
	// button index
	private static final int BUTTON_A_INDEX = 2;
	private static final int BUTTON_B_INDEX = 1;
	private static final int BUTTON_C_INDEX = 0;

	// notes
	private static final double C5 = 523.25;
	private static final double D5 = 587.33;
	private static final double E5 = 659.25;

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * lifecycle
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");

		// bind up buttons, buzzer, colours and display
		configureHardware();

		mMessage = getString(R.string.instructions);
		setState(State.IDLE);
	}

	@Override protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
		ATHelper.releaseButton(mButtonA);
		ATHelper.releaseButton(mButtonB);
		ATHelper.releaseButton(mButtonC);
		ATHelper.releaseDisplay(mDisplay);
		ATHelper.releaseSpeaker(mSpeaker);
		ATHelper.releaseLed(mRedLED);
		ATHelper.releaseLed(mGreenLED);
		ATHelper.releaseLed(mBlueLED);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * hardware
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private void configureHardware() {
		// initialize display
		try {
			mDisplay = new AlphanumericDisplay(BoardDefaults.getI2cBus());
			mDisplay.setEnabled(true);
			mDisplay.display("RDY?");
			Log.d(TAG, "Initialized I2C Display");
		} catch (IOException e) {
			Log.e(TAG, "Error initializing display", e);
			return;
		}

		// configure the LEDs
		try {
			mRedLED = RainbowHat.openLedRed();
			mGreenLED = RainbowHat.openLedGreen();
			mBlueLED = RainbowHat.openLedBlue();
		} catch (IOException e) {
			Log.e(TAG, "Error initializing leds", e);
			return;
		}

		// initialize buzzer
		try {
			mSpeaker = new Speaker(BoardDefaults.getPwmPin());
			mSpeaker.stop(); // in case the PWM pin was enabled already
		} catch (IOException e) {
			Log.e(TAG, "Error initializing speaker", e);
			return; // don't initialize the handler
		}

		// initialize capacitive touch buttons
		try {
			mButtonA = RainbowHat.openButtonA();
			// Detect Button A press.
			mButtonA.setOnButtonEventListener(new Button.OnButtonEventListener() {
				@Override public void onButtonEvent(Button button, boolean pressed) {
					if (!pressed) return;
					Log.d(TAG, "Button A pressed.");
					if (mState == State.IDLE || mState == State.END) startGame();
					if(mState == State.LISTENING) checkSequence(BUTTON_A_INDEX);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			mButtonB = RainbowHat.openButtonB();
			// Detect Button B press.
			mButtonB.setOnButtonEventListener(new Button.OnButtonEventListener() {
				@Override public void onButtonEvent(Button button, boolean pressed) {
					if (!pressed) return;
					Log.d(TAG, "Button B pressed.");
					if(mState == State.LISTENING) checkSequence(BUTTON_B_INDEX);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			mButtonC = RainbowHat.openButtonC();
			// Detect Button C press.
			mButtonC.setOnButtonEventListener(new Button.OnButtonEventListener() {
				@Override public void onButtonEvent(Button button, boolean pressed) {
					if (!pressed) return;
					Log.d(TAG, "Button C pressed.");
					if(mState == State.LISTENING) checkSequence(BUTTON_C_INDEX);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Checks the button that was pressed against the sequence and progresses the game or continues listening.
	 *
	 * @param buttonIndex the index of the pressed button.
	 */
	private void checkSequence(int buttonIndex) {
		boolean correctButton;
		// check the sequence
		if(mSequence.get(mListeningIndex) == buttonIndex) {
			Log.d(TAG, "Correct.");
			correctButton = true;
		} else {
			Log.d(TAG, "Incorrect.");
			correctButton = false;
		}

		// if the end of the sequence is reached, continue on
		if(correctButton) {
			switch (buttonIndex) {
				case BUTTON_A_INDEX:
					playNote(C5, NOTE_DURATION_IN_MILLIS);
					break;
				case BUTTON_B_INDEX:
					playNote(D5, NOTE_DURATION_IN_MILLIS);
					break;
				case BUTTON_C_INDEX:
					playNote(E5, NOTE_DURATION_IN_MILLIS);
					break;

			}
			updateScore();
			if(mListeningIndex == mSequence.size() - 1) {
				mListeningIndex = 0;
				playSequence();
			} else {
				mListeningIndex++;
			}
		}

		// end the game if you make a mistake
		if(!correctButton) {
			mListeningIndex = 0;
			finishGame();
		}
	}

	/**
	 * Increments the score and displays it.
	 */
	private void updateScore() {
		mScore++;
		mMessage = String.valueOf(mScore);
		showMessage();
	}

	/**
	 * Moves the game to the END state and plays the end tune.
	 */
	private void finishGame() {
		setState(State.END);
		playEnd();
	}

	/**
	 * Updates the display and LEDs.
	 */
	private void updateHardware() {
		// disable buttons
		switch (mState) {
			case State.IDLE:
				// show the instructions
				mMessage = getString(R.string.instructions);
				showMessage();

				// show default colours
				try {
					mBlueLED.setValue(true);
					mRedLED.setValue(true);
					mGreenLED.setValue(true);
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case State.LISTENING:
				break;
			case State.PLAYING:
				showMessage();
				break;
			case State.START:
				break;
			case State.END:
				// show the instructions
				mMessage = getString(R.string.score, mScore, getMessageForCurrentLevel());
				showMessage();

				try {
					mBlueLED.setValue(true);
					mRedLED.setValue(true);
					mGreenLED.setValue(true);
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * leds
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	/**
	 * Plays the sequence by turning lights on and off.
	 */
	private Runnable mLedPlaybackRunnable = new Runnable() {
		@Override public void run() {
			// get item
			Integer item = mSequence.get(mCurrentSequencePlaybackIndex);

			// log current colour
			String colour = item == 0 ? "BLUE" : item == 1 ? "GREEN" : "RED";
			Log.d(TAG, "displaying " + colour);

			// turn on the light
			Gpio led = item == 2 ? mRedLED : item == 1 ? mGreenLED : mBlueLED;
			UpdateLEDRunnable.updateLED(led, true);

			// move to the next in the sequence
			mCurrentSequencePlaybackIndex++;
			if (mCurrentSequencePlaybackIndex < mSequence.size()) {
				// show the next led after a delay
				mHandler.postDelayed(mLedPlaybackRunnable, PLAYBACK_DELAY_IN_MILLIS);
				// clear the current one after half the delay
				mHandler.postDelayed(new UpdateLEDRunnable(led, false), PLAYBACK_DELAY_IN_MILLIS / 2);
			} else {
				// clear the lights after half the delay
				mHandler.postDelayed(new UpdateLEDRunnable(mRedLED, false), PLAYBACK_DELAY_IN_MILLIS / 2);
				mHandler.postDelayed(new UpdateLEDRunnable(mGreenLED, false), PLAYBACK_DELAY_IN_MILLIS / 2);
				mHandler.postDelayed(new UpdateLEDRunnable(mBlueLED, false), PLAYBACK_DELAY_IN_MILLIS / 2);
				// move to the listening state
				setState(State.LISTENING);
			}
		}
	};

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * state
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public void setState(@State int newState) {
		mState = newState;
		updateHardware();
	}

	/**
	 * Starts the game and plays the sequence.
	 */
	private void startGame() {
		mScore = 0;
		mSequence.clear();
		playStart();

		playSequence();
	}

	/**
	 * Plays the new sequence:
	 *  - resets the playback index
	 *  - shows the message for the current level
	 *  - resets the LEDs
	 *  - increments the sequence
	 *  - starts playback of the sequence
	 */
	private void playSequence() {
		mCurrentSequencePlaybackIndex = 0;
		if(mSequence.size() == 0) mMessage = getString(R.string.game_intro);
		else mMessage = getMessageForCurrentLevel();
		setState(State.PLAYING);

		// hide all lights
		try {
			mBlueLED.setValue(false);
			mRedLED.setValue(false);
			mGreenLED.setValue(false);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// add item to sequence
		incrementSequence();

		// start player
		startPlayer();
	}

	/**
	 * Increments the sequence
	 */
	private void incrementSequence() {
		mSequence.add(mRandomGenerator.nextInt(3));
	}

	/**
	 * Sets up playback of the whole sequence of lights.
	 */
	private void startPlayer() {
		long delay = PLAYBACK_DELAY_IN_MILLIS;
		// if the game introduction text is displaying, we want to delay playback of the sequence so the player can read the text
		boolean introDelay = mSequence.size() == 1; // check whether we should delay
		if(introDelay) delay = (getString(R.string.game_intro).length() + 8) * TEXT_INCREMENT_DELAY_IN_MILLIS;
		mHandler.postDelayed(mLedPlaybackRunnable, delay);
	}

	/**
	 * Get a message to give the user after each level
	 *
	 * @return the message
	 */
	private String getMessageForCurrentLevel() {
		String message = getString(R.string.good);
		if(mScore >= 10) message = getString(R.string.neat);
		if(mScore > 20) message = getString(R.string.allg);
		if(mScore > 30) message = getString(R.string.awsm);
		if(mScore > 40) message = getString(R.string.gold);
		if(mScore > 50) message = getString(R.string.plat);
		if(mScore > 60) message = getString(R.string.pro);
		if(mScore > 70) message = getString(R.string.mlg);
		if(mScore > 80) message = getString(R.string.mstr);
		if(mScore > 90) message = getString(R.string.leet);
		if(mScore > 100) message = getString(R.string.peng);
		if(mScore > 110) message = getString(R.string.boss);
		if(mScore > 120) message = getString(R.string.bot);
		return message;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * display
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	/**
	 * Starts to show the current message on the display
	 */
	private void showMessage() {
		if(mDisplayRunnable != null) {
			mHandler.removeCallbacks(mDisplayRunnable);
			mDisplayRunnable.setEnd();
		}
		mCurrentMessageStartIndex = 0;
		mDisplayRunnable = new MessageDisplayer(mMessage);
		mHandler.post(mDisplayRunnable);
	}

	/**
	 * Displays the message on the display. Just 4 characters at a time with a delay in showing the new ste of characters.
	 */

	class MessageDisplayer implements Runnable {

		String message;
		boolean end;

		MessageDisplayer(String message) {
			this.message = message;
		}

		void setEnd() {
			this.end = true;
		}

		@Override public void run() {
			int length = message.length();
			String text = message.substring(mCurrentMessageStartIndex);
			mCurrentMessageStartIndex = (mCurrentMessageStartIndex == length - 1) ? 0 : mCurrentMessageStartIndex + 1;
			try {
				mDisplay.display(text);
			} catch (IOException e) {
				Log.d(TAG, "error displaying message ", e);
			}
			if(length > DISPLAY_LENGTH && !end)
				mHandler.postDelayed(this, TEXT_INCREMENT_DELAY_IN_MILLIS);
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * buzzer
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	/**
	 * Plays a note for a period
	 * @param note the note to play
	 * @param duration the duration of playback
	 */
	private void playNote(double note, long duration) {
		mHandler.post(new NotePlayer(note, mSpeaker));
		mHandler.postDelayed(new Runnable() {
			@Override public void run() {
				try {
					mSpeaker.stop();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, duration);
	}

	/**
	 * Play the intro music.
	 */
	private void playStart() {
		mHandler.postDelayed(new NotePlayer(C5, mSpeaker), NOTE_DURATION_IN_MILLIS * 3);
		mHandler.postDelayed(new NotePlayer(D5, mSpeaker), NOTE_DURATION_IN_MILLIS * 6);
		mHandler.postDelayed(new NotePlayer(E5, mSpeaker), NOTE_DURATION_IN_MILLIS * 9);
		mHandler.postDelayed(new NotePlayer(0, mSpeaker), NOTE_DURATION_IN_MILLIS * 12);
	}

	/**
	 * Plays the game over music.
	 */
	private void playEnd() {
		mHandler.postDelayed(new NotePlayer(E5, mSpeaker), NOTE_DURATION_IN_MILLIS * 3);
		mHandler.postDelayed(new NotePlayer(C5, mSpeaker), NOTE_DURATION_IN_MILLIS * 6);
		mHandler.postDelayed(new NotePlayer(D5, mSpeaker), NOTE_DURATION_IN_MILLIS * 9);
		mHandler.postDelayed(new NotePlayer(0, mSpeaker), NOTE_DURATION_IN_MILLIS * 12);
	}

}
