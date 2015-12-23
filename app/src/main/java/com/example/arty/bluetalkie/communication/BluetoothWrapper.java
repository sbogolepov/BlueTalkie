package com.example.arty.bluetalkie.communication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * Created by sergey on 18/12/15.
 */
public class BluetoothWrapper {

    private static final String LOG_TAG = BluetoothWrapper.class.getName();

    private static final String SERVICE_UUID = "00001101-0000-1000-8000-00805f9b34fb";

    public static final int DATA_RECEIVED = 0;
    public static final int CONNECTION_ESTABLISHED = 1;

    private static BluetoothWrapper wrapper;

    private BluetoothSocket socket;

    private BluetoothAdapter bluetoothAdapter;
    private Context ctx;

    /**
     * @param ctx – Application context
     * @return singleton
     */
    public static BluetoothWrapper get(Context ctx) {
        if (wrapper == null) {
            wrapper = new BluetoothWrapper(ctx);
        }
        return wrapper;
    }

    private BluetoothWrapper(Context ctx) {
        this.ctx = ctx;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void storeSocket(BluetoothSocket socket) {
        this.socket = socket;
    }

    public BluetoothSocket getSocket() {
        return socket;
    }

    /**
     * call startActivityForResult with this intent to enable BT
     *
     * @return intent to enable BT
     */
    public Intent enable() {
        return new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    }

    /**
     * call startActivityForResult with this intent to make device discoverable
     *
     * @return
     */
    public Intent makeDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        return discoverableIntent;
    }

    /**
     * @return is BT enabled?
     */
    public boolean isEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    /**
     * Start device discovery process
     */
    public void startDiscovery() {
        bluetoothAdapter.startDiscovery();
    }

    /**
     * Register broadcast receiver that will track device discovering process
     * Important! Shoud be registered before you call startDiscovery()
     *
     * @param onDeviceDiscoveredCallback – called when single device discovered
     * @return
     */
    public Intent registerDiscoveredReceiver(final OnDeviceDiscoveredCallback onDeviceDiscoveredCallback) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    onDeviceDiscoveredCallback.onDeviceDiscovered(device);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        return ctx.registerReceiver(receiver, intentFilter);
    }

    public Set<BluetoothDevice> getPairedDevices() {
        return bluetoothAdapter.getBondedDevices();
    }

    /**
     * Starts a thread that is listening for incoming connection
     *
     * @param name
     * @param callback called when somebody is connected
     */
    public void startServerThread(String name, OnDeviceConnectedCallback callback) {
        new ServerThread(name, UUID.fromString(SERVICE_UUID), callback).start();
    }

    /**
     * Starts a thread that is connecting to target device
     *
     * @param device
     * @param callback called when connected
     */
    public void startClientThread(BluetoothDevice device, OnDeviceConnectedCallback callback) {
        new ClientThread(device, UUID.fromString(SERVICE_UUID), callback).start();
    }

    /**
     * Thread that will listen for incoming data from socket and process it with handler
     *
     * @param socket
     * @param callback
     * @return
     */
    public ConnectionThread createConnectionThread(BluetoothSocket socket, DataReceivedCallback callback) {
        return new ConnectionThread(socket, callback);
    }

    public interface OnDeviceDiscoveredCallback {
        void onDeviceDiscovered(BluetoothDevice device);
    }

    public interface OnDeviceConnectedCallback {
        void onDeviceConnected(BluetoothSocket socket);
    }

    public class ClientThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        private OnDeviceConnectedCallback callback;

        public ClientThread(BluetoothDevice device, UUID uuid, OnDeviceConnectedCallback callback) {
            this.callback = callback;
            BluetoothSocket tmp = null;
            this.device = device;

            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                d(e.getMessage());
            }
            socket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
            } catch (IOException connectException) {
                d(connectException.getMessage());
                try {
                    socket.close();
                } catch (IOException closeException) {
                }
                return;
            }
            callback.onDeviceConnected(socket);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    public class ServerThread extends Thread {
        private final BluetoothServerSocket serverSocket;
        private OnDeviceConnectedCallback callback;

        public ServerThread(String name, UUID uuid, OnDeviceConnectedCallback callback) {
            BluetoothServerSocket tmp = null;
            this.callback = callback;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
            } catch (IOException e) {
            }
            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = serverSocket.accept();
                    d("accepted connection");
                } catch (IOException e) {
                    break;
                }
                if (socket != null) {
                    callback.onDeviceConnected(socket);
                    d("Callback called");
                    cancel();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public class ConnectionThread extends Thread {

        //public static final int BUFFER_SIZE = 1024 * 8;

        private BluetoothSocket socket;
        private DataReceivedCallback callback;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        public ConnectionThread(BluetoothSocket socket, DataReceivedCallback callback) {

            this.socket = socket;
            this.callback = callback;
            if (socket == null) {
                throw new RuntimeException("BluetoothSocket is null!");
            }
            d("trying to create streams");
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                tmpOut.flush();
            } catch (IOException e) {
                d(e.getMessage());
            }
            try {
                oos = new ObjectOutputStream(tmpOut);
                oos.flush();
                ois = new ObjectInputStream(tmpIn);
            } catch (IOException e) {}
        }

        @Override
        public void run() {
            Packet packet;
            while (true) {
                try {
                    packet = (Packet) ois.readObject();
                    d("Got from socket: " + Arrays.toString(packet.getData()));
                    callback.onDataReceived(packet);
                } catch (IOException | ClassNotFoundException e) {
                    d(e.getMessage());
                    break;
                }
            }
        }


        /**
         * Call from UI thread to write data to target device
         *
         * @param packet
         */
        public synchronized void write(Packet packet) {
            try {
                d("Put to the socket: " + Arrays.toString(packet.getData()));
                oos.writeObject(packet);
                oos.reset();
            } catch (IOException e) {
                d(e.getMessage());
            }
        }

        public void cancel() {
            try {
                oos.close();
                ois.close();
                socket.close();
            } catch (IOException e) {
                d(e.getMessage());
            }
        }
    }

    public interface DataReceivedCallback {
        void onDataReceived(Packet packet);
    }

    private void d(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
