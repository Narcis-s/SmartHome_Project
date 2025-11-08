#ifndef FAN_CONTROL_H
#define FAN_CONTROL_H

#include <Arduino.h>

#define IN3_PIN 32
#define IN4_PIN 26
// dacă vei folosi PWM la viteză:
#define ENB_PIN 35
const int HEAT_LED = 25;

extern bool fanOn;

void initFanControl();
void startFan();
void stopFan();

#endif
