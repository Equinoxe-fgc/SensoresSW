package com.equinoxe.sensoressw;

import java.util.ArrayList;
import java.util.List;

class BluetoothDataList {
    private List<BluetoothData> vectorData;

    BluetoothDataList(int iSize, String[] sAddresses) {
        this.vectorData = new ArrayList<>();

        for (int i = 0; i < iSize; i++) {
            vectorData.add(new BluetoothData(sAddresses[i]));
        }
    }

    BluetoothData getBluetoothData(int iPos) {
        return vectorData.get(iPos);
    }

    int getSize() {
        return vectorData.size();
    }

    void setAddress(int iPos, String sAddress) {
        vectorData.get(iPos).setAddress(sAddress);
    }

    void setHumedad(int iPos, String sHumedad) {
        vectorData.get(iPos).setHumedad(sHumedad);
    }

    void setBarometro(int iPos, String sBarometro) {
        vectorData.get(iPos).setBarometro(sBarometro);
    }

    void setLuz(int iPos, String sLuz) {
        vectorData.get(iPos).setLuz(sLuz);
    }

    void setTemperatura(int iPos, String sTemperatura) {
        vectorData.get(iPos).setTemperatura(sTemperatura);
    }

    void setMovimiento1(int iPos, String sMovimiento1) {
        vectorData.get(iPos).setMovimiento1(sMovimiento1);
    }

    void setMovimiento2(int iPos, String sMovimiento2) {
        vectorData.get(iPos).setMovimiento2(sMovimiento2);
    }

    void setMovimiento3(int iPos, String sMovimiento3) {
        vectorData.get(iPos).setMovimiento3(sMovimiento3);
    }

    void setPaquetes(int iPos, String sPaquetes) {
        vectorData.get(iPos).setPaquetes(sPaquetes);
    }
}
