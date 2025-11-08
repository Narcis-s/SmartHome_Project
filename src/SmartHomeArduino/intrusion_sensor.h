#ifndef INTRUSION_SENSOR_H
#define INTRUSION_SENSOR_H

#include <Arduino.h>

constexpr uint8_t INTRUSION_PIN = 4;    // GPIO4 pentru reed-switch
constexpr unsigned long DEBOUNCE_MS = 200;  // perioadă de ignorare bounce

// Apelează în setup()
void initIntrusionSensor();

// Apelează în loop() pentru detecție cu debounce
void handleIntrusionSensor();

#endif // INTRUSION_SENSOR_H
