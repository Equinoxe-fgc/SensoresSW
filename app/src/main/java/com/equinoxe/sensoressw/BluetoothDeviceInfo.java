package com.equinoxe.sensoressw;


public class BluetoothDeviceInfo {
    private boolean bSelected;
    private String sName;
    private String sAddress;

    public BluetoothDeviceInfo(boolean bSelected, String sName, String sAddress) {
        this.bSelected = bSelected;
        this.sName = sName;
        this.sAddress = sAddress;
    }

    public boolean isSelected() {
        return bSelected;
    }

    public String getDescription() {
        return sName;
    }

    public String getAddress() {
        return sAddress;
    }

    public void setSelected(boolean bSelected) {
        this.bSelected = bSelected;
    }

    public void setName(String sDescription) {
        this.sName = sDescription;
    }

    public void setAddress(String sAddress) {
        this.sAddress = sAddress;
    }
}
