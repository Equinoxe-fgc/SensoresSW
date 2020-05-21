package com.equinoxe.sensoressw;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ServiceDatosInternalSensor extends Service implements SensorEventListener {
    private boolean bAcelerometro, bGiroscopo, bMagnetometro, bHeartRate;
    private boolean bLocation, bSendServer;
    private int iPeriodo;

    private SensorManager sensorManager;
    private Sensor sensorAcelerometro, sensorGiroscopo, sensorMagnetometro, sensorHeartRate;

    String sCadenaGiroscopo, sCadenaMagnetometro, sCadenaAcelerometro, sCadenaHeartRate;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    DecimalFormat df;

    boolean bLogData, bLogStats;
    Timer timerGrabarDatos;
    String sFileNameDataLog;
    BatteryInfoBT batInfo;

    SimpleDateFormat sdf;
    FileOutputStream fOutDataLog;
    FileOutputStream fOut;

    long lNumMsgGiroscopo, lNumMsgMagnetometro, lNumMsgAcelerometro, lNumMsgHR;

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceDatosInternalSensor", HandlerThread.MIN_PRIORITY);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceDatosInternalSensor.ServiceHandler(mServiceLooper);
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case Datos.GIROSCOPO:
                    publishSensorValues(Datos.GIROSCOPO, msg.arg2, sCadenaGiroscopo);
                    break;
                case Datos.MAGNETOMETRO:
                    publishSensorValues(Datos.MAGNETOMETRO, msg.arg2, sCadenaMagnetometro);
                    break;
                case Datos.ACELEROMETRO:
                    publishSensorValues(Datos.ACELEROMETRO, msg.arg2, sCadenaAcelerometro);
                    break;
                case Datos.HEART_RATE:
                    publishSensorValues(Datos.HEART_RATE, msg.arg2, sCadenaHeartRate);
                    break;
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //createNotificationChannel();
        lNumMsgGiroscopo = 0;
        lNumMsgMagnetometro = 0;
        lNumMsgAcelerometro = 0;
        lNumMsgHR = 0;

        df = new DecimalFormat("###.##");

        int iNumDevices = intent.getIntExtra("NumDevices", 1);

        bLocation = intent.getBooleanExtra("Location", false);
        bSendServer = intent.getBooleanExtra("SendServer", false);

        bAcelerometro = intent.getBooleanExtra(getString(R.string.Accelerometer), true);
        bGiroscopo = intent.getBooleanExtra(getString(R.string.Gyroscope), true);
        bMagnetometro = intent.getBooleanExtra(getString(R.string.Magnetometer), true);
        bHeartRate = intent.getBooleanExtra(getString(R.string.HeartRate), true);
        iPeriodo = intent.getIntExtra("Periodo", 20);

        bLogData = intent.getBooleanExtra("LogData", false);
        sFileNameDataLog = intent.getStringExtra("FileNameDataLog");
        bLogStats = intent.getBooleanExtra("LogStats", false);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (bAcelerometro) {
            sensorAcelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, sensorAcelerometro, SensorManager.SENSOR_DELAY_GAME);
        }
        if (bGiroscopo) {
            sensorGiroscopo = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorManager.registerListener(this, sensorGiroscopo, SensorManager.SENSOR_DELAY_GAME);
        }
        if (bMagnetometro) {
            sensorMagnetometro = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            sensorManager.registerListener(this, sensorMagnetometro, SensorManager.SENSOR_DELAY_GAME);
        }
        if (bHeartRate) {
            sensorHeartRate = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            sensorManager.registerListener(this, sensorHeartRate, SensorManager.SENSOR_DELAY_GAME);
        }

        if (bLogData) {
            sdf = new SimpleDateFormat("HHmmss_SS", Locale.UK);
            try {
                fOutDataLog = new FileOutputStream(sFileNameDataLog, true);
            } catch (Exception e) {
                Toast.makeText(this, getResources().getString(R.string.ERROR_FICHERO), Toast.LENGTH_LONG).show();
            }
        }

        // Si es el único dispositivo hay que grabar el log para saber lo que dura encendido
        if (bLogStats && iNumDevices == 1) {
            sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK);

            batInfo = new BatteryInfoBT();

            File file;
            int iNumFichero = 0;
            String sFichero;
            do {
                sFichero = Environment.getExternalStorageDirectory() + "/" + Build.MODEL + "_" + iNumDevices + "_" + iPeriodo + "_" + iNumFichero + ".txt";
                file = new File(sFichero);
                iNumFichero++;
            } while (file.exists());

            try {
                String currentDateandTime = sdf.format(new Date());

                fOut = new FileOutputStream(sFichero, false);
                String sModel = Build.MODEL;
                sModel = sModel.replace(" ", "_");
                String sCadena = sModel + " " + iNumDevices + " " + iPeriodo + " " + bLocation + " " + bSendServer + " " + currentDateandTime + "\n";
                fOut.write(sCadena.getBytes());
                fOut.flush();
            } catch (Exception e) {
                Toast.makeText(this, getResources().getString(R.string.ERROR_FICHERO), Toast.LENGTH_LONG).show();
            }

            final TimerTask timerTaskGrabarDatos = new TimerTask() {
                public void run() {
                    grabarMedidas();
                }
            };

            timerGrabarDatos = new Timer();
            timerGrabarDatos.scheduleAtFixedRate(timerTaskGrabarDatos, Datos.lTiempoGrabacionDatos, Datos.lTiempoGrabacionDatos);
        }

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    private void getBatteryInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        try {
            batInfo.setBatteryLevel(batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
            batInfo.setVoltaje(batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1));
            batInfo.setTemperature(batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1));

            BatteryManager mBatteryManager = (BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);
            batInfo.setCurrentAverage(mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE));
            batInfo.setCurrentNow(mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
        } catch (NullPointerException e) {
            Log.e("NullPointerException", "ServiceDatos - getBatteryInfo");
        }
    }

    public void grabarMedidas() {
        getBatteryInfo();

        try {
            String sCadena = sdf.format(new Date()) + ":" +
                    batInfo.getBatteryLevel() + ":";
            //batInfo.getVoltaje() + ":" +
            //batInfo.getTemperature() + ":" +
            //batInfo.getCurrentAverage() + ":" +
            //batInfo.getCurrentNow() + " - ";
            fOut.write(sCadena.getBytes());

            sCadena = "";
            for (int i = 0; i < Datos.MAX_SENSOR_NUMBER; i++) {
                sCadena += "(0,0)";
            }
            sCadena += "(0,0)\n";
            fOut.write(sCadena.getBytes());
            fOut.flush();
        } catch (Exception e) {
            Log.e("Fichero de resultados", e.getMessage(), e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String sCadenaFichero = "";

        Message msg = mServiceHandler.obtainMessage();

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                lNumMsgAcelerometro++;
                sCadenaAcelerometro = "A: " + df.format(event.values[0]) + " "
                                              + df.format(event.values[1]) + " "
                                              + df.format(event.values[2]);
                sCadenaFichero = sCadenaAcelerometro;
                msg.arg1 = Datos.ACELEROMETRO;
                break;
            case Sensor.TYPE_GYROSCOPE:
                lNumMsgGiroscopo++;
                sCadenaGiroscopo = "G: " + df.format(event.values[0]) + " "
                                           + df.format(event.values[1]) + " "
                                           + df.format(event.values[2]);
                sCadenaFichero = sCadenaGiroscopo;
                msg.arg1 = Datos.GIROSCOPO;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                lNumMsgMagnetometro++;
                sCadenaMagnetometro = "M: " + df.format(event.values[0]) + " "
                                              + df.format(event.values[1]) + " "
                                              + df.format(event.values[2]);
                sCadenaFichero = sCadenaMagnetometro;
                msg.arg1 = Datos.MAGNETOMETRO;
                break;
            case Sensor.TYPE_HEART_RATE:
                long lNumMsg = lNumMsgGiroscopo + lNumMsgMagnetometro + lNumMsgAcelerometro + lNumMsgHR;
                lNumMsgHR++;
                sCadenaHeartRate = "HR: " + df.format(event.values[0]) + " - " + lNumMsgHR;
                sCadenaFichero = sCadenaHeartRate;
                msg.arg1 = Datos.HEART_RATE;
                break;
        }

        if (bLogData) {
            try {
                sCadenaFichero =  sdf.format(new Date()) + ":0:" + sCadenaFichero + "\n";
                fOutDataLog.write(sCadenaFichero.getBytes());
            } catch (Exception e) {}
        }

        msg.arg2 = 0;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.channel_name), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void publishSensorValues(int iSensor, int iDevice, String sCadena) {
        Intent intent = new Intent(Datos.NOTIFICATION);
        intent.putExtra("Sensor", iSensor);
        intent.putExtra("Device", iDevice);
        intent.putExtra("Cadena", sCadena);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (bLogStats) {
            try {
                timerGrabarDatos.cancel();
                grabarMedidas();
                fOut.close();
            } catch (Exception e) {}
        }
        /*if (bLogData) {
            try {
                fOutDataLog.close();
            } catch (Exception e) {}
        }*/

        if (bAcelerometro) {
            sensorManager.unregisterListener(this, sensorAcelerometro);
        }
        if (bGiroscopo) {
            sensorManager.unregisterListener(this, sensorGiroscopo);
        }
        if (bMagnetometro) {
            sensorManager.unregisterListener(this, sensorMagnetometro);
        }
        if (bHeartRate) {
            sensorManager.unregisterListener(this, sensorHeartRate);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }
}
