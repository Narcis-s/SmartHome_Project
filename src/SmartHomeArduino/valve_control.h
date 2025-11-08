#ifndef VALVE_CONTROL_H
#define VALVE_CONTROL_H

#include <Arduino.h>

// Inițializează pinul de control al electrovalvei (MOSFET)
void initValveControl();

// Închide electrovalva (pornește MOSFET-ul, curent spre electrovalvă)
void closeValve();

// Deschide electrovalva (oprește MOSFET-ul)
void openValve();

// Returnează starea curentă: true = valve closed
bool isValveClosed();

#endif // VALVE_CONTROL_H
