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
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;


public class ServiceDatos extends Service {
    final static long lTiempoComprobacionDesconexion = 5 * 1000;  // Tiempo cada cuanto se comprueba si ha habido desconexión

    final static long lDelayComprobacionDesconexion = 60000;

    final static int SENSOR_MOV_DATA_LEN = 19;
    final static int SENSOR_MOV_SEC_POS = SENSOR_MOV_DATA_LEN - 1;

    public static final String NOTIFICATION = "com.equinoxe.bluetoothle.android.service.receiver";

    public static final int CHANNEL_ID = 128;

    String[] sCadenaGiroscopo;
    String[] sCadenaMagnetometro;
    String[] sCadenaAcelerometro;

    private long lMensajesParaEnvio;
    private long lMensajesPorSegundo;

    SimpleDateFormat sdf;

    FileOutputStream fOut;
    FileOutputStream fLog;
    BatteryInfoBT batInfo;

    private int iNumDevices;
    private int iPeriodo;
    private long lTiempoRefrescoDatos;
    private boolean bInternadDevice;

    long valorGiroX, valorGiroY, valorGiroZ;
    float fValorGiroX, fValorGiroY, fValorGiroZ;
    long valorAcelX, valorAcelY, valorAcelZ;
    float fValorAcelX, fValorAcelY, fValorAcelZ;
    long valorMagX, valorMagY, valorMagZ;
    float fValorMagX, fValorMagY, fValorMagZ;

    boolean bLocation;
    boolean bLogStats;
    boolean bLogData;
    String sFileNameDataLog;
    FileOutputStream fOutDataLog;

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

    byte[][] movimiento;
    boolean bSensing;
    DecimalFormat df;

    private boolean bAcelerometro, bGiroscopo, bMagnetometro;
    private String[] sAddresses = new String[Datos.MAX_SENSOR_NUMBER];
    boolean bSendServer;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    Timer timerComprobarDesconexion;
    Timer timerGrabarDatos;

    boolean bReiniciar = false;
    boolean bReinicio;

    String sServer;
    int iPuerto;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case Datos.GIROSCOPO:
                    publishSensorValues(Datos.GIROSCOPO, msg.arg2, sCadenaGiroscopo[msg.arg2]);
                    break;
                case Datos.MAGNETOMETRO:
                    publishSensorValues(Datos.MAGNETOMETRO, msg.arg2, sCadenaMagnetometro[msg.arg2]);
                    break;
                case Datos.ACELEROMETRO:
                    publishSensorValues(Datos.ACELEROMETRO, msg.arg2, sCadenaAcelerometro[msg.arg2]);
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
        super.onDestroy();

        if (bLogStats)
            timerGrabarDatos.cancel();
        timerComprobarDesconexion.cancel();

        /*if (bSendServer)
            timerGrabarGPS.cancel();*/

        cerrarConexiones();

        wakeLock.release();
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

        // Necesario para los logs
        bLocation = intent.getBooleanExtra("Location", false);
        bSendServer = intent.getBooleanExtra("SendServer", false);

        bLogStats = intent.getBooleanExtra("LogStats", true);
        bLogData = intent.getBooleanExtra("LogData", false);
        sFileNameDataLog = intent.getStringExtra("FileNameDataLog");

        bReinicio = intent.getBooleanExtra("Reinicio", false);

        bInternadDevice = intent.getBooleanExtra("InternalDevice", false);

        bSensores = new boolean[iNumDevices][4];
        bActivacion = new boolean[iNumDevices][4];
        bConfigPeriodo = new boolean[iNumDevices][4];

        // Se inicializan todos los sensores porque se guarda la estadística de todos aunque no se usen
        lDatosRecibidos = new long[Datos.MAX_SENSOR_NUMBER];
        lDatosPerdidos = new long[Datos.MAX_SENSOR_NUMBER];
        iSecuencia = new byte[iNumDevices];
        bPrimerDato = new boolean[iNumDevices];
        lDatosRecibidosAnteriores = new long[iNumDevices];

