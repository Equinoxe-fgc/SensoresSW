package com.equinoxe.sensoressw;

class BluetoothData {
    private String sAddress;
    private String sHumedad;
    private String sBarometro;
    private String sLuz;
    private String sTemperatura;
    private String sMovimiento1;
    private String sMovimiento2;
    private String sMovimiento3;

    private String sPaquetes;

    BluetoothData(String sAddress) {
        /*this.sHumedad = "";
        this.sBarometro = "";
        this.sLuz = "";
        this.sTemperatura = "";*/
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

    String getHumedad() {
        return sHumedad;
    }

    String getBarometro() {
        return sBarometro;
    }

    String getLuz() {
        return sLuz;
    }

    String getTemperatura() {
        return sTemperatura;
    }

    void setHumedad(String sHumedad) {
        this.sHumedad = sHumedad;
    }

    void setBarometro(String sBarometro) {
        this.sBarometro = sBarometro;
    }

    void setLuz(String sLuz) {
        this.sLuz = sLuz;
    }

    void setTemperatura(String sTemperatura) {
        this.sTemperatura = sTemperatura;
    }

    void setPaquetes(String sPaquetes) { this.sPaquetes = sPaquetes;
    }
}
