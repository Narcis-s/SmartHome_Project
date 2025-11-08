#include "fan_control.h"
#include <Arduino.h>


void initFanControl() {
  pinMode(IN3_PIN, OUTPUT);
  pinMode(IN4_PIN, OUTPUT);
  digitalWrite(IN3_PIN, LOW);
  digitalWrite(IN4_PIN, LOW);
  pinMode(ENB_PIN, OUTPUT);
  digitalWrite(ENB_PIN, LOW);
}

void startFan() {
  digitalWrite(ENB_PIN, HIGH);
  Serial.println("Fan ON");
  digitalWrite(IN3_PIN, LOW);
  digitalWrite(IN4_PIN, HIGH);

}

void stopFan() {
  digitalWrite(ENB_PIN, LOW);
  Serial.println("Fan OFF");
  digitalWrite(IN3_PIN, LOW);
  digitalWrite(IN4_PIN, LOW);
}
