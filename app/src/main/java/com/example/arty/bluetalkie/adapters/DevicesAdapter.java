package com.example.arty.bluetalkie.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.arty.bluetalkie.R;
import com.example.arty.bluetalkie.models.Device;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by arty on 19.12.15.
 */
public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {
    private final List<Device> mDevices;
    private final AdapterCallback callback;

    public DevicesAdapter(AdapterCallback callback) {
        mDevices = new ArrayList<>();
        this.callback = callback;
    }

    public void add(Device device) {
        if (!mDevices.contains(device)) {
            mDevices.add(device);
            notifyDataSetChanged();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View devicesView = inflater.inflate(R.layout.item_device, parent, false);
        return new ViewHolder(devicesView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        viewHolder.setDevice(mDevices.get(position));
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView addressTextView;

        private Device device;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = (TextView) itemView.findViewById(R.id.device_name);
            addressTextView = (TextView) itemView.findViewById(R.id.device_address);
            itemView.setOnClickListener(v -> callback.onItemClick(device));
        }

        public void setDevice(Device device) {
            this.device = device;
            nameTextView.setText(device.getName());
            addressTextView.setText(device.getAddress());
        }
    }

    public interface AdapterCallback {
        void onItemClick(Device device);
    }
}
