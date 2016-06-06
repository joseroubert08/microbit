package com.samsung.microbit.core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.Gson;
import com.samsung.microbit.BuildConfig;
import com.samsung.microbit.common.PreferencesInteraction;
import com.samsung.microbit.model.ConnectedDevice;

import java.util.Set;

public class BluetoothUtils {
    private static final String TAG = BluetoothUtils.class.getSimpleName();

    public static final String PREFERENCES_KEY = "Microbit_PairedDevices";
    public static final String PREFERENCES_PAIREDDEV_KEY = "PairedDeviceDevice";

    private static ConnectedDevice pairedDevice = new ConnectedDevice();

    private static final Object LOCK = new Object();

    private static BluetoothUtils instance;

    private boolean isDebug = BuildConfig.DEBUG;

    private void logi(String message) {
        if (isDebug) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    public static BluetoothUtils getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    BluetoothUtils u = new BluetoothUtils();
                    pairedDevice = new ConnectedDevice();
                    instance = u;
                }
            }
        }

        return instance;
    }

    public SharedPreferences getPreferences(Context ctx) {

        logi("getPreferences() :: ctx.getApplicationContext() = " + ctx.getApplicationContext());
        SharedPreferences p = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_MULTI_PROCESS);
        return p;
    }

    public void preferencesInteraction(Context ctx, PreferencesInteraction interAction) {
        synchronized (LOCK) {
            logi("preferencesInteraction()");
            interAction.interAct(getPreferences(ctx));
        }
    }

    public static int getTotalPairedMicroBitsFromSystem() {
        int totalPairedMicroBits = 0;
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice bt : pairedDevices) {
                if (bt.getName().contains("micro:bit")) {
                    ++totalPairedMicroBits;
                }
            }
        }
        return totalPairedMicroBits;
    }

    public static String parse(final BluetoothGattCharacteristic characteristic) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        final byte[] data = characteristic.getValue();
        if (data == null)
            return "";
        final int length = data.length;
        if (length == 0)
            return "";

        final char[] out = new char[length * 3 - 1];
        for (int j = 0; j < length; j++) {
            int v = data[j] & 0xFF;
            out[j * 3] = HEX_ARRAY[v >>> 4];
            out[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            if (j != length - 1)
                out[j * 3 + 2] = '-';
        }
        return new String(out);
    }

    public static boolean inZenMode(Context paramContext) {
        /*
         /**
         * Defines global zen mode.  ZEN_MODE_OFF, ZEN_MODE_IMPORTANT_INTERRUPTIONS,

         public static final String ZEN_MODE = "zen_mode";
         public static final int ZEN_MODE_OFF = 0;
         public static final int ZEN_MODE_IMPORTANT_INTERRUPTIONS = 1;
         public static final int ZEN_MODE_NO_INTERRUPTIONS = 2;
         public static final int ZEN_MODE_ALARMS = 3;
        */
        int zenMode = Settings.Global.getInt(paramContext.getContentResolver(), "zen_mode", 0);
        Log.i("MicroBit", "zen_mode : " + zenMode);
        return (zenMode != 0);
    }

    public static void updateFirmwareMicrobit(Context ctx, String firmware) {
        SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY,
                 Context.MODE_MULTI_PROCESS);
        if (pairedDevicePref.contains(PREFERENCES_PAIREDDEV_KEY)) {
            String pairedDeviceString = pairedDevicePref.getString(PREFERENCES_PAIREDDEV_KEY, null);
            Log.v("BluetoothUtils", "Updating the microbit firmware");
            ConnectedDevice deviceInSharedPref = new ConnectedDevice();
            Gson gson = new Gson();
            deviceInSharedPref = gson.fromJson(pairedDeviceString, ConnectedDevice.class);
            deviceInSharedPref.mfirmware_version = firmware;
            setPairedMicroBit(ctx, deviceInSharedPref);
        }
    }

    public static void updateConnectionStartTime(Context ctx, long time) {
        SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY,
                 Context.MODE_MULTI_PROCESS);
        if (pairedDevicePref.contains(PREFERENCES_PAIREDDEV_KEY)) {
            String pairedDeviceString = pairedDevicePref.getString(PREFERENCES_PAIREDDEV_KEY, null);
            Log.e("BluetoothUtils", "Updating the microbit firmware");
            ConnectedDevice deviceInSharedPref = new ConnectedDevice();
            Gson gson = new Gson();
            deviceInSharedPref = gson.fromJson(pairedDeviceString, ConnectedDevice.class);
            deviceInSharedPref.mlast_connection_time = time;
            setPairedMicroBit(ctx, deviceInSharedPref);
        }
    }

    public static ConnectedDevice getPairedMicrobit(Context ctx) {
        SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY,
                 Context.MODE_MULTI_PROCESS);

        if (pairedDevice == null) {
            pairedDevice = new ConnectedDevice();
        }

        if (pairedDevicePref.contains(PREFERENCES_PAIREDDEV_KEY)) {
            boolean pairedMicrobitInSystemList = false;
            String pairedDeviceString = pairedDevicePref.getString(PREFERENCES_PAIREDDEV_KEY, null);
            Gson gson = new Gson();
            pairedDevice = gson.fromJson(pairedDeviceString, ConnectedDevice.class);
            //Check if the microbit is still paired with our mobile
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                for (BluetoothDevice bt : pairedDevices) {
                    if (bt.getAddress().equals(pairedDevice.mAddress)) {
                        pairedMicrobitInSystemList = true;
                        break;
                    }
                }
            } else {
                //Do not change the list until the Bluetooth is back ON again
                pairedMicrobitInSystemList = true;
            }

            if (!pairedMicrobitInSystemList) {
                Log.e("BluetoothUtils", "The last paired microbit is no longer in the system list. Hence removing it");
                //Return a NULL device & update preferences
                pairedDevice.mPattern = null;
                pairedDevice.mName = null;
                pairedDevice.mStatus = false;
                pairedDevice.mAddress = null;
                pairedDevice.mPairingCode = 0;
                pairedDevice.mfirmware_version = null;
                pairedDevice.mlast_connection_time = 0;

                setPairedMicroBit(ctx, null);
            }
        } else {
            pairedDevice.mPattern = null;
            pairedDevice.mName = null;
        }
        return pairedDevice;
    }

    public static void setPairedMicroBit(Context ctx, ConnectedDevice newDevice) {
        SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY,
                 Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = pairedDevicePref.edit();
        if (newDevice == null) {
            editor.clear();
        } else {
            Gson gson = new Gson();
            String jsonActiveDevice = gson.toJson(newDevice);
            editor.putString(PREFERENCES_PAIREDDEV_KEY, jsonActiveDevice);
        }

        editor.apply();
    }

    //TODO moved to BitUtils
    /*
    // bit position to value mask
    public static int getBitMask(int x) {
        return (0x01 << x);
    }

    // multiple bit positions to value mask
    public static int getBitMask(int[] x) {
        int rc = 0;
        for (int xVal : x) {
            rc |= getBitMask(xVal);
        }

        return rc;
    }

    public int setBit(int v, int x) {
        v |= getBitMask(x);
        return v;
    }

    public int setBits(int v, int[] x) {
        v |= getBitMask(x);
        return v;
    }

    public static int clearBit(int v, int x) {
        v &= ~getBitMask(x);
        return v;
    }

    public static int clearBits(int v, int[] x) {
        v &= ~getBitMask(x);
        return v;
    }

    public static int maskBit(int v, int x) {
        v &= getBitMask(x);
        return v;
    }

    public static int maskBits(int v, int[] x) {
        v &= getBitMask(x);
        return v;
    }*/
}
