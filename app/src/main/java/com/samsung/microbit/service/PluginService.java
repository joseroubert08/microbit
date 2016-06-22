package com.samsung.microbit.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.samsung.microbit.BuildConfig;
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

public class PluginService extends Service {

	static final String TAG = "PluginService";
	private boolean debug = BuildConfig.DEBUG;

	void logi(String message) {
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + " PluginService: # "  + message);
	}

	public static Messenger mClientMessenger = null;
	public static PluginService instance = null;

	public PluginService() {
		instance = this;
		startIPCListener();
	}

	//MBS Services
	public static final int ALERT = 0;
	public static final int FEEDBACK = 1;
	public static final int INFORMATION = 2;
	public static final int AUDIO = 3;
	public static final int REMOTE_CONTROL = 4;
	public static final int TELEPHONY = 5;
	public static final int CAMERA = 6;
	public static final int FILE = 7;

	/**
	 * Handler of incoming messages from BLEListener.
	 */
	public void handleMessage(Message msg) {
		if (debug) logi("handleMessage() ");
		Bundle data = msg.getData();
		mClientMessenger = msg.replyTo;
		CmdArg cmd = new CmdArg(data.getInt(IPCMessageManager.BUNDLE_DATA), data.getString(IPCMessageManager.BUNDLE_VALUE));

		if (debug) logi("handleMessage() ## msg.arg1 = " + msg.arg1);
		if (debug) logi("handleMessage() ## data.getInt=" + data.getInt(IPCMessageManager.BUNDLE_DATA));
		if (debug) logi("handleMessage() ## data.getString=" + data.getString(IPCMessageManager.BUNDLE_VALUE));
		switch (msg.arg1) {

			case Constants.SAMSUNG_REMOTE_CONTROL_ID:
				if (debug) logi("handleMessage() ##  SAMSUNG_REMOTE_CONTROL_ID");
				RemoteControlPlugin.pluginEntry(PluginService.this, cmd);
				break;

			case Constants.SAMSUNG_ALERTS_ID:
				if (debug) logi("handleMessage() ##  SAMSUNG_ALERTS_ID");
				AlertPlugin.pluginEntry(PluginService.this, cmd);
				break;

			case Constants.SAMSUNG_AUDIO_RECORDER_ID:
				if (debug) logi("handleMessage() ##  SAMSUNG_AUDIO_RECORDER_ID");
				AudioPlugin.pluginEntry(PluginService.this, cmd);
				break;

			case Constants.SAMSUNG_CAMERA_ID:
				if (debug) logi("handleMessage() ##  SAMSUNG_CAMERA_ID");
				CameraPlugin.pluginEntry(PluginService.this, cmd);
				break;

			case Constants.SAMSUNG_SIGNAL_STRENGTH_ID:
				if (debug) logi("handleMessage() ##  SAMSUNG_SIGNAL_STRENGTH_ID");
				InformationPlugin.pluginEntry(PluginService.this, cmd);
				break;

			case Constants.SAMSUNG_DEVICE_INFO_ID:
				if (debug) logi("handleMessage() ##  SAMSUNG_DEVICE_INFO_ID");
				InformationPlugin.pluginEntry(PluginService.this, cmd);
				break;

            case Constants.SAMSUNG_TELEPHONY_ID:
                if (debug) logi("handleMessage() ##  SAMSUNG_TELEPHONY_ID");
                TelephonyPlugin.pluginEntry(PluginService.this, cmd);
                break;

            default:
				break;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (debug) logi("onStartCommand() :: start");

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
                /*
				if (debug) logi("onStartCommand().run() ::  Starting Constants.REG_SIGNALSTRENGTH");
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

		if (debug) logi("startIPCListener()");
		if (IPCMessageManager.getInstance() == null) {
			if (debug) logi("startIPCListener() :: IPCMessageManager.getInstance() == null");
			IPCMessageManager inst = IPCMessageManager.getInstance("PluginServiceReceiver", new android.os.Handler() {
				@Override
				public void handleMessage(Message msg) {
					if (debug) logi("startIPCListener().handleMessage");
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
						sendtoBLEService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CODE_INIT, null, null);
						sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CODE_INIT, null, null);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();

		}
	}

	public static void sendMessageToBle(int value)
	{
		//TODO: instance is null because anysystem broadcast receiver run inside different process than the process who built the PluginService instance.
        Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + "sendMessageToBle()");
		NameValuePair[] args = new NameValuePair[4];
		args[0] = new NameValuePair(IPCMessageManager.BUNDLE_SERVICE_GUID, Constants.EVENT_SERVICE.toString());
		args[1] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_GUID, Constants.ES_CLIENT_EVENT.toString());
		args[2] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_VALUE, value);
		args[3] = new NameValuePair(IPCMessageManager.BUNDLE_CHARACTERISTIC_TYPE, Constants.FORMAT_UINT32);
		PluginService.instance.sendtoBLEService(IPCMessageManager.MICROBIT_MESSAGE,
				IPCMessageManager.IPC_FUNCTION_WRITE_CHARACTERISTIC, null, args);
	}

	public void sendtoBLEService(int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {
		if (debug) {
			//logi("sendtoBLEService()");
            logi("sendtoBLEService()" + ", " + mbsService + ", " + functionCode + (cmd != null ? ", " + cmd.getValue() +
                    ", " + cmd.getCMD() : ""));
		}
		Class destService = BLEService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd, args);
	}

	public void sendtoIPCService(int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {
		if (debug) {
            //logi("sendtoIPCService()");
            logi("sendtoIPCService()" + ", " + mbsService + ", " + functionCode + (cmd != null ? ", " + cmd.getValue() +
                    ", " + cmd.getCMD() : ""));
        }
		Class destService = IPCService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd, args);
	}

	public void sendIPCMessge(Class destService, int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {

		IPCMessageManager inst = IPCMessageManager.getInstance();
		if (!inst.isConnected(destService)) {
			inst.configureServerConnection(destService, this);
		}

		if (mbsService != IPCMessageManager.ANDROID_MESSAGE && mbsService != IPCMessageManager.MICROBIT_MESSAGE) {
			return;
		}

		Message msg = Message.obtain(null, mbsService);
		msg.arg1 = functionCode;
		Bundle bundle = new Bundle();
		if (cmd != null) {
			bundle.putInt(IPCMessageManager.BUNDLE_DATA, cmd.getCMD());
			bundle.putString(IPCMessageManager.BUNDLE_VALUE, cmd.getValue());
		}

		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				bundle.putSerializable(args[i].getName(), args[i].getValue());
			}
		}

		msg.setData(bundle);
		try {
			inst.sendMessage(destService, msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void handleIncomingMessage(Message msg) {
		if (debug) logi("handleIncomingMessage() :: Start PluginService");
		if (msg.what == IPCMessageManager.ANDROID_MESSAGE) {
			if (debug) logi("handleIncomingMessage() :: IPCMessageManager.ANDROID_MESSAGE msg.arg1 = " + msg.arg1);
		} else if (msg.what == IPCMessageManager.MICROBIT_MESSAGE) {
			if (debug) logi("handleIncomingMessage() :: IPCMessageManager.MICROBIT_MESSAGE msg.arg1 = " + msg.arg1);
			handleMessage(msg);
		}
	}
}
