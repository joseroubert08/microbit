package com.samsung.microbit.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.model.ConnectedDevice;
import com.samsung.microbit.data.model.NameValuePair;
import com.samsung.microbit.utils.ServiceUtils;

import java.util.UUID;

import static com.samsung.microbit.BuildConfig.DEBUG;

/**
 * Inter process communication service that used to handle interaction between
 * {@link BLEService} and {@link PluginService}.
 */
public class IPCService extends Service {
    private static final String TAG = IPCService.class.getSimpleName();

    public static final String INTENT_MICROBIT_BUTTON_NOTIFICATION = "com.samsung.microbit.service.IPCService.INTENT_MICROBIT_BUTTON_NOTIFICATION";

    public static final String INTENT_BLE_NOTIFICATION = "com.samsung.microbit.service.IPCService.INTENT_BLE_NOTIFICATION";
    public static final String INTENT_MICROBIT_NOTIFICATION = "com.samsung.microbit.service.IPCService.INTENT_MICROBIT_NOTIFICATION";

    public static final String NOTIFICATION_CAUSE = "com.samsung.microbit.service.IPCService.CAUSE";

    public static void logi(String message) {
        Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    public IPCService() {
        super();
        startIPCListener();
    }

    private void startIPCListener() {
        if (DEBUG) {
            logi("startIPCListener()");
            logi("make :: ipc start");
        }

        IPCMessageManager ipcMessageManager = IPCMessageManager.getInstance();

        IPCMessageManager.configureMessageManager("IPCServiceListener", new IPCMessagesHandler());

        //TODO why we need that check
        if(ipcMessageManager == null) {
		    /*
             * Make the initial connection to other processes
    	     */
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(IPCMessageManager.STARTUP_DELAY);
                        logi("make :: ipc send");
                        ServiceUtils.sendtoBLEService(IPCService.class, IPCMessageManager.MESSAGE_ANDROID,
                                 EventCategories.IPC_INIT, null, null);
                        ServiceUtils.sendtoPluginService(IPCService.class, IPCMessageManager.MESSAGE_ANDROID,
                                 EventCategories.IPC_INIT, null, null);
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }).start();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) {
            logi("onStartCommand()");
        }

        return START_STICKY;
    }

	/*
     * Business method
	 */

    public static void bleDisconnect() {
        ServiceUtils.sendtoBLEService(IPCService.class, IPCMessageManager.MESSAGE_ANDROID, EventCategories
                 .IPC_BLE_DISCONNECT, null, null);
    }

    public static void bleConnect() {
        ServiceUtils.sendtoBLEService(IPCService.class, IPCMessageManager.MESSAGE_ANDROID, EventCategories
                .IPC_BLE_CONNECT, null, null);
    }

    public static void bleReconnect() {
        ServiceUtils.sendtoBLEService(IPCService.class, IPCMessageManager.MESSAGE_ANDROID, EventCategories
                 .IPC_BLE_RECONNECT, null, null);
    }

    public static void writeCharacteristic(UUID service, UUID characteristic, int value, int type) {
        NameValuePair[] args = new NameValuePair[4];
        args[0] = new NameValuePair(IPCMessageManager.BUNDLE_SERVICE_GUID, service.toString());
        args[1] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_GUID, characteristic.toString());
        args[2] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_VALUE, value);
        args[3] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_TYPE, type);
        ServiceUtils.sendtoBLEService(IPCService.class, IPCMessageManager.MESSAGE_ANDROID, EventCategories
                .IPC_WRITE_CHARACTERISTIC, null, args);
    }

    /*
     * setup IPCMessageManager
     */
    @Override
    public IBinder onBind(Intent intent) {
        return IPCMessageManager.getInstance().getClientMessenger().getBinder();
    }

    private static class IPCMessagesHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            handleIncomingMessage(msg);
        }
    }

    private static void handleIncomingMessage(Message msg) {
        if (DEBUG) {
            logi("IPCService :: handleIncomingMessage()");
            logi("IPC :: count = " + IPCMessageManager.getInstance().getServicesCount());
        }

        if (msg.what == IPCMessageManager.MESSAGE_ANDROID) {
            if (DEBUG) {
                logi("handleIncomingMessage() :: IPCMessageManager.MESSAGE_ANDROID msg.arg1 = " + msg.arg1);
            }

            if (msg.arg1 == EventCategories.IPC_BLE_NOTIFICATION_GATT_CONNECTED ||
                    msg.arg1 == EventCategories.IPC_BLE_NOTIFICATION_GATT_DISCONNECTED) {

                Context appContext = MBApp.getApp();

                ConnectedDevice cd = BluetoothUtils.getPairedMicrobit(MBApp.getApp());
                cd.mStatus = (msg.arg1 == EventCategories.IPC_BLE_NOTIFICATION_GATT_CONNECTED);
                BluetoothUtils.setPairedMicroBit(appContext, cd);
            }

            int errorCode = (int) msg.getData().getSerializable(IPCMessageManager.BUNDLE_ERROR_CODE);

            String error_message = (String) msg.getData().getSerializable(IPCMessageManager.BUNDLE_ERROR_MESSAGE);

            String firmware = (String) msg.getData().getSerializable(IPCMessageManager.BUNDLE_MICROBIT_FIRMWARE);

            int microbitRequest = -1;
            if (msg.getData().getSerializable(IPCMessageManager.BUNDLE_MICROBIT_REQUESTS) != null) {
                microbitRequest = (int) msg.getData().getSerializable(IPCMessageManager.BUNDLE_MICROBIT_REQUESTS);
            }

            Intent intent = new Intent(INTENT_BLE_NOTIFICATION);
            intent.putExtra(NOTIFICATION_CAUSE, msg.arg1);
            intent.putExtra(IPCMessageManager.BUNDLE_ERROR_CODE, errorCode);
            intent.putExtra(IPCMessageManager.BUNDLE_ERROR_MESSAGE, error_message);
            intent.putExtra(IPCMessageManager.BUNDLE_MICROBIT_FIRMWARE, firmware);
            intent.putExtra(IPCMessageManager.BUNDLE_MICROBIT_REQUESTS, microbitRequest);

            LocalBroadcastManager.getInstance(MBApp.getApp()).sendBroadcast(intent);
        } else if (msg.what == IPCMessageManager.MESSAGE_MICROBIT) {
            if (DEBUG) {
                logi("handleIncomingMessage() :: IPCMessageManager.MESSAGE_MICROBIT msg.arg1 = " + msg.arg1);
            }

            Intent intent = new Intent(INTENT_MICROBIT_NOTIFICATION);
            LocalBroadcastManager.getInstance(MBApp.getApp()).sendBroadcast(intent);
        }
    }
}
