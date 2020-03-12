package com.equinoxe.sensoressw;

import java.util.ArrayList;
import java.util.List;


public class BluetoothDeviceInfoList {
    protected List<BluetoothDeviceInfo> vectorDevices;

    public BluetoothDeviceInfoList() {
        vectorDevices = new ArrayList<>();
    }

    public void addBluetoothDeviceInfo(BluetoothDeviceInfo btInfo) {
        vectorDevices.add(btInfo);
    }

    public BluetoothDeviceInfo getBluetoothDeviceInfo(int iPos) {
        return vectorDevices.get(iPos);
    }

    public boolean isPresent(String sAdress) {
        boolean bPresent = false;
        for (int i = 0; i < vectorDevices.size() && !bPresent;i++)
            if (vectorDevices.get(i).getAddress().equals(sAdress))
                bPresent = true;

        return bPresent;
    }

    public int getSize() {
        return vectorDevices.size();
    }

    public int getNumSelected() {
        int iNumDevices = 0;

        for (int i = 0; i < vectorDevices.size(); i++) {
            if (vectorDevices.get(i).isSelected())
                iNumDevices++;
        }

        return iNumDevices;
    }

    public void deleteBluetoothDeviceInfo(int iPos) {
        vectorDevices.remove(iPos);
    }

    public void clearAllBluetoothDeviceInfo() {
        vectorDevices.clear();
    }
}
