package com.samsung.microbit.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.core.BroadcastMonitor;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.service.PluginService;

/**
 * Provides methods to send reply commands about sharing statistics.
 */
public class FeedbackPlugin {

    private static final String TAG = FeedbackPlugin.class.getSimpleName();

    private static BroadcastReceiver sReceiver = null;

    //Feedback plugin action
    public static final int DISPLAY = 0;

    public static void pluginEntry(Context ctx, CmdArg cmd) {
        switch (cmd.getCMD()) {
            case DISPLAY:
                toggleRegisteringScreenOnOffIntent();
                break;
        }
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

    private static void toggleRegisteringScreenOnOffIntent() {
        if (sReceiver == null) {
            final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            sReceiver = new BroadcastMonitor();
            MBApp.getApp().registerReceiver(sReceiver, filter);

            CmdArg cmd = new CmdArg(0, "Registered Screen On/Off event.");
            FeedbackPlugin.sendReplyCommand(PluginService.FEEDBACK, cmd);
        } else { //TODO - When and where to unresgister????
            MBApp.getApp().unregisterReceiver(sReceiver);
            sReceiver = null;

            CmdArg cmd = new CmdArg(0, "Unregistered Screen On/Off event.");
            FeedbackPlugin.sendReplyCommand(PluginService.FEEDBACK, cmd);
        }
    }
}
