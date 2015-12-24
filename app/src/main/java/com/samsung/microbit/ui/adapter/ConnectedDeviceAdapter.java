package com.samsung.microbit.ui.adapter;


import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        parentActivity = context;
    }

    public void updateAdapter(List<ConnectedDevice> list) {
        mListConnectedDevice = list;
        notifyDataSetChanged();
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
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(MBApp.getContext());
            convertView = inflater.inflate(R.layout.connected_device_item, null);
        }

        TextView deviceConnectionStatus = (TextView) convertView.findViewById(R.id.device_status_txt);
        deviceConnectionStatus.setTypeface(MBApp.getApp().getTypeface());

        LinearLayout itemLayout = (LinearLayout) convertView.findViewById(R.id.connected_device_adapter_item);
        Button deviceName = (Button) convertView.findViewById(R.id.deviceName);
        deviceName.setTypeface(MBApp.getApp().getTypeface());
        deviceName.setFocusable(false);
        deviceName.setFocusableInTouchMode(false);
        deviceName.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ImageButton connectBtn = (ImageButton) convertView.findViewById(R.id.connectBtn);
        connectBtn.setFocusable(false);
        connectBtn.setFocusableInTouchMode(false);
        connectBtn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ImageButton deleteBtn = (ImageButton) convertView.findViewById(R.id.deleteBtn);
        deleteBtn.setFocusable(false);
        deleteBtn.setFocusableInTouchMode(false);
        deleteBtn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        // set name and pattern
        if (entry == null || entry.mPattern == null) {
            deviceName.setEnabled(false);
            connectBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
        } else {
            String styledText;
            if (entry.mName != null)
                styledText = entry.mName;
            else
                styledText = entry.mPattern;
            deviceName.setText(styledText);
            deviceName.setEnabled(false);

            if (!entry.mStatus) {
                connectBtn.setImageResource(R.drawable.device_status_disconnected);
                itemLayout.setBackgroundResource(R.drawable.grey_btn);
                itemLayout.setAlpha(1.0f);
                deviceName.setTextColor(Color.WHITE);
                deviceConnectionStatus.setText(R.string.most_recent_device_status);
                deviceConnectionStatus.setAlpha(1.0f);
            } else {
                connectBtn.setImageResource(R.drawable.device_connected);
                itemLayout.setBackgroundResource(R.drawable.white_btn);
                itemLayout.setAlpha(1.0f);
                deviceName.setTextColor(Color.BLACK);
                deviceConnectionStatus.setText(R.string.device_connected_device_status);
                deviceConnectionStatus.setAlpha(1.0f);
            }
            deviceName.setTag(pos);
            connectBtn.setTag(pos);
            deleteBtn.setTag(pos);

            // disabled state
            if (PairingActivity.disableListView()) {
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
