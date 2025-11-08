#include <Arduino.h>
#include <Wire.h>
#include <BH1750.h>
#include "light_control.h"

BH1750 lightMeter(0x23);

const uint8_t LED_PIN = 5;

int currentBrightness = 0;
int targetBrightness = 0;
unsigned long fadeStartTime = 0;
unsigned long fadeDuration = 0;
int initialBrightness = 0;
uint16_t brightness;

void initLightControl() {
  pinMode(LED_PIN, OUTPUT);
  Wire.begin(21, 22);  // SDA -> D21, SCL -> D22
  if (lightMeter.begin()) {
    Serial.println("Senzor BH1750 inițializat cu succes.");
  } else {
    Serial.println("Eroare la inițializarea senzorului BH1750.");
  }
}

void startFade(unsigned long now, int target, unsigned long duration) {
  fadeStartTime = now;
  fadeDuration = duration;
  targetBrightness = target;
  initialBrightness = currentBrightness;
}

void updateLightControl(unsigned long now) {
  if (fadeDuration == 0) return;

  unsigned long elapsed = now - fadeStartTime;
  if (elapsed >= fadeDuration) {
    currentBrightness = targetBrightness;
    analogWrite(LED_PIN, currentBrightness);
    fadeDuration = 0;
    return;
  }

  float progress = (float)elapsed / fadeDuration;
  currentBrightness = initialBrightness + (targetBrightness - initialBrightness) * progress;
  analogWrite(LED_PIN, currentBrightness);
}

void led_on() {
  if (lightMeter.readLightLevel() > 1000) {
    brightness = 100;
  }
  else if(lightMeter.readLightLevel() < 50) {
    brightness = 255;
  }
  else {
    brightness = (uint8_t)(((-163L * lightMeter.readLightLevel()) / 1000) + 263);
  }
  analogWrite(LED_PIN, brightness);
  Serial.println("[DEBUG] Led Aprins!");
}
void led_off() {
  analogWrite(LED_PIN, 0);
  Serial.println("[DEBUG] Led Stins!");
}