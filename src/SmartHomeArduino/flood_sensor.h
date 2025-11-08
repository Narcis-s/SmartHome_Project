#ifndef FLOOD_SENSOR_H
#define FLOOD_SENSOR_H

#include <Arduino.h>

// Inițializează pinul senzorului de inundații
void initFloodSensor();

// Returnează true dacă este detectată apă (LOW pe senzor)
bool isFloodDetected();

#endif // FLOOD_SENSOR_H
