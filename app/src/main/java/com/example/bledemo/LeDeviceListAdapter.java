package com.example.bledemo;

import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/8/24.
 */

public class LeDeviceListAdapter extends RecyclerView.Adapter<LeDeviceListAdapter.ViewHolder> {

    private ArrayList<BluetoothDevice> mLeDevices;

    public LeDeviceListAdapter() {
        super();
        mLeDevices = new ArrayList<BluetoothDevice>();
    }

    public void addDevice(BluetoothDevice device) {
        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device);
        }
    }

    public void clearDevice() {
        mLeDevices.clear();
        notifyDataSetChanged();
    }

    public ArrayList<BluetoothDevice> getData(){
        return mLeDevices;
    }
    @Override
    public LeDeviceListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false));
    }

    @Override
    public void onBindViewHolder(LeDeviceListAdapter.ViewHolder holder, final int position) {
        String name = mLeDevices.get(position).getName();
        final String address = mLeDevices.get(position).getAddress();
        holder.device.setText(TextUtils.isEmpty(name) ? address : name);
        holder.info.setText(position+1+"");
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBus.getDefault().post(new MessageType(MessageType.CONNECT, address,position+1));
            }
        });
    }

    @Override
    public int getItemCount() {
        return mLeDevices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView device;
        private TextView info;

        public ViewHolder(View itemView) {
            super(itemView);
            device = itemView.findViewById(R.id.tv_device);
            info = itemView.findViewById(R.id.tv_connect);
        }
    }
}
