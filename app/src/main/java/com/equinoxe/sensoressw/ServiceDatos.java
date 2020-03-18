package com.equinoxe.sensoressw;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;


public class ServiceDatos extends Service {
    final static long lTiempoGPS = 10 * 1000;                   // Tiempo de toma de muestras de GPS (en ms)
    final static long lTiempoGrabacionDatos = 120 * 1000;       // Tiempo de grabación de las estadísticas (en ms)
    final static long lTiempoGrabacionCorriente = 10;           // Tiempo de grabación del log de corriente
    final static long lTiempoComprobacionDesconexion = 5 * 1000;  // Tiempo cada cuanto se comprueba si ha habido desconexión

    final static long lDelayComprobacionDesconexion = 30000;

    final static int MAX_SENSOR_NUMBER = 8;

    final static int SENSOR_MOV_DATA_LEN = 19;
    final static int SENSOR_MOV_SEC_POS = SENSOR_MOV_DATA_LEN - 1;

    final static int GIROSCOPO    = 0;
    final static int ACELEROMETRO = 1;
    final static int MAGNETOMETRO = 2;
    /*final static int HUMEDAD      = 3;
    final static int LUZ          = 4;
    final static int BAROMETRO    = 5;
    final static int TEMPERATURA  = 6;*/
    final static int LOCALIZACION_LAT  = 7;
    final static int LOCALIZACION_LONG = 8;
    final static int PAQUETES = 9;
    public final static int ERROR = 20;
    public final static int MSG = 30;

    public static final String NOTIFICATION = "com.equinoxe.bluetoothle.android.service.receiver";

    public static final int CHANNEL_ID = 128;

    String[] sCadenaGiroscopo;
    String[] sCadenaMagnetometro;
    String[] sCadenaAcelerometro;

    private long lMensajesParaEnvio;
    private long lMensajesPorSegundo;

    boolean bLOGCurrent;

    SimpleDateFormat sdf;

    FileOutputStream fOut, fOutCurrent;
    FileOutputStream fLog;
    BatteryInfoBT batInfo;

    private int iNumDevices;
    private int iPeriodo;
    private long lTiempoRefrescoDatos;

    /*byte []barometro = new byte[4];
    long valorBarometro, valorTemperatura;
    float fValorBarometro, fValorTemperatura;

    byte []luz = new byte[2];
    float fValorLuz;*/

    long valorGiroX, valorGiroY, valorGiroZ;
    float fValorGiroX, fValorGiroY, fValorGiroZ;
    long valorAcelX, valorAcelY, valorAcelZ;
    float fValorAcelX, fValorAcelY, fValorAcelZ;
    long valorMagX, valorMagY, valorMagZ;
    float fValorMagX, fValorMagY, fValorMagZ;

    /*byte []humedad = new byte[4];
    long valorHumedad;
    float fValorHumedad;*/

    boolean bLocation;
    LocationManager locManager;
    Location mejorLocaliz = null;
    boolean bGPSEnabled;
    boolean bNetworkEnabled;

    boolean bNetConnected;
    EnvioDatosSocket envioAsync;

    BluetoothGatt[] btGatt;
    private boolean[][] bSensores;
    private boolean[][] bActivacion;
    private boolean[][] bConfigPeriodo;

    long[] lDatosRecibidos;
    long[] lDatosPerdidos;
    byte[] iSecuencia;
    boolean[] bPrimerDato;
    long[] lDatosRecibidosAnteriores;
    /*boolean[] bConfigTiempos;
    boolean[] bConfigPeriodoMaxRes;*/

    byte[][] movimiento;
    boolean bSensing;
    DecimalFormat df;

    //private boolean bHumedad, bBarometro, bLuz, bTemperatura;
    private boolean bAcelerometro, bGiroscopo, bMagnetometro;
    private String[] sAddresses = new String[MAX_SENSOR_NUMBER];
    boolean bSendServer;

    //long iMaxInterval, iMinInterval,  iLatency, iTimeout, iPeriodoMaxRes;

    boolean bTime;
    long lTime;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    Timer timerComprobarDesconexion;
    Timer timerGrabarDatos;
    Timer timerGrabarGPS;
    Timer timerGrabarCorriente;

    boolean bReiniciar = false;
    boolean bReinicio;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    int notificationId = 0;
    NotificationCompat.Builder mBuilder;

    String sServer;
    int iPuerto;

    WifiManager wifi;

