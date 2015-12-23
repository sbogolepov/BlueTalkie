package com.example.arty.bluetalkie.models;

import android.bluetooth.BluetoothDevice;


/**
 * Created by arty on 19.12.15.
 */
public class Device {
    private BluetoothDevice device;

    public Device(BluetoothDevice device) {
        this.device = device;
    }

    public String getName() {
        return device.getName();
    }

    public String getAddress() {
        return device.getAddress();
    }

    public BluetoothDevice getDevice() {
        return device;
    }
}
