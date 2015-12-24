package com.example.arty.bluetalkie.presenters;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;

import com.example.arty.bluetalkie.audio.AudioPlayer;
import com.example.arty.bluetalkie.audio.AudioRecorder;
import com.example.arty.bluetalkie.communication.BluetoothWrapper;
import com.example.arty.bluetalkie.communication.Packet;
import com.example.arty.bluetalkie.utils.ImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by sergey on 24/12/15.
 */
public class CallPresenter {

    private static final int MSG_AVATAR_LOADED = 5076;
    private static final String AVATAR_FILENAME = "avatar.png";


    private CallView callView;

    private AudioPlayer player;
    private AudioRecorder recorder;

    private BluetoothWrapper bluetoothWrapper;
    private BluetoothWrapper.ConnectionThread connectionThread;
    Handler avatarHandler;

    BluetoothSocket socket;

    public CallPresenter(CallView view) {
        callView = view;
        player = AudioPlayer.get(view.getApplicationContext());
        recorder = AudioRecorder.get();
        bluetoothWrapper = BluetoothWrapper.get(view.getApplicationContext());
    }

    public void onCreate(AvatarLoadedListener avatarLoadedListener, DisconnectListener disconnectListener) {
        player.start();

        avatarHandler = new Handler(msg -> {
            if (msg.what == MSG_AVATAR_LOADED) {
                byte[] data = (byte[]) msg.obj;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, msg.arg1);
                if (bitmap != null) {
                    avatarLoadedListener.onAvatarLoaded(bitmap);
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
                    break;
                case BYE:
                    disconnect(disconnectListener);
                    break;
            }
        });
        connectionThread.start();

        Bitmap avatar = ImageUtils.loadImage(callView.getApplicationContext(), AVATAR_FILENAME);
        uploadAvatar(avatar);
    }

    public void startRecording() {
        recorder.start((chunk, chunkLength) -> {
            Packet packet = new Packet(Packet.Type.AUDIO_CHUNK, chunk);
            connectionThread.write(packet);
        });
    }

    public void stopRecording() {
        recorder.stop();
    }

    private void uploadAvatar(Bitmap avatar) {
        Thread uploadAvatarThread = new Thread(() -> {
            if (avatar != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                avatar.compress(Bitmap.CompressFormat.PNG, 100, baos);
                connectionThread.write(new Packet(Packet.Type.AVATAR, baos.toByteArray()));
            }
        });
        uploadAvatarThread.start();
    }

    public void onDisconnect(DisconnectListener disconnectListener) {
        connectionThread.write(new Packet(Packet.Type.BYE, null));
        disconnect(disconnectListener);
    }

    private void disconnect(DisconnectListener disconnectListener) {
        player.stop();
        recorder.stop();
        connectionThread.cancel();
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        disconnectListener.onDisconnect();
    }

    public interface DisconnectListener {
        void onDisconnect();
    }

    public interface AvatarLoadedListener {
        void onAvatarLoaded(Bitmap bitmap);
    }

    public interface CallView {
        Context getApplicationContext();
    }

}
