package com.samsung.microbit.presentation;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.TelephonyPluginNew;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.utils.Utils;

public class IncomingCallPresenter implements Presenter {
    private static final String TAG = IncomingCallPresenter.class.getSimpleName();

    private PhoneStateListener incomingCallListener = new PhoneStateListener() {
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
    };

    private TelephonyManager telephonyManager;
    private boolean isRegistered;
    private TelephonyPluginNew telephonyPluginNew;

    public IncomingCallPresenter() {
        telephonyManager = (TelephonyManager) MBApp.getApp().getSystemService(Context.TELEPHONY_SERVICE);
    }

    public void setTelephonyPluginNew(TelephonyPluginNew telephonyPluginNew) {
        this.telephonyPluginNew = telephonyPluginNew;
    }

    @Override
    public void start() {
        if (!isRegistered) {
            isRegistered = true;
            telephonyManager.listen(incomingCallListener, PhoneStateListener.LISTEN_CALL_STATE);

            if(telephonyPluginNew != null) {
                CmdArg cmd = new CmdArg(0, "Registered Incoming Call Alert");
                telephonyPluginNew.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
            }
        }
    }

    @Override
    public void stop() {
        if (isRegistered) {
            telephonyManager.listen(incomingCallListener, TelephonyManager.PHONE_TYPE_NONE);

            if(telephonyPluginNew != null) {
                CmdArg cmd = new CmdArg(0, "Unregistered Incoming Call Alert");
                telephonyPluginNew.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
            }

            isRegistered = false;
        }
    }

    @Override
    public void destroy() {
        stop();
    }
}
