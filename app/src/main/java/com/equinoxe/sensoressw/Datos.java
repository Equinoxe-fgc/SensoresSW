package com.equinoxe.sensoressw;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;


public class Datos extends WearableActivity {
    //final static long lTiempoRefrescoDatos = 120 * 1000;  // Tiempo de muestra de datos
    final static long lTiempoRefrescoDatos = 10 * 1000;  // Tiempo de muestra de datos

    BluetoothDataList listaDatos;

    private MiAdaptadorDatos adaptadorDatos;
    private TextView txtLongitud;
    private  TextView txtLatitud;
    private TextView txtMensajes;

    Handler handlerDatos;
    Handler handlerWeb;
    boolean bSensing;
    DecimalFormat df;

    boolean bAcelerometro;
    boolean bGiroscopo;
    boolean bMagnetometro;

    boolean bInternalDevice;

    boolean bLocation;
    boolean bSendServer;

    boolean bLogCurrent;

    boolean bTime;
    long lTime;

    int iNumDevices;
    int iPeriodo;

    String[] sAddresses;

    boolean bServicioParado;
    Intent intentChkServicio = null;
    Intent intentServicioDatosInternalSensor = null;

    Timer timerTiempo;

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

        bAcelerometro = extras.getBoolean("Acelerometro");
        bGiroscopo = extras.getBoolean("Giroscopo");
        bMagnetometro = extras.getBoolean("Magnetometro");

        bLocation = extras.getBoolean("Location");
        bSendServer = extras.getBoolean("SendServer");

        bInternalDevice = extras.getBoolean("InternalDevice");

        bTime = extras.getBoolean("bTime");
        lTime = extras.getLong("Time");

        bLogCurrent = extras.getBoolean("LOGCurrent");

        recyclerViewDatos = findViewById(R.id.recycler_viewDatos);
        txtLatitud = findViewById(R.id.textViewLatitud);
        txtLongitud = findViewById(R.id.textViewLongitud);
        txtMensajes = findViewById(R.id.textViewMensajes);

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

        crearServicio();

        registerReceiver(receiver, new IntentFilter(ServiceDatos.NOTIFICATION));

        if (bTime) {
            final TimerTask timerTaskTiempo = new TimerTask() {
                public void run() {
                    btnPararClick(null);
                }
            };

            timerTiempo = new Timer();
            timerTiempo.schedule(timerTaskTiempo, lTime);
        }


        handlerDatos = new Handler();
        handlerWeb = new Handler();
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if (bSensing) {
                    adaptadorDatos.notifyDataSetChanged();
                }
                handlerDatos.postDelayed(this, lTiempoRefrescoDatos);

            }
        });
    }

    private void crearServicio() {
        if (bInternalDevice) {
            intentServicioDatosInternalSensor = new Intent(this, ServiceDatosInternalSensor.class);

            intentServicioDatosInternalSensor.putExtra("Acelerometro", bAcelerometro);
            intentServicioDatosInternalSensor.putExtra("Giroscopo", bGiroscopo);
            intentServicioDatosInternalSensor.putExtra("Magnetometro", bMagnetometro);

            intentServicioDatosInternalSensor.putExtra("NumDevices", iNumDevices);

            startService(intentServicioDatosInternalSensor);
        }

        // Si no se selecciona dispositivo interno o se selecciona el interno y hay más de un dispositivo seleccionado
        if (!bInternalDevice || (bInternalDevice && iNumDevices > 1)) {
            intentChkServicio = new Intent(this, checkServiceDatos.class);

            intentChkServicio.putExtra("Periodo", iPeriodo);
            intentChkServicio.putExtra("Refresco", lTiempoRefrescoDatos);

            int iPos = 0;
            for (int i = 0; i < iNumDevices; i++) {
                String sAddress = sAddresses[i];

                if (sAddress.compareTo(getString(R.string.Internal)) == 0)
                    continue;
                intentChkServicio.putExtra("Address" + iPos, sAddresses[i]);
                iPos++;
            }

            intentChkServicio.putExtra("NumDevices", bInternalDevice?iNumDevices-1:iNumDevices);

            intentChkServicio.putExtra("Acelerometro", bAcelerometro);
            intentChkServicio.putExtra("Giroscopo", bGiroscopo);
            intentChkServicio.putExtra("Magnetometro", bMagnetometro);
            intentChkServicio.putExtra("Location", bLocation);
            intentChkServicio.putExtra("SendServer", bSendServer);
            intentChkServicio.putExtra("LOGCurrent", bLogCurrent);

            intentChkServicio.putExtra("Time", lTime);

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

                if (iDevice == ServiceDatos.MSG) {
                    txtMensajes.append(sCadena.substring(16));
                }
                else {
                    if (iDevice != ServiceDatos.ERROR)
                        switch (iSensor) {
                            case ServiceDatos.GIROSCOPO:
                                listaDatos.setMovimiento1(iDevice, sCadena);
                                break;
                            case ServiceDatos.ACELEROMETRO:
                                listaDatos.setMovimiento2(iDevice, sCadena);
                                break;
                            case ServiceDatos.MAGNETOMETRO:
                                listaDatos.setMovimiento3(iDevice, sCadena);
                                break;

                            case ServiceDatos.LOCALIZACION_LAT:
                                txtLatitud.setText("Lat: " + sCadena);
                                break;
                            case ServiceDatos.LOCALIZACION_LONG:
                                txtLongitud.setText("Long: " + sCadena);
                                break;
                            case ServiceDatos.PAQUETES:
                                listaDatos.setPaquetes(iDevice, sCadena);
                                break;
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
        if (bInternalDevice)
            stopService(intentServicioDatosInternalSensor);

        if (!bInternalDevice || (bInternalDevice && iNumDevices > 1))
            stopService(intentChkServicio);

        unregisterReceiver(receiver);

        finish();
    }
}
