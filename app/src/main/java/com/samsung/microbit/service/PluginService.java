package com.samsung.microbit.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.NameValuePair;
import com.samsung.microbit.plugin.AlertPlugin;
import com.samsung.microbit.plugin.AudioPlugin;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.plugin.InformationPlugin;
import com.samsung.microbit.plugin.RemoteControlPlugin;
import com.samsung.microbit.plugin.TelephonyPlugin;

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

    public PluginService() {
        startIPCListener();
    }

    private static void logi(String message) {
        Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    /**
     * Handler of incoming messages from BLEListener.
     */
    public void handleMessage(Message msg) {
        if (DEBUG) {
            logi("handleMessage() ");
        }

        Bundle data = msg.getData();
        CmdArg cmd = new CmdArg(data.getInt(IPCMessageManager.BUNDLE_DATA), data.getString(IPCMessageManager.BUNDLE_VALUE));

        if (DEBUG) {
            logi("handleMessage() ## msg.arg1 = " + msg.arg1);
            logi("handleMessage() ## data.getInt=" + data.getInt(IPCMessageManager.BUNDLE_DATA));
            logi("handleMessage() ## data.getString=" + data.getString(IPCMessageManager.BUNDLE_VALUE));
        }

        switch (msg.arg1) {
            case Constants.SAMSUNG_REMOTE_CONTROL_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_REMOTE_CONTROL_ID");
                }

                RemoteControlPlugin.pluginEntry(PluginService.this, cmd);
                break;

            case Constants.SAMSUNG_ALERTS_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_ALERTS_ID");
                }

                AlertPlugin.pluginEntry(PluginService.this, cmd);
                break;

            case Constants.SAMSUNG_AUDIO_RECORDER_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_AUDIO_RECORDER_ID");
                }

                AudioPlugin.pluginEntry(PluginService.this, cmd);
                break;

            case Constants.SAMSUNG_CAMERA_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_CAMERA_ID");
                }

                CameraPlugin.pluginEntry(PluginService.this, cmd);
                break;

            case Constants.SAMSUNG_SIGNAL_STRENGTH_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_SIGNAL_STRENGTH_ID");
                }

                InformationPlugin.pluginEntry(PluginService.this, cmd);
                break;

            case Constants.SAMSUNG_DEVICE_INFO_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_DEVICE_INFO_ID");
                }

                InformationPlugin.pluginEntry(PluginService.this, cmd);
                break;

            case Constants.SAMSUNG_TELEPHONY_ID:
                if (DEBUG) {
                    logi("handleMessage() ##  SAMSUNG_TELEPHONY_ID");
                }

                TelephonyPlugin.pluginEntry(PluginService.this, cmd);
                break;

            default:
                break;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) {
            logi("onStartCommand() :: start");
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                /*
                if (DEBUG) logi("onStartCommand().run() ::  Starting Constants.REG_SIGNALSTRENGTH");
				Message msg = Message.obtain(null, Constants.REG_SIGNALSTRENGTH);
				msg.arg1 = Constants.SAMSUNG_SIGNAL_STRENGTH_ID;
				Bundle bundle = new Bundle();
				bundle.putInt(IPCMessageManager.BUNDLE_DATA, Constants.REG_SIGNALSTRENGTH);
				bundle.putString(IPCMessageManager.BUNDLE_VALUE, "on");
				msg.setData(bundle);
				handleMessage(msg);
				*/


                //bundle.putInt(IPCMessageManager.BUNDLE_DATA, Constants.REG_DEVICEORIENTATION);
                //bundle.putInt(IPCMessageManager.BUNDLE_DATA, Constants.REG_DEVICEGESTURE);
                //bundle.putInt(IPCMessageManager.BUNDLE_DATA, Constants.REG_DISPLAY);


            }
        }).run();

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

    public void startIPCListener() {
        if (DEBUG) {
            logi("startIPCListener()");
        }

        IPCMessageManager.connectMaybeInit(PluginService.class.getName(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (DEBUG) {
                    logi("startIPCListener().handleMessage");
                }

                handleIncomingMessage(msg);
                return true;
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
                    sendtoBLEService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CODE_INIT, null, null);
                    sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CODE_INIT, null, null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void sendMessageToBle(int value) {
        //TODO: instance is null because anysystem broadcast receiver run inside different process than the process who built the PluginService instance.
        Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + "sendMessageToBle()");
        NameValuePair[] args = new NameValuePair[4];
        args[0] = new NameValuePair(IPCMessageManager.BUNDLE_SERVICE_GUID, Constants.EVENT_SERVICE.toString());
        args[1] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_GUID, Constants.ES_CLIENT_EVENT.toString());
        args[2] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_VALUE, value);
        args[3] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_TYPE, Constants.FORMAT_UINT32);
        PluginService.sendtoBLEService(IPCMessageManager.MICROBIT_MESSAGE,
                IPCMessageManager.IPC_FUNCTION_WRITE_CHARACTERISTIC, null, args);
    }

    public static void sendtoBLEService(int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {
        if (DEBUG) {
            logi("sendtoBLEService()");
        }

        IPCMessageManager.sendIPCMessage(BLEService.class, mbsService, functionCode, cmd, args);
    }

    public static void sendtoIPCService(int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {
        if (DEBUG) {
            logi("sendtoIPCService()");
        }

        IPCMessageManager.sendIPCMessage(IPCService.class, mbsService, functionCode, cmd, args);
    }

    private void handleIncomingMessage(Message msg) {
        if (DEBUG) {
            logi("handleIncomingMessage() :: Start PluginService");
        }

        if (msg.what == IPCMessageManager.ANDROID_MESSAGE) {
            if (DEBUG) {
                logi("handleIncomingMessage() :: IPCMessageManager.ANDROID_MESSAGE msg.arg1 = " + msg.arg1);
            }

        } else if (msg.what == IPCMessageManager.MICROBIT_MESSAGE) {
            if (DEBUG) {
                logi("handleIncomingMessage() :: IPCMessageManager.MICROBIT_MESSAGE msg.arg1 = " + msg.arg1);
            }

            handleMessage(msg);
        }
    }
}
