package com.samsung.microbit.ui;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.view.View;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;

/**
 * Represents a bluetooth switcher that allows to turn on/off
 * a bluetooth adapter and check current bluetooth state.
 */
public class BluetoothSwitch {

    private static BluetoothSwitch instance = null;

    private static BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothSwitch() {
        if (mBluetoothAdapter == null) {
            final BluetoothManager bluetoothManager = (BluetoothManager) MBApp.getApp().getSystemService(Context
                    .BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    public static BluetoothSwitch getInstance() {
        if (instance == null) {
            instance = new BluetoothSwitch();
        }
        return instance;
    }

    public boolean isBluetoothON() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * Checks if bluetooth adapter is enabled and provides
     * a dialog window to allow a user to enable it.
     *
     * @return True - if bluetooth is already on.
     */
    public boolean checkBluetoothAndStart() {
        if (!mBluetoothAdapter.isEnabled()) {
            MBApp application = MBApp.getApp();

            PopUp.show(application.getString(R.string.bluetooth_turn_on_guide),
                    application.getString(R.string.turn_on_bluetooth),
                    R.drawable.bluetooth, R.drawable.blue_btn,
                    0,
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
