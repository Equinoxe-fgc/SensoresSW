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

    /*boolean bHumedad;
    boolean bBarometro;
    boolean bLuz;
    boolean bTemperatura;*/
    boolean bAcelerometro;
    boolean bGiroscopo;
    boolean bMagnetometro;

    boolean bLocation;
    boolean bSendServer;
    boolean bLOGCurrent;

    boolean bTime;
    long lTime;

    int iNumDevices;
    int iPeriodo;
    long lTiempoRefrescoDatos;

    //long iMaxInterval, iMinInterval,  iLatency, iTimeout, iPeriodoMaxRes;

    String[] sAddresses = new String[8];


    public checkServiceDatos() {
        super();
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("checkServiceDatos", HandlerThread.MIN_PRIORITY);
        thread.start();

        /*mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);*/
    }

    private void crearServicio() {
        intentServicio = new Intent(this, ServiceDatos.class);

        intentServicio.putExtra("Periodo", iPeriodo);
        intentServicio.putExtra("NumDevices", iNumDevices);
        intentServicio.putExtra("Refresco", lTiempoRefrescoDatos);
        for (int i = 0; i < iNumDevices; i++)
            intentServicio.putExtra("Address" + i, sAddresses[i]);
        /*intentServicio.putExtra("Humedad", bHumedad);
        intentServicio.putExtra("Barometro", bBarometro);
        intentServicio.putExtra("Luz", bLuz);
        intentServicio.putExtra("Temperatura", bTemperatura);*/
        intentServicio.putExtra("Acelerometro", bAcelerometro);
        intentServicio.putExtra("Giroscopo", bGiroscopo);
        intentServicio.putExtra("Magnetometro", bMagnetometro);
        intentServicio.putExtra("Location", bLocation);
        intentServicio.putExtra("SendServer", bSendServer);

        intentServicio.putExtra("LOGCurrent", bLOGCurrent);

        /*intentServicio.putExtra("MaxInterval", iMaxInterval);
        intentServicio.putExtra("MinInterval", iMinInterval);
        intentServicio.putExtra("Latency", iLatency);
        intentServicio.putExtra("Timeout", iTimeout);
        intentServicio.putExtra("PeriodoMaxRes", iPeriodoMaxRes);*/
        //intentServicio.putExtra("bTime", bTime);
        intentServicio.putExtra("Time", lTime);

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
        /*bHumedad = intent.getBooleanExtra("Humedad", false);
        bBarometro = intent.getBooleanExtra("Barometro", false);
        bTemperatura = intent.getBooleanExtra("Temperatura", false);
        bLuz = intent.getBooleanExtra("Luz", false);*/

        bLOGCurrent = intent.getBooleanExtra("LOGCurrent", false);

        bLocation = intent.getBooleanExtra("Location", false);
        bSendServer = intent.getBooleanExtra("SendServer", false);

        /*iMaxInterval = intent.getLongExtra("MaxInterval", 0);
        iMinInterval = intent.getLongExtra("MinInterval", 0);
        iLatency = intent.getLongExtra("Latency", 0);
        iTimeout = intent.getLongExtra("Timeout", 0);
        iPeriodoMaxRes = intent.getLongExtra("PeriodoMaxRes", 0);*/

        //bTime = intent.getBooleanExtra("bTime", false);
        lTime = intent.getLongExtra("Time", 0);

        crearServicio();

        registerReceiver(receiver, new IntentFilter(ServiceDatos.NOTIFICATION));

        return START_NOT_STICKY;
        //return START_STICKY;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();

            if (bundle != null) {
                int iDevice = bundle.getInt("Device");

                if (iDevice == ServiceDatos.ERROR) {
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
