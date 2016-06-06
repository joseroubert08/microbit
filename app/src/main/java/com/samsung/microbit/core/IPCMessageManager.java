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

import com.samsung.microbit.BuildConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class IPCMessageManager {

	public static final long STARTUP_DELAY = 1000L;
	public static final String BUNDLE_DATA = "data";
	public static final String BUNDLE_VALUE = "value";
	public static final String BUNDLE_MICROBIT_FIRMWARE = "BUNDLE_MICROBIT_FIRMWARE";
    public static final String BUNDLE_MICROBIT_REQUESTS = "BUNDLE_MICROBIT_REQUESTS";
	public static final String BUNDLE_ERROR_CODE = "BUNDLE_ERROR_CODE";
	public static final String BUNDLE_ERROR_MESSAGE = "BUNDLE_ERROR_MESSAGE";
	public static final String BUNDLE_SERVICE_GUID = "BUNDLE_SERVICE_GUID";
	public static final String BUNDLE_CHARACTERISTIC_GUID = "BUNDLE_CHARACTERISTIC_GUID";
	public static final String BUNDLE_CHARACTERISTIC_TYPE = "BUNDLE_CHARACTERISTIC_TYPE";
	public static final String BUNDLE_CHARACTERISTIC_VALUE = "BUNDLE_CHARACTERISTIC_VALUE";
	public static final String BUNDLE_DEVICE_ADDRESS = "BUNDLE_DEVICE_ADDRESS";

	public static final int ANDROID_MESSAGE = 1;
	public static final int MICROBIT_MESSAGE = 2;

	public static final int IPC_FUNCTION_CODE_INIT = 0;
	public static final int IPC_FUNCTION_DISCONNECT = 1;
	public static final int IPC_FUNCTION_CONNECT = 2;
	public static final int IPC_FUNCTION_RECONNECT = 3;
	public static final int IPC_FUNCTION_WRITE_CHARACTERISTIC = 4;
	public static final int IPC_FUNCTION_DISCONNECT_FOR_FLASH = 5;

	public static final int IPC_NOTIFICATION_GATT_CONNECTED = 4000;
	public static final int IPC_NOTIFICATION_GATT_DISCONNECTED = 4001;
	public static final int IPC_NOTIFICATION_CHARACTERISTIC_CHANGED = 4002;

    public static final int IPC_NOTIFICATION_INCOMING_CALL_REQUESTED = 4003;
    public static final int IPC_NOTIFICATION_INCOMING_SMS_REQUESTED = 4002;

    private static final String TAG = "IPCMessageManager";

    private static final Object LOCK = new Object();

    private static volatile IPCMessageManager instance;

    public static IPCMessageManager getInstance() {
        return instance;
    }

    public static IPCMessageManager getInstance(String serviceName, Handler clientHandler) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    IPCMessageManager ni = new IPCMessageManager();
                    ni.configureClientHandler(serviceName, clientHandler);
                    instance = ni;
                }
            }
        }

        return instance;
    }


	private IncomingHandler incomingHandler = null;
	private HandlerThread handlerThread = null;
	private Messenger clientMessenger = null;

	private Map<String, Messenger> remoteServices = new HashMap<String, Messenger>();

	private boolean isDebug = BuildConfig.DEBUG;

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if(isDebug) {
                logi("serviceConnection.onServiceConnected() :: name.getClassName() " + name.getClassName());
            }

            remoteServices.put(name.getClassName(), new Messenger(service));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if(isDebug) {
                logi("serviceConnection.onServiceDisconnected() :: name.getClassName() " + name.getClassName());
            }

            remoteServices.remove(name.getClassName());
        }
    };

	static void logi(String message) {
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
	}

	// #######################################
	private IPCMessageManager() {
	}

	public Messenger getClientMessenger() {
		return clientMessenger;
	}

	public void configureClientHandler(String serviceName, Handler clientHandler) {
		if(isDebug) {
            logi("configureClientHandler()");
        }

		synchronized (LOCK) {
			if (incomingHandler != null) {
				incomingHandler.clientHandler = null;
				handlerThread.quit();
			}

			handlerThread = new HandlerThread(serviceName);
			handlerThread.start();
			incomingHandler = new IncomingHandler(handlerThread, clientHandler, isDebug);
			clientMessenger = new Messenger(incomingHandler);
		}
	}

	public void configureServerConnection(Class serviceClass, Context context) {
		if(isDebug) {
            logi("configureServerConnection() :: serviceClass.getName() = " + serviceClass.getName());
        }

		Intent intent = new Intent();
		intent.setAction(serviceClass.getName());
		intent = createExplicitFromImplicitIntent(context.getApplicationContext(), intent);
		context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	public boolean isConnected(Class serviceClass) {
		return remoteServices.containsKey(serviceClass.getName());
	}

	public void sendMessage(Class serviceClass, Message msg) throws RemoteException {
		if(isDebug) {
            logi("sendMessage()");
        }

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
	static class IncomingHandler extends Handler {

		private Handler clientHandler;
        private boolean isDebug;

		public IncomingHandler(HandlerThread thr, Handler clientHandler, boolean isDebug) {
			super(thr.getLooper());
			if(isDebug) {
                logi("IncomingHandler.IncomingHandler() :: clientHandler = " + clientHandler);
            }

			this.clientHandler = clientHandler;
            this.isDebug = isDebug;
		}

		@Override
		public void handleMessage(Message msg) {
			if(isDebug) {
                logi("IncomingHandler.handleMessage()");
            }

			super.handleMessage(msg);

			if (msg.what == ANDROID_MESSAGE) {
				if (msg.arg1 == IPC_FUNCTION_CODE_INIT) {
					if(isDebug) {
                        logi("IncomingHandler.handleMessage() :: ANDROID_MESSAGE.IPC_FUNCTION_CODE_INIT");
                    }
					return;
				}
			}

			Handler clientHandler;
			synchronized (LOCK) {
				if(isDebug) {
                    logi("IncomingHandler.handleMessage() :: getting clientHandler");
                }

				clientHandler = this.clientHandler;
			}

			if (clientHandler != null) {
				if(isDebug) {
                    logi("IncomingHandler.handleMessage() :: c != null");
                }

				clientHandler.handleMessage(msg);
			}
		}
	}
}
