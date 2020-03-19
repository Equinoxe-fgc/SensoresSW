package com.equinoxe.sensoressw;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends WearableActivity {
    final private String SENSORTAG_STRING = "CC2650 SensorTag";

    private boolean bScanning = false;
    private BluetoothLeScanner scanner;

    private Button btnScan, btnConnect;
    private RecyclerView recyclerView;
    private MiAdaptador adaptador;
    private RecyclerView.LayoutManager layoutManager;
    private final Handler handler = new Handler();

    private BluetoothDeviceInfoList btDeviceInfoList;

    private int iContador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnScan = findViewById(R.id.btnScan);
        btnConnect = findViewById(R.id.btnConnect);
        recyclerView = findViewById(R.id.recycler_view);

        btDeviceInfoList = new BluetoothDeviceInfoList();
        adaptador = new MiAdaptador(this, btDeviceInfoList, this);
        layoutManager = new LinearLayoutManager(this);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        iContador = 0;
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if (bScanning) {
                    iContador++;
                    if (btDeviceInfoList.getSize() != 0)
                        adaptador.notifyDataSetChanged();
                    if (iContador == 4)
                        btnScanOnClick(btnScan);
                } else
                    iContador = 0;

                handler.postDelayed(this, 1000);
            }
        });

        btnScanOnClick(btnScan);

        // Enables Always-on
        setAmbientEnabled();
    }

    public void btnScanOnClick(View v) {
        if (bScanning) {
            btnScan.setText(getString(R.string.scan));
            scanner.stopScan(mScanCallback);

            if (btDeviceInfoList.getSize() != 0) {
                recyclerView.setAdapter(adaptador);
                recyclerView.setLayoutManager(layoutManager);
            }
        } else {
            btnConnect.setVisibility(View.INVISIBLE);

            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mBluetoothAdapter.isEnabled()) {
                Toast.makeText(this, getString(R.string.EnableBluetooth), Toast.LENGTH_LONG).show();
                return;
            }

            btDeviceInfoList.clearAllBluetoothDeviceInfo();
            adaptador.notifyDataSetChanged();

            scanner = mBluetoothAdapter.getBluetoothLeScanner();

            checkForPermissions();
            scanner.startScan(mScanCallback);

            btnScan.setText(getString(R.string.stop));
        }

        bScanning = !bScanning;
    }

    private void checkForPermissions() {
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION} , 1);
            }

            permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }

            permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1);
            }
    }


    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (!btDeviceInfoList.isPresent(device.getAddress())) {
                String sName = device.getName();
                if (sName != null && sName.compareTo(SENSORTAG_STRING) == 0) {
                    BluetoothDeviceInfo btDeviceInfo = new BluetoothDeviceInfo(false, device.getName(), device.getAddress());
                    btDeviceInfoList.addBluetoothDeviceInfo(btDeviceInfo);
                    //btnScanOnClick(null);
                }
            }
        }
    };

    public void btnConnectClick(View v) {
        int iNumSelected = btDeviceInfoList.getNumSelected();

        Intent intent = new Intent(this, Conexion.class);
        intent.putExtra("NumDevices", iNumSelected);

        int iPos = 0;
        for (int i = 0; i < btDeviceInfoList.getSize(); i++)
            if (btDeviceInfoList.getBluetoothDeviceInfo(i).isSelected()) {
                intent.putExtra("Address" + iPos, btDeviceInfoList.getBluetoothDeviceInfo(i).getAddress());
                iPos++;
            }

        startActivity(intent);
    }

    public void notifySomeSelected(boolean bSomeSelected) {
        if (bSomeSelected)
            btnConnect.setVisibility(View.VISIBLE);
        else
            btnConnect.setVisibility(View.INVISIBLE);
    }

    public void connectOne(int iPos) {
        Intent intent = new Intent(this, Conexion.class);
        intent.putExtra("NumDevices", 1);
        intent.putExtra("Address0", btDeviceInfoList.getBluetoothDeviceInfo(iPos).getAddress());

        startActivity(intent);
    }
}
