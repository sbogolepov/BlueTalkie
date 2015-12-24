package com.example.arty.bluetalkie.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;
import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by sergey on 21/12/15.
 */
public final class AudioPlayer {
    private final AudioTrack audioTrack;
    private boolean isPlaying;

    private static final String LOG_TAG = AudioPlayer.class.getName();

    private static AudioPlayer audioPlayer;

    final BlockingQueue<byte[]> queue;

    public static AudioPlayer get(Context ctx) {
        if (audioPlayer == null) {
            audioPlayer = new AudioPlayer(ctx);
        }
        return audioPlayer;
    }

    private AudioPlayer(Context ctx) {
        int playBufSize = AudioTrack.getMinBufferSize(AudioSettings.FREQUENCY, AudioSettings.CHANNEL_CONF_OUT,
                AudioSettings.AUDIO_ENCODING);
        audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, AudioSettings.FREQUENCY,
                AudioSettings.CHANNEL_CONF_OUT, AudioSettings.AUDIO_ENCODING, playBufSize * 3, AudioTrack.MODE_STREAM);
        queue = new ArrayBlockingQueue<>(1);
        audioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());

    }

    public void start() {
        new Thread() {
            @Override
            public void run() {
                byte[] buffer;
                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                audioTrack.play();
                isPlaying = true;
                int count = 0;
                while (isPlaying) {
                    try {
                        buffer = queue.take();
                        d("Buffer to play:" + Arrays.toString(buffer));
                        if (buffer != null && buffer.length > 0) {
                            audioTrack.write(buffer, 0, buffer.length);
                        }
                    } catch (InterruptedException e) {
                        d("ERROR: " + e.getMessage());
                    }
                }
                audioTrack.stop();
            }
        }.start();
    }

    public synchronized void addChunk(byte[] chunk) {
        byte[] b = new byte[chunk.length];
        System.arraycopy(chunk, 0, b, 0, chunk.length);
        d("Array to put in queue: " + Arrays.toString(b));
        try {
            queue.put(b);
        } catch (InterruptedException e) {
            d("ERROR: " + e.getMessage());
        }
    }

    public synchronized void stop() {
        isPlaying = false;
        queue.clear();
        audioTrack.flush();
    }

    public void d(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
