package com.samsung.microbit.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.data.constants.CharacteristicUUIDs;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.GattFormats;
import com.samsung.microbit.data.constants.GattServiceUUIDs;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.model.NameValuePair;
import com.samsung.microbit.plugin.AlertPlugin;
import com.samsung.microbit.plugin.AudioPlugin;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.plugin.InformationPlugin;
import com.samsung.microbit.plugin.RemoteControlPlugin;
import com.samsung.microbit.plugin.TelephonyPlugin;
import com.samsung.microbit.utils.ServiceUtils;

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

    public PluginService() {
        super();
        startIPCListener();
    }

    private void startIPCListener() {
        if (DEBUG) {
            logi("startIPCListener()");
            logi("make :: plugin start");
        }

        if (IPCMessageManager.getInstance() == null) {
            if (DEBUG) {
                logi("startIPCListener() :: IPCMessageManager.getInstance() == null");
            }

            IPCMessageManager inst = IPCMessageManager.getInstance("PluginServiceReceiver", new android.os.Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (DEBUG) {
                        logi("startIPCListener().handleMessage");
                    }
                    handleIncomingMessage(msg);
                }
            });

			/*
			 * Make the initial connection to other processes
			 */
            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        Thread.sleep(IPCMessageManager.STARTUP_DELAY);

                        logi("make :: plugin send");

                        ServiceUtils.sendtoBLEService(PluginService.class, IPCMessageManager.MESSAGE_ANDROID,
                                 EventCategories.IPC_INIT, null, null);
                        ServiceUtils.sendtoIPCService(PluginService.class, IPCMessageManager.MESSAGE_ANDROID,
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
        Toast.makeText(this, "Plugin Service Destroyed", Toast.LENGTH_SHORT).show();
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
        ServiceUtils.sendtoBLEService(PluginService.class, IPCMessageManager.MESSAGE_MICROBIT,
                EventCategories.IPC_WRITE_CHARACTERISTIC, null, args);
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

        switch (msg.arg1) {
            case EventCategories.SAMSUNG_REMOTE_CONTROL_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_REMOTE_CONTROL_ID");
                }

                RemoteControlPlugin.pluginEntry(PluginService.this, cmd);
                break;

            case EventCategories.SAMSUNG_ALERTS_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_ALERTS_ID");
                }

                AlertPlugin.pluginEntry(PluginService.this, cmd);
                break;

            case EventCategories.SAMSUNG_AUDIO_RECORDER_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_AUDIO_RECORDER_ID");
                }

                AudioPlugin.pluginEntry(PluginService.this, cmd);
                break;

            case EventCategories.SAMSUNG_CAMERA_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_CAMERA_ID");
                }

                CameraPlugin.pluginEntry(PluginService.this, cmd);
                break;

            case EventCategories.SAMSUNG_SIGNAL_STRENGTH_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_SIGNAL_STRENGTH_ID");
                }

                InformationPlugin.pluginEntry(PluginService.this, cmd);
                break;

            case EventCategories.SAMSUNG_DEVICE_INFO_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_DEVICE_INFO_ID");
                }

                InformationPlugin.pluginEntry(PluginService.this, cmd);
                break;

            case EventCategories.SAMSUNG_TELEPHONY_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_TELEPHONY_ID");
                }

                TelephonyPlugin.pluginEntry(PluginService.this, cmd);
                break;

            default:
                break;
        }
    }

    private void handleAndroidMessage(Message msg) {
        if(msg.arg1 == EventCategories.IPC_PLUGIN_STOP_PLAYING) {
            AlertPlugin.pluginEntry(PluginService.this, new CmdArg(EventSubCodes.SAMSUNG_ALERT_STOP_PLAYING, null));
        }
    }
}