        for (int i = 0; i < Datos.MAX_SENSOR_NUMBER; i++) {
            if (i < iNumDevices) {
                bSensores[i][0] = bActivacion[i][0] = bConfigPeriodo[i][0] = bAcelerometro || bGiroscopo || bMagnetometro;

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
        sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK);

        if (bLogData) {
            sdf = new SimpleDateFormat("HHmmss_SS", Locale.UK);
            try {
                fOutDataLog = new FileOutputStream(sFileNameDataLog, true);
            } catch (Exception e) {
                Toast.makeText(this, getResources().getString(R.string.ERROR_FICHERO), Toast.LENGTH_LONG).show();
            }
        }
        
        if (bLogStats) {
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
                    for (int i = 0; i < Datos.MAX_SENSOR_NUMBER; i++) {
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
                    String sModel = Build.MODEL;
                    sModel = sModel.replace(" ", "_");
                    String sCadena =  sModel + " " + iNumDevices + " " + iPeriodo + " " + bLocation + " " + bSendServer + " " + currentDateandTime + "\n";
                    fOut.write(sCadena.getBytes());
                    fOut.flush();
                }

                fLog = new FileOutputStream(Environment.getExternalStorageDirectory() + "/ServiceLog.txt", true);
            } catch (Exception e) {
                Toast.makeText(this, getResources().getString(R.string.ERROR_FICHERO), Toast.LENGTH_LONG).show();
            }
        }

        batInfo = new BatteryInfoBT();

        if (bLogStats) {
            final TimerTask timerTaskGrabarDatos = new TimerTask() {
                public void run() {
                    grabarMedidas();
                }
            };

            timerGrabarDatos = new Timer();
            timerGrabarDatos.scheduleAtFixedRate(timerTaskGrabarDatos, Datos.lTiempoGrabacionDatos, Datos.lTiempoGrabacionDatos);
        }

        final TimerTask timerTaskComprobarDesconexion = new TimerTask() {
            public void run() {
                int i = bInternadDevice?1:0;
                for (; i < iNumDevices && !bReiniciar; i++)
                    if (lDatosRecibidos[i] == lDatosRecibidosAnteriores[i] /*&& lDatosRecibidos[i] != 0*/) {
                        bReiniciar = true;
                        String sCadena = sdf.format(new Date()) + " ERROR: No recibidos datos de " + sAddresses[i] + "\n";
                        publishSensorValues(0, Datos.MSG, sCadena);
                        publishSensorValues(0, Datos.ERROR, "");
                    } else
                        lDatosRecibidosAnteriores[i] = lDatosRecibidos[i];
            }
        };

        timerComprobarDesconexion = new Timer();
        timerComprobarDesconexion.scheduleAtFixedRate(timerTaskComprobarDesconexion, lDelayComprobacionDesconexion, lTiempoComprobacionDesconexion);

        realizarConexiones();

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return START_NOT_STICKY;
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


