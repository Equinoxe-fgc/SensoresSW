package com.equinoxe.sensoressw;

import java.util.ArrayList;
import java.util.List;

public class BluetoothServiceInfoList {
    protected List<BluetoothServiceInfo> vectorServices;

    public BluetoothServiceInfoList() {
        vectorServices = new ArrayList<>();
    }

    public void addBluetoothServiceInfo(BluetoothServiceInfo btInfo) {
        vectorServices.add(btInfo);
    }

    public BluetoothServiceInfo getBluetoothServiceInfo(int iPos) {
        return vectorServices.get(iPos);
    }

    public boolean isPresent(String sUUID) {
        boolean bPresent = false;
        for (int i = 0; i < vectorServices.size() && !bPresent;i++)
            if (vectorServices.get(i).getUUID().equals(sUUID))
                bPresent = true;

        return bPresent;
    }

    public int getSize() {
        return vectorServices.size();
    }

    public void deleteBluetoothServiceInfo(int iPos) {
        vectorServices.remove(iPos);
    }

    public void clearAllBluetoothServiceInfo() {
        vectorServices.clear();
    }
}
