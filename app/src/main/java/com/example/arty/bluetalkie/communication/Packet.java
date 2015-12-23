package com.example.arty.bluetalkie.communication;

import android.util.Log;

import java.io.Serializable;

/**
 * Created by sergey on 22/12/15.
 */
public class Packet implements Serializable {

    private static final String LOG_TAG = Packet.class.getName();


    public enum Type {
        FLOOD(0),
        AVATAR(1),
        AUDIO_CHUNK(2),
        BYE(3);

        int code;

        Type(int code) {
            this.code = code;
        }

    }

    private byte[] data;
    private Type type;

    public Packet(Type type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    private static void d(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
