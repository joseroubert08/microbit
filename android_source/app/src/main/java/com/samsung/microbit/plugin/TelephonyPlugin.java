package com.samsung.microbit.plugin;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.service.PluginService;

/**
 * Created by t.maestri on 16/05/2015.
 */
public class TelephonyPlugin
{
    private static Context mContext = null;

    //Telephony plugin source
    public static final int CALL = 0;
    public static final int SMS = 1;

    public static void pluginEntry(Context ctx, CmdArg cmd) {
        mContext = ctx;
        boolean register = cmd.getValue().equals("on");
        switch (cmd.getCMD()) {
            case CALL: {
                if (register)
                    registerIncomingCall();
                else
                    unregisterIncomingCall();
                break;
            }
            case SMS: {
                if (register)
                    registerIncomingSMS();
                else
                    unregisterIncomingSMS();
                break;
            }
        }
    }

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
            CmdArg cmd;
            switch (state)
            {
                case TelephonyManager.CALL_STATE_IDLE:
                    cmd = new CmdArg(TelephonyPlugin.CALL,"Incoming Call Alert False");
                    TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    cmd = new CmdArg(TelephonyPlugin.CALL,"Incoming Call Alert True");
                    TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    cmd = new CmdArg(TelephonyPlugin.CALL,"Incoming Call Alert False");
                    TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);
                    break;
            }
        }
    }

    static class IncomingSMSListener extends PhoneStateListener
    {
        @Override
        public void onMessageWaitingIndicatorChanged (boolean mwi)
        {
            String mex;
            if(mwi)
                mex = "Incoming SMS Alert True";
            else
                mex = "Incoming SMS Alert False";
            CmdArg cmd = new CmdArg(TelephonyPlugin.SMS,mex);
            TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);

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
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);
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
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);
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

        mTelephonyManager.listen(mIncomingSMSListener, PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR);
        CmdArg cmd = new CmdArg(0,"Registered Incoming SMS Alert");
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);
    }

    public static void unregisterIncomingSMS()
    {
        if (mTelephonyManager == null)
            return;

        if(mIncomingSMSListener == null)
            return;

        mTelephonyManager.listen(mIncomingSMSListener, PhoneStateListener.LISTEN_NONE);
        mIncomingSMSListener = null;

        if(mIncomingCallListener==null)
            mTelephonyManager = null;

        CmdArg cmd = new CmdArg(0,"Unregistered Incoming SMS Alert");
        TelephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);
    }

    public static boolean isIncomingSMSRegistered()
    {
        return mIncomingSMSListener != null;
    }
}
