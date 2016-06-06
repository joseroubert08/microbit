package com.samsung.microbit.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.activity.PopUpActivity;

//This broadcastreceiver intercepts "PopUp.showFromService" requests from background Service
//like PluginService.
//Note that custom OnClickListener are not supported because Service and App run in different process.
//Support for custom OnClickListener may require RPC implementation.
//PopUp requested from PluginService do not currently need custom OnClickListener.
public class PopUpServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PopUp.show(MBApp.getApp(), intent.getStringExtra(PopUpActivity.INTENT_EXTRA_MESSAGE),
                intent.getStringExtra(PopUpActivity.INTENT_EXTRA_TITLE),
                intent.getIntExtra(PopUpActivity.INTENT_EXTRA_ICON, 0),
                intent.getIntExtra(PopUpActivity.INTENT_EXTRA_ICONBG, 0),
                intent.getIntExtra(PopUpActivity.INTENT_GIFF_ANIMATION_CODE, 0), /* Default 0 */
                intent.getIntExtra(PopUpActivity.INTENT_EXTRA_TYPE, PopUp.TYPE_MAX),
                null, null);
    }
}