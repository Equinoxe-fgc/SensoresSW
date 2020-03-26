package com.equinoxe.sensoressw;

class BluetoothData {
    private String sAddress;
    private String sMovimiento1;
    private String sMovimiento2;
    private String sMovimiento3;

    private String sPaquetes;

    BluetoothData(String sAddress) {
        this.sAddress = sAddress;
        this.sMovimiento1 = "";
        this.sMovimiento2 = "";
        this.sMovimiento3 = "";

        this.sPaquetes = "";
    }

    String getAddress() {
        return sAddress;
    }

    String getMovimiento1() {
        return sMovimiento1;
    }

    String getMovimiento2() {
        return sMovimiento2;
    }

    String getMovimiento3() {
        return sMovimiento3;
    }

    String getPaquetes() { return sPaquetes;
    }

    void setAddress(String sAddress) {
        this.sAddress = sAddress;
    }

    void setMovimiento1(String sMovimiento1) {
        this.sMovimiento1 = sMovimiento1;
    }

    void setMovimiento2(String sMovimiento2) {
        this.sMovimiento2 = sMovimiento2;
    }

    void setMovimiento3(String sMovimiento3) {
        this.sMovimiento3 = sMovimiento3;
    }

    void setPaquetes(String sPaquetes) { this.sPaquetes = sPaquetes;
    }
}
