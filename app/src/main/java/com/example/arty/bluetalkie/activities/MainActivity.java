package com.example.arty.bluetalkie.activities;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;

import com.example.arty.bluetalkie.adapters.DevicesAdapter;
import com.example.arty.bluetalkie.databinding.ActivityMainBinding;
import com.example.arty.bluetalkie.presenters.MainPresenter;
import com.example.arty.bluetalkie.utils.ImageUtils;
import com.example.arty.bluetalkie.views.ImagePicker;
import com.example.arty.bluetalkie.R;
import com.example.arty.bluetalkie.communication.BluetoothWrapper;
import com.example.arty.bluetalkie.models.Device;

public class MainActivity extends AppCompatActivity implements MainPresenter.MainView {

    private static final String LOG_TAG = MainActivity.class.getName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PICK_IMAGE = 1;
    private static final String AVATAR_FILENAME = "avatar.png";

    private MainPresenter presenter;

    private ActivityMainBinding binding;

    private Bitmap bitmap;

    private BluetoothWrapper bluetoothWrapper;

    private Handler connectionHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setSupportActionBar(binding.toolbar);

        presenter = new MainPresenter(this);


        binding.fab.setOnClickListener(view -> {
            Intent chooseImageIntent = ImagePicker.getPickImageIntent(this);
            startActivityForResult(chooseImageIntent, PICK_IMAGE);
        });


        DevicesAdapter adapter = new DevicesAdapter(device -> {
            bluetoothWrapper.startClientThread(device.getDevice(), socket -> {
                connectionHandler.obtainMessage(BluetoothWrapper.CONNECTION_ESTABLISHED, socket).sendToTarget();
            });
        });
        binding.rvDevices.setAdapter(adapter);
        binding.rvDevices.setLayoutManager(new LinearLayoutManager(this));



        bitmap = ImageUtils.loadImage(getApplicationContext(), AVATAR_FILENAME);
        if (bitmap != null) {
            binding.avatarView.setImageBitmap(ThumbnailUtils.extractThumbnail(bitmap, 300, 300));
        }
        else {
            binding.avatarView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.default_avatar));
        }

        bluetoothWrapper = BluetoothWrapper.get(getApplication());
        bluetoothWrapper.registerDiscoveredReceiver(device -> adapter.add(new Device(device)));
        if (!bluetoothWrapper.isEnabled()) {
            startActivityForResult(bluetoothWrapper.enable(), REQUEST_ENABLE_BT);
        }

        connectionHandler = new Handler(msg -> {
            if (msg.what == BluetoothWrapper.CONNECTION_ESTABLISHED) {
                BluetoothSocket socket = (BluetoothSocket) msg.obj;
                bluetoothWrapper.storeSocket(socket);
                Intent call = new Intent(this, CallActivity.class);
                startActivity(call);
            }
            return true;
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
                startActivity(bluetoothWrapper.makeDiscoverable());
                bluetoothWrapper.startServerThread("Hello", socket -> {
                    connectionHandler.obtainMessage(BluetoothWrapper.CONNECTION_ESTABLISHED, socket).sendToTarget();
                });
                bluetoothWrapper.startDiscovery();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case PICK_IMAGE:
                bitmap = ImagePicker.getImageFromResult(this, resultCode, data);
                if (bitmap == null)
                    break;
                ImageUtils.saveImage(getApplicationContext(), AVATAR_FILENAME, bitmap);
                binding.avatarView.setImageBitmap(ThumbnailUtils.extractThumbnail(bitmap, 300, 300));
                break;
        }
    }
}
