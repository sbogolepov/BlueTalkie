package com.example.arty.bluetalkie.activities;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;

import com.example.arty.bluetalkie.R;
import com.example.arty.bluetalkie.databinding.ActivityCallBinding;
import com.example.arty.bluetalkie.presenters.CallPresenter;


/**
 * Created by arty on 19.12.15.
 */
public class CallActivity extends AppCompatActivity implements CallPresenter.CallView {

    private ActivityCallBinding binding;
    private static final String LOG_TAG = CallActivity.class.getName();

    private CallPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call);

        presenter = new CallPresenter(this);
        presenter.onCreate(binding.contactAvatarView::setImageBitmap, this::onDisconnect);

        binding.disconnect.setOnClickListener(v ->
                presenter.onDisconnect(this::onDisconnect)
        );

        binding.talk.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    presenter.startRecording();
                    break;
                case MotionEvent.ACTION_UP:
                    presenter.stopRecording();
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

    private void onDisconnect() {
        startActivity(new Intent(this, MainActivity.class));
    }
}
