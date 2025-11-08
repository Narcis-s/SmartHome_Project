#include "blinds_control.h"
#include "firebase_connection.h"
#include <Arduino.h>

#define IN1 13
#define IN2 14
#define PWM_DUTY 110
#define RUN_DURATION 750  // 2 secunde

unsigned long motorStartTime = 0;
bool motorRunning = false;

void initBlindsMotor() {
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  stopBlinds();
}

void openBlinds() {
  analogWrite(IN1, 48);
  analogWrite(IN2, 0);
  motorStartTime = millis();
  motorRunning = true;
}

void closeBlinds() {
  analogWrite(IN1, 0);
  analogWrite(IN2, 55);
  motorStartTime = millis();
  motorRunning = true;
  Serial.println("[DEBUG] Motor pornit...");
}

void stopBlinds() {
  analogWrite(IN1, 0);
  analogWrite(IN2, 0);
  motorRunning = false;
  Serial.println("[DEBUG] Motor oprit.");
}

void updateBlinds() {
  if (motorRunning && (millis() - motorStartTime >= RUN_DURATION)) {
    stopBlinds();
  }
}

