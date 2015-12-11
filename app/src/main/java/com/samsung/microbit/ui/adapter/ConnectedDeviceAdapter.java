package com.samsung.microbit.ui.adapter;


import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.ui.activity.PairingActivity;

import java.util.List;

public class ConnectedDeviceAdapter extends BaseAdapter {

    private List<ConnectedDevice> mListConnectedDevice;
    private Context parentActivity;

    public ConnectedDeviceAdapter(Context context, List<ConnectedDevice> list) {
        mListConnectedDevice = list;
        parentActivity=context;
    }

    public void updateAdapter(List<ConnectedDevice> list)
    {
        mListConnectedDevice = list;
        notifyDataSetChanged();;

    }
    @Override
    public int getCount() {
        return mListConnectedDevice.size();
    }

    @Override
    public Object getItem(int pos) {
        return mListConnectedDevice.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        // get selected entry
        ConnectedDevice entry = mListConnectedDevice.get(pos);

        // inflating list view layout if null
        if(convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(MBApp.getContext());
            convertView = inflater.inflate(R.layout.connected_device_item, null);
        }

        Button deviceName = (Button)convertView.findViewById(R.id.deviceName);
        ImageButton connectBtn = (ImageButton)convertView.findViewById(R.id.connectBtn);
        ImageButton deleteBtn = (ImageButton)convertView.findViewById(R.id.deleteBtn);

        // set name and pattern
        if(entry == null || entry.mPattern == null) {
            deviceName.setEnabled(false);
            connectBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
        }
        else {
            String styledText;
            if(entry.mName !=null)
            	styledText = entry.mName;
            else
                styledText = entry.mPattern;
            deviceName.setText(styledText);
            deviceName.setTextAppearance(parentActivity, R.style.p1);
            deviceName.setGravity(Gravity.LEFT);
            deviceName.setEnabled(false);

            if(!entry.mStatus) {
                connectBtn.setImageResource(R.drawable.disconnect_device);
                connectBtn.setBackground(MBApp.getContext().getResources().getDrawable(R.drawable.red_btn));
            } else {
                connectBtn.setImageResource(R.drawable.device_connected);
                connectBtn.setBackground(MBApp.getContext().getResources().getDrawable(R.drawable.green_btn));
            }
            deviceName.setTag(pos);
            connectBtn.setTag(pos);
            deleteBtn.setTag(pos);

            if(PairingActivity.disableListView()) {
                connectBtn.setEnabled(false);
                deleteBtn.setEnabled(false);
            } else {
                connectBtn.setOnClickListener((View.OnClickListener) parentActivity);
                deleteBtn.setOnClickListener((View.OnClickListener) parentActivity);
            }
        }

        return convertView;
    }
}
