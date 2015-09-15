package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.ui.BluetoothSwitch;
import com.samsung.microbit.ui.PopUp;

import java.util.List;
import java.util.logging.Handler;

import static com.samsung.microbit.core.CommonGUI.commonAlertDialog;

public class HomeActivity extends Activity implements View.OnClickListener {


    SharedPreferences prefs = null;
    ListView helpList = null ;
    ArrayAdapter adapter = null ;
	/* *************************************************
	 * TODO setup to Handle BLE Notiifications
	 */
	IntentFilter broadcastIntentFilter;
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			int v = intent.getIntExtra(IPCMessageManager.BUNDLE_ERROR_CODE, 0);
			if (Constants.BLE_DISCONNECTED_FOR_FLASH == v){
				logi("Bluetooth disconnected for flashing. No need to display pop-up");
                handleBLENotification(context, intent, false);
				return;
			}
            handleBLENotification(context, intent, true);
			if (v != 0) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						PopUp.show(MBApp.getContext(),
							MBApp.getContext().getString(R.string.micro_bit_reset_msg),
							"",
							R.drawable.error_face, R.drawable.red_btn,
							PopUp.TYPE_ALERT, null, null);
					}
				});
			}
		}
	};

	protected String TAG = "HomeActivity";
	protected boolean debug = true;

	protected void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}

	private void handleBLENotification(Context context, Intent intent, boolean hide) {
        final boolean popupHide = hide;
		logi("handleBLENotification()");
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateConnectBarView();
                if(popupHide)
                    PopUp.hide();
			}
		});
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //handle orientation change to prevent re-creation of activity.
        //i.e. while recording we need to preserve state of recorder
        super.onConfigurationChanged(newConfig);

        //setBackground();
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (debug) logi("onCreate() :: ");
		MBApp.setContext(this);
		/* *************************************************
		 * TODO setup to Handle BLE Notiification
		 */
		if (broadcastIntentFilter == null) {
			broadcastIntentFilter = new IntentFilter(IPCService.INTENT_BLE_NOTIFICATION);
			LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(broadcastReceiver, broadcastIntentFilter);
		}

		setContentView(R.layout.activity_home);

		RelativeLayout connectBarView = (RelativeLayout) findViewById(R.id.connectBarView);
		connectBarView.getBackground().setAlpha(128);

		updateConnectBarView();

		RelativeLayout projectBarView = (RelativeLayout) findViewById(R.id.projectBarView);
		projectBarView.getBackground().setAlpha(128);

		updateProjectBarView();

		// Start the other services - local service to handle IPC in the main process
		Intent ipcIntent = new Intent(this, IPCService.class);
		startService(ipcIntent);

		Intent bleIntent = new Intent(this, BLEService.class);
		startService(bleIntent);

		final Intent intent = new Intent(this, PluginService.class);
		startService(intent);

        prefs = getSharedPreferences("com.samsung.microbit", MODE_PRIVATE);

        helpList = (ListView) findViewById(R.id.moreItems);
        if (helpList != null){
            String [] items= getResources().getStringArray(R.array.moreListItems);
            adapter = new ArrayAdapter<String>(this, R.layout.aligned_right, items) ;
            helpList.setAdapter(adapter);

            helpList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    switch (position) {
                        case 0:
                        case 1:
                            startGeneralView(position);
                            helpList.setVisibility(View.INVISIBLE);
                            break;
                    }
                }
            });
        }
	}

    private void startGeneralView(int position) {
        Intent i = new Intent(this, GeneralWebView.class);
        switch (position){
            case 0:
                i.putExtra("url", "file:///android_asset/www/help.html");
                break;
            case 1:
                i.putExtra("url", "file:///android_asset/www/about.html");
                break;
        }
        i.putExtra("title", adapter.getItem(position).toString());
        startActivity(i);
    }

    @Override
	protected void onStart() {
		super.onStart();
	}


	public void onClick(final View v) {
		if (debug) logi("onBtnClicked() :: ");
		if (v.getId() == R.id.addDevice || v.getId() == R.id.addDeviceEmpty) {
			Intent intent = new Intent(this, ConnectActivity.class);
			startActivity(intent);
		} else if (v.getId() == R.id.startNewProject) {
			Intent intent = new Intent(this, TouchDevActivity.class);
			intent.putExtra(Constants.URL, getString(R.string.touchDevURLNew));
			startActivity(intent);
		} else if (v.getId() == R.id.numOfProjectsHolder) {
			Intent intent = new Intent(this, ProjectActivity.class);
			startActivity(intent);
		} else if (v.getId() == R.id.connectBtn) {
			updateConnectBarView();
			ConnectedDevice connectedDevice = Utils.getPairedMicrobit(this);
			if (!BluetoothSwitch.getInstance().checkBluetoothAndStart()){
				return;
			}
			if (connectedDevice.mPattern != null) {
				if (connectedDevice.mStatus) {
					if (debug) logi("onBtnClicked() :: IPCService.getInstance().bleDisconnect()");
					IPCService.getInstance().bleDisconnect();
				} else {
					if (debug) logi("onBtnClicked() :: IPCService.getInstance().bleConnect()");

					// using MBApp.getContext() instead of this causes problem after flashing
					PopUp.show(MBApp.getContext(),
						getString(R.string.init_connection),
						"",
						R.drawable.flash_face, R.drawable.blue_btn,
						PopUp.TYPE_SPINNER,
						null, null);

					IPCService.getInstance().bleConnect();
				}
			} else {
				PopUp.show(MBApp.getContext(),
					getString(R.string.no_device_paired),
					"",
					R.drawable.error_face, R.drawable.red_btn,
					PopUp.TYPE_ALERT,
					null, null);
			}
		} else if (v.getId() == R.id.moreButton){
            //Display the ListView
            if (helpList != null){
                if (helpList.getVisibility() == View.INVISIBLE) {
                    helpList.setVisibility(View.VISIBLE);
                    helpList.bringToFront();
                }  else {
                    helpList.setVisibility(View.INVISIBLE);
                }
            }
        }
	}

	private final void updateConnectBarView() {
		Button addDeviceButton = (Button) findViewById(R.id.addDevice);
		Button addDeviceEmpty = (Button) findViewById(R.id.addDeviceEmpty);
		ImageButton connectButton = (ImageButton) findViewById(R.id.connectBtn);
		ConnectedDevice connectedDevice = Utils.getPairedMicrobit(this);

		if (connectedDevice.mPattern != null) {
			addDeviceButton.setVisibility(View.VISIBLE);
			connectButton.setVisibility(View.VISIBLE);
			if (connectedDevice.mName!= null)
				addDeviceButton.setText(connectedDevice.mName + " (" + connectedDevice.mPattern + ")" );
			else
				addDeviceButton.setText("");
			addDeviceEmpty.setVisibility(View.GONE);
		} else {
			addDeviceButton.setVisibility(View.GONE);
			connectButton.setVisibility(View.GONE);
			addDeviceEmpty.setVisibility(View.VISIBLE);
            addDeviceEmpty.setText(R.string.connect_to_mbit);
		}

		if (connectedDevice.mPattern != null && connectedDevice.mStatus) {
			connectButton.setImageResource(R.drawable.device_connected);
			connectButton.setBackground(getResources().getDrawable(R.drawable.green_btn));
		} else {
			connectButton.setImageResource(R.drawable.disconnect_device);
			connectButton.setBackground(getResources().getDrawable(R.drawable.red_btn));
		}
	}

	private final void updateProjectBarView() {
		TextView numOfProjects = (TextView) findViewById(R.id.numOfProjects);
		numOfProjects.setText(Integer.toString(Utils.findProgramsAndPopulate(null, null)));
	}

	@Override
	public void onResume() {
		if (debug) logi("onResume() :: ");
		super.onResume();


        if (prefs.getBoolean("firstrun", true)) {
            //First Run. Install the Sample applications
            Toast.makeText(MBApp.getContext(), "Installing Sample HEX files. The projects number will be updated in some time" ,Toast.LENGTH_LONG).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Utils.installSamples();
                    prefs.edit().putBoolean("firstrun", false).commit();
                }
            }).start();
        } else {
            logi("Not the first run");
        }

		MBApp.setContext(this);
		updateConnectBarView();
        updateProjectBarView();

	}
}
