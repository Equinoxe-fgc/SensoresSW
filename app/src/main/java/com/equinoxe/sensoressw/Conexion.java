package com.equinoxe.sensoressw;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

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

    private TextView txtPeriodo;
    private Button btnStart;
    private CheckBox chkGPS;
    private CheckBox chkSendServer;
    private CheckBox chkTiempo;
    private CheckBox chkLogCurrent;
    //private CheckBox chkScreenOn;
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
        chkTiempo = findViewById(R.id.chkTiempo);
        txtTiempo = findViewById(R.id.txtTiempo);

        chkLogCurrent = findViewById(R.id.chkLogConsumoCorriente);

        listaServicesInfo = new BluetoothServiceInfoList();

        adaptadorSensores = new MiAdaptadorSensores(this, listaServicesInfo);
        layoutManager = new LinearLayoutManager(this);

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        Bundle extras = getIntent().getExtras();
        iNumDevices = extras.getInt("NumDevices");
        for (int i = 0; i < iNumDevices; i++)
            sAddresses[i] = extras.getString("Address" + i);

        BluetoothDevice device = adapter.getRemoteDevice(sAddresses[0]);

        btGatt = device.connectGatt(this, false, mBluetoothGattCallback);

        handler.removeCallbacks(sendUpdatesToUI);


        chkTiempo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((CompoundButton) view).isChecked()){
                    txtTiempo.setEnabled(true);
                } else {
                    txtTiempo.setEnabled(false);
                }
            }
        });
    }


    @Override
    public void onBackPressed() {
        //btGatt.disconnect();
        //btGatt.close();

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
                            serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Gyroscope), service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                            serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Accelerometer), service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                            serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Magnetometer), service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);

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
        for (int i = 0; i < iNumDevices; i++)
            intent.putExtra("Address" + i, sAddresses[i]);
        intent.putExtra("Periodo", Integer.valueOf(txtPeriodo.getText().toString()));

        for (int i = 0; i < listaServicesInfo.getSize(); i++) {
            BluetoothServiceInfo serviceInfo = listaServicesInfo.getBluetoothServiceInfo(i);
            String sName = serviceInfo.getName();

            /*if (sName.compareTo(getString(R.string.Humidity)) == 0)
                intent.putExtra("Humedad", serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Barometer)) == 0)
                intent.putExtra("Barometro", serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Light)) == 0)
                intent.putExtra("Luz", serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Temperature)) == 0)
                intent.putExtra("Temperatura", serviceInfo.isSelected());
            else*/ if (sName.compareTo(getString(R.string.Gyroscope)) == 0)
                intent.putExtra("Giroscopo", serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Accelerometer)) == 0)
                intent.putExtra("Acelerometro", serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Magnetometer)) == 0)
                intent.putExtra("Magnetometro", serviceInfo.isSelected());
        }

        intent.putExtra("LOGCurrent", chkLogCurrent.isChecked());

        intent.putExtra("Location", chkGPS.isChecked());
        intent.putExtra("SendServer", chkSendServer.isChecked());
        intent.putExtra("bTime",chkTiempo.isChecked());

        intent.putExtra("InternalSensor", false);

        if (!chkTiempo.isChecked())
            txtTiempo.setText("0");
        long lTime = 1000*Integer.valueOf(txtTiempo.getText().toString());
        intent.putExtra("Time", lTime);

        startActivity(intent);
    }

}
