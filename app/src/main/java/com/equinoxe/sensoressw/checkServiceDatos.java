package com.equinoxe.sensoressw;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import androidx.annotation.Nullable;

import static java.lang.Thread.sleep;

public class checkServiceDatos extends Service {
    final static long lDelayReconexion = 4000;

    Intent intentServicio = null;

    boolean bAcelerometro;
    boolean bGiroscopo;
    boolean bMagnetometro;

    boolean bLocation;
    boolean bSendServer;

    boolean bLogStats;
    boolean bLogData;
    String sFileNameDataLog;

    boolean bInternalDevice;
    int iNumDevices;
    int iPeriodo;
    long lTiempoRefrescoDatos;

    String[] sAddresses = new String[8];


    public checkServiceDatos() {
        super();
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("checkServiceDatos", HandlerThread.MIN_PRIORITY);
        thread.start();
    }

    private void crearServicio() {
            // Aquí solo se accede si hay dispositivos externos (puede haber internos)
            intentServicio = new Intent(this, ServiceDatos.class);

            intentServicio.putExtra("Periodo", iPeriodo);
            intentServicio.putExtra("NumDevices", iNumDevices);
            intentServicio.putExtra("Refresco", lTiempoRefrescoDatos);
            for (int i = 0; i < iNumDevices; i++)
                intentServicio.putExtra("Address" + i, sAddresses[i]);

            intentServicio.putExtra("Acelerometro", bAcelerometro);
            intentServicio.putExtra("Giroscopo", bGiroscopo);
            intentServicio.putExtra("Magnetometro", bMagnetometro);
            intentServicio.putExtra("Location", bLocation);
            intentServicio.putExtra("SendServer", bSendServer);
            intentServicio.putExtra("InternalDevice", bInternalDevice);

            intentServicio.putExtra("LogStats", bLogStats);
            intentServicio.putExtra("LogData", bLogData);
            intentServicio.putExtra("FileNameDataLog", sFileNameDataLog);

            intentServicio.putExtra("Reinicio", false);

            startService(intentServicio);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        iNumDevices = intent.getIntExtra("NumDevices",1);

        iPeriodo = intent.getIntExtra("Periodo",20);
        lTiempoRefrescoDatos = intent.getLongExtra("Refresco", 120000);
        for (int i = 0; i < iNumDevices; i++)
            sAddresses[i] = intent.getStringExtra("Address" + i);

        bAcelerometro = intent.getBooleanExtra("Acelerometro", true);
        bGiroscopo = intent.getBooleanExtra("Giroscopo", true);
        bMagnetometro = intent.getBooleanExtra("Magnetometro", true);

        bLogStats = intent.getBooleanExtra("LogStats", false);
        bLogData = intent.getBooleanExtra("LogData", false);
        sFileNameDataLog = intent.getStringExtra("FileNameDataLog");

        bLocation = intent.getBooleanExtra("Location", false);
        bSendServer = intent.getBooleanExtra("SendServer", false);
        bInternalDevice = intent.getBooleanExtra("InternalDevice", false);

        crearServicio();

        registerReceiver(receiver, new IntentFilter(ServiceDatos.NOTIFICATION));

        return START_NOT_STICKY;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();

            if (bundle != null) {
                int iDevice = bundle.getInt("Device");

                if (iDevice == Datos.ERROR) {
                    stopService(intentServicio);
                    try {
                        sleep(lDelayReconexion);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    intentServicio.putExtra("Reinicio", true);
                    startService(intentServicio);
                }

            }
        }
    };

    @Override
    public void onDestroy() {
        stopService(intentServicio);
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
