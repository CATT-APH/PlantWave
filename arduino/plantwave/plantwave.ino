// PlantWave Arduino sketch
//
// Reads the plant electrode on pin A0 and sends readings to the Monarch app
// over USB serial at 9600 baud, one whole number per line.
//
// Wiring: 5V -> 1 megohm resistor -> A0 and the breakout's tip pin.
//         Breakout sleeve pin -> GND.
//
// Commands from the app (one per line):
//   START  begin sending readings, about every half second
//   STOP   stop sending readings

bool sampling = false;

int readAvg() {
  long sum = 0;
  for (int i = 0; i < 80; i++) {
    sum += analogRead(A0);
    delayMicroseconds(250);   // 80 reads x ~360us = ~29 ms, covers a full mains cycle
  }
  return sum / 80;
}

void setup() {
  Serial.begin(9600);
}

void loop() {
  if (Serial.available()) {
    String cmd = Serial.readStringUntil('\n');
    cmd.trim();
    if (cmd == "START") {
      sampling = true;
    } else if (cmd == "STOP") {
      sampling = false;
    }
  }

  if (sampling) {
    int value = readAvg();
    Serial.println(value);
    delay(500);
  }
}
