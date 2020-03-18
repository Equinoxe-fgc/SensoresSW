package com.equinoxe.sensoressw;

public class BatteryInfoBT {
    private int iBatteryLevel;
    private float fVoltaje;
    private float fTemperature;
    private float fCurrentAverage;
    private float fCurrentNow;

    BatteryInfoBT() {
        iBatteryLevel = 0;
        fVoltaje = 0;
        fTemperature = 0;
        fCurrentAverage = 0;
        fCurrentNow = 0;
    }

    public int getBatteryLevel() {
        return iBatteryLevel;
    }

    public float getVoltaje() {
        return fVoltaje;
    }

    public float getTemperature() {
        return fTemperature;
    }

    public float getCurrentAverage() {
        return fCurrentAverage;
    }

    public float getCurrentNow() {
        return fCurrentNow;
    }

    public void setBatteryLevel(int iBatteryLevel) {
        this.iBatteryLevel = iBatteryLevel;
    }

    public void setVoltaje(int iVoltaje) {
        this.fVoltaje = iVoltaje / 1000;
    }

    public void setTemperature(int iTemperature) {
        this.fTemperature = iTemperature / 10;
    }

    public void setCurrentAverage(float fCurrentAverage) {
        this.fCurrentAverage = fCurrentAverage / 1000;
    }

    public void setCurrentNow(float fCurrentNow) {
        this.fCurrentNow = fCurrentNow / 1000;
    }
}
