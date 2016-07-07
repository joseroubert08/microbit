package com.samsung.microbit.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Telephony;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.RegistrationIds;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.utils.Utils;

/**
 * Allows to handle incoming telephone calls and sms on a mobile device
 * using a micro:bit board.
 */
public class TelephonyPlugin {

    private static final String TAG = TelephonyPlugin.class.getSimpleName();

    /**
     * Provides general method to perform all available plugin action
     * using a command argument to identify which action should be performed.
     *
     * @param ctx Context.
     * @param cmd Command argument that defines action that should be pro
     */
    public static void pluginEntry(Context ctx, CmdArg cmd) {
        boolean register = false;
        if (cmd.getValue() != null) {
            register = cmd.getValue().toLowerCase().equals("on");
        }

        switch (cmd.getCMD()) {
            case RegistrationIds.REG_TELEPHONY: {
                if (register) {
                    registerIncomingCall();
                } else {
                    unregisterIncomingCall();
                }
                break;
            }
            case RegistrationIds.REG_MESSAGING: {
                if (register) {
                    registerIncomingSMS();
                } else {
                    unregisterIncomingSMS();
                }
                break;
            }
        }
    }

    //TODO: needed?
    public static void sendCommandBLE(int mbsService, CmdArg cmd) {
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

    /**
     * Listener to handle incoming call.
     */
    static class IncomingCallListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.i(TAG, "onCallStateChanged: " + state);

                    PluginService.sendMessageToBle(Utils.makeMicroBitValue(EventCategories.SAMSUNG_DEVICE_INFO_ID,
                            EventSubCodes.SAMSUNG_INCOMING_CALL));
                    break;
            }
        }
    }

    /**
     * Listener to handle incoming SMS messages.
     */
    static class IncomingSMSListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                PluginService.sendMessageToBle(Utils.makeMicroBitValue(EventCategories.SAMSUNG_DEVICE_INFO_ID,
                        EventSubCodes.SAMSUNG_INCOMING_SMS));
            }
        }
    }

    static TelephonyManager sTelephonyManager;
    static IncomingCallListener sIncomingCallListener;
    static IncomingSMSListener sIncomingSMSListener;

    static {
        sTelephonyManager = null;
        sIncomingCallListener = null;
        sIncomingSMSListener = null;
    }

    /**
     * Registers incoming call listener.
     */
    public static void registerIncomingCall() {
        Log.i(TAG, "registerIncomingCall: ");

        if (sTelephonyManager == null) {
            sTelephonyManager = (TelephonyManager) MBApp.getApp().getSystemService(Context.TELEPHONY_SERVICE);
        }

        if (sIncomingCallListener == null) {
            sIncomingCallListener = new IncomingCallListener();
        }

        sTelephonyManager.listen(sIncomingCallListener, PhoneStateListener.LISTEN_CALL_STATE);
        CmdArg cmd = new CmdArg(0, "Registered Incoming Call Alert");
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
    }

    /**
     * Unregisters incoming call listener.
     */
    public static void unregisterIncomingCall() {
        Log.i(TAG, "unregisterIncomingCall: ");

        if (sTelephonyManager == null) {
            return;
        }

        if (sIncomingCallListener == null) {
            return;
        }

        sTelephonyManager.listen(sIncomingCallListener, PhoneStateListener.LISTEN_NONE);
        sIncomingCallListener = null;

        if (sIncomingSMSListener == null) {
            sTelephonyManager = null;
        }

        CmdArg cmd = new CmdArg(0, "Unregistered Incoming Call Alert");
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
    }

    //TODO: consider to use or remove
    public static boolean isIncomingCallRegistered() {
        return sIncomingCallListener != null;
    }

    /**
     * Registers incoming sms message listener.
     */
    public static void registerIncomingSMS() {
        Log.i(TAG, "registerIncomingSMS: ");

        Context mContext = MBApp.getApp();

        if (sTelephonyManager == null) {
            sTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }

        if (sIncomingSMSListener == null) {
            sIncomingSMSListener = new IncomingSMSListener();
        }

        mContext.registerReceiver(sIncomingSMSListener, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));

        Log.d(TAG, "registerIncomingSMS");
        CmdArg cmd = new CmdArg(0, "Registered Incoming SMS Alert");
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
    }

    /**
     * Uregisters incoming sms message listener.
     */
    public static void unregisterIncomingSMS() {
        Log.i(TAG, "unregisterIncomingSMS: ");

        if (sTelephonyManager == null) {
            return;
        }

        if (sIncomingSMSListener == null) {
            return;
        }

        MBApp.getApp().unregisterReceiver(sIncomingSMSListener);
        sIncomingSMSListener = null;

        if (sIncomingCallListener == null)
            sTelephonyManager = null;

        CmdArg cmd = new CmdArg(0, "Unregistered Incoming SMS Alert");
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
    }

    //TODO: consider to use or remove
    public static boolean isIncomingSMSRegistered() {
        return sIncomingSMSListener != null;
    }
}