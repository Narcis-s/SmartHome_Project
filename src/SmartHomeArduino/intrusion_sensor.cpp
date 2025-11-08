#include "intrusion_sensor.h"
#include "firebase_connection.h"

static bool lastStableState;
static bool lastReading;
static unsigned long lastChangeTime = 0;

void initIntrusionSensor() {
  pinMode(INTRUSION_PIN, INPUT_PULLUP);
  lastStableState = digitalRead(INTRUSION_PIN);
  lastReading = lastStableState;
}

void handleIntrusionSensor() {
  bool reading = digitalRead(INTRUSION_PIN);
  unsigned long now = millis();

  if (reading != lastReading) {
    // a apărut o schimbare de stare, resetăm timerul de debounce
    lastChangeTime = now;
    lastReading = reading;
  }
  // dacă starea s-a menținut stabilă > DEBOUNCE_MS, o acceptăm
  if (now - lastChangeTime > DEBOUNCE_MS && reading != lastStableState) {
    lastStableState = reading;
    if (lastStableState == HIGH) {
      Serial.println("[Intrusion] Atenție! Intrare neautorizată detectată!");
      writeFirebase("doorOpen", "true");
    }
    else {
      Serial.println("[Intrusion] Geamul a fost ÎNCHIS");
      writeFirebase("doorOpen", "false");
    }
  }
}
