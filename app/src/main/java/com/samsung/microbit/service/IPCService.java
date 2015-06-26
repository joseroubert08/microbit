package com.samsung.microbit.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.model.CmdArg;

public class IPCService extends Service {

	public static IPCService instance;

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
		sendtoPluginService(1, null);
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
						sendtoBLEService(0, null);
						sendtoPluginService(0, null);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	private void handleIncomingMessage(Message msg) {
		logi("handleIncomingMessage() :: Start IPCService");
		// Broadcast this message on
		/*
		Intent intent = new Intent("ubit-button-press");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		*/
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

	public void sendtoPluginService(int mbsService, CmdArg cmd) {

		logi("sendtoPluginService()");
		Class destService = PluginService.class;
		IPCMessageManager inst = IPCMessageManager.getInstance();
		if (!inst.isConnected(destService)) {
			inst.configureServerConnection(destService, this);
		}

		Message msg = Message.obtain(null, mbsService);
		Bundle bundle = new Bundle();

		if (cmd == null && mbsService == 0) {
			bundle.putString(IPCMessageManager.IPC_INIT_CALL, this.getClass().getName());
		} else {
			if (cmd != null) {
				bundle.putInt(PluginService.BUNDLE_DATA, cmd.getCMD());
				bundle.putString(PluginService.BUNDLE_VALUE, cmd.getValue());
			}
		}

		msg.setData(bundle);
		try {
			inst.sendMessage(destService, msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
