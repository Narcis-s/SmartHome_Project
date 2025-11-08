#include "valve_control.h"

static const uint8_t VALVE_CONTROL_PIN = 32;  // GPIO32
static bool valveClosed = false;

void initValveControl() {
  pinMode(VALVE_CONTROL_PIN, OUTPUT);
  // în mod normal: valve open = MOSFET off = LOW pe Gate
  digitalWrite(VALVE_CONTROL_PIN, LOW);
  valveClosed = false;
}

void closeValve() {
  digitalWrite(VALVE_CONTROL_PIN, HIGH);  // MOSFET on → electrovalvă închisă
  valveClosed = true;
  Serial.println("Electrovalvă ÎNCHISĂ");
}

void openValve() {
  digitalWrite(VALVE_CONTROL_PIN, LOW);   // MOSFET off → electrovalvă deschisă
  valveClosed = false;
  Serial.println("Electrovalvă DESCHISĂ");
}

bool isValveClosed() {
  return valveClosed;
}
