#ifndef BLINDS_CONTROL_H
#define BLINDS_CONTROL_H

void initBlindsMotor();
void openBlinds();
void closeBlinds();
void stopBlinds();
void updateBlinds(); 
extern bool motorRunning;


#endif
