package com.example.arty.bluetalkie.audio;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by sergey on 21/12/15.
 */
public final class AudioRecorder {

    private static final String LOG_TAG = AudioRecorder.class.getName();

    private static AudioRecorder audioRecorder;

    private final AudioRecord audioRecord;
    private boolean isRecording;
    private int recBufSize;

    public static AudioRecorder get() {
        if (audioRecorder == null) {
            audioRecorder = new AudioRecorder();
        }
        return audioRecorder;
    }

    private AudioRecorder() {

        recBufSize = AudioRecord.getMinBufferSize(AudioSettings.FREQUENCY,
                AudioSettings.CHANNEL_CONF_IN, AudioSettings.AUDIO_ENCODING);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AudioSettings.FREQUENCY,
                AudioSettings.CHANNEL_CONF_IN, AudioSettings.AUDIO_ENCODING, recBufSize*3);
        NoiseSuppressor.create(audioRecord.getAudioSessionId());
    }

    public void start(AudioChunkReceiver receiver) {
        new Thread() {
            byte[] buffer = new byte[recBufSize];
            @Override
            public void run() {
                audioRecord.startRecording();
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    Log.d(LOG_TAG, e.getMessage());
                }
                isRecording = true;
                while (isRecording) {
                    int readSize = audioRecord.read(buffer, 0, recBufSize);
                    receiver.onChunkReceived(buffer, readSize);
                }
                audioRecord.stop();
            }
        }.start();
    }

    public void stop() {
        isRecording = false;
    }

    private static void d(String msg) {
        Log.d(LOG_TAG, msg);
    }

    public interface AudioChunkReceiver {
        void onChunkReceived(byte[] chunk, int chunkLength);
    }
}
