package com.example.arty.bluetalkie.activities;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;

import com.example.arty.bluetalkie.R;
import com.example.arty.bluetalkie.audio.AudioPlayer;
import com.example.arty.bluetalkie.audio.AudioRecorder;
import com.example.arty.bluetalkie.communication.BluetoothWrapper;
import com.example.arty.bluetalkie.communication.Packet;
import com.example.arty.bluetalkie.databinding.ActivityCallBinding;
import com.example.arty.bluetalkie.utils.ImageUtils;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Created by arty on 19.12.15.
 */
public class CallActivity extends AppCompatActivity {

    private ActivityCallBinding binding;
    private static final String LOG_TAG = CallActivity.class.getName();

    private static final int MSG_AVATAR_LOADED = 5076;

    private BluetoothWrapper bluetoothWrapper;
    private BluetoothWrapper.ConnectionThread connectionThread;
    private AudioPlayer player;
    private AudioRecorder recorder;

    private static String PACKAGE_NAME;
    private static final String AVATAR_FILENAME = "avatar.png";

    BluetoothSocket socket;
    Handler avatarHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call);

        PACKAGE_NAME = getApplicationContext().getPackageName();

        Bitmap avatar = ImageUtils.loadImage(PACKAGE_NAME, AVATAR_FILENAME);

        player = new AudioPlayer();
        recorder = new AudioRecorder();
        player.start();

        binding.disconnect.setOnClickListener(v -> {
            player.stop();
            recorder.stop();
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            startActivity(new Intent(this, MainActivity.class));
        });

        avatarHandler = new Handler(msg -> {
            if (msg.what == MSG_AVATAR_LOADED) {
                byte[] data = (byte[]) msg.obj;
                d("Data length: " + String.valueOf(data.length));
                //Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, msg.arg1);
                //binding.contactAvatarView.setImageBitmap(bitmap);
            }
            return true;
        });

        bluetoothWrapper = BluetoothWrapper.get(getApplication());
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

//        Handler avatarPostHandler = new Handler();
//        avatarPostHandler.post(() -> {
//            if (avatar != null) {
//                ByteBuffer byteBuffer = ByteBuffer.allocate(avatar.getByteCount());
//                avatar.copyPixelsToBuffer(byteBuffer);
//                connectionThread.write(new Packet(Packet.Type.AVATAR, byteBuffer.array()));
//            }
//        });


        binding.talk.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    recorder.start((chunk, chunkLength) -> {
                        Packet packet = new Packet(Packet.Type.AUDIO_CHUNK, chunk);
                        connectionThread.write(packet);
                    });
                    break;
                case MotionEvent.ACTION_UP:
                    recorder.stop();
                    break;
            }
            return true;
        });
    }

    private void d(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
