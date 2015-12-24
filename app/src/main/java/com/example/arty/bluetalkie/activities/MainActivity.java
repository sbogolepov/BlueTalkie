package com.example.arty.bluetalkie.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;

import com.example.arty.bluetalkie.adapters.DevicesAdapter;
import com.example.arty.bluetalkie.databinding.ActivityMainBinding;
import com.example.arty.bluetalkie.presenters.MainPresenter;
import com.example.arty.bluetalkie.views.ImagePicker;
import com.example.arty.bluetalkie.R;
import com.example.arty.bluetalkie.models.Device;

public class MainActivity extends AppCompatActivity implements MainPresenter.MainView {

    private static final String LOG_TAG = MainActivity.class.getName();
    private static final int PICK_IMAGE = 1;

    private MainPresenter presenter;

    private ActivityMainBinding binding;
    private DevicesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setSupportActionBar(binding.toolbar);

        presenter = new MainPresenter(this, () -> {
            startActivity(new Intent(this, CallActivity.class));
        });
        presenter.onCreate(this::startActivityForResult);


        binding.fab.setOnClickListener(view -> {
            Intent chooseImageIntent = ImagePicker.getPickImageIntent(this);
            startActivityForResult(chooseImageIntent, PICK_IMAGE);
        });

        adapter = new DevicesAdapter(presenter::onDeviceSelected);
        binding.rvDevices.setAdapter(adapter);
        binding.rvDevices.setLayoutManager(new LinearLayoutManager(this));

        presenter.loadAvatar(bitmap -> {
            if (bitmap != null) {
                binding.avatarView.setImageBitmap(ThumbnailUtils.extractThumbnail(bitmap, 300, 300));
            } else {
                binding.avatarView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.default_avatar));
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_discover:
                presenter.startDiscovery(device -> adapter.add(new Device(device)), this::startActivity);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE) {
            presenter.onAvatarPicked(resultCode, data, bitmap -> {
                binding.avatarView.setImageBitmap(ThumbnailUtils.extractThumbnail(bitmap, 300, 300));
            });
        }
    }
}