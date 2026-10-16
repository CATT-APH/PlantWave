# PlantWave

An app for the APH Monarch that draws a plant's electrical signal as a tactile graph, so blind and low-vision students can explore it by touch. Built for the Center for Assistive Technology Training Northwest (CATT-NW) at the Washington State School for the Blind.

A note on the name: this project is called PlantWave because it grew out of work with the PlantWave plant-music device. This app does not read from that device. It reads the plant electrode signal directly through an Arduino. The project is not made by or affiliated with the makers of the PlantWave device.

## What it does

- Starts and stops reading from the app. The app sends START and STOP to the Arduino.
- Graphs the plant's signal on the Monarch's tactile display as data comes in.
- Draws the graph in fixed-length pages instead of a sliding window, so the line stays still long enough to follow by touch.
- Scales the vertical axis automatically to fit the readings.
- Has two views, Live and Overview. Switch between them with the D-pad Up and Down keys.
- Double-tap a point on the graph to hear its value. The current reading in millivolts also shows on the info row.

## What you need

Hardware:
- Plant electrodes with a 3.5 mm plug
- A 3.5 mm breakout board
- A 1 megohm resistor
- A breadboard and jumper wires
- An Arduino Uno
- A USB cable from the Arduino to the Monarch
- An APH Monarch with developer access

Software:
- Arduino IDE
- Android Studio
- Access to the Monarch SDK through APH. You need an APH deploy token to build this app. It is not included here, and you must have one before you start.

## Wiring

- Connect one end of the 1 megohm resistor to the Arduino's 5 V pin.
- Connect the other end of the resistor to pin A0, and also to the tip pin on the breakout board.
- Connect the sleeve pin on the breakout board to GND.
- Plug the electrode cable into the breakout board.

With nothing connected to the electrodes, A0 reads close to 1023 (about 1015 to 1023).

## Setup

1. Wire the circuit as described above.
2. Open `arduino/plantwave/plantwave.ino` in the Arduino IDE and upload it to the Uno.
3. **Important: USB debugging must be turned on on the Monarch, or the app cannot be installed or run.** Turn on developer options, then USB debugging.
4. In the project's root folder, copy `keystore.properties.example` to a new file named `keystore.properties`. Replace `YOUR_TOKEN_HERE` with your APH deploy token. You need to already have this token. Never commit this file to GitHub.
5. Connect the Monarch to your computer by USB and accept the "Allow USB debugging?" prompt on the Monarch.
6. From the project's root folder, build and install the app:
   - Windows: `gradlew.bat installDebug`
   - Mac or Linux: `./gradlew installDebug`
   You can also use the Run button in Android Studio. If Gradle can't find Java, point `JAVA_HOME` to the JDK that comes with Android Studio.
7. Connect the Arduino to the Monarch with the USB cable.
8. Open PlantWave on the Monarch and start a reading.

## How it works

When the Arduino receives START, it takes 80 quick readings of pin A0 and averages them, which smooths out electrical noise from power lines. It sends the average as one whole number per line over USB serial at 9600 baud, about every half second, until it receives STOP.

The app reads the data over USB using the open-source usb-serial-for-android library. It throws out any value outside 0 to 1023, since a real reading can't fall outside that range. It converts each reading to millivolts with mV = reading / 1024 x 5000 (a 5 V reference) and draws it on the graph. With the 1 megohm resistor, readings typically sit around 4180 to 4200 mV.

## Troubleshooting

- If the reading stays at 1023 no matter what you do, the 3.5 mm plug is probably not making contact in the breakout board. Check that the plug fits the board and is pushed in all the way. A quick test: touch the two electrode ends, one in each hand. The reading should drop.
- If data starts arriving before you start a reading, the Arduino is still sampling from an earlier session. Stop the reading in the app or press the reset button on the Arduino.
- If Gradle fails to download the Monarch SDK with a 401 error, check your deploy token. If the SDK has already been downloaded once, you can build with `gradlew.bat installDebug --offline`.
- If Android Studio doesn't see the Monarch, check that USB debugging is still on. It can turn off after a firmware update. Unplug, plug back in, and accept the prompt again.

## Known issues and next steps

- A live stats box (current, minimum and maximum values) is built but not yet connected to the display.
- The Monarch's Zoom keys are reserved by its screen reader, so the app uses the D-pad to switch views.
- On graph screens, the screen reader blocks single taps. Only double-taps reach the app.
- 24-hour sampling is planned. It will need downsampling so a long recording fits on the Monarch's display.
- Reading a PlantWave device directly over USB MIDI is an idea that has not been tested.
- The hardware is a hand-wired prototype. A custom circuit board would make it easier to build and more durable for classroom use.

## Credits

Built for CATT-NW at the Washington State School for the Blind, with the American Printing House for the Blind (APH). Thank you to Dr. Yue-Ting Siu, CATT-NW Coordinator, and Dr. Joshua Miele for their guidance and feedback.

## License

MIT License. See the LICENSE file.

## Contact

CATT-NW: cattnw@wssb.wa.gov
