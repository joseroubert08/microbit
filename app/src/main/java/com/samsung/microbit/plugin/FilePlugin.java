package com.samsung.microbit.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;

import com.samsung.microbit.core.Monitor;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.service.PluginService;

/**
 * Created by kkulendiran on 10/05/2015.
 */
public class FilePlugin {

	private static Context mContext = null;

	//File plugin action
	public static final int DOWNLOAD = 0;

	public static void pluginEntry(Context ctx, CmdArg cmd) {
		mContext = ctx;
		switch (cmd.getCMD()) {
			case DOWNLOAD:
				//TODO CALL THE DOWNLOAD FUNCTION HERE
				break;
		}
	}


}
