package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
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

import java.util.HashMap;
import java.util.List;

import uk.co.bbc.echo.EchoConfigKeys;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {


    SharedPreferences prefs = null;
    StableArrayAdapter adapter = null ;
    private AppCompatDelegate delegate;

    boolean connectionInitiated = false;

    private MBApp app = null ;
	/* *************************************************
	 * TODO setup to Handle BLE Notifications
	 */
	IntentFilter broadcastIntentFilter;
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			int v = intent.getIntExtra(IPCMessageManager.BUNDLE_ERROR_CODE, 0);
            logi("broadcastReceiver Error code = " + v);
            if (Constants.BLE_DISCONNECTED_FOR_FLASH == v){
				logi("Bluetooth disconnected for flashing. No need to display pop-up");
                handleBLENotification(context, intent, false);
				return;
			}
            handleBLENotification(context, intent, true);
			if (v != 0) {
                String message = intent.getStringExtra(IPCMessageManager.BUNDLE_ERROR_MESSAGE);
                logi("broadcastReceiver Error message = " + message);

                if (message == null )
                    message = "Error";
                final String displayTitle = message ;

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						PopUp.show(MBApp.getContext(),
                                MBApp.getContext().getString(R.string.micro_bit_reset_msg),
                                displayTitle,
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
                if(popupHide && connectionInitiated)
                    PopUp.hide();
                    connectionInitiated = false;
			}
		});
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //handle orientation change to prevent re-creation of activity.
        //i.e. while recording we need to preserve state of recorder
        super.onConfigurationChanged(newConfig);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		logi("onCreate() :: ");
		MBApp.setContext(this);

        setContentView(R.layout.activity_home);
        setupDrawer();

        //Set up Echo
        setupEcho();

        if (broadcastIntentFilter == null) {
			broadcastIntentFilter = new IntentFilter(IPCService.INTENT_BLE_NOTIFICATION);
			LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(broadcastReceiver, broadcastIntentFilter);
		}

		RelativeLayout connectBarView = (RelativeLayout) findViewById(R.id.connectBarView);
		connectBarView.getBackground().setAlpha(128);

		updateConnectBarView();

//		RelativeLayout projectBarView = (RelativeLayout) findViewById(R.id.projectBarView);
//		projectBarView.getBackground().setAlpha(128);

	//	updateProjectBarView();

		// Start the other services - local service to handle IPC in the main process
		Intent ipcIntent = new Intent(this, IPCService.class);
		startService(ipcIntent);

		Intent bleIntent = new Intent(this, BLEService.class);
		startService(bleIntent);

		final Intent intent = new Intent(this, PluginService.class);
		startService(intent);

        prefs = getSharedPreferences("com.samsung.microbit", MODE_PRIVATE);

        if (app.getEcho()!= null) {
            logi("Page View test for HomeActivity");
            //Page view test
            app.getEcho().viewEvent("com.samsung.microbit.ui.activity.homeactivity.page", null);
        }
	}


    private void setupEcho(){
        // Echo Config
        app = (MBApp) MBApp.getApp().getApplicationContext();

        HashMap<String, String> config = new HashMap<String, String>();

        //Use ECHO_TRACE value for searching in echo chamber
        config.put(EchoConfigKeys.ECHO_TRACE, "microbit_android_app"); //TODO Change later
        //Use CS debug mode
        config.put(EchoConfigKeys.COMSCORE_DEBUG_MODE, "1");
        // Send Comscore events to EchoChamber
        config.put(EchoConfigKeys.COMSCORE_URL, "http://data.bbc.co.uk/v1/analytics-echo-chamber-inbound/comscore");
        //Enable debug mode
        config.put(EchoConfigKeys.ECHO_DEBUG, "1");
       // Send RUM events to EchoChamber
       //config.put(EchoConfigKeys.RUM_ENABLED, "true");
       //config.put(EchoConfigKeys.RUM_URL, "http://data.bbc.co.uk/v1/analytics-echo-chamber-inbound/rum");

        // Send BARB events
        //config.put(EchoConfigKeys.BARB_ENABLED, "true");
        //config.put(EchoConfigKeys.BARB_SITE_CODE, "bbcandroidtest");

        // Instantiate EchoClient
        app.initialiseEcho(config);
    }

    private void setupDrawer()
    {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.drawable.bbc_microbit);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id){
            case R.id.nav_Help:
                break;
            case R.id.nav_About:
                break;
            case R.id.nav_Privacy:
                break;
            case R.id.nav_TC:
                break;
            case R.id.nav_demo1:
                break;
            case R.id.nav_demo2:
                break;

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }



    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }
    private void startGeneralView(String title, int position) {
        Intent i = new Intent(this, GeneralWebView.class);
        switch (position){
            case 0:
                i.putExtra("url", "file:///android_asset/www/help.html");
                break;
            case 1:
                i.putExtra("url", "file:///android_asset/www/about.html");
                break;
			case 2:
				i.putExtra("url", getString(R.string.privacy_policy_url));
				break;
			case 3:
                i.putExtra("url", getString(R.string.terms_of_use_url));
				break;
        }
        i.putExtra("title", title);
        startActivity(i);
    }

    @Override
	protected void onStart() {
		super.onStart();
	}
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK){
                toggleConnection();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                PopUp.show(MBApp.getContext(),
                        getString(R.string.bluetooth_off_cannot_continue), //message
                        "",
                        R.drawable.error_face, R.drawable.red_btn,
                        PopUp.TYPE_ALERT,
                        null, null);
            }
        }
    }

    private void startBluetooth(){
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
    }

    private void toggleConnection(){
        updateConnectBarView();
        ConnectedDevice connectedDevice = Utils.getPairedMicrobit(this);
        if (connectedDevice.mPattern != null) {
            if (connectedDevice.mStatus) {
                if (debug) logi("onBtnClicked() :: IPCService.getInstance().bleDisconnect()");
                IPCService.getInstance().bleDisconnect();
            } else {
                if (debug) logi("onBtnClicked() :: IPCService.getInstance().bleConnect()");
                connectionInitiated = true;
                PopUp.show(MBApp.getContext(),
                        getString(R.string.init_connection),
                        "",
                        R.drawable.message_face, R.drawable.blue_btn,
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
    }
	public void onClick(final View v) {
		if (debug) logi("onBtnClicked() :: ");


        switch (v.getId()) {
            case R.id.addDevice:
            case R.id.addDeviceEmpty:
                {
                    /*
                    if (app.getEcho()!= null) {
                        logi("User action test for Click on Add microbit");
                        app.getEcho().userActionEvent("click", "AddMicrobitButton", null);
                    }
                    */
                    Intent intent = new Intent(this, PairingActivity.class);
                    startActivity(intent);
                }
                break;
            case R.id.startNewProject:
                {
                    Intent intent = new Intent(this, TouchDevActivity.class);
                    intent.putExtra(Constants.URL, getString(R.string.touchDevLiveURL));
                    startActivity(intent);
                }
                break;
// TODO - remove this case
//      case R.id.numOfProjectsHolder:
//                {
//                    Intent intent = new Intent(this, ProjectActivity.class);
//                    startActivity(intent);
//                }
//                break;
            case R.id.connectBtn:
                {
                    if (!BluetoothSwitch.getInstance().isBluetoothON()) {
                        startBluetooth();
                        return;
                    }
                    toggleConnection();
                }
                break;
        }//Switch Ends
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
		//TextView numOfProjects = (TextView) findViewById(R.id.numOfProjects);
	//	numOfProjects.setText(Integer.toString(Utils.findProgramsAndPopulate(null, null)));
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
        //updateProjectBarView();
	}
}
