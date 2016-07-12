package com.samsung.microbit.utils;

import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.PopUpServiceReceiver;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.model.NameValuePair;
import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;

import static com.samsung.microbit.BuildConfig.DEBUG;

/**
 * Provides additional functionality to work with services,
 * such as send messages to different services using IPC message manager.
 */
public class ServiceUtils {
    private static final String TAG = ServiceUtils.class.getSimpleName();

    private ServiceUtils() {
    }

    /**
     * Unified method to log informational messages in different services.
     *
     * @param serviceClass Service class where the log method should be called.
     * @param message      Message to log.
     */
    private static void logi(final Class serviceClass, final String message) {
        if (serviceClass.equals(IPCService.class)) {
            IPCService.logi(message);
        } else if (serviceClass.equals(PluginService.class)) {
            PluginService.logi(message);
        } else if (serviceClass.equals(BLEService.class)) {
            BLEService.logi(message);
        } else if (serviceClass.equals(PopUpServiceReceiver.class)) {
            PopUpServiceReceiver.logi(message);
        }
    }

    /**
     * Sends a message to IPC service using IPC message manager and logs it
     * if debug is on.
     *
     * @param serviceClass  Destination service class which should receive the message.
     * @param messageType   Type of the message.
     * @param eventCategory Event category.
     * @param cmd           Command argument.
     * @param args          Additional array of arguments.
     */
    public static void sendToIPCService(Class serviceClass, int messageType, int eventCategory, CmdArg cmd,
                                        NameValuePair[] args) {
        if (DEBUG) {
            if (cmd != null) {
                logi(serviceClass, serviceClass.getSimpleName() + ": sendToIPCService(), " + messageType + "," +
                        eventCategory + "," +
                        "(" + cmd.getValue() + "," + cmd.getCMD() + "");
            } else {
                logi(serviceClass, serviceClass.getSimpleName() + ": sendToIPCService(), " + messageType + "," + eventCategory);
            }
        }

        IPCMessageManager.getInstance().sendIPCMessage(IPCService.class, messageType, eventCategory, cmd, args);
    }

    /**
     * Sends a message to Plugin service using IPC message manager and logs it
     * if debug is on.
     *
     * @param serviceClass  Destination service class which should receive the message.
     * @param messageType   Type of the message.
     * @param eventCategory Event category.
     * @param cmd           Command argument.
     * @param args          Additional array of arguments.
     */
    public static void sendToPluginService(Class serviceClass, int messageType, int eventCategory, CmdArg cmd,
                                           NameValuePair[] args) {
        if (DEBUG) {
            if (cmd != null) {
                logi(serviceClass, serviceClass.getSimpleName() + ": sendToPluginService(), " + messageType + "," +
                        eventCategory + "," +
                        "(" + cmd.getValue() + "," + cmd.getCMD() + "");
            } else {
                logi(serviceClass, serviceClass.getSimpleName() + ": sendToPluginService(), " + messageType + "," +
                        eventCategory);
            }
        }

        IPCMessageManager.getInstance().sendIPCMessage(PluginService.class, messageType, eventCategory, cmd, args);
    }

    /**
     * Sends a message to BLE service using IPC message manager and logs it
     * if debug is on.
     *
     * @param serviceClass  Destination service class which should receive the message.
     * @param messageType   Type of the message.
     * @param eventCategory Event category.
     * @param cmd           Command argument.
     * @param args          Additional array of arguments.
     */
    public static void sendToBLEService(Class serviceClass, int messageType, int eventCategory, CmdArg cmd,
                                        NameValuePair[] args) {
        if (DEBUG) {
            if (cmd != null) {
                logi(serviceClass, serviceClass.getSimpleName() + ": sendToBLEService(), " + messageType + "," + eventCategory +
                        "," + cmd
                        .getValue() + "," + cmd.getCMD() + "");
            } else {
                logi(serviceClass, serviceClass.getSimpleName() + ": sendToBLEService(), " + messageType + "," + eventCategory);
            }
        }

        IPCMessageManager.getInstance().sendIPCMessage(BLEService.class, messageType, eventCategory, cmd, args);
    }

    public static void sendReplyCommand(int mbsService, CmdArg cmd) {
        if (IPCMessageManager.getInstance().getClientMessenger() != null) {
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
        }
    }
}
