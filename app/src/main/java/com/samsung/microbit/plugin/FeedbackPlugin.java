package com.samsung.microbit.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;

import com.samsung.microbit.core.BroadcastMonitor;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.service.PluginService;

/**
 * Created by kkulendiran on 10/05/2015.
 */
public class FeedbackPlugin {

	private static Context mContext = null;
	private static BroadcastReceiver mReceiver = null;

	//Feedback plugin action
	public static final int DISPLAY = 0;

	public static void pluginEntry(Context ctx, CmdArg cmd) {
		mContext = ctx;
		switch (cmd.getCMD()) {
			case DISPLAY:
				registerScreenOnOffIntent();
				break;
		}
	}

	public static void sendReplyCommand(int mbsService, CmdArg cmd) {
		if (PluginService.mClientMessenger != null) {
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

	private static void registerScreenOnOffIntent() {
		if (mReceiver == null) {
			final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			mReceiver = new BroadcastMonitor();
			mContext.registerReceiver(mReceiver, filter);

			CmdArg cmd = new CmdArg(0, "Registered Screen On/Off event.");
			FeedbackPlugin.sendReplyCommand(PluginService.FEEDBACK, cmd);
		} else { //TODO - When and where to unresgister????
			mContext.unregisterReceiver(mReceiver);
			mReceiver = null;

			CmdArg cmd = new CmdArg(0, "Unregistered Screen On/Off event.");
			FeedbackPlugin.sendReplyCommand(PluginService.FEEDBACK, cmd);
		}
	}
}
