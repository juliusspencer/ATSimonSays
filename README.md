#Simon Says game with Android Things and a RainbowHAT


This is a demo application which allows a user to play a Simon Says type of game. This is a non-production code example. Press A to re/start the game.

[Video Demo](https://www.youtube.com/watch?v=tkuKwOASQGA)

![hardware](https://github.com/juliusspencer/ATSimonSays/blob/master/doc_resources/simon_says.jpg)

##Pre-requisites

- [Raspberry Pi Model 3 B](https://www.raspberrypi.org/products/raspberry-pi-3-model-b/)
- [RainbowHAT](https://shop.pimoroni.com/products/rainbow-hat-for-android-things)
- Android Studio 2.2+

## Configuration
The application has a number of delays in it to handle things like movement of text, playing sounds and blinking LEDs.

### Text display speed
As the RainbowHAT can only display 4 characters at once, messages longer than 4 characters are “scrolled” by incrementally displaying a window of characters with a delay. The speed of the movement is based on a delay which can be configured setting the constant:
		
	TEXT_INCREMENT_DELAY_IN_MILLIS
		
### LED Game Speed
The speed that the LEDs change from one to the next is configured using:

	PLAYBACK_DELAY_IN_MILLIS

### Note Duration
The application plays some sounds (notes) to the user when the game starts, finishes and each time a correct button is pressed. This can be altered setting the unit:

	NOTE_DURATION_IN_MILLIS

### Levels
The `strings.xml` contains the levels and other messages. A level is incremented each 10 points. See: `getMessageForCurrentLevel`.

## Build and install


In Android Studio, click on the "Run" button.

If you prefer to use the command line, type

```bash
./gradlew installDebug
adb shell am start co.jsaltd.at.simonsays.myproject/.MainActivity
```

## Final note
This was thrown together over three and a half hours across two evenings so there will be a better way to handle threads and state, let
alone class files. This was hacked together to make something interactive for my children. The project was started with the [new-project-template]
(https://github.com/androidthings/new-project-template).