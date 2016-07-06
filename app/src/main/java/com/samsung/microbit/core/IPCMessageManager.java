package com.samsung.microbit.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.model.NameValuePair;

import java.util.HashMap;
import java.util.List;

import static com.samsung.microbit.BuildConfig.DEBUG;

public final class IPCMessageManager {
    private static final String TAG = IPCMessageManager.class.getSimpleName();

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

    public static final int MESSAGE_ANDROID = 1;
    public static final int MESSAGE_MICROBIT = 2;

    private IncomingHandler incomingHandler;
    private HandlerThread handlerThread;
    private Messenger clientMessenger;

    private static volatile IPCMessageManager instance;
    private static final Object lock = new Object();

    private HashMap<String, Messenger> remoteServices = new HashMap<>();

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if(DEBUG) {
                logi("serviceConnection.onServiceConnected() :: name.getClassName() " + name.getClassName());
            }

            remoteServices.put(name.getClassName(), new Messenger(service));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if(DEBUG) {
                logi("serviceConnection.onServiceDisconnected() :: name.getClassName() " + name.getClassName());
            }

            remoteServices.remove(name.getClassName());
        }
    };

    private static void logi(String message) {
        Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    // #######################################
    private IPCMessageManager() {
    }

    public static IPCMessageManager getInstance() {
        return instance;
    }

    public static void configureMessageManager(String serviceName, Handler clientHandler) {
        synchronized (lock) {
            if (instance == null) {
                instance = new IPCMessageManager();
            }
            instance.configureClientHandler(serviceName, clientHandler);
        }
    }

    public void sendIPCMessage(Class destService, int messageType, int eventCategory, CmdArg cmd,
                                      NameValuePair[] args) {
        if (!isConnected(destService)) {
            configureServerConnection(destService, MBApp.getApp());
        }

        if (messageType != IPCMessageManager.MESSAGE_ANDROID && messageType != IPCMessageManager.MESSAGE_MICROBIT) {
            return;
        }

        Message msg = Message.obtain(null, messageType);
        msg.arg1 = eventCategory;
        Bundle bundle = new Bundle();
        if (cmd != null) {
            bundle.putInt(IPCMessageManager.BUNDLE_DATA, cmd.getCMD());
            bundle.putString(IPCMessageManager.BUNDLE_VALUE, cmd.getValue());
        }

        if (args != null) {
            for (NameValuePair arg : args) {
                bundle.putSerializable(arg.getName(), arg.getValue());
            }
        }

        msg.setData(bundle);
        try {
            sendMessage(destService, msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public Messenger getClientMessenger() {
        return clientMessenger;
    }

    private void configureClientHandler(String serviceName, Handler clientHandler) {
        if(DEBUG) {
            logi("configureClientHandler()");
            logi("Init handler ::" +  clientHandler.getLooper().getThread().getName());
        }

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

    private void configureServerConnection(Class serviceClass, Context context) {
        if(DEBUG) {
            logi("configureServerConnection() :: serviceClass.getName() = " + serviceClass.getName());
        }

        Intent intent = new Intent();
        intent.setAction(serviceClass.getName());
        intent = createExplicitFromImplicitIntent(context.getApplicationContext(), intent);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private boolean isConnected(Class serviceClass) {
        return remoteServices.containsKey(serviceClass.getName());
    }

    private void sendMessage(Class serviceClass, Message msg) throws RemoteException {
        if(DEBUG) {
            logi("sendMessage()");
        }

        Messenger messenger = remoteServices.get(serviceClass.getName());
        if (messenger != null) {
            msg.replyTo = clientMessenger;
            messenger.send(msg);
        }
    }

    public int getServicesCount() {
        return remoteServices.size();
    }

    private static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
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
    private static class IncomingHandler extends Handler {

        private volatile Handler clientHandler;

        public IncomingHandler(HandlerThread thr, Handler clientHandler) {
            super(thr.getLooper());
            if(DEBUG) {
                logi("IncomingHandler.IncomingHandler() :: clientHandler = " + clientHandler);
            }

            this.clientHandler = clientHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            if(DEBUG) {
                logi("IncomingHandler.handleMessage()");
            }

            super.handleMessage(msg);

            if (msg.what == MESSAGE_ANDROID) {
                if (msg.arg1 == EventCategories.IPC_INIT) {
                    if(DEBUG) {
                        logi("IncomingHandler.handleMessage() :: MESSAGE_ANDROID.IPC_INIT");
                    }

                    return;
                }
            }

            Handler c;
            synchronized (lock) {
                if(DEBUG) {
                    logi("IncomingHandler.handleMessage() :: getting clientHandler");
                }

                c = clientHandler;
            }

            if (c != null) {
                if(DEBUG) {
                    logi("IncomingHandler.handleMessage() :: c != null");
                }

                c.handleMessage(msg);
            }
        }
    }
}