    int iMuestraCorriente;
    float []fCorriente;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case GIROSCOPO:
                    publishSensorValues(GIROSCOPO, msg.arg2, sCadenaGiroscopo[msg.arg2]);
                    break;
                case MAGNETOMETRO:
                    publishSensorValues(MAGNETOMETRO, msg.arg2, sCadenaMagnetometro[msg.arg2]);
                    break;
                case ACELEROMETRO:
                    publishSensorValues(ACELEROMETRO, msg.arg2, sCadenaAcelerometro[msg.arg2]);
                    break;
            }
        }
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceDatos", HandlerThread.MIN_PRIORITY);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }


    @Override
    public void onDestroy() {
        timerGrabarDatos.cancel();
        timerComprobarDesconexion.cancel();
        if (bLOGCurrent)
            timerGrabarCorriente.cancel();
        if (bSendServer)
            timerGrabarGPS.cancel();

        cerrarConexiones();

        wakeLock.release();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        try {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyApp::MyWakelockTag");
            wakeLock.acquire();
        } catch (NullPointerException e) {
            Log.e("NullPointerException", "ServiceDatos - onStartCommand");
        }

        createNotificationChannel();

        mBuilder = new NotificationCompat.Builder(this, getString(R.string.channel_name))
                //.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Refresco")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ALARM);

        iNumDevices = intent.getIntExtra("NumDevices",1);
        iPeriodo = intent.getIntExtra("Periodo",20);
        lTiempoRefrescoDatos = intent.getLongExtra("Refresco", 120000);
        lMensajesParaEnvio = lTiempoRefrescoDatos / iPeriodo;
        lMensajesPorSegundo = 1000 / iPeriodo;

        for (int i = 0; i < iNumDevices; i++)
            sAddresses[i] = intent.getStringExtra("Address" + i);

        bAcelerometro = intent.getBooleanExtra("Acelerometro", true);
        bGiroscopo = intent.getBooleanExtra("Giroscopo", true);
        bMagnetometro = intent.getBooleanExtra("Magnetometro", true);
        /*bHumedad = intent.getBooleanExtra("Humedad", false);
        bBarometro = intent.getBooleanExtra("Barometro", false);
        bTemperatura = intent.getBooleanExtra("Temperatura", false);
        bLuz = intent.getBooleanExtra("Luz", false);*/

        bLocation = intent.getBooleanExtra("Location", false);
        bSendServer = intent.getBooleanExtra("SendServer", false);

        bLOGCurrent = intent.getBooleanExtra("LOGCurrent", false);

        lTime = intent.getLongExtra("Time", 0);

        /*iMaxInterval = intent.getLongExtra("MaxInterval", 0);
        iMinInterval = intent.getLongExtra("MinInterval", 0);
        iLatency = intent.getLongExtra("Latency", 0);
        iTimeout = intent.getLongExtra("Timeout", 0);
        iPeriodoMaxRes = intent.getLongExtra("PeriodoMaxRes", 0);*/

        if (bLOGCurrent) {
            int iTamanoMuestraCorriente = (int)(lTime / lTiempoGrabacionCorriente) + 1000;
            fCorriente = new float[iTamanoMuestraCorriente];
        }

        //bTime = intent.getBooleanExtra("bTime", false);
        //lTime = intent.getLongExtra("Time", 0);

        /*if (bSendServer) {
            wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifi.setWifiEnabled(true);
        }*/

        bReinicio = intent.getBooleanExtra("Reinicio", false);

        bSensores = new boolean[iNumDevices][4];
        bActivacion = new boolean[iNumDevices][4];
        bConfigPeriodo = new boolean[iNumDevices][4];

        /*bConfigTiempos = new boolean[iNumDevices];
        bConfigPeriodoMaxRes = new boolean[iNumDevices];*/

        // Se inicializan todos los sensores porque se guarda la estadística de todos aunque no se usen
        lDatosRecibidos = new long[MAX_SENSOR_NUMBER];
        lDatosPerdidos = new long[MAX_SENSOR_NUMBER];
        iSecuencia = new byte[iNumDevices];
        bPrimerDato = new boolean[iNumDevices];
        lDatosRecibidosAnteriores = new long[iNumDevices];

        for (int i = 0; i < MAX_SENSOR_NUMBER; i++) {
            if (i < iNumDevices) {
                bSensores[i][0] = bActivacion[i][0] = bConfigPeriodo[i][0] = bAcelerometro || bGiroscopo || bMagnetometro;
                /*bSensores[i][1] = bActivacion[i][1] = bConfigPeriodo[i][1] = bHumedad;
                bSensores[i][2] = bActivacion[i][2] = bConfigPeriodo[i][2] = bBarometro || bTemperatura;
                bSensores[i][3] = bActivacion[i][3] = bConfigPeriodo[i][3] = bLuz;

                bConfigTiempos[i] = (iMaxInterval != 0 || iMinInterval != 0 || iLatency != 0 || iTimeout != 0);
                bConfigPeriodoMaxRes [i] = (iPeriodoMaxRes != 0);*/

                lDatosRecibidosAnteriores[i] = 0;
                iSecuencia[i] = 0;
                bPrimerDato[i] = true;
            }

            lDatosRecibidos[i] = 0;
            lDatosPerdidos[i] = 0;
        }

        movimiento = new byte[iNumDevices][SENSOR_MOV_DATA_LEN];

        sCadenaGiroscopo = new String[iNumDevices];
        sCadenaMagnetometro = new String[iNumDevices];
        sCadenaAcelerometro = new String[iNumDevices];

        btGatt = new BluetoothGatt[iNumDevices];

        df = new DecimalFormat("###.##");
        sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

        if (bLOGCurrent) {
            String sFichero = Environment.getExternalStorageDirectory() + "/" + Build.MODEL + "_" + iNumDevices + "_" + iPeriodo + "_Current.txt";
            try {
                fOutCurrent = new FileOutputStream(sFichero, false);
            } catch (Exception e) {}
        }

        String currentDateandTime = sdf.format(new Date());
        try {
            File file;
            int iNumFichero = 0;
            String sFichero;
            do {
                sFichero = Environment.getExternalStorageDirectory() + "/" + Build.MODEL + "_" + iNumDevices + "_" + iPeriodo + "_" + iNumFichero + ".txt";
                file = new File(sFichero);
                iNumFichero++;
            } while (file.exists());

            if (bReinicio) {
                iNumFichero -= 2;
                sFichero = Environment.getExternalStorageDirectory() + "/" + Build.MODEL + "_" + iNumDevices + "_" + iPeriodo + "_" + iNumFichero + ".txt";
                FileInputStream fIn = new FileInputStream(sFichero);
                InputStreamReader sReader = new InputStreamReader(fIn);
                BufferedReader buffreader = new BufferedReader(sReader);

                String sLinea, sUltimaLinea = null;
                do {
                    sLinea = buffreader.readLine();
                    if (sLinea != null)
                        sUltimaLinea = sLinea;
                } while (sLinea != null);
                buffreader.close();
                sReader.close();
                fIn.close();

                int iPosInicio = sUltimaLinea.indexOf('(');
                sLinea = sUltimaLinea.substring(iPosInicio + 1);
                for (int i = 0; i < MAX_SENSOR_NUMBER; i++) {
                    int iPosComa = sLinea.indexOf(',');
                    int iPosFin = sLinea.indexOf(')');
                    String sCadena1 = sLinea.substring(0, iPosComa);
                    String sCadena2 = sLinea.substring(iPosComa + 1, iPosFin);
                    lDatosRecibidos[i] = Long.parseLong(sCadena1);
                    lDatosPerdidos[i] = Long.parseLong(sCadena2);
                    iPosInicio = iPosFin + 1;
                    sLinea = sLinea.substring(iPosInicio + 1);
                }
                fOut = new FileOutputStream(sFichero, true);
            } else {
                fOut = new FileOutputStream(sFichero, false);
                String sCadena = Build.MODEL + " " + iNumDevices + " " + iPeriodo + " " + bLocation + " " + bSendServer + " " + currentDateandTime + "\n";
                fOut.write(sCadena.getBytes());
                fOut.flush();
            }

            fLog = new FileOutputStream(Environment.getExternalStorageDirectory() + "/ServiceLog.txt", true);
        } catch (Exception e) {
            Toast.makeText(this, getResources().getString(R.string.ERROR_FICHERO), Toast.LENGTH_LONG).show();
        }

        batInfo = new BatteryInfoBT();

        ///////////////////////////////////////////////////
        if (bLOGCurrent) {
            iMuestraCorriente = 0;
            for (int i = 0; i < fCorriente.length; i++)
                fCorriente[i] = 0.0f;
        }
        ///////////////////////////////////////////////////


        final TimerTask timerTaskGrabarDatos = new TimerTask() {
            public void run() {
                grabarMedidas();
                //notiticacion();
            }
        };

        timerGrabarDatos = new Timer();
        timerGrabarDatos.scheduleAtFixedRate(timerTaskGrabarDatos, lTiempoGrabacionDatos, lTiempoGrabacionDatos);

        final TimerTask timerTaskGrabarCorriente = new TimerTask() {
            public void run() {
                guardarCorriente();
                //notiticacion();
            }
        };

        if (bLOGCurrent) {
            timerGrabarCorriente = new Timer();
            timerGrabarCorriente.scheduleAtFixedRate(timerTaskGrabarCorriente, lTiempoGrabacionCorriente, lTiempoGrabacionCorriente);
        }

        final TimerTask timerTaskComprobarDesconexion = new TimerTask() {
            public void run() {
                for (int i = 0; i < iNumDevices && !bReiniciar; i++)
                    if (lDatosRecibidos[i] == lDatosRecibidosAnteriores[i] /*&& lDatosRecibidos[i] != 0*/) {
                        bReiniciar = true;
                        String sCadena = sdf.format(new Date()) + " ERROR: No recibidos datos de " + sAddresses[i] + "\n";
                        publishSensorValues(0, MSG, sCadena);
                        publishSensorValues(0, ERROR, "");
                    } else
                        lDatosRecibidosAnteriores[i] = lDatosRecibidos[i];
            }
        };

        timerComprobarDesconexion = new Timer();
        timerComprobarDesconexion.scheduleAtFixedRate(timerTaskComprobarDesconexion, lDelayComprobacionDesconexion, lTiempoComprobacionDesconexion);

        final TimerTask timerTaskGrabarGPS = new TimerTask() {
            public void run() {
                if (mejorLocaliz != null)
                    envioAsync.setGPS(mejorLocaliz.getLatitude(), mejorLocaliz.getLongitude());
            }
        };

        timerGrabarGPS = new Timer();
        if (bSendServer && bLocation)
            timerGrabarGPS.scheduleAtFixedRate(timerTaskGrabarGPS, lTiempoGPS, lTiempoGPS);

        realizarConexiones();

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        //notiticacion();

        return START_NOT_STICKY;
        //return START_STICKY;
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

    private void notiticacion() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId, mBuilder.build());
        notificationId++;
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            Method localMethod = gatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                return ((Boolean) localMethod.invoke(gatt, new Object[0])).booleanValue();
            }
        }
        catch (Exception localException) {
            Log.e("Exception", "ServiceDatos - refreshDeviceCache");
        }
        return false;
    }


    private void realizarConexiones() {
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        BluetoothDevice device;
        for (int i = 0; i < iNumDevices; i++) {
            device = adapter.getRemoteDevice(sAddresses[i]);

            String sCadena = sdf.format(new Date()) + " Solicitud de conexión con " + sAddresses[i].substring(sAddresses[i].length()-2) + "\n";
            enviarMensaje(sCadena);

            btGatt[i] = device.connectGatt(this, true, mBluetoothGattCallback);
            refreshDeviceCache(btGatt[i]);
        }

        bNetConnected = false;
        if (bSendServer) {
            try {
                ConnectivityManager check = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = check.getActiveNetworkInfo();
                if (!info.isConnected()) {
                    String sCadena = sdf.format(new Date()) + getResources().getString(R.string.ERROR_RED) + "\n";
                    enviarMensaje(sCadena);

                    Toast.makeText(this, getResources().getString(R.string.ERROR_RED), Toast.LENGTH_LONG).show();
                } else bNetConnected = true;
            } catch (Exception e) {
                String sCadena = sdf.format(new Date()) + getResources().getString(R.string.ERROR_RED) + "\n";
                enviarMensaje(sCadena);

                Toast.makeText(this, getResources().getString(R.string.ERROR_RED), Toast.LENGTH_LONG).show();
            }

            if (bNetConnected) {
                SharedPreferences pref = getApplicationContext().getSharedPreferences("Settings", MODE_PRIVATE);
                sServer = pref.getString("server", "127.0.0.1");
                iPuerto = pref.getInt("puerto", 8000);

                String sCadena = sdf.format(new Date()) + " Creación de servicio de envío a servidor " + sServer + ":" + iPuerto + "\n";
                enviarMensaje(sCadena);

                envioAsync = new EnvioDatosSocket(sServer, iPuerto, SENSOR_MOV_DATA_LEN + 1);
                envioAsync.start();
            }
        }

        if (bLocation) {
            locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            ultimaLocalizacion();
            activarProveedores();
        }
    }

    private void enviarMensaje(String sMsg) {
        publishSensorValues(0, MSG, sMsg);
        try {
            fLog.write(sMsg.getBytes());
        } catch (IOException e) {
            Log.e("IOException", "ServiceDatos - enviarMensaje");
        }
    }

    private void ultimaLocalizacion() {
        try {
            if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                actualizaMejorLocaliz(locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
            }
            if (locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                actualizaMejorLocaliz(locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
            }
        } catch (SecurityException e) {
            Toast.makeText(this, getResources().getString(R.string.LOCATION_FAILED), Toast.LENGTH_SHORT).show();
        }
    }

    private void activarProveedores() {
        try {
            bGPSEnabled = bNetworkEnabled = false;
            if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                bGPSEnabled = true;
                locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, lTiempoGPS, 0, locListener, Looper.getMainLooper());
            }
            if (locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                bNetworkEnabled = true;
                locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, lTiempoGPS, 0, locListener, Looper.getMainLooper());
            }
            if (!bGPSEnabled && !bNetworkEnabled)
                Toast.makeText(this, getResources().getString(R.string.LOCATION_DISABLED), Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, getResources().getString(R.string.LOCATION_FAILED), Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizaMejorLocaliz(Location localiz) {
        if (localiz != null && (mejorLocaliz == null ||
                localiz.getAccuracy() < 2 * mejorLocaliz.getAccuracy() ||
                localiz.getTime() - mejorLocaliz.getTime() > 20000)) {
            mejorLocaliz = localiz;
        }
    }

    public LocationListener locListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            actualizaMejorLocaliz(location);
            /*if (bSendServer) {
                envioAsync.setGPS(mejorLocaliz.getLatitude(), mejorLocaliz.getLongitude());
            }*/
            try {
                publishSensorValues(LOCALIZACION_LAT, 0, Double.toString(mejorLocaliz.getLatitude()));
                publishSensorValues(LOCALIZACION_LONG, 0, Double.toString(mejorLocaliz.getLongitude()));
            } catch (Exception e) {
                Log.e("Exception", "ServiceDatos - onLocationChanged");
            }
        }

        public void onProviderDisabled(String provider) {
            activarProveedores();
        }

        public void onProviderEnabled(String provider) {
            activarProveedores();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            activarProveedores();
        }
    };

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

    public void guardarCorriente() {
        getBatteryInfo();

        try {
            fCorriente[iMuestraCorriente] = batInfo.getCurrentNow();
            iMuestraCorriente++;
        } catch (Exception e) {
            Log.e("Fichero de resultados", e.getMessage(), e);
        }
    }

    public void grabarMedidas() {
        getBatteryInfo();

        try {
            String sCadena = sdf.format(new Date()) + ":" +
                    batInfo.getBatteryLevel() + ":" +
                    batInfo.getVoltaje() + ":" +
                    batInfo.getTemperature() + ":" +
                    batInfo.getCurrentAverage() + ":" +
                    batInfo.getCurrentNow() + " - ";
            fOut.write(sCadena.getBytes());

            long lDatosRecibidosTotal = 0;
            long lDatosPerdidosTotal = 0;
            sCadena = "";
            for (int i = 0; i < MAX_SENSOR_NUMBER; i++) {
                lDatosRecibidosTotal += lDatosRecibidos[i];
                lDatosPerdidosTotal += lDatosPerdidos[i];
                sCadena += "(" +  lDatosRecibidos[i] + "," + lDatosPerdidos[i] + ")";
            }
            sCadena += "(" +  lDatosRecibidosTotal + "," + lDatosPerdidosTotal + ")\n";
            fOut.write(sCadena.getBytes());
            fOut.flush();
        } catch (Exception e) {
            Log.e("Fichero de resultados", e.getMessage(), e);
        }
    }

    private void cerrarConexiones() {
        bSensing = false;

        String sCadena = sdf.format(new Date()) + " Cerrando conexiones\n";
        try {
            fLog.write(sCadena.getBytes());
        } catch (IOException e) {
            Log.e("IOException", "ServiceDatos - cerrarConexiones");
        }

        grabarMedidas();

        try {
            for (int i = 0; i < fCorriente.length; i++) {
                sCadena = "" + fCorriente[i] + "\n";
                fOutCurrent.write(sCadena.getBytes());
            }
        } catch (Exception e) {}

        //envioAsync.cancel(true);

        for (int i = 0; i < iNumDevices; i++) {
            btGatt[i].disconnect();
            btGatt[i].close();
        }

        if (bLocation)
            locManager.removeUpdates(locListener);

        try {
            if (bLOGCurrent)
                fOutCurrent.close();

            fOut.close();
            fLog.close();
            envioAsync.finalize();
        } catch (Exception e) {
            Log.e("Error - ", "Error cerrando fichero");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        /*if (wifi.isWifiEnabled())
            wifi.setWifiEnabled(false);*/
    }

    private int findGattIndex(BluetoothGatt btGatt) {
        int iIndex = 0;
        String sAddress = btGatt.getDevice().getAddress();

        while (sAddresses[iIndex].compareTo(sAddress) != 0)
            iIndex++;

        return iIndex;
    }

    private final BluetoothGattCallback mBluetoothGattCallback;
    {
        mBluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (status == BluetoothGatt.GATT_SUCCESS)
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        String sAddress = gatt.getDevice().getAddress();
                        String sCadena = sdf.format(new Date()) + " Descubriendo servicios de " + sAddress.substring(sAddress.length()-2) + "\n";
                        publishSensorValues(0, MSG, sCadena);

                        btGatt[findGattIndex(gatt)].discoverServices();
                    }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Se obtiene el primer sensor a activar
                    int iDevice = findGattIndex(gatt);
                    int firstSensor = findFirstSensor(iDevice);
                    // Se actualiza para saber que ya se ha activado
                    bSensores[iDevice][firstSensor] = false;

                    habilitarServicio(gatt, firstSensor);
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Se obtiene el primer sensor a activar
                    int iDevice = findGattIndex(gatt);
                    int firstSensor = findFirstSensor(iDevice);

                    if (firstSensor < 4) {
                        // Se actualiza para saber que ya se ha activado
                        bSensores[iDevice][firstSensor] = false;

                        habilitarServicio(gatt, firstSensor);
                    } else {
                        int firstActivar = firstSensorActivar(iDevice);

                        bActivacion[iDevice][firstActivar] = false;
                        activarServicio(gatt, firstActivar);
                    }
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    int iDevice = findGattIndex(gatt);

                    int firstActivar = firstSensorActivar(iDevice);
                    if (firstActivar < 4) {
                        bActivacion[iDevice][firstActivar] = false;

                        activarServicio(gatt, firstActivar);
                    } else {
                        /*if (bConfigTiempos[iDevice]) {
                            bConfigTiempos[iDevice] = false;
                            configTiempos(gatt, iDevice);
                        } else if (bConfigPeriodoMaxRes[iDevice]) {
                            bConfigPeriodoMaxRes[iDevice] = false;
                            configPeriodoMaxRes(gatt, iDevice);
                        } else*/
                            bSensing = true;
                        //adaptadorDatos.notifyDataSetChanged();
                        int firstPeriodo = firstSensorPeriodo(iDevice);
                        if (firstPeriodo < 4) {
                            bConfigPeriodo[iDevice][firstPeriodo] = false;

                            configPeriodo(gatt, firstPeriodo);
                        }
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                int iDevice = findGattIndex(gatt);

                /*if (characteristic.getUuid().compareTo(UUIDs.UUID_BAR_DATA) == 0) {
                    barometro = characteristic.getValue();
                    procesaBarometro(barometro, findGattIndex(gatt));
                } else if (characteristic.getUuid().compareTo(UUIDs.UUID_OPT_DATA) == 0) {
                    luz = characteristic.getValue();
                    procesaLuz(luz, findGattIndex(gatt));
                } else if (characteristic.getUuid().compareTo(UUIDs.UUID_MOV_DATA) == 0) {*/
                movimiento[iDevice] = characteristic.getValue();

                lDatosRecibidos[iDevice]++;

                boolean bDatosParaEnvio = ((lDatosRecibidos[iDevice] + lMensajesPorSegundo) % lMensajesParaEnvio) == 0;
                procesaMovimiento(movimiento[iDevice], iDevice, bDatosParaEnvio);
                if (bDatosParaEnvio) {
                    String sCadena = "   Recibidos: " + lDatosRecibidos[iDevice] + " - Perdidos: " + lDatosPerdidos[iDevice];
                    publishSensorValues(PAQUETES, iDevice, sCadena);
                }

                if (bPrimerDato[iDevice]) {
                    bPrimerDato[iDevice] = false;
                    iSecuencia[iDevice] = movimiento[iDevice][SENSOR_MOV_SEC_POS];
                } else {
                    iSecuencia[iDevice]++;
                    if (iSecuencia[iDevice] != movimiento[iDevice][SENSOR_MOV_SEC_POS]) {
                        if (iSecuencia[iDevice] > movimiento[iDevice][SENSOR_MOV_SEC_POS])
                            lDatosPerdidos[iDevice] += (256 - iSecuencia[iDevice] + movimiento[iDevice][SENSOR_MOV_SEC_POS]);
                        else
                            lDatosPerdidos[iDevice] += movimiento[iDevice][SENSOR_MOV_SEC_POS] - iSecuencia[iDevice];

                        iSecuencia[iDevice] = movimiento[iDevice][SENSOR_MOV_SEC_POS];
                    }
                }

                try {
                    if (bSendServer) {
                        if (envioAsync == null) {
                            String sCadena = sdf.format(new Date()) + " envioAsync es NULL\n";
                            publishSensorValues(0, MSG, sCadena);
                            //notiticacion();
                            envioAsync = new EnvioDatosSocket(sServer, iPuerto, SENSOR_MOV_DATA_LEN + 1);
                            envioAsync.start();
                        }
                        //if (lDatosRecibidos[iDevice] % 512 == 0)
                        envioAsync.setData((byte) iDevice, movimiento[iDevice]);
                    }
                } catch (Exception e) {
                    String sCadena = sdf.format(new Date()) + " Excepción de envío: " + e.getMessage() + "\n";
                    publishSensorValues(0, MSG, sCadena);
                }

                /*} else if (characteristic.getUuid().compareTo(UUIDs.UUID_HUM_DATA) == 0) {
                    humedad = characteristic.getValue();
                    procesaHumedad(humedad, findGattIndex(gatt));
                } else {
                    Log.e("BluetoothLE", "Dato deslocalizado");
                }*/

            }
        };
    }

    /*private void configTiempos(BluetoothGatt btGatt, int iDevice) {
        String sAddress = btGatt.getDevice().getAddress();
        String sCadena = sdf.format(new Date()) + " Config Tiempos " + sAddress.substring(sAddress.length()-2) + "\n";
        publishSensorValues(0, MSG, sCadena);

        BluetoothGattCharacteristic characteristic;
        characteristic = btGatt.getService(UUID_MOV_SERV).getCharacteristic(UUID_PARAM_CON);
        byte[] highByte = new byte[4];
        byte[] lowByte = new byte[4];

        highByte[0] = (byte) ((iMaxInterval & 0x0000FF00) >> 8);
        lowByte[0] = (byte) (iMaxInterval & 0x000000FF);

        highByte[1] = (byte) ((iMinInterval & 0x0000FF00) >> 8);
        lowByte[1] = (byte) (iMinInterval & 0x000000FF);

        highByte[2] = (byte) ((iLatency & 0x0000FF00) >> 8);
        lowByte[2] = (byte) (iLatency & 0x000000FF);

        highByte[3] = (byte) ((iTimeout & 0x0000FF00) >> 8);
        lowByte[3] = (byte) (iTimeout & 0x000000FF);

        byte [] valor = new byte[]{lowByte[0], highByte[0], lowByte[1], highByte[1], lowByte[2], highByte[2], lowByte[3], highByte[3]};
        characteristic.setValue(valor);
        btGatt.writeCharacteristic(characteristic);
    }*/

    /*private void configPeriodoMaxRes(BluetoothGatt btGatt, int iDevice) {
        String sAddress = btGatt.getDevice().getAddress();
        String sCadena = sdf.format(new Date()) + " Config Periodo Max Res " + sAddress.substring(sAddress.length()-2) + "\n";
        publishSensorValues(0, MSG, sCadena);

        BluetoothGattCharacteristic characteristic;
        characteristic = btGatt.getService(UUID_MOV_SERV).getCharacteristic(UUID_PERIODO);

        byte highByte = (byte) ((iPeriodoMaxRes & 0x0000FF00) >> 8);
        byte lowByte= (byte) (iPeriodoMaxRes & 0x000000FF);

        byte [] valor = new byte[]{lowByte, highByte};
        characteristic.setValue(valor);
        btGatt.writeCharacteristic(characteristic);
    }*/

    private void configPeriodo(BluetoothGatt btGatt, int firstPeriodo) {
        String sAddress = btGatt.getDevice().getAddress();
        String sCadena = sdf.format(new Date()) + " Config periodo " + sAddress.substring(sAddress.length()-2) + "\n";
        publishSensorValues(0, MSG, sCadena);

        BluetoothGattCharacteristic characteristic;

        characteristic = btGatt.getService(getServerUUID(firstPeriodo)).getCharacteristic(getPeriodoUUID(firstPeriodo));
        characteristic.setValue(iPeriodo * 11 / 110, FORMAT_SINT8, 0);
        btGatt.writeCharacteristic(characteristic);
    }

    private void activarServicio(BluetoothGatt btGatt, int firstActivar) {
        String sAddress = btGatt.getDevice().getAddress();
        String sCadena = sdf.format(new Date()) + " Activar servicio en " + sAddress.substring(sAddress.length()-2) + "\n";
        publishSensorValues(0, MSG, sCadena);

        BluetoothGattCharacteristic characteristic;

        characteristic = btGatt.getService(getServerUUID(firstActivar)).getCharacteristic(getConfigUUID(firstActivar));
        byte lowByte = 0;
        switch (firstActivar) {
            case 0:
                if (bMagnetometro) lowByte |= 0b01000000;
                if (bAcelerometro) lowByte |= 0b00111000;
                if (bGiroscopo)    lowByte |= 0b00000111;
                characteristic.setValue(new byte[]{lowByte,1});
                break;
            case 1:
            case 2:
            case 3:
                characteristic.setValue(new byte[]{1});
                break;
        }

        btGatt.writeCharacteristic(characteristic);
    }


    private void habilitarServicio(BluetoothGatt gatt, int firstSensor) {
        String sAddress = gatt.getDevice().getAddress();
        String sCadena = sdf.format(new Date()) + " Habilitar servicio " + sAddress.substring(sAddress.length()-2) + "\n";
        publishSensorValues(0, MSG, sCadena);

        BluetoothGattService service;
        BluetoothGattDescriptor descriptor;
        BluetoothGattCharacteristic characteristic;
        int iPosGatt = findGattIndex(gatt);

        service = btGatt[iPosGatt].getService(getServerUUID(firstSensor));
        characteristic = service.getCharacteristic(getDataUUID(firstSensor));
        gatt.setCharacteristicNotification(characteristic, true);

        descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        btGatt[iPosGatt].writeDescriptor(descriptor);
    }

    private UUID getConfigUUID(int iSensor) {
        UUID UUIDConfig = UUIDs.UUID_MOV_CONF;

        switch (iSensor) {
            case 0:
                UUIDConfig = UUIDs.UUID_MOV_CONF;
                break;
            case 1:
                UUIDConfig = UUIDs.UUID_HUM_CONF;
                break;
            case 2:
                UUIDConfig = UUIDs.UUID_BAR_CONF;
                break;
            case 3:
                UUIDConfig = UUIDs.UUID_OPT_CONF;
                break;
        }

        return UUIDConfig;
    }

    private UUID getDataUUID(int iSensor) {
        UUID UUIDData = UUIDs.UUID_MOV_DATA;

        switch (iSensor) {
            case 0:
                UUIDData = UUIDs.UUID_MOV_DATA;
                break;
            case 1:
                UUIDData = UUIDs.UUID_HUM_DATA;
                break;
            case 2:
                UUIDData = UUIDs.UUID_BAR_DATA;
                break;
            case 3:
                UUIDData = UUIDs.UUID_OPT_DATA;
                break;
        }

        return UUIDData;
    }

    private UUID getServerUUID(int iSensor) {
        UUID UUIDServer = UUIDs.UUID_MOV_SERV;

        switch (iSensor) {
            case 0:
                UUIDServer = UUIDs.UUID_MOV_SERV;
                break;
            case 1:
                UUIDServer = UUIDs.UUID_HUM_SERV;
                break;
            case 2:
                UUIDServer = UUIDs.UUID_BAR_SERV;
                break;
            case 3:
                UUIDServer = UUIDs.UUID_OPT_SERV;
                break;
        }

        return UUIDServer;
    }

    private UUID getPeriodoUUID(int iSensor) {
        UUID UUIDServer = UUIDs.UUID_MOV_PERI;

        switch (iSensor) {
            case 0:
                UUIDServer = UUIDs.UUID_MOV_PERI;
                break;
            case 1:
                UUIDServer = UUIDs.UUID_HUM_PERI;
                break;
            case 2:
                UUIDServer = UUIDs.UUID_BAR_PERI;
                break;
            case 3:
                UUIDServer = UUIDs.UUID_OPT_PERI;
                break;
        }

        return UUIDServer;
    }


    private int findFirstSensor(int iDevice) {
        int first = 0;

        while (first < 4 && !bSensores[iDevice][first])
            first++;

        return first;
    }

    private int firstSensorPeriodo(int iDevice) {
        int first = 0;

        while (first < 4 && !bConfigPeriodo[iDevice][first])
            first++;

        return first;
    }

    private int firstSensorActivar(int iDevice) {
        int first = 0;

        while (first < 4 && !bActivacion[iDevice][first])
            first++;

        return first;
    }

    private void procesaMovimiento(byte[] movimiento, int iDevice, boolean bEnviarDatos) {
        long aux;

        // Giróscopo
        valorGiroX = movimiento[1];
        //valorGiroX &= 0x00000000000000FF;
        valorGiroX = valorGiroX << 8;

        aux = movimiento[0];
        //aux &= 0x00000000000000FF;
        valorGiroX |= aux;
        fValorGiroX = (float) (((float) valorGiroX / 65536.0) * 500.0);

        valorGiroY = movimiento[3];
        //valorGiroY &= 0x00000000000000FF;
        valorGiroY = valorGiroY << 8;

        aux = movimiento[2];
        //aux &= 0x00000000000000FF;
        valorGiroY |= aux;
        fValorGiroY = (float) (((float) valorGiroY / 65536.0) * 500.0);

        valorGiroZ = movimiento[5];
        //valorGiroZ &= 0x00000000000000FF;
        valorGiroZ = valorGiroZ << 8;

        aux = movimiento[4];
        //aux &= 0x00000000000000FF;
        valorGiroZ |= aux;
        fValorGiroZ = (float) (((float) valorGiroZ / 65536.0) * 500.0);

        sCadenaGiroscopo[iDevice] = "G -> X: " + df.format(fValorGiroX) + " " + getString(R.string.GyroscopeUnit) + " ";
        sCadenaGiroscopo[iDevice] += "   Y: " + df.format(fValorGiroY) + " " + getString(R.string.GyroscopeUnit) + " ";
        sCadenaGiroscopo[iDevice] += "   Z: " + df.format(fValorGiroZ) + " " + getString(R.string.GyroscopeUnit);

        //publishSensorValues(GIROSCOPO, iDevice,sCadena);
        if (bEnviarDatos) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = GIROSCOPO;
            msg.arg2 = iDevice;
            mServiceHandler.sendMessage(msg);
        }


        // Acelerómetro
        valorAcelX = movimiento[7];
        //valorAcelX &= 0x00000000000000FF;
        valorAcelX = valorAcelX << 8;

        aux = movimiento[6];
        //aux &= 0x00000000000000FF;
        valorAcelX |= aux;
        fValorAcelX = (float) valorAcelX / (32768f / 4f);

        valorAcelY = movimiento[9];
        //valorAcelY &= 0x00000000000000FF;
        valorAcelY = valorAcelY << 8;

        aux = movimiento[8];
        //aux &= 0x00000000000000FF;
        valorAcelY |= aux;
        fValorAcelY = (float) valorAcelY / (32768f / 4f);

        valorAcelZ = movimiento[11];
        //valorAcelZ &= 0x00000000000000FF;
        valorAcelZ = valorAcelZ << 8;

        aux = movimiento[10];
        //aux &= 0x00000000000000FF;
        valorAcelZ |= aux;
        fValorAcelZ = (float) valorAcelZ / (32768f / 4f);

        sCadenaAcelerometro[iDevice] = "A -> X: " + df.format(fValorAcelX) + " " + getString(R.string.AccelerometerUnit) + " ";
        sCadenaAcelerometro[iDevice] += "   Y: " + df.format(fValorAcelY) + " " + getString(R.string.AccelerometerUnit) + " ";
        sCadenaAcelerometro[iDevice] += "   Z: " + df.format(fValorAcelZ) + " " + getString(R.string.AccelerometerUnit);

        //publishSensorValues(ACELEROMETRO, iDevice,sCadena);
        if (bEnviarDatos) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = ACELEROMETRO;
            msg.arg2 = iDevice;
            mServiceHandler.sendMessage(msg);
        }


        // Magnetómetro
        valorMagX = movimiento[13];
        //valorMagX &= 0x00000000000000FF;
        valorMagX = valorMagX << 8;

        aux = movimiento[12];
        //aux &= 0x00000000000000FF;
        valorMagX |= aux;
        fValorMagX = (float) valorMagX;

        valorMagY = movimiento[15];
        //valorMagY &= 0x00000000000000FF;
        valorMagY = valorMagY << 8;

        aux = movimiento[14];
        //aux &= 0x00000000000000FF;
        valorMagY |= aux;
        fValorMagY = (float) valorMagY;

        valorMagZ = movimiento[17];
        //valorMagZ &= 0x00000000000000FF;
        valorMagZ = valorMagZ << 8;

        aux = movimiento[16];
        //aux &= 0x00000000000000FF;
        valorMagZ |= aux;
        fValorMagZ = (float) valorMagZ;

        sCadenaMagnetometro[iDevice] =  "M -> X: " + fValorMagX + " " + getString(R.string.MagnetometerUnit) + " ";
        sCadenaMagnetometro[iDevice] += "   Y: " + fValorMagY + " " + getString(R.string.MagnetometerUnit) + " ";
        sCadenaMagnetometro[iDevice] += "   Z: " + fValorMagZ + " " + getString(R.string.MagnetometerUnit);

        //publishSensorValues(MAGNETOMETRO, iDevice,sCadena);
        if (bEnviarDatos) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = MAGNETOMETRO;
            msg.arg2 = iDevice;
            mServiceHandler.sendMessage(msg);
        }

    }

    /*private void procesaHumedad(byte humedad[], int iDevice) {
        long aux;

        valorHumedad = humedad[3];
        valorHumedad &= 0x00000000000000FF;
        valorHumedad = valorHumedad << 8;

        aux = humedad[2];
        aux &= 0x00000000000000FF;
        valorHumedad |= aux;

        valorHumedad *= 100;
        fValorHumedad = valorHumedad / 65536;

        String sCadena = Float.toString(fValorHumedad) + " " + getString(R.string.HumidityUnit);
        //listaDatos.setHumedad(iDevice, sCadena);
        publishSensorValues(HUMEDAD, iDevice,sCadena);
    }

    private void procesaLuz(byte luz[], int iDevice) {
        long auxM, auxE;

        auxM = luz[1];
        auxM = auxM << 8;
        auxM |= luz[0];
        auxM &= 0x0000000000000FFF;

        auxE = (auxM & 0x000000000000F000) >> 12;

        auxE = (auxE == 0)?1:2<<(auxE-1);

        fValorLuz = (float)auxM * (((float) auxE) / 100);

        String sCadena = df.format(fValorLuz) + " " + getString(R.string.LightUnit);
        //listaDatos.setLuz(iDevice, sCadena);
        publishSensorValues(LUZ, iDevice,sCadena);
    }

    private void procesaBarometro(byte barometro[], int iDevice) {
        long aux;

        // Barómetro
        valorBarometro = barometro[5];
        valorBarometro &= 0x00000000000000FF;
        valorBarometro = valorBarometro << 8;

        aux = barometro[4];
        aux &= 0x00000000000000FF;
        valorBarometro |= aux;
        valorBarometro = valorBarometro << 8;

        aux = barometro[3];
        aux &= 0x00000000000000FF;
        valorBarometro |= aux;
        fValorBarometro = valorBarometro / 100;

        String sCadena = df.format(fValorBarometro) + " " + getString(R.string.BarometerUnit);
        // listaDatos.setBarometro(iDevice, sCadena);
        publishSensorValues(BAROMETRO, iDevice,sCadena);


        // Temperatura
        valorTemperatura = barometro[2];
        valorTemperatura &= 0x00000000000000FF;
        valorTemperatura = valorTemperatura << 8;

        aux = barometro[1];
        aux &= 0x00000000000000FF;
        valorTemperatura |= aux;
        valorTemperatura = valorTemperatura << 8;

        aux = barometro[0];
        aux &= 0x00000000000000FF;
        valorTemperatura |= aux;
        fValorTemperatura = valorTemperatura / 100;

        sCadena = df.format(fValorTemperatura) + " " + getString(R.string.TemperatureUnit);
        //listaDatos.setTemperatura(iDevice, sCadena);
        publishSensorValues(TEMPERATURA, iDevice,sCadena);
    }*/


    private void publishSensorValues(int iSensor, int iDevice, String sCadena) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra("Sensor", iSensor);
        intent.putExtra("Device", iDevice);
        intent.putExtra("Cadena", sCadena);
        sendBroadcast(intent);
    }
}