package com.equinoxe.sensoressw;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


public class Datos extends WearableActivity {
    final static int GIROSCOPO    = 0;
    final static int ACELEROMETRO = 1;
    final static int MAGNETOMETRO = 2;
    final static int HEART_RATE   = 3;

    public final static int PAQUETES = 9;
    public final static int ERROR = 20;
    public final static int MSG = 30;

    final static long lTiempoGPS = 10 * 1000;                   // Tiempo de toma de muestras de GPS (en ms)
    final static long lTiempoGrabacionCorriente = 10;           // Tiempo de grabación del log de corriente
    final static long lTiempoRefrescoDatos = 30 * 1000;  // Tiempo de muestra de datos

    public final static long lTiempoGrabacionDatos = 120 * 1000;       // Tiempo de grabación de las estadísticas (en ms)
    public final static int MAX_SENSOR_NUMBER = 8;

    BluetoothDataList listaDatos;

    private MiAdaptadorDatos adaptadorDatos;
    private TextView txtLongitud;
    private TextView txtLatitud;
    private TextView txtMensajes;
    private TextView txtBateria;

    Handler handlerDatos;
    boolean bSensing;
    DecimalFormat df;

    boolean bAcelerometro;
    boolean bGiroscopo;
    boolean bMagnetometro;
    boolean bHeartRate;

    boolean bInternalDevice;

    boolean bLocation;
    boolean bSendServer;

    long lTime;

    int iNumDevices;
    int iPeriodo;

    String[] sAddresses;

    boolean bServicioParado;
    Intent intentChkServicio = null;
    Intent intentServicioDatosInternalSensor = null;

    Timer timerTiempo;

    Timer timerGrabarCorriente;
    FileOutputStream fOutCurrent;
    boolean bLOGCurrent, bLogStats, bLogData;
    int iMuestraCorriente;
    float []fCorriente;
    BatteryManager mBatteryManager;

    private FusedLocationProviderClient fusedLocationClient;
    Timer timerGPS;
    private LocationCallback locationCallback;
    LocationRequest locationRequest = null;

