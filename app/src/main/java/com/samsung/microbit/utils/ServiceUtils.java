package com.samsung.microbit.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.CharacteristicUUIDs;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.GattFormats;
import com.samsung.microbit.data.constants.GattServiceUUIDs;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.constants.ServiceIds;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.model.NameValuePair;
import com.samsung.microbit.service.BLEServiceNew;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides additional functionality to work with services,
 * such as send messages to different services using IPC message manager.
 */
public class ServiceUtils {
    private static final String TAG = ServiceUtils.class.getSimpleName();

    private ServiceUtils() {
    }

    public static void sendReplyCommand(int mbsService, CmdArg cmd) {
        MBApp application = MBApp.getApp();

        if (application.getIpcMessenger() != null) {
            Message msg = Message.obtain(null, mbsService);
            Bundle bundle = new Bundle();
            bundle.putInt("cmd", cmd.getCMD());
            bundle.putString("value", cmd.getValue());
            msg.setData(bundle);

            try {
                application.getIpcMessenger().send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }

        //TODO check it for work
        /*if (IPCMessageManager.getInstance().getClientMessenger() != null) {
            Message msg = Message.obtain(null, mbsService);
            Bundle bundle = new Bundle();
            bundle.putInt("cmd", cmd.getCMD());
            bundle.putString("value", cmd.getValue());
            msg.setData(bundle);

            try {
                IPCMessageManager.getInstance().getClientMessenger().send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }*/
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

    public static void bindService(Class serviceClass, IMessengerFinder serviceConnection) {
        MBApp application = MBApp.getApp();
        application.bindService(new Intent(application, serviceClass), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public static void unbindService(IMessengerFinder serviceConnection) {
        MBApp.getApp().unbindService(serviceConnection);
    }

    public static IMessengerFinder createMessengerFinder() {
        return new IMessengerFinder() {
            private final Map<String, Messenger> clientMessengers = new HashMap<>();

            @Override
            public Messenger getMessengerForService(String serviceName) {
                return clientMessengers.get(serviceName);
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                clientMessengers.put(name.getClassName(), new Messenger(service));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                clientMessengers.remove(name.getClassName());
            }
        };
    }

    /**
     * Sends message to some service
     * ({@link com.samsung.microbit.service.PluginServiceNew PluginServiceNew},
     * {@link com.samsung.microbit.service.BLEServiceNew BLEServiceNew},
     *
     * @param destService   Class of service, message need sent to.
     * @param messageType   Android or microbit message. One of the {@link com.samsung.microbit.data.constants.IPCConstants#MESSAGE_ANDROID},
     *                      {@link com.samsung.microbit.data.constants.IPCConstants#MESSAGE_MICROBIT}
     * @param eventCategory Event category listed in {@link EventCategories}
     * @param cmd           Command argument.
     * @param args          Array of data.
     */
    public static void sendMessage(IMessengerFinder messengerFinder, Class destService, int messageType, @ServiceIds int
            serviceId, int eventCategory, CmdArg cmd, NameValuePair[] args, Messenger messengerForAnswer) {
        Message msg = composeMessage(messageType, serviceId, eventCategory, cmd, args);

        try {
            sendMessage(messengerFinder, destService, msg, messengerForAnswer);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static Message composeMessage(int messageType, int eventCategory, @ServiceIds int serviceId, CmdArg cmd,
                                         NameValuePair[] args) {
        if (messageType != IPCConstants.MESSAGE_ANDROID && messageType != IPCConstants.MESSAGE_MICROBIT) {
            return null;
        }
        Message msg = Message.obtain(null, messageType);

        msg.arg1 = eventCategory;
        msg.arg2 = serviceId;

        Bundle bundle = new Bundle();
        if (cmd != null) {
            bundle.putInt(IPCConstants.BUNDLE_DATA, cmd.getCMD());
            bundle.putString(IPCConstants.BUNDLE_VALUE, cmd.getValue());
        }

        if (args != null) {
            for (NameValuePair arg : args) {
                bundle.putSerializable(arg.getName(), arg.getValue());
            }
        }

        msg.setData(bundle);

        return msg;
    }

    public static Message composeBLECharacteristicMessage(int value) {
        NameValuePair[] args = new NameValuePair[4];
        args[0] = new NameValuePair(IPCConstants.BUNDLE_SERVICE_GUID, GattServiceUUIDs.EVENT_SERVICE.toString());
        args[1] = new NameValuePair(IPCConstants.BUNDLE_CHARACTERISTIC_GUID, CharacteristicUUIDs.ES_CLIENT_EVENT.toString());
        args[2] = new NameValuePair(IPCConstants.BUNDLE_CHARACTERISTIC_VALUE, value);
        args[3] = new NameValuePair(IPCConstants.BUNDLE_CHARACTERISTIC_TYPE, GattFormats.FORMAT_UINT32);

        return composeMessage(IPCConstants.MESSAGE_MICROBIT, EventCategories.IPC_WRITE_CHARACTERISTIC,
                ServiceIds.SERVICE_NONE, null, args);
    }

    public static Message copyMessageFromOld(Message oldMessage, @ServiceIds int serviceId) {
        Message newMessage = Message.obtain(null, oldMessage.what);
        newMessage.arg1 = oldMessage.arg1;
        newMessage.arg2 = oldMessage.arg2;
        newMessage.setData(new Bundle(oldMessage.getData()));
        return newMessage;
    }

    private static void sendMessage(IMessengerFinder messengerFinder, Class destService, Message msg, Messenger
            messengerForAnswer) throws RemoteException {
        Log.i(TAG, "sendMessage()");

        Messenger messenger = messengerFinder.getMessengerForService(destService.getName());
        if (messenger != null) {
            msg.replyTo = messengerForAnswer;
            messenger.send(msg);
        }
    }

    public static void sendConnectDisconnectMessage(boolean connect) {
        MBApp application = MBApp.getApp();

        if (connect) {
            Message connectMessage = ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID, EventCategories
                    .IPC_BLE_CONNECT, ServiceIds.SERVICE_NONE, null, null);

            if(connectMessage != null) {
                connectMessage.replyTo = application.getIpcMessenger();

                try {
                    Messenger bleMessenger = application.getMessengerFinder().getMessengerForService(BLEServiceNew
                             .class.getName());

                    if (bleMessenger != null) {
                        bleMessenger.send(connectMessage);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Message connectMessage = ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID, EventCategories
                    .IPC_BLE_DISCONNECT, ServiceIds.SERVICE_NONE, null, null);

            if (connectMessage != null) {
                connectMessage.replyTo = application.getIpcMessenger();

                try {
                    Messenger bleMessenger = application.getMessengerFinder().getMessengerForService(BLEServiceNew
                             .class.getName());

                    if (bleMessenger != null) {
                        bleMessenger.send(connectMessage);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public interface IMessengerFinder extends ServiceConnection {
        Messenger getMessengerForService(String serviceName);
    }
}
