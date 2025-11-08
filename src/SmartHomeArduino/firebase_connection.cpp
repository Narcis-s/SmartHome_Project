#include "firebase_connection.h"
#include <WiFi.h>
#include <HTTPClient.h>
#include "blinds_control.h"
#include "window_control.h"
#include "fan_control.h"
#include "light_control.h"
#include "config.h" // contain WIFI_SSID, WIFI_PASSWORD and DATABASE_URL

#define LED_PIN 2  // LED onboard ESP32

unsigned long lastCheck = 0;
const unsigned long interval = 1000;  // 1 secunda
static String lastActionTime = "";
String lightMode = "auto";
static String prevLightMode = "";  


// ------------------------- BLINDS ----------------------------- 
String blindsState = "";    // Starea anterioară
bool   initialSyncDone = false;   // skip la prima citire după boot
// unsigned long lastCheck     = 0;
// const unsigned long interval= 1000;     // 1s

// ------------------------ WINDOW ------------------------------
bool autoWindowInProgress = false;
String windowState = "";

void initFirebase() {
  pinMode(LED_PIN, OUTPUT);
  pinMode(25, OUTPUT);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("[Firebase] Conectare WiFi...");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\n[Firebase] WiFi conectat!");
}

void updateFirebase() {
  unsigned long now = millis();
  if (now - lastCheck >= interval) {
    lastCheck = now;

    if (WiFi.status() == WL_CONNECTED) {
      HTTPClient http;

      // Citire jaluzele
      String urlBlinds = String(DATABASE_URL) + "blinds.json";
      http.begin(urlBlinds);
      int httpCodeBlinds = http.GET();
      if (httpCodeBlinds == 200) {
        String payloadBlinds = http.getString();
        payloadBlinds.trim();

        if (!initialSyncDone) {
          blindsState = payloadBlinds;
        initialSyncDone = true;
        } else if (payloadBlinds != blindsState
                   && payloadBlinds != "\"idle\"" 
                   && !motorRunning) {          
          blindsState = payloadBlinds;
          if (payloadBlinds == "\"open\"") {
            Serial.println("[Firebase] OPEN");
            openBlinds();
          }
          else if (payloadBlinds == "\"closed\"") {
            Serial.println("[Firebase] CLOSED");
            closeBlinds();
          }
        } else {
          Serial.printf("[Firebase] Eroare GET blinds: %d\n", httpCodeBlinds);
        }
      }
      http.end();

        // Citire mode (on|off|auto)
      {
        String urlMode = String(DATABASE_URL) + "mode.json";
        http.begin(urlMode);
        int codeMode = http.GET();
        if (codeMode == 200) {
          String payloadMode = http.getString(); 
          payloadMode.trim();
          // elimină ghilimelele exterioare
          if (payloadMode.startsWith("\"") && payloadMode.endsWith("\"")) {
            payloadMode = payloadMode.substring(1, payloadMode.length()-1);
          }
          prevLightMode = lightMode;
          // Citim noua stare
          lightMode = payloadMode;
          if (lightMode == "on") {
            led_on();
          }
          else if (lightMode == "off") {
            led_off();
          }
          else if (lightMode == "auto") {
            if (prevLightMode == "on") {
              led_off();
            }
          }
          Serial.printf("[Firebase] mode changed: %s → %s\n",
                        prevLightMode.c_str(), lightMode.c_str());
        }
        http.end();
      }


      // Citire starea ferestrei (window)
      String urlWindow = String(DATABASE_URL) + "window.json";
      http.begin(urlWindow);
      int httpCodeWindow = http.GET();
      if (httpCodeWindow == 200) {
        String payloadWindow = http.getString();
        payloadWindow.trim();

        // Dacă nu e blocat de autoWindowInProgress, tratăm imediat payload-ul
        if (!autoWindowInProgress && payloadWindow != windowState) {
          windowState = payloadWindow;
          if (payloadWindow == "\"open\"") {
            openWindow();
            Serial.println("[Firebase] openWindow() apelat la citirea din Firebase");
          }
          else if (payloadWindow == "\"closed\"") {
            closeWindow();
            Serial.println("[Firebase] closeWindow() apelat la citirea din Firebase");
          }
        } 
      }
      http.end();

      
      // AC
      String urlAC = String(DATABASE_URL) + "ac.json";
      http.begin(urlAC);
      int codeAC = http.GET();
      bool acFlag = false;
      if (codeAC == 200) {
        String payload = http.getString();
        payload.trim();
        acFlag = (payload == "true");
      }
      http.end();

      // Heat
      String urlHeat = String(DATABASE_URL) + "heat.json";
      http.begin(urlHeat);
      int codeHeat = http.GET();
      bool heatFlag = false;
      if (codeHeat == 200) {
        String payload = http.getString();
        payload.trim();
        heatFlag = (payload == "true");
      }
      http.end();

      if (acFlag)       startFan();
      else if (!acFlag) stopFan();
      if(heatFlag){
        digitalWrite(25, HIGH);
        Serial.println(heatFlag);
      } else {
        digitalWrite(25, LOW);
      }
      

    } else {
      Serial.println("[Firebase] WiFi deconectat!");
    }
  }
}

//  Funcție: scrierea valorii la un nod din Firebase
void writeFirebase(String node, String valoare) {
  //Serial.println("[DEBUG] writeFirebase() apelată la nod: " + node + " cu valoarea: " + valoare);
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    String url = String(DATABASE_URL) + node + ".json";
    http.begin(url);
    http.addHeader("Content-Type", "application/json");

    int httpCode = http.PUT(valoare);
    if (httpCode > 0) {
      //Serial.printf("[Firebase] Scris la %s: %s (Cod răspuns: %d)\n", node.c_str(), valoare.c_str(), httpCode);
      String payload = http.getString();
      //Serial.println("[Firebase] Răspuns: " + payload);
    } else {
      Serial.printf("[Firebase] Eroare scriere la %s: %s\n", node.c_str(), http.errorToString(httpCode).c_str());
    }

    http.end();
  } else {
    Serial.println("[Firebase] WiFi deconectat!");
  }
}

void writeFloodMessage(const String &message) {
  String payload = "\"" + message + "\"";
  writeFirebase("floodMessage", payload);
}
