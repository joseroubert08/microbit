package com.samsung.microbit.ui;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;

import java.util.concurrent.CountDownLatch;

public class BluetoothSwitch {

    private static BluetoothSwitch instance = null;

    private static BluetoothAdapter mBluetoothAdapter = null;

    static final String TAG = "BluetoothSwitch";


    private BluetoothSwitch() {
        if (mBluetoothAdapter == null) {
            final BluetoothManager bluetoothManager = (BluetoothManager) MBApp.getContext().getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
    }
    public static BluetoothSwitch getInstance() {
        if ( instance == null) {
            instance = new BluetoothSwitch();
        }
        return instance;
    }

    public boolean isBluetoothON() {
        return mBluetoothAdapter.isEnabled();
    }

    public boolean checkBluetoothAndStart(){
        if (!mBluetoothAdapter.isEnabled()) {
            PopUp.show(MBApp.getContext(),
                    MBApp.getContext().getString(R.string.bluetooth_turn_on_guide),
                    MBApp.getContext().getString(R.string.turn_on_bluetooth),
                    R.drawable.bluetooth, R.drawable.blue_btn,
                    PopUp.TYPE_CHOICE,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            PopUp.hide();
                            mBluetoothAdapter.enable();
                        }
                    }, null);
            return false;
        }
        return true;
    }

}
