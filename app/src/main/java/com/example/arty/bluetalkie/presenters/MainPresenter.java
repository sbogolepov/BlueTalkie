package com.example.arty.bluetalkie.presenters;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;

import com.example.arty.bluetalkie.communication.BluetoothWrapper;
import com.example.arty.bluetalkie.models.Device;
import com.example.arty.bluetalkie.utils.ImageUtils;
import com.example.arty.bluetalkie.views.ImagePicker;

/**
 * Created by sergey on 24/12/15.
 */
public class MainPresenter {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String AVATAR_FILENAME = "avatar.png";

    private MainView view;

    private BluetoothWrapper bluetoothWrapper;
    private Handler connectionHandler;

    public MainPresenter(MainView view, ConnectionEstablishedListener connectionEstablishedListener) {
        this.view = view;
        bluetoothWrapper = BluetoothWrapper.get(view.getApplicationContext());

        connectionHandler = new Handler(msg -> {
            if (msg.what == BluetoothWrapper.CONNECTION_ESTABLISHED) {
                BluetoothSocket socket = (BluetoothSocket) msg.obj;
                bluetoothWrapper.storeSocket(socket);
                connectionEstablishedListener.onConnectionEstablished();
            }
            return true;
        });
    }

    public void onCreate(BluetoothNotEnabledListener bluetoothNotEnabledListener) {
        if (!bluetoothWrapper.isEnabled()) {
            bluetoothNotEnabledListener.onBluetoothNotEnabled(bluetoothWrapper.enable(), REQUEST_ENABLE_BT);
        }
    }

    public void onDeviceSelected(Device device) {
        bluetoothWrapper.startClientThread(device.getDevice(), socket -> {
            connectionHandler.obtainMessage(BluetoothWrapper.CONNECTION_ESTABLISHED, socket).sendToTarget();
        });
    }

    public void startDiscovery(DeviceDiscoveredListener deviceDiscoveredListener, DiscoveryPermissionListener permissionListener) {
        permissionListener.onPermissionRequired(bluetoothWrapper.makeDiscoverable());
        bluetoothWrapper.registerDiscoveredReceiver(deviceDiscoveredListener::onDeviceDiscovered);
        bluetoothWrapper.startServerThread("Hello", socket -> {
            connectionHandler.obtainMessage(BluetoothWrapper.CONNECTION_ESTABLISHED, socket).sendToTarget();
        });
        bluetoothWrapper.startDiscovery();
    }

    public void loadAvatar(AvatarLoadedListener listener) {
        listener.onAvatarLoaded(ImageUtils.loadImage(view.getApplicationContext(), AVATAR_FILENAME));
    }

    public void onAvatarPicked(int resultCode, Intent data, AvatarLoadedListener listener) {
        Bitmap bitmap = ImagePicker.getImageFromResult(view.getApplicationContext(), resultCode, data);
        if (bitmap != null) {
            ImageUtils.saveImage(view.getApplicationContext(), AVATAR_FILENAME, bitmap);
            listener.onAvatarLoaded(bitmap);
        }
    }

    public interface AvatarLoadedListener {
        void onAvatarLoaded(Bitmap bitmap);
    }

    public interface BluetoothNotEnabledListener {
        void onBluetoothNotEnabled(Intent intent, int request_code);
    }

    public interface DeviceDiscoveredListener {
        void onDeviceDiscovered(BluetoothDevice device);
    }

    public interface DiscoveryPermissionListener {
        void onPermissionRequired(Intent intent);
    }

    public interface ConnectionEstablishedListener {
        void onConnectionEstablished();
    }

    public interface MainView {
        Context getApplicationContext();
    }
}
