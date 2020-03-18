package com.equinoxe.sensoressw;

import java.util.UUID;

import static java.util.UUID.fromString;

public class UUIDs {
    public final static UUID
            //UUID_DEVINFO_SERV = fromString("0000180a-0000-1000-8000-00805f9b34fb"),
            /*UUID_DEVINFO_FWREV = fromString("00002A26-0000-1000-8000-00805f9b34fb"),*/

            // Sensor de humedad y temperatura
            UUID_HUM_SERV = fromString("f000aa20-0451-4000-b000-000000000000"),
            UUID_HUM_DATA = fromString("f000aa21-0451-4000-b000-000000000000"),
            UUID_HUM_CONF = fromString("f000aa22-0451-4000-b000-000000000000"), // 0: disable, 1: enable
            UUID_HUM_PERI = fromString("f000aa23-0451-4000-b000-000000000000"), // Period in tens of milliseconds

            // Sensor de luz
            UUID_OPT_SERV = fromString("f000aa70-0451-4000-b000-000000000000"),
            UUID_OPT_DATA = fromString("f000aa71-0451-4000-b000-000000000000"),
            UUID_OPT_CONF = fromString("f000aa72-0451-4000-b000-000000000000"), // 0: disable, 1: enable
            UUID_OPT_PERI = fromString("f000aa73-0451-4000-b000-000000000000"), // Period in tens of milliseconds

            // Sensor de temperatura y presión
            UUID_BAR_SERV = fromString("f000aa40-0451-4000-b000-000000000000"),
            UUID_BAR_DATA = fromString("f000aa41-0451-4000-b000-000000000000"),
            UUID_BAR_CONF = fromString("f000aa42-0451-4000-b000-000000000000"), // 0: disable, 1: enable
            UUID_BAR_CALI = fromString("f000aa43-0451-4000-b000-000000000000"), // Calibration characteristic
            UUID_BAR_PERI = fromString("f000aa44-0451-4000-b000-000000000000"), // Period in tens of milliseconds

            // Todos los sensores de movimiento Giróscopo, Acelerómetro y Magnetómetro
            UUID_MOV_SERV = fromString("f000aa80-0451-4000-b000-000000000000"),
            UUID_MOV_DATA = fromString("f000aa81-0451-4000-b000-000000000000"),
            UUID_MOV_CONF = fromString("f000aa82-0451-4000-b000-000000000000"), // 0: disable, bit 0: enable x, bit 1: enable y, bit 2: enable z
            UUID_MOV_PERI = fromString("f000aa83-0451-4000-b000-000000000000"), // Period in tens of milliseconds

            UUID_PARAM_CON = fromString("f000aa84-0451-4000-b000-000000000000"),
            UUID_PERIODO   = fromString("f000aa85-0451-4000-b000-000000000000");

            /*UUID_TST_SERV = fromString("f000aa64-0451-4000-b000-000000000000"),
            UUID_TST_DATA = fromString("f000aa65-0451-4000-b000-000000000000"), // Test result

            UUID_KEY_SERV = fromString("0000ffe0-0000-1000-8000-00805f9b34fb"),
            UUID_KEY_DATA = fromString("0000ffe1-0000-1000-8000-00805f9b34fb");*/
}
