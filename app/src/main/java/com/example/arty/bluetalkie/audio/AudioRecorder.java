package com.example.arty.bluetalkie.audio;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by sergey on 21/12/15.
 */
public class AudioRecorder {

    private static final String LOG_TAG = AudioRecorder.class.getName();

    AudioRecord audioRecord;
    boolean isRecording;
    int recBufSize;

    public interface AudioChunkReceiver {
        void onChunkReceived(byte[] chunk, int chunkLength);
    }

    public AudioRecorder() {
        recBufSize = AudioRecord.getMinBufferSize(AudioSettings.FREQUENCY,
                AudioSettings.CHANNEL_CONFIGURATION, AudioSettings.AUDIO_ENCODING);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AudioSettings.FREQUENCY,
                AudioSettings.CHANNEL_CONFIGURATION, AudioSettings.AUDIO_ENCODING, recBufSize);
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
                    //d("Recorded:" + Arrays.toString(buffer));
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
}
