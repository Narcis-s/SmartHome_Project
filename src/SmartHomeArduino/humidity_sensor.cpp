#include "humidity_sensor.h"
#include "firebase_connection.h"  // 🔴 Adaugat pentru a putea scrie in Firebase
#include "DHT.h"


#define DHTPIN 23
#define DHTTYPE DHT22

static DHT dht(DHTPIN, DHTTYPE);

static unsigned long lastSensorRead = 0;
const unsigned long sensorInterval = 60 * 1000;  // 60 secunde

void initHumiditySensor() {
  dht.begin();
}

float readTemperature() {
  float t = dht.readTemperature();
  if (isnan(t)) {
    return -1000.0;
  } else {
    // 🔵 Scrie temperatura in Firebase (ca String!)
    writeFirebase("temperature", String(t));
    return t;
  }
}

float readHumidity() {
  float h = dht.readHumidity();
  if (isnan(h)) {
    return -1.0;
  } else {
    // 🔵 Scrie umiditatea in Firebase (ca String!)
    writeFirebase("humidity", String(h));
    return h;
  }
}

void updateHumiditySensor() {
  if (millis() - lastSensorRead >= sensorInterval) {
    lastSensorRead = millis();

    float t = readTemperature();
    float h = readHumidity();
  }
}
