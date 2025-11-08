#include "firebase_connection.h"
#include "rain_sensor.h"
#include "window_control.h"
#include "humidity_sensor.h"
#include "fan_control.h"
#include "intrusion_sensor.h"
#include "pir_sensor.h"
#include "light_control.h"
#include "blinds_control.h"


#define PIR_PIN 16

#define BOOT_BUTTON_PIN 0  // folosește GPIO0 (butonul BOOT)

extern String windowState;
extern bool autoWindowInProgress;

bool windowClosed = false;
bool windowIsCurrentlyClosed = false;
const unsigned long SENSOR_READ_INTERVAL = 2000;
unsigned long lastSensorRead = 0;

const unsigned long RAIN_READ_INTERVAL = 1000;  // verifică la fiecare 1000 ms (1 s)
unsigned long lastRainRead = 0;

static const uint8_t ALERT_INPUT_PIN = 27;
static bool lastFloodState = false;


void setup() {
  Serial.begin(115200);
  initWindowControl();
  initFirebase();
  initRainSensor();
  initHumiditySensor();
  initFanControl();
  initIntrusionSensor();
  initPirSensor(PIR_PIN);
  initLightControl();
  initBlindsMotor();

  windowIsCurrentlyClosed = false; 
  pinMode(BOOT_BUTTON_PIN, INPUT_PULLUP);  // buton activ pe LOW
  pinMode(ALERT_INPUT_PIN, INPUT_PULLUP);
  //--------
  lastFloodState = (digitalRead(ALERT_INPUT_PIN) == LOW);
  if (lastFloodState) {
    writeFloodMessage("Inundatie detectata. Sursa de apa oprita");
  } else {
    writeFloodMessage("Sursa apa pornita");
  }
  //--------
}

void loop() {
  unsigned long currentMillis = millis();
  // 1) Control geam
  if (currentMillis - lastRainRead >= RAIN_READ_INTERVAL) {
    lastRainRead = currentMillis;
    bool ploaie = isRaining();
    Serial.print("[Loop] isRaining() = ");
    Serial.println(ploaie ? "DA" : "NU");
  

    if (ploaie) {
      // Dacă plouă și fereastra nu e deja închisă, închidem și scriem în Firebase
      if (!windowIsCurrentlyClosed) {
        Serial.println("[AUTO-WINDOW] Detectata ploaie => inchidere fereastra");
        closeWindow();
        autoWindowInProgress = true;
        // Scriem în Firebase starea "closed"
        writeFirebase("window", "\"closed\"");
        windowState = "\"closed\"";
      }
    } else {
    // Dacă nu mai plouă și a fost o închidere automată, permitem din nou comenzi din Firebase
      if (autoWindowInProgress) {
        Serial.println("[AUTO-WINDOW] Ploaia a incetat => permit comenzi manuale");
        autoWindowInProgress = false;
      }
    }
  }


  updateBlinds();  //  verifică dacă au trecut 2 secunde și oprește motorul
  updateFirebase();

  // 2. Citire senzor temperatură și umiditate periodic

  if (currentMillis - lastSensorRead >= SENSOR_READ_INTERVAL) {
    lastSensorRead = currentMillis;

    float h = readHumidity();
    float t = readTemperature();

    Serial.printf("[Sensor] Temp = %.1f °C, Umiditate = %.1f %%\n", t, h);

  }    

  updateHumiditySensor();
  unsigned long now = millis();

  // 3. Prevenire inundatii

  bool flooded = (digitalRead(ALERT_INPUT_PIN) == LOW);
  if (flooded != lastFloodState) {
    lastFloodState = flooded;
    if (flooded) {
      writeFloodMessage("Inundație detectată. Sursă de apă oprită");
      Serial.println("Inundație detectată. Sursă de apă oprită");
    } else {
      writeFloodMessage("Sursă apă pornită");
      Serial.println("Sursă apă pornită");
    }
  }

  // 4. Intrusion allert

  handleIntrusionSensor();

  // 5. Light Control

  handlePirSensor(now);
  extern String lightMode;
  if (lightMode == "auto") {
    updateLightControl(now);
  }

  // 6

  updateBlinds();  //  verifică dacă au trecut 2 secunde și oprește motorul


}






