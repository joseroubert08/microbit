package com.samsung.microbit.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.ui.activity.MainActivity;

import java.util.HashMap;

public final class IPCMessageManager {

	private IncomingHandler incomingHandler = null;
	private HandlerThread handlerThread = null;
	private Messenger clientMessenger = null;

	private static volatile IPCMessageManager instance;
	private static final Object lock = new Object();

	private HashMap<String, Messenger> remoteServices = new HashMap<String, Messenger>();

	ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			remoteServices.put(name.getClassName(), new Messenger(service));
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			remoteServices.remove(name.getClassName());
		}
	};

	static final String TAG = "IPCMessageManager";
	private boolean debug = true;

	void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}

	// #######################################
	private IPCMessageManager() {
	}

	public static IPCMessageManager getInstance() {

		return instance;
	}

	public static IPCMessageManager getInstance(String serviceName, Handler clientHandler) {

		if (getInstance() == null) {
			synchronized (lock) {
				if (getInstance() == null) {
					IPCMessageManager ni = new IPCMessageManager();
					ni.configureClientHandler(serviceName, clientHandler);
					instance = ni;
				}
			}
		}

		return getInstance();
	}

	public android.os.Messenger getClientMessenger() {
		return clientMessenger;
	}

	public void configureClientHandler(String serviceName, Handler clientHandler) {

		logi("configureClientHandler()");
		synchronized (lock) {

			if (incomingHandler != null) {
				incomingHandler.clientHandler = null;
				handlerThread.quit();
			}

			handlerThread = new HandlerThread(serviceName);
			handlerThread.start();
			incomingHandler = new IncomingHandler(handlerThread, clientHandler);
			clientMessenger = new Messenger(incomingHandler);
		}
	}

	public void configureServerConnection(Class serviceClass, Context context) {

		logi("configureServerConnection()");
		Intent intent = new Intent();
		intent.setAction(serviceClass.getName());
		intent = MainActivity.createExplicitFromImplicitIntent(context.getApplicationContext(), intent);
		context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	public boolean isConnected(Class serviceClass) {

		return remoteServices.containsKey(serviceClass.getName());
	}

	public void sendMessage(Class serviceClass, Message msg) throws RemoteException {

		logi("sendMessage()");
		Messenger messenger = remoteServices.get(serviceClass.getName());
		if (messenger != null) {
			msg.replyTo = clientMessenger;
			messenger.send(msg);
		}
	}

	// ################################################
	class IncomingHandler extends Handler {

		volatile Handler clientHandler;

		public IncomingHandler(HandlerThread thr, Handler clientHandler) {
			super(thr.getLooper());
			logi("IncomingHandler.IncomingHandler()");
			this.clientHandler = clientHandler;
		}

		@Override
		public void handleMessage(Message msg) {

			logi("IncomingHandler.handleMessage()");
			super.handleMessage(msg);

			Handler c = null;
			synchronized (lock) {
				c = clientHandler;
			}

			if (c != null) {
				c.handleMessage(msg);
			}
		}
	}
}
