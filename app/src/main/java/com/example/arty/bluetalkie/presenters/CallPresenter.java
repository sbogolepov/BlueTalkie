package com.example.arty.bluetalkie.presenters;

import android.app.Application;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;

import com.example.arty.bluetalkie.audio.AudioPlayer;
import com.example.arty.bluetalkie.audio.AudioRecorder;
import com.example.arty.bluetalkie.communication.BluetoothWrapper;
import com.example.arty.bluetalkie.communication.Packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by sergey on 24/12/15.
 */
public class CallPresenter {

    private CallView callView;

    private AudioPlayer player;
    private AudioRecorder recorder;

    private BluetoothWrapper bluetoothWrapper;
    private BluetoothWrapper.ConnectionThread connectionThread;
    Handler avatarHandler;

    BluetoothSocket socket;

    private static final int MSG_AVATAR_LOADED = 5076;

    public CallPresenter(CallView view) {
        callView = view;
        player = new AudioPlayer();
        recorder = new AudioRecorder();
        bluetoothWrapper = BluetoothWrapper.get(view.getApplicationContext());
    }

    public void onCreate() {
        player.start();

        avatarHandler = new Handler(msg -> {
            if (msg.what == MSG_AVATAR_LOADED) {
                byte[] data = (byte[]) msg.obj;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, msg.arg1);
                if (bitmap != null) {
                    callView.onAvatarLoaded(bitmap);
                }
            }
            return true;
        });

        connectionThread = bluetoothWrapper.createConnectionThread(bluetoothWrapper.getSocket(), packet -> {
            switch (packet.getType()) {
                case AUDIO_CHUNK:
                    player.addChunk(packet.getData());
                    break;
                case AVATAR:
                    byte[] data = packet.getData();
                    byte[] c = new byte[data.length];
                    System.arraycopy(data, 0, c, 0, data.length);
                    avatarHandler.obtainMessage(MSG_AVATAR_LOADED, c.length, -1, c).sendToTarget();
            }
        });
        connectionThread.start();
    }

    public void onStartRecording() {
        recorder.start((chunk, chunkLength) -> {
            Packet packet = new Packet(Packet.Type.AUDIO_CHUNK, chunk);
            connectionThread.write(packet);
        });
    }

    public void onStopRecording() {
        recorder.stop();
    }

    public void uploadAvatar(Bitmap avatar) {
        Thread uploadAvatarThread = new Thread(() -> {
            if (avatar != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                avatar.compress(Bitmap.CompressFormat.PNG, 100, baos);
                connectionThread.write(new Packet(Packet.Type.AVATAR, baos.toByteArray()));
            }
        });
        uploadAvatarThread.start();
    }

    public void onDisconnect() {
        player.stop();
        recorder.stop();
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        callView.onDisconnect();
    }

    public interface CallView {
        Context getApplicationContext();

        void onAvatarLoaded(Bitmap bitmap);

        void onDisconnect();
    }

}
