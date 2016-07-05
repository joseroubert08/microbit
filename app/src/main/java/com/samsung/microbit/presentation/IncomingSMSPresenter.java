package com.samsung.microbit.presentation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Telephony;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.TelephonyPluginNew;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.utils.Utils;

public class IncomingSMSPresenter implements Presenter {
    private static class IncomingSMSListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                PluginService.sendMessageToBle(Utils.makeMicroBitValue(EventCategories.SAMSUNG_DEVICE_INFO_ID,
                        EventSubCodes.SAMSUNG_INCOMING_SMS));
            }
        }
    }

    private MBApp microBitApp;
    private IncomingSMSListener incomingSMSListener = new IncomingSMSListener();
    private TelephonyPluginNew telephonyPluginNew;

    public IncomingSMSPresenter() {
        microBitApp = MBApp.getApp();
    }

    public void setTelephonyPluginNew(TelephonyPluginNew telephonyPluginNew) {
        this.telephonyPluginNew = telephonyPluginNew;
    }

    @Override
    public void start() {
        microBitApp.registerReceiver(incomingSMSListener, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));

        if(telephonyPluginNew != null) {
            CmdArg cmd = new CmdArg(0, "Registered Incoming SMS Alert");
            telephonyPluginNew.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
        }
    }

    @Override
    public void stop() {
        microBitApp.unregisterReceiver(incomingSMSListener);

        if(telephonyPluginNew != null) {
            CmdArg cmd = new CmdArg(0, "Unregistered Incoming SMS Alert");
            telephonyPluginNew.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report registration status?
        }
    }

    @Override
    public void destroy() {
        stop();
    }
}
