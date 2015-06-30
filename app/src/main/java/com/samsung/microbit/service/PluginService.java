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

import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.NameValuePair;
import com.samsung.microbit.plugin.AlertPlugin;
import com.samsung.microbit.plugin.AudioPlugin;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.plugin.RemoteControlPlugin;

/**
 * Created by kkulendiran on 10/05/2015.
 */
public class PluginService extends Service {

	static final String TAG = "PluginService";
	private boolean debug = true;

	void logi(String message) {
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
	}

	public static Messenger mClientMessenger = null;

	public PluginService() {
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

		if(debug) logi("handleMessage()");
		Bundle data = msg.getData();
		mClientMessenger = msg.replyTo;
		CmdArg cmd = new CmdArg(data.getInt(IPCMessageManager.BUNDLE_DATA), data.getString(IPCMessageManager.BUNDLE_VALUE));

		if(debug) logi("handleMessage() ## msg.arg1 = " + msg.arg1);
		if(debug) logi("handleMessage() ## data.getInt=" + data.getInt(IPCMessageManager.BUNDLE_DATA));
		if(debug) logi("handleMessage() ## data.getString=" + data.getString(IPCMessageManager.BUNDLE_VALUE));
		switch (msg.arg1) {

			case Constants.SAMSUNG_REMOTE_CONTROL_ID:
				RemoteControlPlugin.pluginEntry(PluginService.this, cmd);
				break;

			case Constants.SAMSUNG_ALERTS_ID:
				AlertPlugin.pluginEntry(PluginService.this, cmd);
				break;

			case Constants.SAMSUNG_AUDIO_RECORDER_ID:
				AudioPlugin.pluginEntry(PluginService.this, cmd);
				break;

			case Constants.SAMSUNG_CAMERA_ID:
				CameraPlugin.pluginEntry(PluginService.this, cmd);
				break;

			default:
				break;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(debug) logi("onStartCommand() ## start");
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

		if(debug) logi("startIPCListener()");
		if (IPCMessageManager.getInstance() == null) {
			if(debug) logi("startIPCListener() :: IPCMessageManager.getInstance() == null");
			IPCMessageManager inst = IPCMessageManager.getInstance("PluginServiceReceiver", new android.os.Handler() {
				@Override
				public void handleMessage(Message msg) {
					if(debug) logi("startIPCListener().handleMessage");
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

	public void sendtoBLEService(int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {

		if(debug) logi("sendtoBLEService()");
		Class destService = BLEService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd, args);
	}

	public void sendtoIPCService(int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {

		if(debug) logi("sendtoIPCService()");
		Class destService = IPCService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd, args);
	}

	public void sendIPCMessge(Class destService, int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {

		if(debug) logi("sendIPCMessge()");
		IPCMessageManager inst = IPCMessageManager.getInstance();
		if (!inst.isConnected(destService)) {
			inst.configureServerConnection(destService, this);
		}

		Message msg = Message.obtain(null, mbsService);
		msg.arg1 = functionCode;
		Bundle bundle = new Bundle();
		if (mbsService == IPCMessageManager.ANDROID_MESSAGE) {
			if(debug) logi("sendIPCMessge() :: IPCMessageManager.ANDROID_MESSAGE functionCode=" + functionCode);
		} else if (mbsService == IPCMessageManager.MICIROBIT_MESSAGE) {
			if(debug) logi("sendIPCMessge() :: IPCMessageManager.MICIROBIT_MESSAGE functionCode=" + functionCode);
			if (cmd != null) {
				bundle.putInt(IPCMessageManager.BUNDLE_DATA, cmd.getCMD());
				bundle.putString(IPCMessageManager.BUNDLE_VALUE, cmd.getValue());
			}
		} else {
			return;
		}

		msg.setData(bundle);
		try {
			inst.sendMessage(destService, msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void handleIncomingMessage(Message msg) {
		if(debug) logi("handleIncomingMessage() :: Start PluginService");
		if (msg.what == IPCMessageManager.ANDROID_MESSAGE) {
			if(debug) logi("handleIncomingMessage() :: IPCMessageManager.ANDROID_MESSAGE msg.arg1 = " + msg.arg1);
		} else if (msg.what == IPCMessageManager.MICIROBIT_MESSAGE) {
			handleMessage(msg);
		}
	}
}
