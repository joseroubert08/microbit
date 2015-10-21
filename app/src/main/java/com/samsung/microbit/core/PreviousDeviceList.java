package com.samsung.microbit.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.service.IPCService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PreviousDeviceList{

    private final String PREFERENCES_PREVDEV_PREFNAME = "PreviousDevices";
    private final String PREFERENCES_PREVDEV_KEY = "PreviousDevicesKey";
    public final int PREVIOUS_DEVICES_MAX = 30;

    private static Context context;
    private static PreviousDeviceList instance;
    ArrayList prevMicrobitList;
    ConnectedDevice[] prevDeviceArray;

    public static PreviousDeviceList getInstance(Context ctx) {

        if (instance == null) {
            PreviousDeviceList u = new PreviousDeviceList();
            instance = u;
            context = ctx;
        }
        return instance;
    }

    void updateGlobalPairedDevice() {

        ConnectedDevice currentDevice = Utils.getPairedMicrobit(MBApp.getContext());

        if ((prevDeviceArray != null) && (prevDeviceArray[0] != null)) {

            if((currentDevice.mPattern != null) && currentDevice.mPattern.equals(prevDeviceArray[0].mPattern))
            {
                // Update existing
                if(currentDevice.mStatus != prevDeviceArray[0].mStatus)
                {
                    // Status has changed
                    if(currentDevice.mStatus)
                        disconnectBluetooth();
                    else
                        connectBluetoothDevice();
                }
                Utils.setPairedMicrobit(MBApp.getContext(), prevDeviceArray[0]);
            } else
            {
                // device changed, disconnect previous and connect new
                disconnectBluetooth();
                Utils.setPairedMicrobit(MBApp.getContext(), prevDeviceArray[0]);
                connectBluetoothDevice();
            }
        } else {
            //Disconnect existing Gatt connection
            if(currentDevice.mPattern != null)
                disconnectBluetooth();

            // Remove existing Microbit
            Utils.setPairedMicrobit(MBApp.getContext(), null);
        }
    }

    void connectBluetoothDevice() {
        IPCService.getInstance().bleConnect();
    }

    void disconnectBluetooth() {
        IPCService.getInstance().bleDisconnect();
    }

    public int size()
    {
        if(prevMicrobitList == null)
            return 0;
        return prevMicrobitList.size();
    }
    /* Microbit list management */
    public ConnectedDevice[] storeMicrobits(ArrayList prevDevList) {
        // used for store arrayList in json format
        SharedPreferences settings;
        SharedPreferences.Editor editor;
        settings = context.getSharedPreferences(PREFERENCES_PREVDEV_PREFNAME, Context.MODE_PRIVATE);
        editor = settings.edit();
        Gson gson = new Gson();
        int totalnum = prevDevList.size();
        if(totalnum > 0 ) {
            prevDeviceArray = (ConnectedDevice[]) prevDevList.toArray(new ConnectedDevice[totalnum]);
            String jsonPrevDevices = gson.toJson(prevDeviceArray, ConnectedDevice[].class);
            editor.putString(PREFERENCES_PREVDEV_KEY, jsonPrevDevices);
        } else {
            prevDeviceArray = null;
            editor.clear();
        }
        editor.commit();
        return prevDeviceArray;
    }

    public ConnectedDevice[] loadPrevMicrobits() {
        // used for retrieving arraylist from json formatted string
        SharedPreferences settings;
        List prevMicrobitTemp;
        prevMicrobitList=null;
        settings = context.getSharedPreferences(PREFERENCES_PREVDEV_PREFNAME, Context.MODE_PRIVATE);
        if (settings.contains(PREFERENCES_PREVDEV_KEY)) {
            String prevDevicesStr = settings.getString(PREFERENCES_PREVDEV_KEY, null);
            if (!prevDevicesStr.equals(null)) {
                Gson gson = new Gson();
                prevDeviceArray = gson.fromJson(prevDevicesStr, ConnectedDevice[].class);
                prevMicrobitTemp = Arrays.asList(prevDeviceArray);
                prevMicrobitList = new ArrayList(prevMicrobitTemp);
                prevMicrobitList.removeAll(Collections.singleton(null));
            }
        }

        ConnectedDevice current = Utils.getPairedMicrobit(context);
        if( (prevDeviceArray !=null) && ((current.mPattern != null) && current.mPattern.equals(prevDeviceArray[0].mPattern)))
        {
            if(current.mStatus != prevDeviceArray[0].mStatus)
                prevDeviceArray[0].mStatus = current.mStatus;
        }
        return prevDeviceArray;

    }

    public int checkDuplicateMicrobit(ConnectedDevice newMicrobit) {
        int duplicateIndex = PREVIOUS_DEVICES_MAX;
        if (prevMicrobitList == null)
            return duplicateIndex;

        for (int i = 0; i < prevMicrobitList.size(); i++) {
            if ((prevDeviceArray[i] != null) && (prevDeviceArray[i].mPattern.equals(newMicrobit.mPattern))) {
                return i;
            }
        }
        return duplicateIndex;
    }

    public void addMicrobit(ConnectedDevice newMicrobit, int oldId) {
        if (prevMicrobitList == null)
            prevMicrobitList = new ArrayList(1);

        // This device already exists in the list, so remove it and add as new
        if (oldId != PREVIOUS_DEVICES_MAX) {
            if (prevDeviceArray[oldId].mStatus) {
                // Do nothing as this device is already in the list and is currently active
                return;
            } else {
                // Remove from list and add again
                prevMicrobitList.remove(oldId);
            }
        }

        // new devices are added to top of the list
        prevMicrobitList.add(0, newMicrobit);
        storeMicrobits(prevMicrobitList);
        updateGlobalPairedDevice();
    }

    public void changeMicrobitName(int index, ConnectedDevice modMicrobit) {

        prevMicrobitList.set(index, modMicrobit);
        storeMicrobits(prevMicrobitList);
        updateGlobalPairedDevice();
    }

    public void changeMicrobitState(int index, ConnectedDevice modMicrobit, boolean isTurnedOn, boolean frombroadcast) {
        prevMicrobitList.remove(index);
        if (isTurnedOn)
            prevMicrobitList.add(0, modMicrobit);  // Active should be first item
        else
            prevMicrobitList.add(index, modMicrobit);

        String dbgDevices = "C ";
        int ind = 0;
        for (Iterator<ConnectedDevice> it = prevMicrobitList.iterator(); it.hasNext(); ) {
            ConnectedDevice st = it.next();
            if (isTurnedOn && (ind != index) && (prevDeviceArray[ind] != null)) {
                if(prevDeviceArray[ind].mStatus) {
                    //disconnectBluetooth();
                    prevDeviceArray[ind].mStatus = false; // toggle previously connected BT OFF
                }
            }
            ind++;
        }

        storeMicrobits(prevMicrobitList);
        if(!frombroadcast)
            updateGlobalPairedDevice();
    }

    public void removeMicrobit(int index) {
        if (prevMicrobitList != null) {
            prevMicrobitList.remove(index);
            //	prevMicrobitList.remove(null);
            storeMicrobits(prevMicrobitList);
            updateGlobalPairedDevice();
        }
    }

}