    private void realizarConexiones() {
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        BluetoothDevice device;
        for (int i = 0; i < iNumDevices; i++) {
            btGatt[i] = null;
            if (sAddresses[i].compareTo(getString(R.string.Internal_Address)) == 0)
                continue;

            device = adapter.getRemoteDevice(sAddresses[i]);

            String sCadena = sdf.format(new Date()) + " Solicitud de conexión con " + sAddresses[i].substring(sAddresses[i].length()-2) + "\n";
            enviarMensaje(sCadena);

            btGatt[i] = device.connectGatt(this, true, mBluetoothGattCallback);
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
    }

    private void enviarMensaje(String sMsg) {
        publishSensorValues(0, Datos.MSG, sMsg);

        if (bLogStats) {
            try {
                fLog.write(sMsg.getBytes());
            } catch (IOException e) {
                Log.e("IOException", "ServiceDatos - enviarMensaje");
            }
        }
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

            long lDatosRecibidosTotal = 0;
            long lDatosPerdidosTotal = 0;
            sCadena = "";
            for (int i = 0; i < Datos.MAX_SENSOR_NUMBER; i++) {
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

        /*if (bLogData)
            try {
                fOutDataLog.close();
            } catch (Exception e) {} */

        if (bLogStats) {
            String sCadena = sdf.format(new Date()) + " Cerrando conexiones\n";
            try {
                fLog.write(sCadena.getBytes());
            } catch (IOException e) {
                Log.e("IOException", "ServiceDatos - cerrarConexiones");
            }

            grabarMedidas();
        }

        for (int i = 0; i < iNumDevices; i++) {
            if (btGatt[i] != null) {
                btGatt[i].disconnect();
                btGatt[i].close();
            }
        }

        try {
            if (bLogStats) {
                fOut.close();
                fLog.close();
            }
            if (bSendServer)
                envioAsync.finalize();
        } catch (Exception e) {
            Log.e("Error - ", "Error cerrando fichero");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
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
                        publishSensorValues(0, Datos.MSG, sCadena);

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

                    if (firstSensor < 4) {
                        // Se actualiza para saber que ya se ha activado
                        bSensores[iDevice][firstSensor] = false;

                        habilitarServicio(gatt, firstSensor);
                    }
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
                        bSensing = true;

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

                movimiento[iDevice] = characteristic.getValue();

                lDatosRecibidos[iDevice]++;

                //boolean bDatosParaEnvio = bLogData || ((lDatosRecibidos[iDevice] + lMensajesPorSegundo) % lMensajesParaEnvio) == 0;
                boolean bDatosParaEnvio = ((lDatosRecibidos[iDevice] + lMensajesPorSegundo) % lMensajesParaEnvio) == 0;
                procesaMovimiento(movimiento[iDevice], iDevice, bDatosParaEnvio);
                if (bDatosParaEnvio) {
                    String sCadena = "   Recibidos: " + lDatosRecibidos[iDevice] + " - Perdidos: " + lDatosPerdidos[iDevice];
                    publishSensorValues(Datos.PAQUETES, iDevice, sCadena);
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
                            publishSensorValues(0, Datos.MSG, sCadena);

                            envioAsync = new EnvioDatosSocket(sServer, iPuerto, SENSOR_MOV_DATA_LEN + 1);
                            envioAsync.start();
                        }
                        envioAsync.setData((byte) iDevice, movimiento[iDevice]);
                    }
                } catch (Exception e) {
                    String sCadena = sdf.format(new Date()) + " Excepción de envío: " + e.getMessage() + "\n";
                    publishSensorValues(0, Datos.MSG, sCadena);
                }
            }
        };
    }

    private void configPeriodo(BluetoothGatt btGatt, int firstPeriodo) {
        String sAddress = btGatt.getDevice().getAddress();
        String sCadena = sdf.format(new Date()) + " Config periodo " + sAddress.substring(sAddress.length()-2) + "\n";
        publishSensorValues(0, Datos.MSG, sCadena);

        BluetoothGattCharacteristic characteristic;

        characteristic = btGatt.getService(getServerUUID(firstPeriodo)).getCharacteristic(getPeriodoUUID(firstPeriodo));
        characteristic.setValue(iPeriodo * 11 / 110, FORMAT_SINT8, 0);
        btGatt.writeCharacteristic(characteristic);
    }

    private void activarServicio(BluetoothGatt btGatt, int firstActivar) {
        String sAddress = btGatt.getDevice().getAddress();
        String sCadena = sdf.format(new Date()) + " Activar servicio en " + sAddress.substring(sAddress.length()-2) + "\n";
        publishSensorValues(0, Datos.MSG, sCadena);

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
        publishSensorValues(0, Datos.MSG, sCadena);

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
        valorGiroX = valorGiroX << 8;

        aux = movimiento[0];
        valorGiroX |= aux;
        fValorGiroX = (float) (((float) valorGiroX / 65536.0) * 500.0);

        valorGiroY = movimiento[3];
        valorGiroY = valorGiroY << 8;

        aux = movimiento[2];
        valorGiroY |= aux;
        fValorGiroY = (float) (((float) valorGiroY / 65536.0) * 500.0);

        valorGiroZ = movimiento[5];
        valorGiroZ = valorGiroZ << 8;

        aux = movimiento[4];
        valorGiroZ |= aux;
        fValorGiroZ = (float) (((float) valorGiroZ / 65536.0) * 500.0);

        sCadenaGiroscopo[iDevice] = "G: " + df.format(fValorGiroX) + " ";
        sCadenaGiroscopo[iDevice] += df.format(fValorGiroY) + " ";
        sCadenaGiroscopo[iDevice] += df.format(fValorGiroZ);

        if (bEnviarDatos) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = Datos.GIROSCOPO;
            msg.arg2 = iDevice;
            mServiceHandler.sendMessage(msg);
        }


        // Acelerómetro
        valorAcelX = movimiento[7];
        valorAcelX = valorAcelX << 8;

        aux = movimiento[6];
        valorAcelX |= aux;
        fValorAcelX = (float) valorAcelX / (32768f / 4f);

        valorAcelY = movimiento[9];
        valorAcelY = valorAcelY << 8;

        aux = movimiento[8];
        valorAcelY |= aux;
        fValorAcelY = (float) valorAcelY / (32768f / 4f);

        valorAcelZ = movimiento[11];
        valorAcelZ = valorAcelZ << 8;

        aux = movimiento[10];
        valorAcelZ |= aux;
        fValorAcelZ = (float) valorAcelZ / (32768f / 4f);

        sCadenaAcelerometro[iDevice] = "A: " + df.format(fValorAcelX) + " ";
        sCadenaAcelerometro[iDevice] += df.format(fValorAcelY) + " ";
        sCadenaAcelerometro[iDevice] += df.format(fValorAcelZ);

        if (bEnviarDatos) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = Datos.ACELEROMETRO;
            msg.arg2 = iDevice;
            mServiceHandler.sendMessage(msg);
        }

        // Magnetómetro
        valorMagX = movimiento[13];
        valorMagX = valorMagX << 8;

        aux = movimiento[12];
        valorMagX |= aux;
        fValorMagX = (float) valorMagX;

        valorMagY = movimiento[15];
        valorMagY = valorMagY << 8;

        aux = movimiento[14];
        valorMagY |= aux;
        fValorMagY = (float) valorMagY;

        valorMagZ = movimiento[17];
        valorMagZ = valorMagZ << 8;

        aux = movimiento[16];
        valorMagZ |= aux;
        fValorMagZ = (float) valorMagZ;

        sCadenaMagnetometro[iDevice] =  "M: " + fValorMagX + " ";
        sCadenaMagnetometro[iDevice] += fValorMagY + " " ;
        sCadenaMagnetometro[iDevice] += fValorMagZ;

        if (bEnviarDatos) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = Datos.MAGNETOMETRO;
            msg.arg2 = iDevice;
            mServiceHandler.sendMessage(msg);
        }

        if (bLogData) {
            try {
                String sCadenaFichero =  sdf.format(new Date()) + ":" + iDevice + ":" + sCadenaAcelerometro[iDevice] +
                                                                                  " " + sCadenaGiroscopo[iDevice] +
                                                                                  " " + sCadenaMagnetometro[iDevice] + "\n";
                fOutDataLog.write(sCadenaFichero.getBytes());
            } catch (Exception e) {}
        }
    }

    private void publishSensorValues(int iSensor, int iDevice, String sCadena) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra("Sensor", iSensor);
        intent.putExtra("Device", iDevice);
        intent.putExtra("Cadena", sCadena);
        sendBroadcast(intent);
    }
}