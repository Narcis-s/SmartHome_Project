#include <Arduino.h>
#include "pir_sensor.h"
#include "light_control.h"

volatile bool motionDetected = false;
static unsigned long lastMotionTime = 0;
extern String lightMode;

void IRAM_ATTR pirISR() {
  motionDetected = true;
  Serial.println("[INFO] PIR act.");
}

void initPirSensor(uint8_t pirPin) {
  pinMode(pirPin, INPUT);
  attachInterrupt(digitalPinToInterrupt(pirPin), pirISR, RISING);
  Serial.println("[INFO] PIR sensor initialized.");
}

void handlePirSensor(unsigned long now) {
  if (motionDetected) {
    motionDetected = false;
    lastMotionTime = now;

    Serial.println("[DEBUG] Mișcare detectată de PIR!");
    if(lightMode == "auto") {
      led_on();
    }
  }
  
  // Stingere automată după 5 secunde fără mișcare
  if (now - lastMotionTime >= 5000 && lastMotionTime != 0) {
    if(lightMode == "auto") {
      led_off();
    }
    lastMotionTime = 0; // Resetăm ca să nu se tot repete stingerea
    Serial.println("[DEBUG] Nicio mișcare detectată. LED-ul se stinge.");
  }
}
