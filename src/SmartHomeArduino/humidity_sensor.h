#ifndef HUMIDITY_SENSOR_H
#define HUMIDITY_SENSOR_H

#include <Arduino.h>

void initHumiditySensor();
float readTemperature();  // °C
float readHumidity();     // %RH
void updateHumiditySensor();

#endif
