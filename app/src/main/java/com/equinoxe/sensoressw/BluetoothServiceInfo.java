package com.equinoxe.sensoressw;

public class BluetoothServiceInfo {
    private boolean bSelected;
    private String sName;
    private String sUUID;

    public BluetoothServiceInfo(boolean bSelected, String sName, String sUUID) {
        this.bSelected = bSelected;
        this.sName = sName;
        this.sUUID = sUUID;
    }

    public boolean isSelected() {
        return bSelected;
    }

    public String getName() {
        return sName;
    }

    public String getUUID() {
        return sUUID;
    }

    public void setSelected(boolean bSelected) {
        this.bSelected = bSelected;
    }

    public void setName(String sName) {
        this.sName = sName;
    }

    public void setUUID(String sUUID) {
        this.sUUID = sUUID;
    }
}
