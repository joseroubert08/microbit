package com.samsung.microbit.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.data.constants.CharacteristicUUIDs;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.GattFormats;
import com.samsung.microbit.data.constants.GattServiceUUIDs;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.model.NameValuePair;
import com.samsung.microbit.plugin.AbstractPlugin;
import com.samsung.microbit.plugin.PluginsCreator;
import com.samsung.microbit.utils.ServiceUtils;

import java.lang.ref.WeakReference;

import static com.samsung.microbit.BuildConfig.DEBUG;

public class PluginService extends Service {

    private static final String TAG = PluginService.class.getSimpleName();

    //MBS Services
    public static final int ALERT = 0;
    public static final int FEEDBACK = 1;
    public static final int INFORMATION = 2;
    public static final int AUDIO = 3;
    public static final int REMOTE_CONTROL = 4;
    public static final int TELEPHONY = 5;
    public static final int CAMERA = 6;
    public static final int FILE = 7;

    public static void logi(String message) {
        Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    private PluginsCreator pluginsCreator;

    public PluginService() {
        super();
        startIPCListener();
    }

    private void startIPCListener() {
        if (DEBUG) {
            logi("startIPCListener()");
            logi("make :: plugin start");
        }

        pluginsCreator = new PluginsCreator();

        IPCMessageManager ipcMessageManager = IPCMessageManager.getInstance();

        IPCMessageManager.configureMessageManager("PluginServiceReceiver", new PluginMessagesHandler(this));

        //TODO check if we really need that
        if (ipcMessageManager == null) {
            /*
			 * Make the initial connection to other processes
			 */
            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        Thread.sleep(IPCMessageManager.STARTUP_DELAY);

                        logi("make :: plugin send");

                        ServiceUtils.sendToBLEService(PluginService.class, IPCMessageManager.MESSAGE_ANDROID,
                                EventCategories.IPC_INIT, null, null);
                        ServiceUtils.sendToIPCService(PluginService.class, IPCMessageManager.MESSAGE_ANDROID,
                                EventCategories.IPC_INIT, null, null);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) {
            logi("onStartCommand() :: start");
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        pluginsCreator.destroy();
    }

	/*
     * IPC Messenger handling
	 */

    @Override
    public IBinder onBind(Intent intent) {
        return IPCMessageManager.getInstance().getClientMessenger().getBinder();
    }

    public static void sendMessageToBle(int value) {
        //TODO: instance is null because anysystem broadcast receiver run inside different process than the process who built the PluginService instance.
        Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + "sendMessageToBle()");
        NameValuePair[] args = new NameValuePair[4];
        args[0] = new NameValuePair(IPCMessageManager.BUNDLE_SERVICE_GUID, GattServiceUUIDs.EVENT_SERVICE.toString());
        args[1] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_GUID, CharacteristicUUIDs.ES_CLIENT_EVENT.toString());
        args[2] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_VALUE, value);
        args[3] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_TYPE, GattFormats.FORMAT_UINT32);
        ServiceUtils.sendToBLEService(PluginService.class, IPCMessageManager.MESSAGE_MICROBIT,
                EventCategories.IPC_WRITE_CHARACTERISTIC, null, args);
    }

    private static class PluginMessagesHandler extends Handler {
        private final WeakReference<PluginService> pluginServiceWeakReference;

        private PluginMessagesHandler(PluginService pluginService) {
            this.pluginServiceWeakReference = new WeakReference<>(pluginService);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(pluginServiceWeakReference.get() != null) {
                if (DEBUG) {
                    logi("startIPCListener().handleMessage");
                }
                pluginServiceWeakReference.get().handleIncomingMessage(msg);
            }
        }
    }

    private void handleIncomingMessage(Message msg) {
        if (DEBUG) {
            logi("PluginService :: handleIncomingMessage()");
            logi("Plugin :: count = " + IPCMessageManager.getInstance().getServicesCount());
        }

        if (msg.what == IPCMessageManager.MESSAGE_ANDROID) {
            if (DEBUG) {
                logi("handleIncomingMessage() :: IPCMessageManager.MESSAGE_ANDROID msg.arg1 = " + msg.arg1);
            }

            handleAndroidMessage(msg);
        } else if (msg.what == IPCMessageManager.MESSAGE_MICROBIT) {
            if (DEBUG) {
                logi("handleIncomingMessage() :: IPCMessageManager.MESSAGE_MICROBIT msg.arg1 = " + msg.arg1);
            }

            handleMicroBitMessage(msg);
        }
    }

    /**
     * Handler of incoming messages from BLEListener.
     */
    private void handleMicroBitMessage(Message msg) {
        if (DEBUG) {
            logi("PluginService :: handleMessage()");
        }

        Bundle data = msg.getData();
        CmdArg cmd = new CmdArg(data.getInt(IPCMessageManager.BUNDLE_DATA), data.getString(IPCMessageManager.BUNDLE_VALUE));

        if (DEBUG) {
            logi("handleMessage() ## msg.arg1 = " + msg.arg1);
            logi("handleMessage() ## data.getInt=" + data.getInt(IPCMessageManager.BUNDLE_DATA));
            logi("handleMessage() ## data.getString=" + data.getString(IPCMessageManager.BUNDLE_VALUE));
        }

        AbstractPlugin abstractPlugin = pluginsCreator.createPlugin(msg.arg1);
        abstractPlugin.handleEntry(cmd);
    }

    private void handleAndroidMessage(Message msg) {
        if (msg.arg1 == EventCategories.IPC_PLUGIN_STOP_PLAYING) {
            AbstractPlugin abstractPlugin = pluginsCreator.createPlugin(EventCategories.SAMSUNG_ALERTS_ID);
            abstractPlugin.handleEntry(new CmdArg(EventSubCodes.SAMSUNG_ALERT_STOP_PLAYING, null));
        }
    }
}
