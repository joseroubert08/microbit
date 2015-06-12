package com.samsung.microbit.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.plugin.AlertPlugin;
import com.samsung.microbit.plugin.AudioPlugin;
import com.samsung.microbit.plugin.FeedbackPlugin;
import com.samsung.microbit.plugin.InformationPlugin;
import com.samsung.microbit.plugin.RemoteControlPlugin;
import com.samsung.microbit.plugin.TelephonyPlugin;
import com.samsung.microbit.plugin.CameraPlugin;

/**
 * Created by kkulendiran on 10/05/2015.
 */
public class PluginService extends Service {

	static final String TAG = "PluginService";
	private boolean debug = false;

	void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}

	Messenger mMessenger = new Messenger(new IncomingHandler());
	public static Messenger mClientMessenger = null;

	//MBS Services
	public static final int ALERT = 0;
	public static final int FEEDBACK = 1;
	public static final int INFORMATION = 2;
	public static final int AUDIO = 3;
	public static final int REMOTE_CONTROL = 4;
	public static final int TELEPHONY = 5;
	public static final int CAMERA = 6;

	/**
	 * Handler of incoming messages from BLEListener.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {

			logi("handleMessage()");
			Bundle data = msg.getData();
			mClientMessenger = msg.replyTo;
			CmdArg cmd = new CmdArg(data.getInt("cmd"), data.getString("value"));

			logi("handleMessage() ## msg.what = " + msg.what);
			logi("handleMessage() ## data.getInt=" + data.getInt("cmd") + " data.getString=" + data.getString("value"));

			switch (msg.what) {
				case ALERT:
					AlertPlugin.pluginEntry(PluginService.this, cmd);
					break;

				case FEEDBACK:
					FeedbackPlugin.pluginEntry(PluginService.this, cmd);
					break;


				case INFORMATION:
					InformationPlugin.pluginEntry(PluginService.this, cmd);
					break;

				case AUDIO:
					AudioPlugin.pluginEntry(PluginService.this, cmd);
					break;

				case REMOTE_CONTROL:
					RemoteControlPlugin.pluginEntry(PluginService.this, cmd);
					break;

				case TELEPHONY:
					TelephonyPlugin.pluginEntry(PluginService.this, cmd);
					break;

				case CAMERA:
					CameraPlugin.pluginEntry(PluginService.this, cmd);
					break;

				default:
					super.handleMessage(msg);
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logi("onStartCommand() ## start");
		//Toast.makeText(this, "Plugin Service Started", Toast.LENGTH_SHORT).show();
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Plugin Service Destroyed", Toast.LENGTH_SHORT).show();
	}
}
