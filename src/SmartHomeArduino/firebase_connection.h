#ifndef FIREBASE_CONNECTION_H
#define FIREBASE_CONNECTION_H

#include <Arduino.h>

extern bool autoBlindsInProgress;
extern bool initialSyncDone;

// Pentru control geam
extern bool autoWindowInProgress;
extern bool initialWindowSyncDone;
extern String lightMode;

void initFirebase();
void updateFirebase();
void writeFirebase(String node, String valoare);

/**
 * Trimite mesajul descriptiv legat de inundaţie
 * în câmpul "floodMessage" din baza Firebase.
 * @param message Textul complet de publicat
 */
void writeFloodMessage(const String &message);


#endif