    SimpleDateFormat sdf;
    FileOutputStream fOutDataLog;
    String sFileNameDataLog;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datos);

        setAmbientEnabled();

        RecyclerView recyclerViewDatos;
        RecyclerView.LayoutManager layoutManager;

        df = new DecimalFormat("###.##");

        Bundle extras = getIntent().getExtras();
        iNumDevices = extras.getInt("NumDevices");

        sAddresses = new String[8];
        for (int i = 0; i < iNumDevices; i++)
            sAddresses[i] = extras.getString("Address" + i);
        iPeriodo = extras.getInt("Periodo");

        bAcelerometro = extras.getBoolean(getString(R.string.Accelerometer));
        bGiroscopo = extras.getBoolean(getString(R.string.Gyroscope));
        bMagnetometro = extras.getBoolean(getString(R.string.Magnetometer));
        bHeartRate = extras.getBoolean(getString(R.string.HeartRate));

        bLocation = extras.getBoolean("Location");
        bSendServer = extras.getBoolean("SendServer");

        bInternalDevice = extras.getBoolean("InternalDevice");

        lTime = extras.getLong("Time");
        bLOGCurrent = extras.getBoolean("LOGCurrent");
        bLogStats = extras.getBoolean("LogStats");
        bLogData = extras.getBoolean("LogData");

        recyclerViewDatos = findViewById(R.id.recycler_viewDatos);
        txtLatitud = findViewById(R.id.textViewLatitud);
        txtLongitud = findViewById(R.id.textViewLongitud);
        txtMensajes = findViewById(R.id.textViewMensajes);
        txtBateria = findViewById(R.id.textViewBateria);

        listaDatos = new BluetoothDataList(iNumDevices, sAddresses);

        adaptadorDatos = new MiAdaptadorDatos(this, listaDatos);
        layoutManager = new LinearLayoutManager(this);

        recyclerViewDatos.setAdapter(adaptadorDatos);
        recyclerViewDatos.setLayoutManager(layoutManager);

        if (bLocation) {
            txtLongitud.setVisibility(View.VISIBLE);
            txtLatitud.setVisibility(View.VISIBLE);
        } else {
            txtLongitud.setVisibility(View.GONE);
            txtLatitud.setVisibility(View.GONE);
        }

        bSensing = false;

        bServicioParado = true;

        if (bLogData) {
            sdf = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.UK);
            String currentDateandTime = sdf.format(new Date());

            sdf = new SimpleDateFormat("HHmmss_SS", Locale.UK);
            sFileNameDataLog = Environment.getExternalStorageDirectory() + "/" + Build.MODEL + "_" + currentDateandTime + "__DataLog.txt";
            try {
                fOutDataLog = new FileOutputStream(sFileNameDataLog, false);
            } catch (Exception e) {
                Toast.makeText(this, getResources().getString(R.string.ERROR_FICHERO), Toast.LENGTH_LONG).show();
            }
        }

        if (bLOGCurrent) {
            mBatteryManager = (BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);

            int iTamanoMuestraCorriente = (int)(lTime / lTiempoGrabacionCorriente) + 1000;
            fCorriente = new float[iTamanoMuestraCorriente];

            iMuestraCorriente = 0;
            for (int i = 0; i < fCorriente.length; i++)
                fCorriente[i] = 0.0f;

            String sFichero = Environment.getExternalStorageDirectory() + "/" + Build.MODEL + "_" + iNumDevices + "_" + iPeriodo + "_Current.txt";
            try {
                fOutCurrent = new FileOutputStream(sFichero, false);
            } catch (Exception e) {}

            final TimerTask timerTaskGrabarCorriente = new TimerTask() {
                public void run() {
                    guardarCorriente();
                }
            };

            timerGrabarCorriente = new Timer();
            timerGrabarCorriente.scheduleAtFixedRate(timerTaskGrabarCorriente, lTiempoGrabacionCorriente, lTiempoGrabacionCorriente);

            final TimerTask timerTaskTiempo = new TimerTask() {
                    public void run() { btnPararClick(null);
                    }
                };

            timerTiempo = new Timer();
            timerTiempo.schedule(timerTaskTiempo, lTime);
        }

        crearServicio();

        registerReceiver(receiver, new IntentFilter(ServiceDatos.NOTIFICATION));

        if (bLocation) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            locationRequest = new LocationRequest();
            locationRequest.setInterval(lTiempoGPS);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        String sMensaje = "Lat: " + location.getLatitude();
                        txtLatitud.setText(sMensaje);
                        sMensaje = "Long: " + location.getLongitude();
                        txtLongitud.setText(sMensaje);
                    }
                }
            };
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,null /* Looper */);

            /*fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                txtLatitud.setText("Lat: " + Double.toString(location.getLatitude()));
                                txtLongitud.setText("Long: " + Double.toString(location.getLongitude()));
                            }
                        }
                    });

            final TimerTask timerTaskGPS = new TimerTask() {
                @Override
                public void run() {
                    fusedLocationClient.getLastLocation();
                }
            };

            timerGPS = new Timer();
            timerGPS.schedule(timerTaskGPS, lTiempoGPS);*/
        }

        handlerDatos = new Handler();
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if (bSensing) {
                    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    Intent batteryStatus = registerReceiver(null, ifilter);
                    String sBateria = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) + " %";
                    txtBateria.setText(sBateria);

                    adaptadorDatos.notifyDataSetChanged();
                }
                handlerDatos.postDelayed(this, lTiempoRefrescoDatos);

            }
        });

        setAmbientEnabled();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();

        adaptadorDatos.notifyDataSetChanged();
    }

    public void guardarCorriente() {
        try {
            fCorriente[iMuestraCorriente] = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            iMuestraCorriente++;
        } catch (Exception e) {
            Log.e("Fichero de resultados", e.getMessage(), e);
        }
    }

    private void crearServicio() {
        if (bInternalDevice) {
            intentServicioDatosInternalSensor = new Intent(this, ServiceDatosInternalSensor.class);

            intentServicioDatosInternalSensor.putExtra("Periodo", iPeriodo);
            intentServicioDatosInternalSensor.putExtra("Location", bLocation);
            intentServicioDatosInternalSensor.putExtra("SendServer", bSendServer);

            intentServicioDatosInternalSensor.putExtra(getString(R.string.Accelerometer), bAcelerometro);
            intentServicioDatosInternalSensor.putExtra(getString(R.string.Gyroscope), bGiroscopo);
            intentServicioDatosInternalSensor.putExtra(getString(R.string.Magnetometer), bMagnetometro);
            intentServicioDatosInternalSensor.putExtra(getString(R.string.HeartRate), bHeartRate);

            intentServicioDatosInternalSensor.putExtra("LogData", bLogData);
            intentServicioDatosInternalSensor.putExtra("FileNameDataLog", sFileNameDataLog);
            intentServicioDatosInternalSensor.putExtra("LogStats", bLogStats);

            intentServicioDatosInternalSensor.putExtra("NumDevices", iNumDevices);

            startService(intentServicioDatosInternalSensor);
        }

        // Si no se selecciona dispositivo interno o se selecciona el interno y hay más de un dispositivo seleccionado
        if (!bInternalDevice || (bInternalDevice && iNumDevices > 1)) {
            intentChkServicio = new Intent(this, checkServiceDatos.class);

            intentChkServicio.putExtra("Periodo", iPeriodo);
            intentChkServicio.putExtra("Refresco", lTiempoRefrescoDatos);

            for (int i = 0; i < iNumDevices; i++) {
                intentChkServicio.putExtra("Address" + i, sAddresses[i]);
            }

            intentChkServicio.putExtra("NumDevices", iNumDevices);
            intentChkServicio.putExtra("InternalDevice", bInternalDevice);

            intentChkServicio.putExtra("Acelerometro", bAcelerometro);
            intentChkServicio.putExtra("Giroscopo", bGiroscopo);
            intentChkServicio.putExtra("Magnetometro", bMagnetometro);
            intentChkServicio.putExtra("Location", bLocation);
            intentChkServicio.putExtra("SendServer", bSendServer);
            intentChkServicio.putExtra("LogData", bLogData);
            intentChkServicio.putExtra("LogStats", bLogStats);

            intentChkServicio.putExtra("FileNameDataLog", sFileNameDataLog);

            startService(intentChkServicio);
        }
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            bSensing = true;
            Bundle bundle = intent.getExtras();

            if (bundle != null) {
                int iSensor = bundle.getInt("Sensor");
                int iDevice = bundle.getInt("Device");
                String sCadena = bundle.getString("Cadena");

                if (iDevice == MSG) {
                    txtMensajes.append(sCadena.substring(16));
                }
                else {
                    if (iDevice != ERROR) {
                        /*if (iSensor != PAQUETES) {
                            try {
                                String sCadenaFichero =  sdf.format(new Date()) + ":" + iDevice + ":" + sCadena + "\n";
                                fOutDataLog.write(sCadenaFichero.getBytes());
                            } catch (Exception e) {}
                        }*/
                        switch (iSensor) {
                            case GIROSCOPO:
                                listaDatos.setMovimiento1(iDevice, sCadena);
                                break;
                            case ACELEROMETRO:
                                listaDatos.setMovimiento2(iDevice, sCadena);
                                break;
                            case MAGNETOMETRO:
                                listaDatos.setMovimiento3(iDevice, sCadena);
                                break;
                            case HEART_RATE:
                                listaDatos.setHeartRate(iDevice, sCadena);
                                break;

                            /*case ServiceDatos.LOCALIZACION_LAT:
                                txtLatitud.setText("Lat: " + sCadena);
                                break;
                            case ServiceDatos.LOCALIZACION_LONG:
                                txtLongitud.setText("Long: " + sCadena);
                                break;*/
                            case PAQUETES:
                                listaDatos.setPaquetes(iDevice, sCadena);
                                break;
                        }
                    }
                }
            }
        }
    };


    @Override
    public void onBackPressed() {
        btnPararClick(null);

        super.onBackPressed();
    }

    public  void btnPararClick(View v) {
        if (bLOGCurrent) {
            timerGrabarCorriente.cancel();

            String sCadena;
            try {
                for (float valor: fCorriente) {
                    sCadena = "" + valor + "\n";
                    fOutCurrent.write(sCadena.getBytes());
                }
                fOutCurrent.close();
            } catch (Exception e) {
            }
        }

        if (bInternalDevice)
            stopService(intentServicioDatosInternalSensor);

        if (!bInternalDevice || (bInternalDevice && iNumDevices > 1))
            stopService(intentChkServicio);

        if (bLogData) {
            try {
                fOutDataLog.close();
            } catch (Exception e) {}
        }

        unregisterReceiver(receiver);

        finish();
    }

}
