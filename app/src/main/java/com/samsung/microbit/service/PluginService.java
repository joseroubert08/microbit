package com.samsung.microbit.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.plugin.AlertPlugin;
import com.samsung.microbit.plugin.AudioPlugin;
import com.samsung.microbit.plugin.FeedbackPlugin;
import com.samsung.microbit.plugin.FilePlugin;
import com.samsung.microbit.plugin.InformationPlugin;
import com.samsung.microbit.plugin.RemoteControlPlugin;
import com.samsung.microbit.plugin.TelephonyPlugin;
import com.samsung.microbit.plugin.CameraPlugin;

/**
 * Created by kkulendiran on 10/05/2015.
 */
public class PluginService extends Service {

	public static final String BUNDLE_DATA = "data";
	public static final String BUNDLE_VALUE = "value";


	static final String TAG = "PluginService";
	private boolean debug = true;

	void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
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

		logi("handleMessage()");
		Bundle data = msg.getData();
		if (data.getString(BUNDLE_VALUE) == null) {
			return;
		}

		mClientMessenger = msg.replyTo;
		CmdArg cmd = new CmdArg(data.getInt(BUNDLE_DATA), data.getString(BUNDLE_VALUE));
		logi("handleMessage() ## msg.what = " + msg.what);
		logi("handleMessage() ## data.getInt=" + data.getInt(BUNDLE_DATA) + " data.getString=" + data.getString(BUNDLE_VALUE));
		switch (msg.what) {

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
		logi("onStartCommand() ## start");
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

		logi("startIPCListener()");
		if (IPCMessageManager.getInstance() == null) {
			logi("startIPCListener() :: IPCMessageManager.getInstance() == null");
			IPCMessageManager inst = IPCMessageManager.getInstance("PluginServiceReceiver", new android.os.Handler() {
				@Override
				public void handleMessage(Message msg) {
					logi("startIPCListener().handleMessage");
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
						Thread.sleep(3000);
						sendtoBLEService(0, null);
						sendtoIPCService(0, null);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();

		}
	}

	public void sendtoBLEService(int mbsService, CmdArg cmd) {

		logi("sendtoBLEService()");
		Class destService = BLEService.class;
		IPCMessageManager inst = IPCMessageManager.getInstance();
		if (!inst.isConnected(destService)) {
			inst.configureServerConnection(destService, this);
		}

		Message msg = Message.obtain(null, mbsService);
		Bundle bundle = new Bundle();
		if (cmd == null && mbsService == 0) {
			bundle.putString(IPCMessageManager.IPC_INIT_CALL, this.getClass().getName());
		}

		msg.setData(bundle);
		try {
			inst.sendMessage(destService, msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendtoIPCService(int mbsService, CmdArg cmd) {

		logi("sendtoIPCService()");
		Class destService = IPCService.class;
		IPCMessageManager inst = IPCMessageManager.getInstance();
		if (!inst.isConnected(destService)) {
			inst.configureServerConnection(destService, this);
		}

		Message msg = Message.obtain(null, mbsService);
		Bundle bundle = new Bundle();
		if (cmd == null && mbsService == 0) {
			bundle.putString(IPCMessageManager.IPC_INIT_CALL, this.getClass().getName());
		}

		msg.setData(bundle);
		try {
			inst.sendMessage(destService, msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void handleIncomingMessage(Message msg) {
		logi("handleIncomingMessage() :: Start PluginService");
		handleMessage(msg);
	}
}
