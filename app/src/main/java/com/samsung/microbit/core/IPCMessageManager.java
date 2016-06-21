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

import com.samsung.microbit.BuildConfig;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.NameValuePair;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.utils.ServiceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static IPCMessageManager instance;
    private static final Object initLock = new Object();

    public static IPCMessageManager getInstance() {
        if (instance == null) {
            synchronized (initLock) {
                if (instance == null) {
                    IPCService.startIPCListener();
                }
            }
        }
        return instance;
    }

    public static void initIPCInteraction(String serviceName, Handler.Callback callback) {
        instance = new IPCMessageManager();
        instance.configureClientHandler(serviceName);

        instance.serviceHandlingCallbacks.put(serviceName, callback);

        sendStartCommands();
    }

    public static void sendStartCommands() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(IPCMessageManager.STARTUP_DELAY);
                    ServiceUtils.sendtoBLEService(PluginService.class, IPCMessageManager.ANDROID_MESSAGE,
                            IPCMessageManager.IPC_FUNCTION_CODE_INIT,
                            null, null);
                    ServiceUtils.sendtoIPCService(PluginService.class, IPCMessageManager.ANDROID_MESSAGE,
                            IPCMessageManager.IPC_FUNCTION_CODE_INIT, null, null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private IncomingHandler incomingHandler = null;
    private HandlerThread handlerThread = null;
    private Messenger clientMessenger = null;

    private boolean isDebug = BuildConfig.DEBUG;

    private Map<String, Messenger> remoteServices = new HashMap<>();
    private Map<String, Handler.Callback> serviceHandlingCallbacks = new HashMap<>();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (isDebug) {
                logi("serviceConnection.onServiceConnected() :: name.getClassName() " + name.getClassName());
            }

            Handler.Callback clientCallback = serviceHandlingCallbacks.get(name.getClassName());

            if (clientCallback != null) {
                synchronized (initLock) {
                    incomingHandler.clientCallbacks.add(clientCallback);
                }
            }

            remoteServices.put(name.getClassName(), new Messenger(service));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (isDebug) {
                logi("serviceConnection.onServiceDisconnected() :: name.getClassName() " + name.getClassName());
            }

            synchronized (initLock) {
                incomingHandler.clientCallbacks.remove(serviceHandlingCallbacks.remove(name.getClassName()));
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

    public void configureClientHandler(String serviceName) {
        if (isDebug) {
            logi("configureClientHandler()");
        }

        List<Handler.Callback> clientCallbacks = null;

        if (incomingHandler != null) {
            clientCallbacks = incomingHandler.clientCallbacks;
            handlerThread.quit();
        }

        handlerThread = new HandlerThread(serviceName);
        handlerThread.start();
        incomingHandler = new IncomingHandler(handlerThread, isDebug);

        if (clientCallbacks != null) {
            incomingHandler.clientCallbacks.addAll(clientCallbacks);
        }

        clientMessenger = new Messenger(incomingHandler);
    }

    public static void sendIPCMessage(Class destService, int mbsService, int functionCode, CmdArg cmd,
                                      NameValuePair[] args) {
        if (instance == null) {
            getInstance();
        }

        if (!instance.isConnected(destService)) {
            instance.configureServerConnection(destService);
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
            instance.sendMessage(destService, msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void configureServerConnection(Class serviceClass) {
        Context context = MBApp.getApp();

        if (isDebug) {
            logi("configureServerConnection() :: serviceClass.getName() = " + serviceClass.getName());
        }

        Intent intent = new Intent(serviceClass.getName());
        intent = createExplicitFromImplicitIntent(context.getApplicationContext(), intent);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public boolean isConnected(Class serviceClass) {
        return remoteServices.containsKey(serviceClass.getName());
    }

    public void sendMessage(Class serviceClass, Message msg) throws RemoteException {
        if (isDebug) {
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

    /**
     * This class must be used to extend to show logs.
     */
    public static class IncomingHandler extends Handler {
        private List<Handler.Callback> clientCallbacks;
        private boolean isDebug;

        public IncomingHandler(HandlerThread handlerThread, boolean isDebug) {
            super(handlerThread.getLooper());

            this.clientCallbacks = new ArrayList<>();
            this.isDebug = isDebug;
        }

        @Override
        public void handleMessage(Message msg) {
            if (isDebug) {
                logi("IncomingHandler.handleMessage()");
            }

            super.handleMessage(msg);

            if (msg.what == ANDROID_MESSAGE) {
                if (msg.arg1 == IPC_FUNCTION_CODE_INIT) {
                    if (isDebug) {
                        logi("IncomingHandler.handleMessage() :: ANDROID_MESSAGE.IPC_FUNCTION_CODE_INIT");
                    }
                    return;
                }
            }

            synchronized (initLock) {
                for (Handler.Callback clientCallback : clientCallbacks) {
                    if (clientCallback != null) {
                        if (isDebug) {
                            logi("IncomingHandler.handleMessage() :: c != null");
                        }

                        clientCallback.handleMessage(msg);
                    }
                }
            }
        }
    }
}
