package com.equinoxe.sensoressw;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class Settings extends WearableActivity {

    private TextView txtServer;
    private TextView txtPuerto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        txtServer = findViewById(R.id.txtServerIP);
        txtPuerto = findViewById(R.id.txtServerPort);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("Settings", MODE_PRIVATE);
        txtServer.setText(pref.getString("server", "127.0.0.1"));
        String sCadena = "" + pref.getInt("puerto", 8000);
        txtPuerto.setText(sCadena);

        // Enables Always-on
        setAmbientEnabled();
    }

    public void onClickSaveSettings(View v) {
        switch (comprobarSettingsOK()) {
            case 0:
                SharedPreferences pref = getApplicationContext().getSharedPreferences("Settings", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("server", txtServer.getText().toString());
                editor.putInt("puerto", Integer.parseInt(txtPuerto.getText().toString()));
                editor.apply();

                Toast.makeText(this, getResources().getText(R.string.Options_saved), Toast.LENGTH_SHORT).show();

                finish();
                break;
            case 1:
                Toast.makeText(this, getResources().getText(R.string.Incorrect_IP), Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(this, getResources().getText(R.string.Incorrect_Port), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private int comprobarSettingsOK() {
        String sServer = txtServer.getText().toString();
        if (!Patterns.IP_ADDRESS.matcher(sServer).matches())
            return 1;

        try {
            int iPuerto = Integer.parseInt(txtPuerto.getText().toString());
            if (iPuerto < 0 || iPuerto > 65535)
                return 2;
        } catch (Exception e) {
            return 2;
        }

        return 0;
    }
}