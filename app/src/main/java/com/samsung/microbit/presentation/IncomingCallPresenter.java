package com.samsung.microbit.presentation;

import android.content.Context;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.TelephonyPlugin;
import com.samsung.microbit.service.BLEServiceNew;
import com.samsung.microbit.service.PluginServiceNew;
import com.samsung.microbit.utils.ServiceUtils;
import com.samsung.microbit.utils.Utils;

public class IncomingCallPresenter implements Presenter {
    private static final String TAG = IncomingCallPresenter.class.getSimpleName();

    private PhoneStateListener incomingCallListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.i(TAG, "onCallStateChanged: " + state);

                    ServiceUtils.IMessengerFinder messengerFinder = MBApp.getApp().getMessengerFinder();

                    if(messengerFinder != null) {
                        Messenger bleMessenger = messengerFinder.getMessengerForService(BLEServiceNew.class.getName());

                        if(bleMessenger != null) {
                            Message message = ServiceUtils.composeBLECharacteristicMessage(Utils.makeMicroBitValue
                                     (EventCategories.SAMSUNG_DEVICE_INFO_ID, EventSubCodes.SAMSUNG_INCOMING_CALL));
                            if(message != null) {
                                try {
                                    bleMessenger.send(message);
                                } catch (RemoteException e) {
                                    Log.e(TAG, e.toString());
                                }
                            }
                        }
                    }
                    break;
            }
        }
    };

    private TelephonyManager telephonyManager;
    private boolean isRegistered;
    private TelephonyPlugin telephonyPlugin;

    public IncomingCallPresenter() {
        telephonyManager = (TelephonyManager) MBApp.getApp().getSystemService(Context.TELEPHONY_SERVICE);
    }

    public void setTelephonyPlugin(TelephonyPlugin telephonyPlugin) {
        this.telephonyPlugin = telephonyPlugin;
    }

    @Override
    public void start() {
        if (!isRegistered) {
            isRegistered = true;
            telephonyManager.listen(incomingCallListener, PhoneStateListener.LISTEN_CALL_STATE);

            if(telephonyPlugin != null) {
                CmdArg cmd = new CmdArg(0, "Registered Incoming Call Alert");
                telephonyPlugin.sendCommandBLE(PluginServiceNew.TELEPHONY, cmd);//TODO: do we need to report
                // registration status?
            }
        }
    }

    @Override
    public void stop() {
        if (isRegistered) {
            telephonyManager.listen(incomingCallListener, TelephonyManager.PHONE_TYPE_NONE);

            if(telephonyPlugin != null) {
                CmdArg cmd = new CmdArg(0, "Unregistered Incoming Call Alert");
                telephonyPlugin.sendCommandBLE(PluginServiceNew.TELEPHONY, cmd);//TODO: do we need to report
                // registration status?
            }

            isRegistered = false;
        }
    }

    @Override
    public void destroy() {
        stop();
    }
}
