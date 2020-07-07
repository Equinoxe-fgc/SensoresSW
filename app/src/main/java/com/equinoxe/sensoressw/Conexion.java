package com.equinoxe.sensoressw;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class Conexion extends WearableActivity {
    BluetoothGatt btGatt;
    List<BluetoothGattService> listServices;
    BluetoothServiceInfoList listaServicesInfo;
    private final Handler handler = new Handler();

    int iNumDevices;
    String[] sAddresses = new String[8];
    boolean bInternalDevice;

    private TextView txtPeriodo;
    private Button btnStart;
    private CheckBox chkGPS;
    private CheckBox chkSendServer;
    private TextView txtSendServer;
    private TextView txtSendDatos;
    private CheckBox chkLogCurrent;
    private CheckBox chkLogStats, chkLogData;
    private TextView txtTiempo;
    private RecyclerView recyclerViewSensores;
    private MiAdaptadorSensores adaptadorSensores;
    private RecyclerView.LayoutManager layoutManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conexion);

        recyclerViewSensores = findViewById(R.id.recyclerViewSensores);
        txtPeriodo = findViewById(R.id.txtPeriodo);
        btnStart = findViewById(R.id.btnStart);
        chkGPS = findViewById(R.id.chkGPS);
        chkSendServer = findViewById(R.id.chkEnvioServidor);
        txtSendServer = findViewById(R.id.txtSendServer);
        txtSendDatos = findViewById(R.id.txtSendDatos);

        chkLogStats = findViewById(R.id.chkLogStats);
        chkLogData = findViewById(R.id.chkLogData);
        chkLogCurrent = findViewById(R.id.chkLogConsumoCorriente);
        txtTiempo = findViewById(R.id.txtTiempo);

        listaServicesInfo = new BluetoothServiceInfoList();

        adaptadorSensores = new MiAdaptadorSensores(this, listaServicesInfo);
        layoutManager = new LinearLayoutManager(this);

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        Bundle extras = getIntent().getExtras();
        iNumDevices = extras.getInt("NumDevices");
        bInternalDevice = extras.getBoolean("InternalDevice");

        for (int i = 0; i < iNumDevices; i++)
            sAddresses[i] = extras.getString("Address" + i);

        if (bInternalDevice)
            buscarSensoresInternos();

        if (bInternalDevice && iNumDevices == 1) {
            btnStart.setEnabled(true);
            recyclerViewSensores.setAdapter(adaptadorSensores);
            recyclerViewSensores.setLayoutManager(layoutManager);
        } else {
            BluetoothDevice device = adapter.getRemoteDevice(bInternalDevice?sAddresses[1]:sAddresses[0]);

            btGatt = device.connectGatt(this, false, mBluetoothGattCallback);

            handler.removeCallbacks(sendUpdatesToUI);
        }

        chkLogCurrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtTiempo.setEnabled(((CompoundButton) view).isChecked());
            }
        });

        chkSendServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtSendServer.setEnabled(((CompoundButton) view).isChecked());
                txtSendDatos.setEnabled(((CompoundButton) view).isChecked());
            }
        });
    }

    public void buscarSensoresInternos() {
        BluetoothServiceInfo serviceInfo;
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> lista = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : lista) {
            serviceInfo = null;
            switch (sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    if (!listaServicesInfo.isPresent(getString(R.string.Accelerometer)))
                        serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Accelerometer), getString(R.string.Internal));
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    if (!listaServicesInfo.isPresent(getString(R.string.Gyroscope)))
                        serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Gyroscope), getString(R.string.Internal));
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    if (!listaServicesInfo.isPresent(getString(R.string.Magnetometer)))
                        serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Magnetometer), getString(R.string.Internal));
                    break;
                case Sensor.TYPE_HEART_RATE:
                    if (!listaServicesInfo.isPresent(getString(R.string.HeartRate)))
                        serviceInfo = new BluetoothServiceInfo(true, getString(R.string.HeartRate), getString(R.string.Internal));
                    break;
            }

            if (serviceInfo != null)
                listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            BluetoothGattService service;
            for (int i = 0; i < listServices.size(); i++) {
                service = listServices.get(i);
                if (service != null) {
                    String sServiceName = getServiceName(service.getUuid().toString());
                    if (sServiceName.length() != 0) {
                        BluetoothServiceInfo serviceInfo;

                        if (sServiceName.compareTo(getString(R.string.Motion)) == 0) {   // El servicio de movimiento tiene giróscopo, acelerómetro y magnetómetro
                            if (!listaServicesInfo.isPresent(getString(R.string.Gyroscope))) {
                                serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Gyroscope), service.getUuid().toString());
                                listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                            }
                            if (!listaServicesInfo.isPresent(getString(R.string.Accelerometer))) {
                                serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Accelerometer), service.getUuid().toString());
                                listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                            }
                            if (!listaServicesInfo.isPresent(getString(R.string.Magnetometer))) {
                                serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Magnetometer), service.getUuid().toString());
                                listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                            }

                            btnStart.setEnabled(true);
                        }
                    }
                }
            }

            if (listServices.size() == 0)
                handler.postDelayed(this, 1000); // 1 seconds
            else {
                // Se desconecta una vez encontrados los servicios
                recyclerViewSensores.setAdapter(adaptadorSensores);
                recyclerViewSensores.setLayoutManager(layoutManager);
                btGatt.disconnect();
                btGatt.close();
            }
        }
    };

    String getServiceName(String UUID) {
        String sServiceName = "";

        if (UUID.compareToIgnoreCase(UUIDs.UUID_BAR_SERV.toString()) == 0)
            sServiceName = getString(R.string.Barometer);
        else if (UUID.compareToIgnoreCase(UUIDs.UUID_HUM_SERV.toString()) == 0)
            sServiceName = getString(R.string.Humidity);
        else if (UUID.compareToIgnoreCase(UUIDs.UUID_OPT_SERV.toString()) == 0)
            sServiceName = getString(R.string.Light);
        else if (UUID.compareToIgnoreCase(UUIDs.UUID_MOV_SERV.toString()) == 0)
            sServiceName = getString(R.string.Motion);

        return sServiceName;
    }

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS)
                if (newState == BluetoothGatt.STATE_CONNECTED)
                    btGatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                listServices = gatt.getServices();
                handler.postDelayed(sendUpdatesToUI, 1000); // 1 second
            }
        }
    };

    public void onStartSingle(View v) {
        Intent intent = new Intent(this, Datos.class);
        intent.putExtra("NumDevices", iNumDevices);
        intent.putExtra("InternalDevice", bInternalDevice);

        for (int i = 0; i < iNumDevices; i++)
            intent.putExtra("Address" + i, sAddresses[i]);
        intent.putExtra("Periodo", Integer.valueOf(txtPeriodo.getText().toString()));

        for (int i = 0; i < listaServicesInfo.getSize(); i++) {
            BluetoothServiceInfo serviceInfo = listaServicesInfo.getBluetoothServiceInfo(i);
            String sName = serviceInfo.getName();

            if (sName.compareTo(getString(R.string.Gyroscope)) == 0)
                intent.putExtra(getString(R.string.Gyroscope), serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Accelerometer)) == 0)
                intent.putExtra(getString(R.string.Accelerometer), serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Magnetometer)) == 0)
                intent.putExtra(getString(R.string.Magnetometer), serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.HeartRate)) == 0)
                intent.putExtra(getString(R.string.HeartRate), serviceInfo.isSelected());
        }

        intent.putExtra("LogStats", chkLogStats.isChecked());
        intent.putExtra("LogData", chkLogData.isChecked());
        intent.putExtra("LOGCurrent", chkLogCurrent.isChecked());

        intent.putExtra("Location", chkGPS.isChecked());
        intent.putExtra("SendServer", chkSendServer.isChecked());
        intent.putExtra("timeSendServer", Integer.parseInt(txtSendServer.getText().toString()));
        intent.putExtra("datosSendServer", Integer.parseInt(txtSendDatos.getText().toString()));
        //intent.putExtra("SendServer", false);

        //intent.putExtra("InternalSensor", bInternalDevice);

        if (!chkLogCurrent.isChecked())
            txtTiempo.setText("0");

        long lTime = 1000*Integer.parseInt(txtTiempo.getText().toString());
        intent.putExtra("Time", lTime);

        startActivity(intent);
    }

}
