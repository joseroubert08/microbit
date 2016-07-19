package com.samsung.microbit.plugin;

import android.content.Context;

import com.samsung.microbit.model.CmdArg;

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
