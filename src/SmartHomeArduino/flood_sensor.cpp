#include "flood_sensor.h"

static const uint8_t FLOOD_SENSOR_PIN = 39;

void initFloodSensor() {
  pinMode(FLOOD_SENSOR_PIN, INPUT_PULLUP);
}

bool isFloodDetected() {
  // LOW = apă prezentă
  return digitalRead(FLOOD_SENSOR_PIN) == LOW;
}
