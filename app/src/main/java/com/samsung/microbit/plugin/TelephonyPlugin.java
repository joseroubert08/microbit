package com.samsung.microbit.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.service.PluginService;

public class TelephonyPlugin {

    public static void pluginEntry(Context ctx, CmdArg cmd) {
        boolean register = false;
        if (cmd.getValue() != null) {
            register = cmd.getValue().toLowerCase().equals("on");
        }

        switch (cmd.getCMD()) {
            case Constants.REG_TELEPHONY: {
                if (register) {
                    registerIncomingCall();
                } else {
                    unregisterIncomingCall();
                }
                break;
            }
            case Constants.REG_MESSAGING: {
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
                e.printStackTrace();
            }
        }
    }

    static class IncomingCallListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.i("TelephonyManager", "onCallStateChanged: " + state);

                    PluginService.sendMessageToBle(Constants.makeMicroBitValue(Constants.SAMSUNG_DEVICE_INFO_ID,
                            Constants.SAMSUNG_INCOMING_CALL));
                    break;
            }
        }
    }

    static class IncomingSMSListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                PluginService.sendMessageToBle(Constants.makeMicroBitValue(Constants.SAMSUNG_DEVICE_INFO_ID, Constants.SAMSUNG_INCOMING_SMS));
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

    public static void registerIncomingCall() {
        Log.i("TelephonyPlugin", "registerIncomingCall: ");

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

    public static void unregisterIncomingCall() {
        Log.i("TelephonyPlugin", "unregisterIncomingCall: ");

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

    public static boolean isIncomingCallRegistered() {
        return sIncomingCallListener != null;
    }

    public static void registerIncomingSMS() {
        Log.i("TelephonyPlugin", "registerIncomingSMS: ");

        Context mContext = MBApp.getApp();

        if (sTelephonyManager == null) {
            sTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }

        if (sIncomingSMSListener == null) {
            sIncomingSMSListener = new IncomingSMSListener();
        }

        mContext.registerReceiver(sIncomingSMSListener, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));

        Log.d("FMA", "registerIncomingSMS");
        CmdArg cmd = new CmdArg(0, "Registered Incoming SMS Alert");
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
    }

    public static void unregisterIncomingSMS() {
        Log.i("TelephonyPlugin", "unregisterIncomingSMS: ");

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

    public static boolean isIncomingSMSRegistered() {
        return sIncomingSMSListener != null;
    }
}