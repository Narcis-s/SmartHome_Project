#include <Arduino.h>
#include <Servo.h>

#define CTRL_PIN    2   // conectat la ESP32 WINDOW_CTRL_PIN
#define SERVO_PIN   9   // PWM pin pentru servo

Servo windowServo;
bool lastState;


// Pin pentru senzorul de inundație
static const uint8_t FLOOD_SENSOR_PIN   = A0;

// Pin pentru control releu (module 5 V low-level)
static const uint8_t RELAY_CONTROL_PIN  = 7;

// Pin pentru alertă către ESP32 (open-drain)
static const uint8_t ALERT_PIN          = 8;

// Interval de verificare (ms)
const unsigned long CHECK_INTERVAL = 1000;
unsigned long lastCheck = 0;

// Starea curentă a releului
static bool relayActivated = false;



void setup() {
  Serial.begin(9600);
  pinMode(CTRL_PIN, INPUT_PULLUP);
  windowServo.attach(SERVO_PIN);

  // Aplică starea inițială
  lastState = digitalRead(CTRL_PIN);
  applyState(lastState);

    pinMode(FLOOD_SENSOR_PIN, INPUT_PULLUP);

  // Releu: OUTPUT, dezactivat implicit (HIGH sau hi-Z)
  pinMode(RELAY_CONTROL_PIN, OUTPUT);
    digitalWrite(RELAY_CONTROL_PIN, HIGH);  // modul low-level: HIGH = releu OPRIT

  // Alertă: hi-Z inițial
  pinMode(ALERT_PIN, INPUT);
  Serial.println("Nano initializat");
}


bool isFloodDetected() {
  // LOW = apă detectată
  return digitalRead(FLOOD_SENSOR_PIN) == LOW;
}

void activateRelay() {
  // scade linia la GND → modul 5 V low-level activează releul
  digitalWrite(RELAY_CONTROL_PIN, LOW);
  relayActivated = true;
  Serial.println("Releu ACTIVAT (închide valva)");
}

void deactivateRelay() {
  // ridică linia la 5 V → modul dezactivează releul
  digitalWrite(RELAY_CONTROL_PIN, HIGH);
  relayActivated = false;
  Serial.println("Releu DEZACTIVAT (deschide valva)");
}

bool isRelayActivated() {
  return relayActivated;
}


void loop() {
  bool st = digitalRead(CTRL_PIN);
  if (st != lastState) {
    lastState = st;
    applyState(st);
  }
  delay(10);  // lille debounce

  unsigned long now = millis();
  if (now - lastCheck < CHECK_INTERVAL) return;
  lastCheck = now;

  bool flooded = isFloodDetected();

  // 1) Control releu
  if (flooded && !isRelayActivated())    activateRelay();
  if (!flooded && isRelayActivated())    deactivateRelay();

  // 2) Linie de alertă open-drain către ESP32
  if (flooded) {
    pinMode(ALERT_PIN, OUTPUT);
    digitalWrite(ALERT_PIN, LOW);   // trage la GND
  } else {
    pinMode(ALERT_PIN, INPUT);      // hi-Z
  }
}

void applyState(bool openSignal) {
  if (openSignal == HIGH) {
    windowServo.write(90);  // deschide
    Serial.println("Nano: openWindow()");
  } else {
    windowServo.write(0);   // închide
    Serial.println("Nano: closeWindow()");
  }
}
