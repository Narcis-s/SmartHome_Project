#include "rain_sensor.h"

#define RAIN_SENSOR_PIN 33
#define DEBOUNCE_DELAY 200  // ms

volatile bool rainInterruptFlag = false;

bool lastStableRainState = false;
unsigned long lastDebounceTime = 0;

void IRAM_ATTR rainSensorISR() {
  rainInterruptFlag = true;
}

void initRainSensor() {
  pinMode(RAIN_SENSOR_PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(RAIN_SENSOR_PIN), rainSensorISR, CHANGE);
}

bool isRaining() {
  if (rainInterruptFlag) {
    rainInterruptFlag = false;  // resetăm flagul

    unsigned long currentTime = millis();
    static bool lastRead = digitalRead(RAIN_SENSOR_PIN) == LOW;

    // debounce: așteaptă o perioadă stabilă
    if (currentTime - lastDebounceTime > DEBOUNCE_DELAY) {
      bool currentRead = digitalRead(RAIN_SENSOR_PIN) == LOW;  // LOW = plouă
      if (currentRead != lastRead) {
        lastRead = currentRead;
        lastDebounceTime = currentTime;
        lastStableRainState = currentRead;
      }
    }
  }

  return lastStableRainState;
}
