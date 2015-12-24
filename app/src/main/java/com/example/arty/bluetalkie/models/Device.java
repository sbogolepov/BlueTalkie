package com.example.arty.bluetalkie.models;

import android.bluetooth.BluetoothDevice;


/**
 * Created by arty on 19.12.15.
 */
public final class Device {
    private final BluetoothDevice device;

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

    @Override
    public boolean equals(Object o) {
        if (o instanceof Device) {
            return getName().equals(((Device) o).getName()) && getAddress().equals(((Device) o).getAddress());
        } else {
            return false;
        }
    }
}
