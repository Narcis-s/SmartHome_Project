#ifndef LIGHT_CONTROL_H
#define LIGHT_CONTROL_H

void initLightControl();
void startFade(unsigned long now, int target, unsigned long duration);
void updateLightControl(unsigned long now);
void led_on();
void led_off();

#endif
