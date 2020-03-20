package com.equinoxe.sensoressw;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import androidx.core.app.NotificationCompat;

import java.text.DecimalFormat;
import java.util.List;

public class ServiceDatosInternalSensor extends Service implements SensorEventListener {
    final static int GIROSCOPO    = 0;
    final static int ACELEROMETRO = 1;
    final static int MAGNETOMETRO = 2;

    public static final String NOTIFICATION = "com.equinoxe.bluetoothle.android.service.receiver";

    NotificationCompat.Builder mBuilder;

    private SensorManager sensorManager;
    private Sensor sensorAcelerometro, sensorGiroscopo, sensorMagnetometro;

    String sCadenaGiroscopo, sCadenaMagnetometro, sCadenaAcelerometro;
    //Message msg;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    DecimalFormat df;

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
                case GIROSCOPO:
                    publishSensorValues(GIROSCOPO, msg.arg2, sCadenaGiroscopo);
                    break;
                case MAGNETOMETRO:
                    publishSensorValues(MAGNETOMETRO, msg.arg2, sCadenaMagnetometro);
                    break;
                case ACELEROMETRO:
                    publishSensorValues(ACELEROMETRO, msg.arg2, sCadenaAcelerometro);
                    break;
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        df = new DecimalFormat("###.##");

        /*mBuilder = new NotificationCompat.Builder(this, getString(R.string.channel_name))
                //.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Refresco")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ALARM);*/

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAcelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorGiroscopo = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //sensorMagnetometro = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        /*String sTipos = "";
        List<Sensor> lista = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : lista) {
            sTipos = sensor.getStringType();
            sTipos += "\n";
        }*/

        sensorManager.registerListener(this, sensorAcelerometro, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, sensorGiroscopo, SensorManager.SENSOR_DELAY_GAME);
        //sensorManager.registerListener(this, sensorMagnetometro, SensorManager.SENSOR_DELAY_GAME);

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Message msg = mServiceHandler.obtainMessage();

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                sCadenaAcelerometro = "A -> " + df.format(event.values[0]) + " "
                                              + df.format(event.values[1]) + " "
                                              + df.format(event.values[2]);
                msg.arg1 = ACELEROMETRO;
                break;
            case Sensor.TYPE_GYROSCOPE:
                sCadenaGiroscopo = "G ->  " + df.format(event.values[0]) + " "
                                            + df.format(event.values[1]) + " "
                                            + df.format(event.values[2]);
                msg.arg1 = GIROSCOPO;
                break;
            /*case Sensor.TYPE_MAGNETIC_FIELD:
                sCadenaMagnetometro = "M ->  " + df.format(event.values[0]) + " "
                                               + df.format(event.values[1]) + " "
                                               + df.format(event.values[2]);
                msg.arg1 = MAGNETOMETRO;
                break;*/
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
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra("Sensor", iSensor);
        intent.putExtra("Device", iDevice);
        intent.putExtra("Cadena", sCadena);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }
}
