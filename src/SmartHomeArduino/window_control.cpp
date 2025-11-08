#include "window_control.h"
#include <Arduino.h>

#define WINDOW_CTRL_PIN 19

void initWindowControl() {
  pinMode(WINDOW_CTRL_PIN, OUTPUT);
  // Implicit LOW = fereastra închisă
  digitalWrite(WINDOW_CTRL_PIN, LOW);
  Serial.printf("[WindowControl] Pin %d configurat OUTPUT, setat LOW (închis)\n", WINDOW_CTRL_PIN);
}

void closeWindow() {
  // Semnal LOW = cere Nano-ului să închidă fereastra
  digitalWrite(WINDOW_CTRL_PIN, LOW);
  Serial.println("[WindowControl] closeWindow() → digitalWrite LOW");
}

void openWindow() {
  // Semnal HIGH = cere Nano-ului să deschidă fereastra
  digitalWrite(WINDOW_CTRL_PIN, HIGH);
  Serial.println("[WindowControl] openWindow() → digitalWrite HIGH");
}
