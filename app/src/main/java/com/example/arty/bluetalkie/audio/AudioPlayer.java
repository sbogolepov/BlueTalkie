package com.example.arty.bluetalkie.audio;

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
public class AudioPlayer {
    AudioTrack audioTrack;
    boolean isPlaying;
    int playBufSize;

    private static final String LOG_TAG = AudioPlayer.class.getName();

    final BlockingQueue<byte[]> queue;


    public AudioPlayer() {
        playBufSize = AudioTrack.getMinBufferSize(AudioSettings.FREQUENCY, AudioSettings.CHANNEL_CONFIGURATION,
                AudioSettings.AUDIO_ENCODING);
        audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, AudioSettings.FREQUENCY,
                AudioSettings.CHANNEL_CONFIGURATION, AudioSettings.AUDIO_ENCODING, playBufSize * 2, AudioTrack.MODE_STREAM);
        audioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
        queue = new ArrayBlockingQueue<>(1);
        d("playBufSize = " + String.valueOf(playBufSize));
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

    public void addChunk(byte[] chunk) {
        byte[] b = new byte[chunk.length];
        System.arraycopy(chunk, 0, b, 0, chunk.length);
        d("Array to put in queue: " + Arrays.toString(b));
        try {
            queue.put(b);
        } catch (InterruptedException e) {
            d("ERROR: " + e.getMessage());
        }
    }

    public void stop() {
        isPlaying = false;
        queue.clear();
        audioTrack.flush();
    }

    public void d(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
