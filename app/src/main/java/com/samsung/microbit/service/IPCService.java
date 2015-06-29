package com.samsung.microbit.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.model.CmdArg;

public class IPCService extends Service {

	public static IPCService instance;

	public static final String INTENT_BLE_NOTIFICATION = "com.samsung.microbit.service.IPCService.INTENT_BLE_NOTIFICATION";
	public static final String INTENT_MICROBIT_NOTIFICATION = "com.samsung.microbit.service.IPCService.INTENT_MICROBIT_NOTIFICATION";

	static final String TAG = "IPCService";
	private boolean debug = false;

	void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}

	public IPCService() {
		instance = this;
		startIPCListener();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logi("onStartCommand()");
		return START_STICKY;
	}

	/*
	 * Business method
	 */
	public void sendBleData() {
		sendtoPluginService(IPCMessageManager.ANDROID_MESSAGE, 1, null);
	}

	/*
	 * setup IPCMessageManager
	 */
	@Override
	public IBinder onBind(Intent intent) {

		return IPCMessageManager.getInstance().getClientMessenger().getBinder();
	}

	public void startIPCListener() {

		logi("startIPCListener()");
		if (IPCMessageManager.getInstance() == null) {
			logi("startIPCListener() :: IPCMessageManager.getInstance() == null");
			IPCMessageManager inst = IPCMessageManager.getInstance("IPCServiceListener", new android.os.Handler() {
				@Override
				public void handleMessage(Message msg) {
					super.handleMessage(msg);
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
						sendtoBLEService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CODE_INIT, null);
						sendtoPluginService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CODE_INIT, null);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	public void sendtoBLEService(int mbsService, int functionCode, CmdArg cmd) {

		logi("sendtoBLEService()");
		Class destService = BLEService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd);
	}

	public void sendtoPluginService(int mbsService, int functionCode, CmdArg cmd) {
		logi("sendtoPluginService()");
		Class destService = PluginService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd);
	}

	public void sendIPCMessge(Class destService, int mbsService, int functionCode, CmdArg cmd) {

		logi("sendIPCMessge()");
		IPCMessageManager inst = IPCMessageManager.getInstance();
		if (!inst.isConnected(destService)) {
			inst.configureServerConnection(destService, this);
		}

		Message msg = Message.obtain(null, mbsService);
		msg.arg1 = functionCode;
		Bundle bundle = new Bundle();
		if (mbsService == IPCMessageManager.ANDROID_MESSAGE) {
			logi("sendIPCMessge() :: IPCMessageManager.ANDROID_MESSAGE functionCode=" + functionCode);
		} else if (mbsService == IPCMessageManager.MICIROBIT_MESSAGE) {
			logi("sendIPCMessge() :: IPCMessageManager.MICIROBIT_MESSAGE functionCode=" + functionCode);
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
		logi("handleIncomingMessage() :: Start BLEService");
		if (msg.what == IPCMessageManager.ANDROID_MESSAGE) {
			logi("handleIncomingMessage() :: IPCMessageManager.ANDROID_MESSAGE msg.arg1 = " + msg.arg1);
			Intent intent = new Intent(INTENT_BLE_NOTIFICATION);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		} else if (msg.what == IPCMessageManager.MICIROBIT_MESSAGE) {
			logi("handleIncomingMessage() :: IPCMessageManager.MICIROBIT_MESSAGE msg.arg1 = " + msg.arg1);
			Intent intent = new Intent(INTENT_MICROBIT_NOTIFICATION);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		} else {
			return;
		}
	}
}
