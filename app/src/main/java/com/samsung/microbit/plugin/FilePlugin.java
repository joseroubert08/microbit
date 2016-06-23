package com.samsung.microbit.plugin;

import android.content.Context;

import com.samsung.microbit.data.model.CmdArg;

public class FilePlugin {

	//File plugin action
	public static final int DOWNLOAD = 0;

	public static void pluginEntry(Context ctx, CmdArg cmd) {
		switch (cmd.getCMD()) {
			case DOWNLOAD:
				//TODO CALL THE DOWNLOAD FUNCTION HERE
				break;
		}
	}


}
