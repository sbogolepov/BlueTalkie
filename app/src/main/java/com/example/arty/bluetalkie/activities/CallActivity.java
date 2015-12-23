package com.example.arty.bluetalkie.activities;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;

import com.example.arty.bluetalkie.R;
import com.example.arty.bluetalkie.databinding.ActivityCallBinding;
import com.example.arty.bluetalkie.presenters.CallPresenter;
import com.example.arty.bluetalkie.utils.ImageUtils;


/**
 * Created by arty on 19.12.15.
 */
public class CallActivity extends AppCompatActivity implements CallPresenter.CallView {

    private ActivityCallBinding binding;
    private static final String LOG_TAG = CallActivity.class.getName();


    private static final String AVATAR_FILENAME = "avatar.png";

    private CallPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call);

        presenter = new CallPresenter(this);
        presenter.onCreate();

        Bitmap avatar = ImageUtils.loadImage(getApplicationContext(), AVATAR_FILENAME);
        presenter.uploadAvatar(avatar);

        binding.disconnect.setOnClickListener(v -> presenter.onDisconnect());

        binding.talk.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    presenter.onStartRecording();
                    break;
                case MotionEvent.ACTION_UP:
                    presenter.onStopRecording();
                    break;
            }
            return true;
        });
    }

    private void d(String msg) {
        Log.d(LOG_TAG, msg);
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    @Override
    public void onAvatarLoaded(Bitmap bitmap) {
        binding.contactAvatarView.setImageBitmap(bitmap);
    }

    @Override
    public void onDisconnect() {
        startActivity(new Intent(this, MainActivity.class));
    }
}
