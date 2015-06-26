package com.samsung.microbit.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.List;

public final class IPCMessageManager {

	public static final String IPC_INIT_CALL = "init";

	private IncomingHandler incomingHandler = null;
	private HandlerThread handlerThread = null;
	private Messenger clientMessenger = null;


	private static volatile IPCMessageManager instance;
	private static final Object lock = new Object();

	private HashMap<String, Messenger> remoteServices = new HashMap<String, Messenger>();

	ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {

			logi("serviceConnection.onServiceConnected() :: name.getClassName() " + name.getClassName());
			remoteServices.put(name.getClassName(), new Messenger(service));
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {

			logi("serviceConnection.onServiceDisconnected() :: name.getClassName() " + name.getClassName());
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

		logi("configureServerConnection() :: serviceClass.getName() = " + serviceClass.getName());
		Intent intent = new Intent();
		intent.setAction(serviceClass.getName());
		intent = createExplicitFromImplicitIntent(context.getApplicationContext(), intent);
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

	public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
		// Retrieve all services that can match the given intent
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

		// Make sure only one match was found
		if (resolveInfo == null || resolveInfo.size() != 1) {
			return null;
		}

		// Get component info and create ComponentName
		ResolveInfo serviceInfo = resolveInfo.get(0);
		String packageName = serviceInfo.serviceInfo.packageName;
		String className = serviceInfo.serviceInfo.name;
		ComponentName component = new ComponentName(packageName, className);

		// Create a new intent. Use the old one for extras and such reuse
		Intent explicitIntent = new Intent(implicitIntent);

		// Set the component to be explicit
		explicitIntent.setComponent(component);
		return explicitIntent;
	}

	// ################################################
	class IncomingHandler extends Handler {

		volatile Handler clientHandler;

		public IncomingHandler(HandlerThread thr, Handler clientHandler) {
			super(thr.getLooper());
			logi("IncomingHandler.IncomingHandler() :: clientHandler = " + clientHandler);
			this.clientHandler = clientHandler;
		}

		@Override
		public void handleMessage(Message msg) {

			logi("IncomingHandler.handleMessage()");
			super.handleMessage(msg);

			String s = msg.getData().getString(IPC_INIT_CALL);
			if (s != null) {
				logi("IncomingHandler.handleMessage() :: IPC_INIT_CALL = " + s);
				return;
			}

			Handler c = null;
			synchronized (lock) {
				logi("IncomingHandler.handleMessage() :: getting clientHandler");
				c = clientHandler;
			}

			if (c != null) {
				logi("IncomingHandler.handleMessage() :: c != null");
				c.handleMessage(msg);
			}
		}
	}
}
