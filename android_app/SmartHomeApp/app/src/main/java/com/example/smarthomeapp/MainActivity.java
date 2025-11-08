package com.example.smarthomeapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.SharedPreferences;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // SharedPreferences
    private static final String PREFS_NAME      = "climate_prefs";
    private static final String KEY_DESIRED_TEMP = "desired_temp";
    private static final String KEY_DESIRED_HUM  = "desired_hum";
    private static final String KEY_AUTO_MODE    = "auto_mode";
    private static final String KEY_LIGHT_AUTO = "light_auto";

    private boolean ignoreSwitchCallback = false;
    private SwitchCompat switchLight, switchWindow, switchBlinds, switchHeat, switchAC;
    private TextView textSensorData;
    private String currentTime;  // 🔵 Variabilă globală pentru ora actuală
    private DatabaseReference currentTimeRef;
    // 🔵 Referințe Firebase
    private DatabaseReference ledRef, tempRef, humRef;
    private DatabaseReference lightModeRef;
    private CheckBox checkboxLightAuto;
    private boolean ignoreLightCallbacks = false;
    DatabaseReference blindsRef;
    private EditText editOpenTime, editCloseTime;
    private Button btnSaveTimes;
    private DatabaseReference openTimeRef, closeTimeRef;
    private String blindsLocalState    = "";      // “open”, “closed” sau “idle”
    private String lastBlindsActionTime = "";
    private static final long BLINDS_MOTOR_RUNTIME_MS = 1500;  // milisecunde (>= RUN_DURATION de pe ESP32)
    private Handler uiHandler;  // handler pentru postDelayed

    // pentru doorOpen
    private DatabaseReference doorOpenRef;
    private TextView textDoorStatus;
    private boolean lastDoorOpen = false;

    // canal și ID notificare
    private static final String INTRUSION_CHANNEL_ID = "intrusion_alerts_ch";
    private static final int INTRUSION_NOTIF_ID    = 2001;

    // 🔵 Variabile pentru afișarea temperaturii și umidității
    private String currentTemp = "--";
    private String currentHum = "--";
    private DatabaseReference windowRef;

    private static final String CHANNEL_ID = "flood_alerts_ch";
    private TextView textFloodMessage;
    private DatabaseReference floodMessageRef;
    private static final int NOTIF_ID = 1001;
    private DatabaseReference isNearHomeRef;
    private float desiredTemp, desiredHum;
    private boolean autoMode;
    private SwitchCompat switchAutoClimate;
    private EditText editDesiredTemp, editDesiredHum;
    private Button   btnSaveDesired;

    // Noduri noi în Firebase
    private DatabaseReference heatRef, acRef;

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Flood Alerts";
            String description = "Notifications for flood detection";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);

            CharSequence name2 = "Intrusion Alerts";
            String    desc2 = "Notifications for door intrusions";
            NotificationChannel channel2 = new NotificationChannel(
                    INTRUSION_CHANNEL_ID,
                    name2,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel2.setDescription(desc2);
            nm.createNotificationChannel(channel2);
        }
    }

    private EditText editHomeLat, editHomeLng;
    private Button btnSaveHomeLocation;
    private TextView textDistance;
    private DatabaseReference homeLatRef, homeLngRef;

    private FusedLocationProviderClient fusedLocationClient;
    private double homeLat = 0.0, homeLng = 0.0;
    private Handler distanceHandler;
    private Runnable distanceUpdater;
    private boolean isNearHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        uiHandler = new Handler(Looper.getMainLooper());
        // 🔵 Inițializare UI
        initUI();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        desiredTemp = prefs.getFloat(KEY_DESIRED_TEMP, 22.0f);
        desiredHum  = prefs.getFloat(KEY_DESIRED_HUM, 50.0f);
        autoMode    = prefs.getBoolean(KEY_AUTO_MODE, false);
        boolean lightAuto = prefs.getBoolean(KEY_LIGHT_AUTO, false);
        checkboxLightAuto.setChecked(lightAuto);



        // Populează UI
        editDesiredTemp.setText(String.valueOf(desiredTemp));
        editDesiredHum .setText(String.valueOf(desiredHum));
        switchAutoClimate.setChecked(autoMode);

        // 🔵 Pornim actualizarea orei
        startClockUpdater();

        // 🔵 Inițializăm Firebase
        initFirebase();

        // 🔵 Ascultă modificările în Firebase
        setupFirebaseListeners();

        // 🔵 Ascultă modificările de la switch-ul UI
        setupSwitchListener();

        createNotificationChannel();

        // 🔴 Sincronizare inițială window la pornire
        windowRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String windowState = snapshot.getValue(String.class);
                if (windowState != null) {
                    switchWindow.setChecked(windowState.equals("open"));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });



        setupSaveTimes();

        // 🔴 Încarcă valorile actuale la pornire
        openTimeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String openTime = snapshot.getValue(String.class);
                if (openTime != null) {
                    editOpenTime.setText(openTime);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        closeTimeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String closeTime = snapshot.getValue(String.class);
                if (closeTime != null) {
                    editCloseTime.setText(closeTime);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        //------
        // 1. Inițializează clientul de locație
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 2. Găsește view-urile noi
        editHomeLat        = findViewById(R.id.editHomeLat);
        editHomeLng        = findViewById(R.id.editHomeLng);
        btnSaveHomeLocation= findViewById(R.id.btnSaveHomeLocation);
        textDistance       = findViewById(R.id.textDistance);

        // 3. Inițializează referințe Firebase pentru casa
        homeLatRef = FirebaseDatabase.getInstance()
                .getReference("homeLatitude");
        homeLngRef = FirebaseDatabase.getInstance()
                .getReference("homeLongitude");

        // 4. Încarcă de la pornire coordonatele casei în EditText
        homeLatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                Double v = snap.getValue(Double.class);
                if (v != null) editHomeLat.setText(String.valueOf(v));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
        homeLngRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                Double v = snap.getValue(Double.class);
                if (v != null) editHomeLng.setText(String.valueOf(v));
                // După ce avem ambele, le stocăm local
                loadHomeCoordsLocal();
                updateDistance();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        // 5. Butonul „Salvează casa”
        btnSaveHomeLocation.setOnClickListener(v -> {
            String latS = editHomeLat.getText().toString().trim();
            String lngS = editHomeLng.getText().toString().trim();
            if (latS.isEmpty() || lngS.isEmpty()) {
                Toast.makeText(this, "Completează ambele coordonate!", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                double lat = Double.parseDouble(latS);
                double lng = Double.parseDouble(lngS);
                homeLatRef.setValue(lat);
                homeLngRef.setValue(lng);
                homeLat = lat;
                homeLng = lng;
                Toast.makeText(this, "Locație casă salvată!", Toast.LENGTH_SHORT).show();
                //updateDistance();  // afișează imediat
            } catch (NumberFormatException ex) {
                Toast.makeText(this, "Coordonate invalide!", Toast.LENGTH_SHORT).show();
            }
        });

        // 6. La pornire, nu uita să calculezi distanța o dată
        updateDistance();
        //------

        startDistanceUpdater();

        btnSaveDesired.setOnClickListener(v -> {
            try {
                desiredTemp = Float.parseFloat(editDesiredTemp.getText().toString().trim());
                desiredHum  = Float.parseFloat(editDesiredHum .getText().toString().trim());

                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putFloat(KEY_DESIRED_TEMP, desiredTemp)
                        .putFloat(KEY_DESIRED_HUM,  desiredHum)
                        .apply();

                Toast.makeText(this, "Setări climatizare salvate!", Toast.LENGTH_SHORT).show();

                if (autoMode) evaluateClimate();
            } catch (NumberFormatException ex) {
                Toast.makeText(this, "Introdu valori corecte!", Toast.LENGTH_SHORT).show();
            }
        });

        switchHeat.setOnCheckedChangeListener((b, on) -> {
            if (!autoMode) {
                heatRef.setValue(on);
            } else {
                // dacă am făcut toggle manual, ieșim din auto
                switchAutoClimate.setChecked(false);
            }
        });
        switchAC.setOnCheckedChangeListener((b, on) -> {
            if (!autoMode) {
                acRef.setValue(on);
            } else {
                switchAutoClimate.setChecked(false);
            }
        });

        lightModeRef.get().addOnSuccessListener(snap -> {
            String mode = snap.getValue(String.class);
            if (mode != null) {
                ignoreLightCallbacks = true;
                switchLight.setChecked(mode.equals("on"));
                checkboxLightAuto.setChecked(!mode.equals("off"));
                ignoreLightCallbacks = false;
            }
        });



    }

    // 🔵 Inițializare elemente UI
    private void initUI() {
        switchLight = findViewById(R.id.switchLight);
        checkboxLightAuto= findViewById(R.id.checkboxLightAuto);
        switchWindow = findViewById(R.id.switchWindow);
        switchBlinds = findViewById(R.id.switchBlinds);
        switchHeat = findViewById(R.id.switchHeat);
        switchAC = findViewById(R.id.switchAC);
        textSensorData = findViewById(R.id.textSensorData);
        editOpenTime = findViewById(R.id.editOpenTime);
        editCloseTime = findViewById(R.id.editCloseTime);
        btnSaveTimes = findViewById(R.id.btnSaveTimes);
        textFloodMessage = findViewById(R.id.textFloodMessage);
        floodMessageRef = FirebaseDatabase.getInstance().getReference("floodMessage");
        switchAutoClimate = findViewById(R.id.switchAutoClimate);
        editDesiredTemp = findViewById(R.id.editDesiredTemp);
        editDesiredHum  = findViewById(R.id.editDesiredHum);
        btnSaveDesired  = findViewById(R.id.btnSaveDesired);
        textDoorStatus = findViewById(R.id.textDoorStatus);

    }

    // 🔵 Inițializare referințe Firebase
    private void initFirebase() {
        ledRef = FirebaseDatabase.getInstance().getReference("led");
        tempRef = FirebaseDatabase.getInstance().getReference("temperature");
        humRef = FirebaseDatabase.getInstance().getReference("humidity");
        blindsRef = FirebaseDatabase.getInstance().getReference("blinds");
        openTimeRef = FirebaseDatabase.getInstance().getReference("blindsOpenTime");
        closeTimeRef = FirebaseDatabase.getInstance().getReference("blindsCloseTime");
        windowRef = FirebaseDatabase.getInstance().getReference("window");
        isNearHomeRef = FirebaseDatabase.getInstance().getReference("isNearHome");
        heatRef = FirebaseDatabase.getInstance().getReference("heat");
        acRef   = FirebaseDatabase.getInstance().getReference("ac");
        doorOpenRef = FirebaseDatabase.getInstance().getReference("doorOpen");
        lightModeRef = FirebaseDatabase.getInstance().getReference("mode");

    }

    // 🔵 Ascultă modificările din Firebase și actualizează UI
    private void setupFirebaseListeners() {
        // 🔵 Ascultă LED

        lightModeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                String mode = snap.getValue(String.class);
                if (mode == null) return;

                ignoreLightCallbacks = true;
                // switch = ON doar când mode == "on"
                switchLight.setChecked(mode.equals("on"));
                ignoreLightCallbacks = false;
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

//        ledRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Boolean ledState = snapshot.getValue(Boolean.class);
//                if (ledState != null) {
//                    switchLight.setChecked(ledState);
//                    evaluateClimate();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(MainActivity.this, "Eroare la citire LED.", Toast.LENGTH_SHORT).show();
//            }
//        });

        // 🔵 Ascultă temperatura
        tempRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object temp = snapshot.getValue();
                if (temp != null) {
                    updateSensorDisplay(temp.toString(), null);
                    evaluateClimate();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Eroare la citire temperatură: " + error.getMessage());
            }
        });

        // 🔵 Ascultă umiditatea
        humRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object hum = snapshot.getValue();
                if (hum != null) {
                    updateSensorDisplay(null, hum.toString());
                    evaluateClimate();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Eroare la citire umiditate: " + error.getMessage());
            }
        });

        blindsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // citește starea din Firebase
                String state = snapshot.getValue(String.class);
                if (state == null) return;

                // salvează-o local
                blindsLocalState = state;

                // dacă e "idle", nu schimba UI
                if (state.equals("idle")) return;

                // altfel, actualizează switch-ul fără să reapelăm sendBlindsCommand
                ignoreSwitchCallback = true;
                switchBlinds.setChecked(state.equals("open"));
                ignoreSwitchCallback = false;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // 🔵 Ascultă starea ferestrei (window)
        windowRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String windowState = snapshot.getValue(String.class); // “open” sau “closed”
                if (windowState != null) {
                    boolean isOpen = windowState.equals("open");
                    // Dacă s-a schimbat față de starea curentă a switch-ului, actualizează-l
                    if (switchWindow.isChecked() != isOpen) {
                        switchWindow.setChecked(isOpen);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Eroare la citire Window.", Toast.LENGTH_SHORT).show();
            }
        });

        floodMessageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String msg = snapshot.getValue(String.class);
                if (msg != null) {
                    // 1) Afișează în TextView
                    textFloodMessage.setText(msg);
                    // 2) Trimite notificare
                    sendFloodNotification(msg);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Eroare la citire floodMessage: " + error.getMessage());
            }
        });

        isNearHomeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean near = snapshot.getValue(Boolean.class);
                if (near != null) {
                    isNearHome = near;
                    evaluateClimate();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 🔵 Ascultă starea ușii (intrusion sensor)
        doorOpenRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOpen = snapshot.getValue(Boolean.class);
                if (isOpen == null) return;
                // actualizează UI
                textDoorStatus.setText(isOpen
                        ? "Stare Fereastră: Deschisă"
                        : "Stare Fereastră: Închisă");
                // dacă tocmai s-a schimbat pe true, trimite alerta
                if (isOpen && !lastDoorOpen) {
                    sendIntrusionNotification();
                }
                lastDoorOpen = isOpen;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Eroare la citire doorOpen: " + error.getMessage());
            }
        });

    }

    // 🔵 Actualizează TextView cu datele de la senzor
    private void updateSensorDisplay(String temp, String hum) {
        if (temp != null) currentTemp = temp;
        if (hum != null) currentHum = hum;

        String display = "Temp: " + currentTemp + " °C | Hum: " + currentHum + " %";
        textSensorData.setText(display);
    }

    // 🔵 Ascultă schimbările de stare ale switch-ului și le trimite către Firebase
    private void setupSwitchListener() {
//        switchLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            ledRef.setValue(isChecked);
//        });

        switchLight.setOnCheckedChangeListener((btn, isOn) -> {
            if (ignoreLightCallbacks) return;
            String newMode;
            if (isOn) {
                newMode = "on";
            } else {
                // la OFF manual: dacă checkbox e bifat → "auto", altfel "off"
                newMode = checkboxLightAuto.isChecked() ? "auto" : "off";
            }
            lightModeRef.setValue(newMode);
        });

        checkboxLightAuto.setOnCheckedChangeListener((btn, isAuto) -> {
            // salvează starea
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_LIGHT_AUTO, isAuto)
                    .apply();

            // dacă becul e stins acum, ajustăm mode în Firebase:
            if (!switchLight.isChecked()) {
                String newMode = isAuto ? "auto" : "off";
                lightModeRef.setValue(newMode);
            }
        });

        switchBlinds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ignoreSwitchCallback) return;     // suprimăm apelurile programatice
            String cmd = isChecked ? "open" : "closed";
            sendBlindsCommand(cmd);
        });

        // 🔵 Trimite starea “open”/“closed” pentru fereastră
        switchWindow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                windowRef.setValue("open");
            } else {
                windowRef.setValue("closed");
            }
        });

        switchAutoClimate.setOnCheckedChangeListener((btn, isOn) -> {
            autoMode = isOn;
            // Salvează în prefs
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_AUTO_MODE, autoMode)
                    .apply();

            // Activează/dezactivează UI-ul manual
            switchHeat.setEnabled(!autoMode);
            switchAC .setEnabled(!autoMode);

            // Dacă am intrat pe auto, execut imediat logica
            if (autoMode) {
                evaluateClimate();
            } else {
                heatRef.setValue(switchHeat.isChecked());
                acRef  .setValue(switchAC .isChecked());
            }
        });

    }

    // 🔵 Metodă care actualizează currentTime o dată pe minut
    private void startClockUpdater() {
        Handler handler = new Handler();
        Runnable timeUpdater = new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                currentTime = String.format("%02d:%02d", hour, minute);
                Log.d("TimeUpdate", "Ora actuală: " + currentTime);

                // 🔵 Scriem currentTime în Firebase
                if (currentTimeRef == null) {
                    currentTimeRef = FirebaseDatabase.getInstance().getReference("currentTime");
                }
                currentTimeRef.setValue(currentTime);
                checkBlindsSchedule();
                // rulează din nou peste 60 de secunde
                handler.postDelayed(this, 60_000);
            }
        };
        handler.post(timeUpdater);
    }
    private void setupSaveTimes() {
        btnSaveTimes.setOnClickListener(v -> {
            String openTime = editOpenTime.getText().toString().trim();
            String closeTime = editCloseTime.getText().toString().trim();

            if (!openTime.isEmpty()) {
                openTimeRef.setValue(openTime);
            }
            if (!closeTime.isEmpty()) {
                closeTimeRef.setValue(closeTime);
            }

            Toast.makeText(this, "Orele au fost salvate!", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendFloodNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("SmartHome: Alertă Inundație")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat nm = NotificationManagerCompat.from(this);
        nm.notify(NOTIF_ID, builder.build());
    }

    private void loadHomeCoordsLocal() {
        String latS = editHomeLat.getText().toString();
        String lngS = editHomeLng.getText().toString();
        try {
            homeLat = Double.parseDouble(latS);
            homeLng = Double.parseDouble(lngS);
        } catch (Exception ignored) {}
    }

    private void updateDistance() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 123);
            return;
        }
        loadHomeCoordsLocal();
        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc == null) {
                textDistance.setText("Locație telefon necunoscută");
                return;
            }
            float[] result = new float[1];
            Location.distanceBetween(
                    loc.getLatitude(), loc.getLongitude(),
                    homeLat, homeLng,
                    result
            );
            double km = result[0] / 1000.0;
            textDistance.setText(
                    String.format(Locale.getDefault(),
                            "Distanță până acasă: %.2f km", km)
            );
            boolean isUnder1km = km < 1.0;
            isNearHomeRef.setValue(isUnder1km);

        });
    }









    @Override
    public void onRequestPermissionsResult(int code,
                                           @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == 123 && res.length>0 && res[0]==PackageManager.PERMISSION_GRANTED) {
            updateDistance();
        }
    }

    private void startDistanceUpdater() {
        distanceHandler = new Handler(Looper.getMainLooper());
        distanceUpdater = new Runnable() {
            @Override
            public void run() {
                updateDistance();
                // re-programăm următoarea actualizare peste 10 secunde
                distanceHandler.postDelayed(this, 10_000);
            }
        };
        // prima rulare imediat
        distanceHandler.post(distanceUpdater);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // oprim handler-ul când activitatea dispare
        if (distanceHandler != null) {
            distanceHandler.removeCallbacks(distanceUpdater);
        }
    }

    private void evaluateClimate() {
        // 1) dacă nu suntem în modul auto, nu facem nimic
        if (!autoMode) return;
        // 2) dacă nu suntem acasă, oprim ambele
        if (!isNearHome) {
            heatRef.setValue(false);
            acRef  .setValue(false);
            return;
        }
        // 3) umiditate prea mare?
        if (Float.parseFloat(currentHum) > desiredHum) {
            acRef.setValue(true);
            heatRef.setValue(false);
            return;
        }
        // 4) temperatura
        float tmp = Float.parseFloat(currentTemp);
        if (tmp > desiredTemp) {
            acRef.setValue(true);
            heatRef.setValue(false);
        } else if (tmp < desiredTemp) {
            heatRef.setValue(true);
            acRef.setValue(false);
        } else {
            // egalitate
            heatRef.setValue(false);
            acRef .setValue(false);
        }
    }

    private void sendBlindsCommand(String cmd) {
        // 1) dacă e aceeași stare cu cea locală, nu facem nimic
        if (cmd.equals(blindsLocalState)) return;

        // 2) trimitem comanda în Firebase
        blindsRef.setValue(cmd, (error, ref) -> {
            if (error != null) {
                Toast.makeText(MainActivity.this,
                        "Eroare la jaluzele: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            } else {
                // 3) actualizăm starea locală și UI imediat
                blindsLocalState    = cmd;
                lastBlindsActionTime = currentTime;
                switchBlinds.setChecked(cmd.equals("open"));

                // 4) după rularea motorului, revenim la "idle"
                uiHandler.postDelayed(() -> {
                    blindsRef.setValue("idle", (err2, ref2) -> {
                        if (err2 != null) {
                            Toast.makeText(MainActivity.this,
                                    "Eroare reset jaluzele: " + err2.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            blindsLocalState = "idle";
                        }
                    });
                }, BLINDS_MOTOR_RUNTIME_MS);
            }
        });
    }

    private void checkBlindsSchedule() {
        String openT  = editOpenTime.getText().toString().trim();
        String closeT = editCloseTime.getText().toString().trim();
        // dacă nu avem ore setate, iesim
        if (openT.isEmpty() && closeT.isEmpty()) return;

        // deschidere automată
        if (currentTime.equals(openT)
                && !blindsLocalState.equals("open")
                && !currentTime.equals(lastBlindsActionTime)) {
            sendBlindsCommand("open");
        }
        // închidere automată
        if (currentTime.equals(closeT)
                && !blindsLocalState.equals("closed")
                && !currentTime.equals(lastBlindsActionTime)) {
            sendBlindsCommand("closed");
        }
    }

    private void sendIntrusionNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, INTRUSION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("SmartHome: Alertă Intruziune")
                .setContentText("Atenție! Intrare neautorizată detectată!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this)
                .notify(INTRUSION_NOTIF_ID, builder.build());
    }



}









































