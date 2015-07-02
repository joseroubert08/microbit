package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.adapter.ConnectedDeviceAdapter;
import com.samsung.microbit.ui.adapter.LEDAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class ConnectActivity extends Activity implements View.OnClickListener {
	private SharedPreferences preferences;
	private final String PREFERENCES_PREVDEV_PREFNAME = "PreviousDevices";
	private final String PREFERENCES_PREVDEV_KEY = "PreviousDevicesKey";
	private final int PREVIOUS_DEVICES_MAX = 3;
	private static boolean DISABLE_DEVICE_LIST = false;

	ConnectedDevice[] prevDeviceArray;
	ArrayList prevMicrobitList;

	private enum PAIRING_STATE {
		PAIRING_STATE_CONNECT_BUTTON,
		PAIRING_STATE_TIP,
		PAIRING_STATE_PATTERN_EMPTY,
		PAIRING_STATE_PATTERN_CHANGED,
		PAIRING_STATE_SEARCHING,
		PAIRING_STATE_ERROR,
		PAIRING_STATE_NEW_NAME
	}

	;

	private PAIRING_STATE state;

	private String newDeviceName;
	private String newDeviceCode;
	private String newDeviceDisplayName;

	// @formatter:off
    private String deviceCodeArray[] = {
            "0","0","0","0","0",
            "0","0","0","0","0",
            "0","0","0","0","0",
            "0","0","0","0","0",
            "0","0","0","0","0"};

    private String deviceNameMapArray[] = {
            "T","A","T","A","T",
            "P","E","P","E","P",
            "G","I","G","I","G",
            "V","O","V","O","V",
            "Z","U","Z","U","Z"};
    // @formatter:on


	RelativeLayout connectButtonView;
	RelativeLayout connectTipView;
	RelativeLayout newDeviceView;
	RelativeLayout connectSearchView;


	List<ConnectedDevice> connectedDeviceList = new ArrayList<ConnectedDevice>();
	ConnectedDeviceAdapter connectedDeviceAdapter;
	private ListView lvConnectedDevice;


	private Handler mHandler;
	private Runnable scanFailedCallback;
	private boolean mScanning;
	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 15000;
	private ProgressDialog pairingProgressDialog;
	private BluetoothAdapter mBluetoothAdapter = null;
	private Boolean isBLuetoothEnabled = false;
	final private int REQUEST_BT_ENABLE = 1;

	/* *************************************************
	 * TODO setup to Handle BLE Notiifications
	 */
	IntentFilter broadcastIntentFilter;
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			handleBLENotification(context, intent);
		}
	};

	private void handleBLENotification(Context context, Intent intent) {

		logi("handleBLENotification()");
		ConnectedDevice changedDev = Utils.getPairedMicrobit(this);

		if(prevMicrobitList == null )
			loadPrevMicrobits();

		if ((changedDev.mPattern != null) && (changedDev.mPattern.equals(prevDeviceArray[0].mPattern)))
		{
			prevDeviceArray[0].mStatus=changedDev.mStatus;
			prevMicrobitList.set(0, prevDeviceArray[0]);
			storeMicrobits(prevMicrobitList, true);

		}
	}

	// *************************************************

	// DEBUG
	protected boolean debug = true;
	protected String TAG = "ConnectActivity";

	protected void logi(String message) {
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getResources().getBoolean(R.bool.portrait_only)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}

		MBApp.setContext(this);

		/* *************************************************
		 * TODO setup to Handle BLE Notiification
		 */
		if (broadcastIntentFilter == null) {
			broadcastIntentFilter = new IntentFilter(IPCService.INTENT_BLE_NOTIFICATION);
			LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(broadcastReceiver, broadcastIntentFilter);
		}

		// ************************************************
		//Remove title barproject_list
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_connect);

		LinearLayout mainContentView = (LinearLayout) findViewById(R.id.mainContentView);
		mainContentView.getBackground().setAlpha(128);

		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Toast.makeText(this.getApplicationContext(), R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);
		}

		mHandler = new Handler(Looper.getMainLooper());

		prevDeviceArray = new ConnectedDevice[3];
		lvConnectedDevice = (ListView) findViewById(R.id.connectedDeviceList);
		populateConnectedDeviceList(false);

		connectButtonView = (RelativeLayout) findViewById(R.id.connectButtonView);
		connectTipView = (RelativeLayout) findViewById(R.id.connectTipView);
		newDeviceView = (RelativeLayout) findViewById(R.id.newDeviceView);
		connectSearchView = (RelativeLayout) findViewById(R.id.connectSearchView);

		displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
		findViewById(R.id.connectButton).setOnClickListener(this);
		findViewById(R.id.ok_pattern_button).setOnClickListener(this);
		findViewById(R.id.ok_name_button).setOnClickListener(this);
		findViewById(R.id.cancel_name_button).setOnClickListener(this);
		findViewById(R.id.cancel_search_button).setOnClickListener(this);

		//Animation
		WebView animation = (WebView) findViewById(R.id.animationwebView);
		animation.setBackgroundColor(Color.TRANSPARENT);
		animation.loadUrl("file:///android_asset/htmls/animation.html");


	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.d("Microbit", "onActivityResult");

		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_BT_ENABLE && resultCode == Activity.RESULT_CANCELED) {
			Toast.makeText(this, "You must enable Bluetooth to continue", Toast.LENGTH_LONG).show();
		} else {
			isBLuetoothEnabled = true;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void displayLedGrid() {
		GridView gridview = (GridView) findViewById(R.id.gridview);
		gridview.setAdapter(new LEDAdapter(this));
		gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
									int position, long id) {
				if (state != PAIRING_STATE.PAIRING_STATE_NEW_NAME) {

					if ((findViewById(R.id.ok_pattern_button).getVisibility() != View.VISIBLE)
							&& (findViewById(R.id.ok_name_button).getVisibility() != View.VISIBLE)) {
						findViewById(R.id.ok_pattern_button).setVisibility(View.VISIBLE);
						((TextView) findViewById(R.id.newDeviceTxt)).setText(R.string.new_devices);
					}
					toggleLED((ImageView) v, position);
					//Toast.makeText(MBApp.getContext(), "LED Clicked: " + position, Toast.LENGTH_SHORT).show();
				}
				//TODO KEEP TRACK OF ALL LED STATUS AND TOGGLE COLOR

			}
		});
	}

	private void generateName() {

		newDeviceName = "";
		newDeviceCode = "";
		//Columns
		for (int col = 0; col < 5; col++) {
			//Rows
			for (int row = 0; row < 5; row++) {
				if (deviceCodeArray[(col + (5 * row))] == "1") {
					newDeviceName += deviceNameMapArray[(col + (5 * row))];
					break;
				}
			}
		}
		newDeviceCode = newDeviceName;
		newDeviceName = "BBC microbit [" + newDeviceName + "]";
		// Toast.makeText(this, "Pattern :"+newDeviceCode, Toast.LENGTH_SHORT).show();
	}

	private void toggleLED(ImageView image, int pos) {
		//Toast.makeText(this, "Pos :" +  pos, Toast.LENGTH_SHORT).show();
		if (image.getTag() != "1") {
			deviceCodeArray[pos] = "1";
			image.setBackground(getApplication().getResources().getDrawable(R.drawable.red_white_led_btn));
			image.setTag("1");
		} else {
			deviceCodeArray[pos] = "0";
			image.setBackground(getApplication().getResources().getDrawable(R.drawable.white_red_led_btn));
			image.setTag("0");
		}
	}

	private void populateConnectedDeviceList(boolean isupdate) {
		connectedDeviceList.clear();
		int numOfPreviousItems = 0;
		/* Get Previous connected devices */
		prevMicrobitList = loadPrevMicrobits();
		if (prevMicrobitList != null)
			numOfPreviousItems = prevMicrobitList.size();


		for (int i = 0; i < numOfPreviousItems; i++) {
			connectedDeviceList.add(prevDeviceArray[i]);
		}
		for (int i = numOfPreviousItems; i < PREVIOUS_DEVICES_MAX; i++) {
			connectedDeviceList.add(new ConnectedDevice(null, null, false, null));
		}


		if (isupdate) {
			connectedDeviceAdapter.updateAdapter(connectedDeviceList);
			lvConnectedDevice.setAdapter(connectedDeviceAdapter);

		} else {
			connectedDeviceAdapter = new ConnectedDeviceAdapter(this, connectedDeviceList);
			lvConnectedDevice.setAdapter(connectedDeviceAdapter);
		}
	}

	public static boolean disableListView() {
		return DISABLE_DEVICE_LIST;
	}


	private void displayConnectScreen(PAIRING_STATE gotoState) {
		connectButtonView.setVisibility(View.GONE);
		connectTipView.setVisibility(View.GONE);
		newDeviceView.setVisibility(View.GONE);
		connectSearchView.setVisibility(View.GONE);

		if(gotoState == PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON)
			DISABLE_DEVICE_LIST = false;
		else
			DISABLE_DEVICE_LIST = true;

		populateConnectedDeviceList(true);

		switch (gotoState) {
			case PAIRING_STATE_CONNECT_BUTTON:
				connectButtonView.setVisibility(View.VISIBLE);
				lvConnectedDevice.setEnabled(true);
				Arrays.fill(deviceCodeArray, "0");
				findViewById(R.id.gridview).setEnabled(true);
				break;
			case PAIRING_STATE_TIP:
				connectTipView.setVisibility(View.VISIBLE);
				findViewById(R.id.ok_connect_button).setOnClickListener(this);
				break;
			case PAIRING_STATE_PATTERN_EMPTY:
				findViewById(R.id.gridview).setEnabled(true);
				findViewById(R.id.connectedDeviceList).setClickable(true);
				newDeviceView.setVisibility(View.VISIBLE);
				findViewById(R.id.ok_pattern_button).setVisibility(View.GONE);
				findViewById(R.id.ok_name_button).setVisibility(View.GONE);
				findViewById(R.id.newDeviceTxt).setVisibility(View.VISIBLE);
				findViewById(R.id.nameNewTxt).setVisibility(View.GONE);
				findViewById(R.id.nameNewEdit).setVisibility(View.GONE);
				findViewById(R.id.ok_name_button).setVisibility(View.GONE);
				findViewById(R.id.cancel_name_button).setVisibility(View.GONE);
				break;
			case PAIRING_STATE_PATTERN_CHANGED:
				//newDeviceView.setVisibility(View.VISIBLE);
				findViewById(R.id.ok_pattern_button).setVisibility(View.VISIBLE);
				break;
			case PAIRING_STATE_NEW_NAME:
				findViewById(R.id.gridview).setEnabled(false);
				findViewById(R.id.connectedDeviceList).setClickable(false);
				newDeviceView.setVisibility(View.VISIBLE);
				findViewById(R.id.ok_pattern_button).setVisibility(View.GONE);
				((EditText) findViewById(R.id.nameNewEdit)).setText(" ");
				((TextView) findViewById(R.id.nameNewTxt)).setText(getString(R.string.name_device) + " " + newDeviceCode);
				findViewById(R.id.nameNewTxt).setVisibility(View.VISIBLE);
				EditText editText = (EditText) findViewById(R.id.nameNewEdit);
				editText.setVisibility(View.VISIBLE);
				editText.requestFocus();
				findViewById(R.id.ok_name_button).setVisibility(View.VISIBLE);
				findViewById(R.id.cancel_name_button).setVisibility(View.VISIBLE);
				break;
			case PAIRING_STATE_SEARCHING:
				connectSearchView.setVisibility(View.VISIBLE);
				break;
			case PAIRING_STATE_ERROR:
				connectSearchView.setVisibility(View.GONE);
				newDeviceView.setVisibility(View.VISIBLE);
				break;
		}
		;
	}

	public void onClick(final View v) {

		int pos;

		switch (v.getId()) {
			case R.id.connectButton:
				state = PAIRING_STATE.PAIRING_STATE_TIP;
				displayConnectScreen(PAIRING_STATE.PAIRING_STATE_TIP);
				break;
			case R.id.ok_connect_button:
				state = PAIRING_STATE.PAIRING_STATE_PATTERN_EMPTY;
				displayConnectScreen(PAIRING_STATE.PAIRING_STATE_PATTERN_EMPTY);
				displayLedGrid();
				break;
			case R.id.ok_pattern_button:
				state = PAIRING_STATE.PAIRING_STATE_SEARCHING;
				generateName();
				if (newDeviceCode.isEmpty()) {
					findViewById(R.id.ok_pattern_button).setVisibility(View.GONE);
					Toast.makeText(MBApp.getContext(), "Enter Valid Pattern", Toast.LENGTH_SHORT).show();
					return;
				}
				scanLeDevice(true);
				displayConnectScreen(state);

				break;
			case R.id.ok_name_button:
				EditText editText = (EditText) findViewById(R.id.nameNewEdit);
				String newname = editText.getText().toString().trim();
				if (newname.isEmpty()) {
					editText.setText("");
					editText.setError(getString(R.string.name_empty_error));
				}
				else {
					hideKeyboard();
					prevDeviceArray[0].mName = newname;
					changeMicrobitName(0, prevDeviceArray[0]);
					state = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
					displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
				}

				break;
			case R.id.cancel_name_button:
				displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
				state = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
				break;
			case R.id.cancel_search_button:
				scanLeDevice(false);
				displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
				state = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
				break;

			case R.id.connectBtn:
				pos = (Integer) v.getTag();
				boolean toTurnON = false;
				boolean currentState = prevDeviceArray[pos].mStatus;
				if (!currentState)
					toTurnON = true;

				prevDeviceArray[pos].mStatus = !currentState;
				changeMicrobitState(pos, prevDeviceArray[pos], toTurnON);
				if(debug) logi("onClick() :: connectBtn");
				break;

			case R.id.deleteBtn:
				pos = (Integer) v.getTag();
				handleDeleteMicrobit(pos);
				break;
			default:
				Toast.makeText(MBApp.getContext(), "Default Item Clicked: " + v.getId(), Toast.LENGTH_SHORT).show();
				break;

		}
	}

	private void handleDeleteMicrobit(final int pos) {
		PopUp.show(this,
				getString(R.string.deleteMicrobitMessage), //message
				getString(R.string.deleteMicrobitTitle), //title
				R.drawable.delete, R.drawable.red_btn,
				PopUp.TYPE_CHOICE, //type of popup.
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						PopUp.hide();
						removeMicrobit(pos);
					}
				},//override click listener for ok button
				null);//pass null to use default listener

	}

	public void hideKeyboard() {
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);
	}

	public void onHomeBtnClicked(View v) {
		finish();
	}

	private volatile boolean deviceFound = false;

	private void handle_pairing_failed() {

		if(debug) logi("handle_pairing_failed() :: Start");

		// dummy code to test addition of MBits
		/* if(debug) {
            if (!newDeviceCode.equalsIgnoreCase("vuvuv")) {

                state = PAIRING_STATE.PAIRING_STATE_NEW_NAME;
                displayConnectScreen(state);
                ConnectedDevice newDev = new ConnectedDevice(null, newDeviceCode, true, "ab.cd.ef.gh.ij.56");
                addMicrobit(newDev,3);
                return;

            }
        } */

		displayConnectScreen(PAIRING_STATE.PAIRING_STATE_ERROR);

		PopUp.show(this,
				getString(R.string.pairingErrorMessage), //message
				getString(R.string.pairingErrorTitle), //title
				R.drawable.exclamation, //image icon res id
				0,
				PopUp.TYPE_ALERT, //type of popup.
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						PopUp.hide();
						state = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
						displayConnectScreen(state);
					}
				},//override click listener for ok button
				null);//pass null to use default listener

	}

	private void handle_pairing_successful(final ConnectedDevice newDev) {

		if(debug) logi("handle_pairing_successful() :: Start");


		final Runnable task = new Runnable() {

			@Override
			public void run() {

				getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
				//= new ConnectedDevice(null, newDeviceCode, true, device.getAddress() );
				int oldId = checkDuplicateMicrobit(newDev);
				addMicrobit(newDev, oldId);

				if(debug) logi("mLeScanCallback.onLeScan() ::   Matching DEVICE FOUND, Pairing");
				if(debug) logi("handle_pairing_successful() :: sending intent to BLEService.class");

				state = PAIRING_STATE.PAIRING_STATE_NEW_NAME;
				displayConnectScreen(state);

			}
		};
		new Handler(Looper.getMainLooper()).post(task);
	}



	void updateGlobalPairedDevice() {

		ConnectedDevice currentDevice = Utils.getPairedMicrobit(MBApp.getContext());

		if (prevDeviceArray[0] != null) {

			if((currentDevice.mPattern != null) && currentDevice.mPattern.equals(prevDeviceArray[0].mPattern))
			{
				// Update existing
				if(currentDevice.mStatus != prevDeviceArray[0].mStatus)
				{
					// Status has changed
					if(currentDevice.mStatus)
						disconnectBluetooth();
					else
						connectBluetoothDevice();
				}
				Utils.setPairedMicrobit(MBApp.getContext(), prevDeviceArray[0]);
			} else
			{
				// device changed, disconnect previous and connect new
				disconnectBluetooth();
				Utils.setPairedMicrobit(MBApp.getContext(), prevDeviceArray[0]);
				connectBluetoothDevice();
			}
		} else {
			//Disconnect existing Gatt connection
			if(currentDevice.mPattern != null)
				disconnectBluetooth();

			// Remove existing Microbit
			Utils.setPairedMicrobit(MBApp.getContext(), null);
		}
	}

	void connectBluetoothDevice() {
		IPCService.getInstance().bleConnect();
	}

	void disconnectBluetooth() {

		IPCService.getInstance().bleDisconnect();

	}
	private void scanningFailed() {

		if(debug) logi("scanningFailed() :: scanning Failed to find a matching device");
		if (deviceFound) {
			return;
		}
		scanLeDevice(false);
		handle_pairing_failed();
	}

	private void scanLeDevice(final boolean enable) {

		if(debug) logi("scanLeDevice() :: enable = " + enable);
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mScanning = true;
			scanFailedCallback = new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					scanningFailed();
				}
			};

			mHandler.postDelayed(scanFailedCallback, SCAN_PERIOD);
			deviceFound = false;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mHandler.removeCallbacks(scanFailedCallback);
			scanFailedCallback = null;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);

		}
	}

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

			if(debug) logi("mLeScanCallback.onLeScan() :: Start");
			if (device == null) {
				return;
			}

			if ((newDeviceName.isEmpty()) || (device.getName() == null)) {
				if(debug) logi("mLeScanCallback.onLeScan() ::   Cannot Compare");
			} else {
				String s = device.getName().toLowerCase();
				if (newDeviceName.toLowerCase().equals(s)
					|| (s.contains(newDeviceCode.toLowerCase()) && s.contains("microbit"))) {

					// if(debug) logi("mLeScanCallback.onLeScan() ::   deviceName == " + newDeviceName.toLowerCase());
					if(debug) logi("mLeScanCallback.onLeScan() ::   device.getName() == " + device.getName().toLowerCase());

					// Stop scanning as device is found.
					deviceFound = true;
					scanLeDevice(false);

					ConnectedDevice newDev = new ConnectedDevice(null, newDeviceCode.toUpperCase(), true, device.getAddress());
					handle_pairing_successful(newDev);
				} else {
					if(debug) logi("mLeScanCallback.onLeScan() ::   non-matching - deviceName == " + newDeviceName.toLowerCase());
					if(debug) logi("mLeScanCallback.onLeScan() ::   non-matching found - device.getName() == " + device.getName().toLowerCase());
				}
			}
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_launcher, menu);
		return true;
	}


	/* Microbit list management */
	private void storeMicrobits(ArrayList prevDevList, boolean fromBroadcast) {
		// used for store arrayList in json format
		SharedPreferences settings;
		SharedPreferences.Editor editor;
		settings = getSharedPreferences(PREFERENCES_PREVDEV_PREFNAME, MODE_PRIVATE);
		editor = settings.edit();
		Gson gson = new Gson();
		//ConnectedDevice[] prevMicrobitItems = new ConnectedDevice[prevDevList.size()];
		prevDevList.toArray(prevDeviceArray);
		String jsonPrevDevices = gson.toJson(prevDeviceArray, ConnectedDevice[].class);
		editor.putString(PREFERENCES_PREVDEV_KEY, jsonPrevDevices);
		editor.commit();
		if(!fromBroadcast)
		{
			updateGlobalPairedDevice();
		}
		//connectedDeviceAdapter.notifyDataSetChanged();
		populateConnectedDeviceList(true);
	}

	private ArrayList loadPrevMicrobits() {
		// used for retrieving arraylist from json formatted string
		SharedPreferences settings;
		List prevMicrobitTemp;
		settings = getSharedPreferences(PREFERENCES_PREVDEV_PREFNAME, MODE_PRIVATE);
		if (settings.contains(PREFERENCES_PREVDEV_KEY)) {
			String prevDevicesStr = settings.getString(PREFERENCES_PREVDEV_KEY, null);
			if (!prevDevicesStr.equals(null)) {
				Gson gson = new Gson();
				prevDeviceArray = gson.fromJson(prevDevicesStr, ConnectedDevice[].class);
				prevMicrobitTemp = Arrays.asList(prevDeviceArray);
				prevMicrobitList = new ArrayList(prevMicrobitTemp);
				return (ArrayList) prevMicrobitList;
			}

		}

		ConnectedDevice current = Utils.getPairedMicrobit(this);
		if( (current.mPattern != null) && current.mPattern.equals(prevDeviceArray[0].mPattern))
		{
			if(current.mStatus != prevDeviceArray[0].mStatus)
				prevDeviceArray[0].mStatus = current.mStatus;
		}
		return null;

	}

	public int checkDuplicateMicrobit(ConnectedDevice newMicrobit) {
		int duplicateIndex = PREVIOUS_DEVICES_MAX;
		if (prevMicrobitList == null)
			return duplicateIndex;

		for (int i = 0; i < prevMicrobitList.size(); i++) {
			if ((prevDeviceArray[i] != null) && (prevDeviceArray[i].mPattern.equals(newMicrobit.mPattern))) {
				return i;
			}
		}
		return duplicateIndex;
	}

	public void addMicrobit(ConnectedDevice newMicrobit, int oldId) {
		if (prevMicrobitList == null)
			prevMicrobitList = new ArrayList(PREVIOUS_DEVICES_MAX);

		// This device already exists in the list, so remove it and add as new
		if (oldId != PREVIOUS_DEVICES_MAX) {
			if (prevDeviceArray[oldId].mStatus) {
				// Do nothing as this device is already in the list and is currently active
				return;
			} else {
				// Remove from list and add again
				prevMicrobitList.remove(oldId);
			}
		}

		// If there are already 3 devices, delete last one
		if (prevMicrobitList.size() == PREVIOUS_DEVICES_MAX)
			prevMicrobitList.remove(PREVIOUS_DEVICES_MAX - 1);

		// new devices are added to top of the list
		prevMicrobitList.add(0, newMicrobit);

		String dbgDevices = "A ";
		int ind = 0;
		for (Iterator<ConnectedDevice> it = prevMicrobitList.iterator(); it.hasNext(); ) {
			ConnectedDevice st = it.next();
			if ((st != null) && (ind != 0)) {
				st.mStatus = false; // turn off the previously connected devive
				//disconnectBluetooth();
			}
			prevDeviceArray[ind++] = st;
		}

		storeMicrobits(prevMicrobitList, false);
	}

	public void changeMicrobitName(int index, ConnectedDevice modMicrobit) {
		prevMicrobitList.remove(index);
		prevMicrobitList.add(index, modMicrobit);
		storeMicrobits(prevMicrobitList, false);
	}

	public void changeMicrobitState(int index, ConnectedDevice modMicrobit, boolean isTurnedOn) {
		prevMicrobitList.remove(index);
		if (isTurnedOn)
			prevMicrobitList.add(0, modMicrobit);  // Active should be first item
		else
			prevMicrobitList.add(index, modMicrobit);

		String dbgDevices = "C ";
		int ind = 0;
		for (Iterator<ConnectedDevice> it = prevMicrobitList.iterator(); it.hasNext(); ) {
			ConnectedDevice st = it.next();
			if (isTurnedOn && (ind != index) && (prevDeviceArray[ind] != null)) {
				if(prevDeviceArray[ind].mStatus) {
					//disconnectBluetooth();
					prevDeviceArray[ind].mStatus = false; // toggle previously connected BT OFF
				}
			}
			ind++;
		}

		storeMicrobits(prevMicrobitList, false);
	}

	public void removeMicrobit(int index) {
		if (prevMicrobitList != null) {
			prevMicrobitList.remove(index);
			// prevMicrobitList.trimToSize();
			storeMicrobits(prevMicrobitList, false);
		}
	}

}
