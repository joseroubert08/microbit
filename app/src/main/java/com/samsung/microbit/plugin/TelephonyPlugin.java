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

import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.service.PluginService;

/**
 * Created by t.maestri on 16/05/2015.
 */
public class TelephonyPlugin
{
    private static Context mContext = null;

    public static void pluginEntry(Context ctx, CmdArg cmd) {
        mContext = ctx;
        boolean register = cmd.getValue().equals("on");
        switch (cmd.getCMD()) {
            case Constants.REG_TELEPHONY: {
                if (register)
                    registerIncomingCall();
                else
                    unregisterIncomingCall();
                break;
            }
            case Constants.REG_MESSAGING: {
                if (register)
                    registerIncomingSMS();
                else
                    unregisterIncomingSMS();
                break;
            }
        }
    }

    //TODO: needed?
    public static void sendCommandBLE(int mbsService, CmdArg cmd) {
        if(PluginService.mClientMessenger != null) {
            Message msg = Message.obtain(null, mbsService);
            Bundle bundle = new Bundle();
            bundle.putInt("cmd", cmd.getCMD());
            bundle.putString("value", cmd.getValue());
            msg.setData(bundle);

            try {
                PluginService.mClientMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    static class IncomingCallListener extends PhoneStateListener
    {
        @Override
        public void onCallStateChanged(int state, String incomingNumber)
        {
            switch (state)
            {
                case TelephonyManager.CALL_STATE_RINGING:
                    //TODO: fix ble calls
                    //PluginService.sendMessageToBle(Constants.makeMicroBitValue(Constants.SAMSUNG_TELEPHONY_ID,Constants.SAMSUNG_INCOMING_CALL));
                    break;
            }
        }
    }

    static class IncomingSMSListener extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                //TODO: fix ble calls
                //PluginService.sendMessageToBle(Constants.makeMicroBitValue(Constants.SAMSUNG_TELEPHONY_ID, Constants.SAMSUNG_INCOMING_SMS));
            }
        }
    }

    static TelephonyManager mTelephonyManager;
    static IncomingCallListener mIncomingCallListener;
    static IncomingSMSListener mIncomingSMSListener;

    static
    {
        mTelephonyManager = null;
        mIncomingCallListener = null;
        mIncomingSMSListener = null;
    }

    public static void registerIncomingCall()
    {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }

        if(mIncomingCallListener == null) {
            mIncomingCallListener = new IncomingCallListener();
        }

        mTelephonyManager.listen(mIncomingCallListener, PhoneStateListener.LISTEN_CALL_STATE);
        CmdArg cmd = new CmdArg(0,"Registered Incoming Call Alert");
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
    }

    public static void unregisterIncomingCall()
    {
        if (mTelephonyManager == null) {
            return;
        }

        if(mIncomingCallListener == null) {
            return;
        }

        mTelephonyManager.listen(mIncomingCallListener, PhoneStateListener.LISTEN_NONE);
        mIncomingCallListener = null;

        if(mIncomingSMSListener==null) {
            mTelephonyManager = null;
        }

        CmdArg cmd = new CmdArg(0,"Unregistered Incoming Call Alert");
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
    }

    public static boolean isIncomingCallRegistered()
    {
        return mIncomingCallListener != null;
    }

    public static void registerIncomingSMS()
    {
        if (mTelephonyManager == null)
            mTelephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);

        if(mIncomingSMSListener == null)
            mIncomingSMSListener = new IncomingSMSListener();

        mContext.registerReceiver(mIncomingSMSListener, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));

        Log.d("FMA", "registerIncomingSMS");
        CmdArg cmd = new CmdArg(0,"Registered Incoming SMS Alert");
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
    }

    public static void unregisterIncomingSMS()
    {
        if (mTelephonyManager == null)
            return;

        if(mIncomingSMSListener == null)
            return;

        mContext.unregisterReceiver(mIncomingSMSListener);
        mIncomingSMSListener = null;

        if(mIncomingCallListener==null)
            mTelephonyManager = null;

        CmdArg cmd = new CmdArg(0,"Unregistered Incoming SMS Alert");
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
    }

    public static boolean isIncomingSMSRegistered()
    {
        return mIncomingSMSListener != null;
    }
}
