package com.equinoxe.sensoressw;

import android.os.Environment;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EnvioDatosSocket extends Thread {
    private final static byte ID_GPS = 127;
    private OutputStream outputStream = null;
    private FileOutputStream fOut = null;
    private Socket socket = null;
    private SimpleDateFormat sdf;
    private byte data[];
    //private byte dataBuffer[][];
    //private int iDeviceToSendBuffer;
    /*private byte dataGPS[];
    private byte bytesLat[];
    private byte bytesLong[];*/
    private boolean bDataToSend = false;
    //private boolean bDataToSendBuffer = false;
    //private boolean bGPSToSend = false;
    private String sServer;
    private int iPuerto;
    private int iTamano;
    //private int iNumDatos;
    //private int iDataLeftToSend;
    private String sCadena;


    public EnvioDatosSocket(String sServer, int iPuerto, int iTamano) {
        this.sServer = sServer;
        this.iPuerto = iPuerto;
        this.iTamano = iTamano;
        data = new byte[iTamano];
        /*dataGPS = new byte[20];

        bytesLat = new byte[8];
        bytesLong = new byte[8];*/

        sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        try {
            fOut = new FileOutputStream(Environment.getExternalStorageDirectory() + "/LOG_Envio.txt", true);
            sCadena = sdf.format(new Date()) + " - Inicio sesión\n";
            fOut.write(sCadena.getBytes());
        } catch (IOException e) {}
    }

    /*public EnvioDatosSocket(String sServer, int iPuerto, int iNumDatos, int iTamano) {
        this.sServer = sServer;
        this.iPuerto = iPuerto;
        this.iTamano = iTamano;
        this.iNumDatos = iNumDatos;
        dataBuffer = new byte[iNumDatos][iTamano];
        /*dataGPS = new byte[20];

        bytesLat = new byte[8];
        bytesLong = new byte[8];*/

        /*sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        try {
            fOut = new FileOutputStream(Environment.getExternalStorageDirectory() + "/LOG_Envio.txt", true);
            sCadena = sdf.format(new Date()) + " - Inicio sesión\n";
            fOut.write(sCadena.getBytes());
        } catch (IOException e) {}
    }*/

    public void setData(byte iDevice, byte data[]) {
        synchronized (this) {
            this.data[0] = iDevice;
            System.arraycopy(data, 0, this.data, 1, iTamano-1);

            bDataToSend = true;
        }
    }

    /*public void setDataBuffer(byte iDevice, byte data[][]) {
        synchronized (this) {
            for (int i = 0; i < iNumDatos; i++) {
                dataBuffer[i][0] = iDevice;
                System.arraycopy(data[i], 0, dataBuffer[i], 1, iTamano - 1);
            }

            bDataToSendBuffer = true;
            iDeviceToSendBuffer = iDevice;
            iDataLeftToSend = iNumDatos;

            Log.d("setDataBuffer", "Datos a enviar: " + iNumDatos);
        }
    }*/

    /*public void setGPS(double dLat, double dLong) {
        synchronized (this) {
            ByteBuffer.wrap(bytesLat).putDouble(dLat);
            ByteBuffer.wrap(bytesLong).putDouble(dLong);

            dataGPS[0] = ID_GPS;
            System.arraycopy(bytesLat, 0, dataGPS, 1, 8);
            System.arraycopy(bytesLong, 0, dataGPS, 9, 8);

            bGPSToSend = true;
        }
    }*/

    private void finishSend() {
        try {
            outputStream.close();
            socket.close();
        } catch (Exception e) {
                try {
                    if (!socket.isClosed())
                        socket.close();
                } catch (Exception ee) {}
        }
    }

    @Override
    public void run() {
        try {
            //String currentDateandTime = sdf.format(new Date()) + "\n";

            socket = new Socket(sServer, iPuerto);
            outputStream = socket.getOutputStream();

            while (!socket.isClosed()) {
                //if (bDataToSend || bGPSToSend) {
                    synchronized (this) {
                        try {
                            if (bDataToSend) {
                                outputStream.write(data);
                                bDataToSend = false;
                            }
                            /*if (bDataToSendBuffer) {
                                outputStream.write(dataBuffer[iNumDatos - iDataLeftToSend]);
                                iDataLeftToSend--;
                                //outputStream.write(dataBuffer[iDeviceToSendBuffer][0]);

                                if (iDataLeftToSend == 0) {
                                    bDataToSendBuffer = false;
                                    Log.d("run", "Fin envio datos");
                                }
                            }*/

                            /*else {
                                outputStream.write(dataGPS);
                                bGPSToSend = false;
                            }*/
                        } catch (Exception e) {
                            sCadena = sdf.format(new Date()) + " While Exception " + e.getMessage() + "\n";
                            fOut.write(sCadena.getBytes());
                            Log.d("EnvioDatosSocket.java", "Error al enviar");
                            e.printStackTrace();

                            // Se cierran las conexiones
                            finishSend();

                            sCadena = sdf.format(new Date()) + " - Reconexión\n";
                            fOut.write(sCadena.getBytes());
                            // Se vuelve a crear la conexión
                            socket = new Socket(sServer, iPuerto);
                            outputStream = socket.getOutputStream();
                        }
                    }
                //}
            }

            fOut.close();
            sCadena = sdf.format(new Date()) + " Socket cerrado\n";
        } catch (Exception e) {
            sCadena = sdf.format(new Date()) + " While Exception " + e.getMessage() + "\n";
            try {
                fOut.write(sCadena.getBytes());
            } catch (IOException ee) {}
            Log.d("EnvioDatosSocket.java", "Error al crear socket o stream");
        }

        try {
            fOut.write(sCadena.getBytes());
            fOut.close();
        } catch (Exception e) {}
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            fOut.close();
        } catch (Exception e) {}

        finishSend();
        super.finalize();
    }
}
