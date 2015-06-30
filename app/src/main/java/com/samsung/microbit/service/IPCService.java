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
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.ConnectedDevice;

import java.util.UUID;

public class IPCService extends Service {

	private static IPCService instance;

	public static final String INTENT_BLE_NOTIFICATION = "com.samsung.microbit.service.IPCService.INTENT_BLE_NOTIFICATION";
	public static final String INTENT_MICROBIT_NOTIFICATION = "com.samsung.microbit.service.IPCService.INTENT_MICROBIT_NOTIFICATION";

	public static final String NOTIFICATION_CAUSE = "com.samsung.microbit.service.IPCService.CAUSE";

	static final String TAG = "IPCService";
	private boolean debug = true;

	void logi(String message) {
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
	}

	public IPCService() {
		instance = this;
		startIPCListener();
	}

	public static IPCService getInstance() {
		return instance;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (debug) logi("onStartCommand()");
		return START_STICKY;
	}

	/*
	 * Business method
	 */
	public void bleDisconnect() {
		sendtoBLEService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_DISCONNECT, null);
	}

	public void bleConnect() {
		sendtoBLEService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CONNECT, null);
	}

	public void bleReconnect() {
		sendtoBLEService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_RECONNECT, null);
	}

	public void writeCharacteristic(UUID service, UUID Characteristic, int value, int type) {
		//sendtoBLEService();
	}

	/*
	 * setup IPCMessageManager
	 */
	@Override
	public IBinder onBind(Intent intent) {

		return IPCMessageManager.getInstance().getClientMessenger().getBinder();
	}

	public void startIPCListener() {

		if (debug) logi("startIPCListener()");
		if (IPCMessageManager.getInstance() == null) {
			if (debug) logi("startIPCListener() :: IPCMessageManager.getInstance() == null");
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
						Thread.sleep(IPCMessageManager.STARTUP_DELAY);
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

		if (debug) logi("sendtoBLEService()");
		Class destService = BLEService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd);
	}

	public void sendtoPluginService(int mbsService, int functionCode, CmdArg cmd) {
		if (debug) logi("sendtoPluginService()");
		Class destService = PluginService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd);
	}

	public void sendIPCMessge(Class destService, int mbsService, int functionCode, CmdArg cmd) {

		if (debug) logi("sendIPCMessge()");
		IPCMessageManager inst = IPCMessageManager.getInstance();
		if (!inst.isConnected(destService)) {
			inst.configureServerConnection(destService, this);
		}

		Message msg = Message.obtain(null, mbsService);
		msg.arg1 = functionCode;
		Bundle bundle = new Bundle();
		if (mbsService == IPCMessageManager.ANDROID_MESSAGE) {
			if (debug) logi("sendIPCMessge() :: IPCMessageManager.ANDROID_MESSAGE functionCode=" + functionCode);
		} else if (mbsService == IPCMessageManager.MICIROBIT_MESSAGE) {
			if (debug) logi("sendIPCMessge() :: IPCMessageManager.MICIROBIT_MESSAGE functionCode=" + functionCode);
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
		if (debug) logi("handleIncomingMessage() :: Start BLEService");
		if (msg.what == IPCMessageManager.ANDROID_MESSAGE) {
			if (debug) logi("handleIncomingMessage() :: IPCMessageManager.ANDROID_MESSAGE msg.arg1 = " + msg.arg1);


			if (msg.arg1 == IPCMessageManager.IPC_NOTIFICATION_GATT_CONNECTED ||
				msg.arg1 == IPCMessageManager.IPC_NOTIFICATION_GATT_DISCONNECTED) {

				//Bundle data = msg.getData();
				//CmdArg cmd = new CmdArg(data.getInt(IPCMessageManager.BUNDLE_DATA), data.getString(IPCMessageManager.BUNDLE_VALUE));

				ConnectedDevice cd = Utils.getPairedMicrobit(this);
				cd.mStatus = (msg.arg1 == IPCMessageManager.IPC_NOTIFICATION_GATT_CONNECTED);
				Utils.setPairedMicrobit(this, cd);
			}

			Intent intent = new Intent(INTENT_BLE_NOTIFICATION);
			intent.putExtra(NOTIFICATION_CAUSE, msg.arg1);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		} else if (msg.what == IPCMessageManager.MICIROBIT_MESSAGE) {
			if (debug) logi("handleIncomingMessage() :: IPCMessageManager.MICIROBIT_MESSAGE msg.arg1 = " + msg.arg1);
			Intent intent = new Intent(INTENT_MICROBIT_NOTIFICATION);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		}
	}
}
