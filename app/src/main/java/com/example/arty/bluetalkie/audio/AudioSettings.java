package com.example.arty.bluetalkie.audio;

import android.media.AudioFormat;

/**
 * Created by sergey on 22/12/15.
 */
public final class AudioSettings {
    public static final int FREQUENCY = 22050;
    public static final int CHANNEL_CONF_OUT = AudioFormat.CHANNEL_OUT_MONO;
    public static final int CHANNEL_CONF_IN = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioSettings() {}
}
