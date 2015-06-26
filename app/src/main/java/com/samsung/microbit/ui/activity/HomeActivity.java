package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;

import java.util.List;
import java.util.logging.Handler;

public class HomeActivity extends Activity {


	protected String TAG = "BLEBaseService";
	protected boolean debug = true;

	protected void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getResources().getBoolean(R.bool.portrait_only)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}

		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		MBApp.setContext(this);

		setContentView(R.layout.activity_home);

		RelativeLayout connectBarView = (RelativeLayout) findViewById(R.id.connectBarView);
		connectBarView.getBackground().setAlpha(128);

		updateConnectBarTitle();

		RelativeLayout projectBarView = (RelativeLayout) findViewById(R.id.projectBarView);
		projectBarView.getBackground().setAlpha(128);

		updateProjectBarProjects();

		// Start the other services - local service to handle IPC in the main process
		Intent ipcIntent = new Intent(this, IPCService.class);
		startService(ipcIntent);

		Intent bleIntent = new Intent(this, BLEService.class);
		startService(bleIntent);

		Intent intent = new Intent(this, PluginService.class);
		startService(intent);
	}

	public void onBtnClicked(View v) {
		if (v.getId() == R.id.addDevice) {
			Intent intent = new Intent(this, ConnectActivity.class);
			startActivity(intent);
		} else if (v.getId() == R.id.startNewProject) {

			/*
			Intent intent = new Intent(this, WebViewActivity.class);
			intent.putExtra(Constants.URL, getString(R.string.touchDevURLNew));
			startActivity(intent);
			*/
			IPCService.instance.sendtoBLEService(1, null);

		} else if (v.getId() == R.id.numOfProjects) {
			Intent intent = new Intent(this, ProjectActivity.class);
			startActivity(intent);
		}
	}

	private final void updateConnectBarTitle() {
		Button addDeviceButton = (Button) findViewById(R.id.addDevice);
		SharedPreferences p = Utils.getInstance().getPreferences(this);
		addDeviceButton.setText(p.getString(Utils.PREFERENCES_NAME_KEY, "Connect to your Micro:Bit"));
	}

	private final void updateProjectBarProjects() {
		Button numOfProjects = (Button) findViewById(R.id.numOfProjects);
		numOfProjects.setText(Integer.toString(Utils.findProgramsAndPopulate(null, null)) + " saved projects");
	}

	public void onResume() {
		super.onResume();
		updateConnectBarTitle();
		updateProjectBarProjects();
	}
}