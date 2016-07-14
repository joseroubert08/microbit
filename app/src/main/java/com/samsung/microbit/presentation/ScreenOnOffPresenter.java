package com.samsung.microbit.presentation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.service.BLEServiceNew;
import com.samsung.microbit.utils.ServiceUtils;
import com.samsung.microbit.utils.Utils;

public class ScreenOnOffPresenter implements Presenter {
    private static final String TAG = ScreenOnOffPresenter.class.getSimpleName();

    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                ServiceUtils.IMessengerFinder messengerFinder = MBApp.getApp().getMessengerFinder();

                if(messengerFinder != null) {
                    Messenger bleMessenger = messengerFinder.getMessengerForService(BLEServiceNew.class.getName());

                    if(bleMessenger != null) {
                        Message message = ServiceUtils.composeBLECharacteristicMessage(Utils.makeMicroBitValue
                                 (EventCategories.SAMSUNG_DEVICE_INFO_ID, EventSubCodes.SAMSUNG_DEVICE_DISPLAY_OFF));
                        if(message != null) {
                            try {
                                bleMessenger.send(message);
                            } catch (RemoteException e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    }
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                ServiceUtils.IMessengerFinder messengerFinder = MBApp.getApp().getMessengerFinder();

                if(messengerFinder != null) {
                    Messenger bleMessenger = messengerFinder.getMessengerForService(BLEServiceNew.class.getName());

                    if(bleMessenger != null) {
                        Message message = ServiceUtils.composeBLECharacteristicMessage(Utils.makeMicroBitValue
                                 (EventCategories.SAMSUNG_DEVICE_INFO_ID, EventSubCodes.SAMSUNG_DEVICE_DISPLAY_ON));
                        if(message != null) {
                            try {
                                bleMessenger.send(message);
                            } catch (RemoteException e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    }
                }
            }
        }
    };

    private MBApp application;
    private boolean isRegistered;

    public ScreenOnOffPresenter() {
        application = MBApp.getApp();
    }

    @Override
    public void start() {
        if (!isRegistered) {
            isRegistered = true;
            Log.i(TAG, "registerDisplay() ");

            IntentFilter screenStateFilter = new IntentFilter();
            screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
            screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
            application.registerReceiver(screenReceiver, screenStateFilter);
        }
    }

    @Override
    public void stop() {
        if (isRegistered) {
            Log.i(TAG, "unregisterDisplay() ");

            application.unregisterReceiver(screenReceiver);
            isRegistered = false;
        }
    }

    @Override
    public void destroy() {
        stop();
    }
}
