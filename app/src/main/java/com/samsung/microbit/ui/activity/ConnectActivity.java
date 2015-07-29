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
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.PreviousDeviceList;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.adapter.ConnectedDeviceAdapter;
import com.samsung.microbit.ui.adapter.LEDAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ConnectActivity extends Activity implements View.OnClickListener {

	private static boolean DISABLE_DEVICE_LIST = false;

	ConnectedDevice[] prevDeviceArray;
	PreviousDeviceList prevDevList;

	private enum PAIRING_STATE {
		PAIRING_STATE_CONNECT_BUTTON,
		PAIRING_STATE_TIP,
		PAIRING_STATE_PATTERN_EMPTY,
		PAIRING_STATE_SEARCHING,
		PAIRING_STATE_ERROR,
		PAIRING_STATE_NEW_NAME
	}

	;

	private static PAIRING_STATE state = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;

	private static String newDeviceName;
	private static String newDeviceCode;

	// @formatter:off
    private static String deviceCodeArray[] = {
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
	RelativeLayout bottomConnectButton;
	RelativeLayout prevDeviceView;

	List<ConnectedDevice> connectedDeviceList = new ArrayList<ConnectedDevice>();
	ConnectedDeviceAdapter connectedDeviceAdapter;
	private ListView lvConnectedDevice;

	private Handler mHandler;

	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 15000;
	private ProgressDialog pairingProgressDialog;
	private Boolean isBLuetoothEnabled = false;

	final private int REQUEST_BT_ENABLE = 1;

	/*
	 * TODO : HACK 20150729
	 * A bit of a hack to make sure the scan finishes properly.  Needs top be done properly
	 * =================================================================
	 */
	private static ConnectActivity instance;
	private static BluetoothAdapter mBluetoothAdapter = null;
	private static volatile boolean mScanning = false;
	//private Runnable scanFailedCallback;

	/*
	 * =================================================================
	 */

	/* *************************************************
	 * TODO setup to Handle BLE Notiifications
	 */
	static IntentFilter broadcastIntentFilter;

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			handleBLENotification(context, intent);
			int v = intent.getIntExtra(IPCMessageManager.BUNDLE_ERROR_CODE, 0);
			if (v != 0) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						PopUp.show(MBApp.getContext(),
							MBApp.getContext().getString(R.string.micro_bit_reset_msg),
							"",
							0, 0,
							PopUp.TYPE_ALERT, null, null);
					}
				});
			}
		}
	};

	private void handleBLENotification(Context context, Intent intent) {

		logi("handleBLENotification()");
		ConnectedDevice changedDev = Utils.getPairedMicrobit(this);

		if (prevDevList == null) {
			prevDevList = PreviousDeviceList.getInstance(this);
		}
		prevDeviceArray = prevDevList.loadPrevMicrobits();

		if (changedDev.mPattern != null && changedDev.mPattern.equals(prevDeviceArray[0].mPattern)) {
			prevDeviceArray[0].mStatus = changedDev.mStatus;
			prevDevList.changeMicrobitState(0, prevDeviceArray[0], prevDeviceArray[0].mStatus, true);
			populateConnectedDeviceList(true);

		}

		PopUp.hide();
	}

	// *************************************************

	// DEBUG
	protected boolean debug = true;
	protected String TAG = "ConnectActivity";

	protected void logi(String message) {
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
	}

	@Override
	public void onResume() {
		super.onResume();
		MBApp.setContext(this);
		populateConnectedDeviceList(false);
	}

	public ConnectActivity() {
		logi("ConnectActivity() ::");
		instance = this;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		logi("onCreate() ::");

		super.onCreate(savedInstanceState);

		MBApp.setContext(this);

		if (broadcastIntentFilter == null) {
			broadcastIntentFilter = new IntentFilter(IPCService.INTENT_BLE_NOTIFICATION);
			LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(broadcastReceiver, broadcastIntentFilter);
		}

		// ************************************************
		//Remove title barproject_list
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_connect);

		/*
	 	* TODO : Part of HACK 20150729
	 	* =================================================================
	 	*/

		if (mBluetoothAdapter == null) {
			final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			logi("onCreate() :: mBluetoothAdapter == null");
			mBluetoothAdapter = bluetoothManager.getAdapter();
			// Checks if Bluetooth is supported on the device.
			if (mBluetoothAdapter == null) {
				Toast.makeText(this.getApplicationContext(), R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
		}

		/*
		 * =================================================================
		 */

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);
		}

		mHandler = new Handler(Looper.getMainLooper());
		if (prevDevList == null) {
			prevDevList = PreviousDeviceList.getInstance(this);
			prevDeviceArray = prevDevList.loadPrevMicrobits();
		}
		//prevDeviceArray = new ConnectedDevice[PREVIOUS_DEVICES_MAX];
		lvConnectedDevice = (ListView) findViewById(R.id.connectedDeviceList);
		populateConnectedDeviceList(false);

		bottomConnectButton = (RelativeLayout) findViewById(R.id.bottomConnectButton);
		prevDeviceView = (RelativeLayout) findViewById(R.id.prevDeviceView);

		connectButtonView = (RelativeLayout) findViewById(R.id.connectButtonView);
		connectTipView = (RelativeLayout) findViewById(R.id.connectTipView);
		newDeviceView = (RelativeLayout) findViewById(R.id.newDeviceView);
		connectSearchView = (RelativeLayout) findViewById(R.id.connectSearchView);

		displayConnectScreen(state);
		findViewById(R.id.connectButton).setOnClickListener(this);
		findViewById(R.id.cancel_tip_button).setOnClickListener(this);
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
		gridview.setAdapter(new LEDAdapter(this, deviceCodeArray));
		gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
									int position, long id) {
				if (state != PAIRING_STATE.PAIRING_STATE_NEW_NAME) {

					if ((findViewById(R.id.ok_name_button).getVisibility() != View.VISIBLE)) {
						findViewById(R.id.ok_name_button).setVisibility(View.VISIBLE);
					}

					boolean isOn = toggleLED((ImageView) v, position);
					setCol(parent, position, isOn);
					//Toast.makeText(MBApp.getContext(), "LED Clicked: " + position, Toast.LENGTH_SHORT).show();

					if (!Arrays.asList(deviceCodeArray).contains("1")) {
						findViewById(R.id.ok_name_button).setVisibility(View.INVISIBLE);
					}
				}
				//TODO KEEP TRACK OF ALL LED STATUS AND TOGGLE COLOR

			}
        });

		if (!Arrays.asList(deviceCodeArray).contains("1")) {
			findViewById(R.id.ok_name_button).setVisibility(View.INVISIBLE);
		} else
			findViewById(R.id.ok_name_button).setVisibility(View.VISIBLE);
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
		//Toast.makeText(this, "Pattern :"+newDeviceCode, Toast.LENGTH_SHORT).show();
	}

	private void setCol(AdapterView<?> parent, int pos, boolean enabledlandscape) {
		int index = pos - 5;
		ImageView v;

		while (index >= 0) {
			v = (ImageView) parent.getChildAt(index);
			v.setBackground(getApplication().getResources().getDrawable(R.drawable.white_red_led_btn));
			v.setTag("0");
			deviceCodeArray[index] = "0";
			index -= 5;
		}
		index = pos + 5;
		while (index < 25) {
			v = (ImageView) parent.getChildAt(index);
			v.setBackground(getApplication().getResources().getDrawable(R.drawable.red_white_led_btn));
			v.setTag("1");
			index += 5;
		}

	}

	private boolean toggleLED(ImageView image, int pos) {
		boolean isOn;
		//Toast.makeText(this, "Pos :" +  pos, Toast.LENGTH_SHORT).show();
		if (image.getTag() != "1") {
			deviceCodeArray[pos] = "1";
			image.setBackground(getApplication().getResources().getDrawable(R.drawable.red_white_led_btn));
			image.setTag("1");
			isOn = true;
		} else {
			deviceCodeArray[pos] = "0";
			image.setBackground(getApplication().getResources().getDrawable(R.drawable.white_red_led_btn));
			image.setTag("0");
			isOn = false;
			// Update the code to consider the still ON LED below the toggled one
			if (pos < 20)
				deviceCodeArray[pos + 5] = "1";
		}
		return isOn;
	}

	private void populateConnectedDeviceList(boolean isupdate) {
		connectedDeviceList.clear();
		int numOfPreviousItems = 0;
		/* Get Previous connected devices */
		prevDeviceArray = prevDevList.loadPrevMicrobits();
		if (prevDevList != null)
			numOfPreviousItems = prevDevList.size();


		for (int i = 0; i < numOfPreviousItems; i++) {
			connectedDeviceList.add(prevDeviceArray[i]);
		}
		for (int i = numOfPreviousItems; i < 1; i++) {
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

	private void enablePortraitMode() {
		if (bottomConnectButton != null) {
			prevDeviceView.setVisibility(View.GONE);
		} else
			prevDeviceView.setVisibility(View.VISIBLE);
	}

	private void displayConnectScreen(PAIRING_STATE gotoState) {
		connectButtonView.setVisibility(View.GONE);
		connectTipView.setVisibility(View.GONE);
		newDeviceView.setVisibility(View.GONE);
		connectSearchView.setVisibility(View.GONE);

		Log.d("Microbit", "********** Connect: state from " + state + " to " +gotoState);
        state = gotoState;

		if (gotoState == PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON)
			DISABLE_DEVICE_LIST = false;
		else
			DISABLE_DEVICE_LIST = true;

		populateConnectedDeviceList(true);

		if (DISABLE_DEVICE_LIST)
			enablePortraitMode();

		switch (gotoState) {
			case PAIRING_STATE_CONNECT_BUTTON:
			case PAIRING_STATE_ERROR:
				connectButtonView.setVisibility(View.VISIBLE);
				lvConnectedDevice.setEnabled(true);
				Arrays.fill(deviceCodeArray, "0");
				findViewById(R.id.gridview).setEnabled(true);
                newDeviceName = "";
                newDeviceCode = "";
				break;

			case PAIRING_STATE_TIP:
				connectTipView.setVisibility(View.VISIBLE);
				findViewById(R.id.ok_connect_button).setOnClickListener(this);
				break;

			case PAIRING_STATE_PATTERN_EMPTY:
				findViewById(R.id.gridview).setEnabled(true);
				findViewById(R.id.connectedDeviceList).setClickable(true);
				newDeviceView.setVisibility(View.VISIBLE);
				findViewById(R.id.cancel_name_button).setVisibility(View.VISIBLE);
				findViewById(R.id.newDeviceTxt).setVisibility(View.VISIBLE);
				findViewById(R.id.nameNewEdit).setVisibility(View.GONE);
				findViewById(R.id.ok_name_button).setVisibility(View.GONE);
				displayLedGrid();
				break;

			case PAIRING_STATE_NEW_NAME:
				findViewById(R.id.gridview).setEnabled(false);
				findViewById(R.id.connectedDeviceList).setClickable(false);
				newDeviceView.setVisibility(View.VISIBLE);
				((EditText) findViewById(R.id.nameNewEdit)).setText(" ");
				EditText editText = (EditText) findViewById(R.id.nameNewEdit);
				editText.setText(newDeviceCode);
				editText.setVisibility(View.VISIBLE);
				editText.requestFocus();
				findViewById(R.id.ok_name_button).setVisibility(View.VISIBLE);
				findViewById(R.id.cancel_name_button).setVisibility(View.VISIBLE);
				displayLedGrid();
				break;

			case PAIRING_STATE_SEARCHING:
				connectSearchView.setVisibility(View.VISIBLE);
				break;
		}
	}

	public void onClick(final View v) {

		int pos;

		switch (v.getId()) {
			case R.id.connectButton:
				if (bottomConnectButton != null) {
					prevDeviceView.setVisibility(View.GONE);
				}
				if (connectButtonView != null) {
					displayConnectScreen(PAIRING_STATE.PAIRING_STATE_TIP);
				}
				break;

			case R.id.ok_connect_button:
				displayConnectScreen(PAIRING_STATE.PAIRING_STATE_PATTERN_EMPTY);
				break;

			case R.id.ok_name_button:
				if (state == PAIRING_STATE.PAIRING_STATE_PATTERN_EMPTY) {
					generateName();
					scanLeDevice(true);
					displayConnectScreen(PAIRING_STATE.PAIRING_STATE_SEARCHING);
					break;
				}
				EditText editText = (EditText) findViewById(R.id.nameNewEdit);
				String newname = editText.getText().toString().trim();
				if (newname.isEmpty()) {
					editText.setText("");
					editText.setError(getString(R.string.name_empty_error));
				} else {
					hideKeyboard();
					if (bottomConnectButton != null) {
						prevDeviceView.setVisibility(View.VISIBLE);
					}
					prevDeviceArray[0].mName = newname;
					prevDevList.changeMicrobitName(0, prevDeviceArray[0]);
					populateConnectedDeviceList(true);
					displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
				}

				break;

			case R.id.cancel_tip_button:
			case R.id.cancel_name_button:
				displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
				break;

			case R.id.cancel_search_button:
				scanLeDevice(false);
				displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
				break;

			case R.id.connectBtn:
				pos = (Integer) v.getTag();
				boolean toTurnON = false;
				boolean currentState = prevDeviceArray[pos].mStatus;
				if (!currentState) {
					PopUp.show(MBApp.getContext(),
						getString(R.string.init_connection),
						"",
						R.drawable.mbit, R.drawable.blue_btn,
						PopUp.TYPE_SPINNER,
						null, null);

					toTurnON = true;
				}

				prevDeviceArray[pos].mStatus = !currentState;
				prevDevList.changeMicrobitState(pos, prevDeviceArray[pos], toTurnON, false);
				populateConnectedDeviceList(true);
				if (debug) logi("onClick() :: connectBtn");
				break;

			case R.id.deleteBtn:
				pos = (Integer) v.getTag();
				handleDeleteMicrobit(pos);
				break;
			case R.id.backBtn:
				Arrays.fill(deviceCodeArray,"0");
                if(state == PAIRING_STATE.PAIRING_STATE_SEARCHING) {
                    scanLeDevice(false);
                }
                state = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
				finish();
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
					prevDevList.removeMicrobit(pos);
					populateConnectedDeviceList(true);
				}
			},//override click listener for ok button
			null);//pass null to use default listener

	}

	public void hideKeyboard() {
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
			InputMethodManager.HIDE_NOT_ALWAYS);
	}

	private volatile boolean deviceFound = false;

	private void handle_pairing_failed() {

		if (debug) logi("handle_pairing_failed() :: Start");

		// dummy code to test addition of MBits
		/*if(debug) {
			if (!newDeviceCode.equalsIgnoreCase("vuvuv")) {

                state = PAIRING_STATE.PAIRING_STATE_NEW_NAME;
                displayConnectScreen(state);
                ConnectedDevice newDev = new ConnectedDevice(null, newDeviceCode, false, "ab.cd.ef.gh.ij.56");
                prevDevList.addMicrobit(newDev, prevDevList.PREVIOUS_DEVICES_MAX);
				populateConnectedDeviceList(true);
                return;

            }
        }*/

		displayConnectScreen(PAIRING_STATE.PAIRING_STATE_ERROR);

		PopUp.show(this,
			getString(R.string.pairingErrorMessage), //message
			getString(R.string.pairingErrorTitle), //title
			R.drawable.error, //image icon res id
			R.drawable.red_btn,
			PopUp.TYPE_ALERT, //type of popup.
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (bottomConnectButton != null) {
						prevDeviceView.setVisibility(View.VISIBLE);
					}
					PopUp.hide();
					displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
				}
			},//override click listener for ok button
			null);//pass null to use default listener

	}

	private void handle_pairing_successful(final ConnectedDevice newDev) {

		if (debug) logi("handle_pairing_successful() :: Start");


		final Runnable task = new Runnable() {

			@Override
			public void run() {

				getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
				//= new ConnectedDevice(null, newDeviceCode, true, device.getAddress() );
				int oldId = prevDevList.checkDuplicateMicrobit(newDev);
				prevDevList.addMicrobit(newDev, oldId);
				populateConnectedDeviceList(true);

				if (debug) logi("mLeScanCallback.onLeScan() ::   Matching DEVICE FOUND, Pairing");
				if (debug) logi("handle_pairing_successful() :: sending intent to BLEService.class");

				displayConnectScreen(PAIRING_STATE.PAIRING_STATE_NEW_NAME);

			}
		};

		new Handler(Looper.getMainLooper()).post(task);
	}

	private void scanningFailed() {

		if (debug) logi("scanningFailed() :: scanning Failed to find a matching device");
		if (deviceFound) {
			return;
		}

		scanLeDevice(false);
		handle_pairing_failed();
	}

	/*
 	* TODO : Part of HACK 20150729
 	* =================================================================
 	*/
	private void scanLeDevice(final boolean enable) {

		if (debug) logi("scanLeDevice() :: enable = " + enable);
		if (mScanning && enable) {
			return;
		}

		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mScanning = true;
			mHandler.postDelayed(scanFailedCallback, SCAN_PERIOD);
			deviceFound = false;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mHandler.removeCallbacks(scanFailedCallback);
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}

	private static Runnable scanFailedCallback = new Runnable() {
		@Override
		public void run() {
			ConnectActivity.instance.scanFailedCallbackImpl();
		}
	};

	private void scanFailedCallbackImpl() {

		if (mScanning) {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			scanningFailed();
		}
	}

	// Device scan callback.
	private static BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			ConnectActivity.instance.onLeScan(device, rssi, scanRecord);
		}
	};

	/*
	 * =================================================================
	 */

	public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

		if (debug) logi("mLeScanCallback.onLeScan() :: Start");

		/*
	 	* TODO : Part of HACK 20150729
	 	* =================================================================
	 	*/
		if (!mScanning) {
			return;
		}
		/*
		 * =================================================================
		 */

		if (device == null) {
			return;
		}

		if ((newDeviceName.isEmpty()) || (device.getName() == null)) {
			if (debug) logi("mLeScanCallback.onLeScan() ::   Cannot Compare");
		} else {
			String s = device.getName().toLowerCase();
			if (newDeviceName.toLowerCase().equals(s)) {

				// if(debug) logi("mLeScanCallback.onLeScan() ::   deviceName == " + newDeviceName.toLowerCase());
				if (debug) logi("mLeScanCallback.onLeScan() ::   device.getName() == " + device.getName().toLowerCase());

				// Stop scanning as device is found.
				deviceFound = true;
				scanLeDevice(false);

				ConnectedDevice newDev = new ConnectedDevice(null, newDeviceCode.toUpperCase(), false, device.getAddress());
				handle_pairing_successful(newDev);
			} else {
				if (debug) logi("mLeScanCallback.onLeScan() ::   non-matching - deviceName == " + newDeviceName.toLowerCase());
				if (debug)
					logi("mLeScanCallback.onLeScan() ::   non-matching found - device.getName() == " + device.getName().toLowerCase());
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		connectButtonView.setVisibility(View.GONE);
		connectTipView.setVisibility(View.GONE);
		newDeviceView.setVisibility(View.GONE);
		connectSearchView.setVisibility(View.GONE);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_launcher, menu);
		return true;
	}
}
