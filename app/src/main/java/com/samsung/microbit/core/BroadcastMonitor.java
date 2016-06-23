package com.samsung.microbit.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.FeedbackPlugin;
import com.samsung.microbit.service.PluginService;

public class BroadcastMonitor extends BroadcastReceiver{

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            CmdArg cmd = new CmdArg(FeedbackPlugin.DISPLAY,"Screen Off");
            FeedbackPlugin.sendReplyCommand(PluginService.FEEDBACK, cmd);
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            CmdArg cmd = new CmdArg(FeedbackPlugin.DISPLAY,"Screen On");
            FeedbackPlugin.sendReplyCommand(PluginService.FEEDBACK, cmd);
        }
    }
}
