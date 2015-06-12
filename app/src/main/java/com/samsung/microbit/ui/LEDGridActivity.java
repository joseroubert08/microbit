package com.samsung.microbit.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.service.DfuService;
import com.samsung.microbit.ui.fragment.FlashSectionFragment;

import java.util.ArrayList;

public class LEDGridActivity extends Activity implements View.OnClickListener {

	private static final int NUM_LED_ELEMENTS = 25;
	private int state = 0;
	private static final int STATE_AWAITING_USER_INITIATION = 0;
	private static final int STATE_LED_GRID_DISPLAYED = 1;
	private static final int STATE_READY_TO_SCAN = 2;
	private static final int STATE_SCANNING = 3;
	private static final int STATE_DEVICE_DISCOVERED = 4;
	private static final int STATE_DEVICE_PAIRED = 5;
	private static final int STATE_PHASE1_COMPLETE = 6;
	private ImageAdapter imgAdapter;
	private GridView devicesGridview = null;
	private Button devicesButton = null;
	private TextView pairingStatus = null;
	private TextView pairingMessage = null;
	final ArrayList<String> list = new ArrayList<String>();

	private String deviceName = "";
	private String deviceCode = "";

	private static SharedPreferences preferences;
	private static final String PREFERENCES_NAME_KEY = "PairedDeviceName";
	private static final String PREFERENCES_ADDRESS_KEY = "PairedDeviceAddress";
	private Handler mHandler;
	private Runnable scanFailedCallback;
	private boolean mScanning;
	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 15000;
	private ProgressDialog pairingProgressDialog;
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothDevice mSelectedDevice;
	private DFUResultReceiver dfuResultReceiver;
	private String file_to_be_downloaded;
	private Boolean use_existing_device;
	private String existing_device_name;
	private String existing_device_address;

	protected String TAG = "LEDGridActivity";
	protected boolean debug = true;

	protected void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}


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

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		MBApp.setContext(this);

		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_ledgrid);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, list);

		state = STATE_LED_GRID_DISPLAYED;

		//programList.setAdapter(adapter);
		devicesButton = (Button) findViewById(R.id.devicesButton);
		devicesButton.setText(R.string.devices_find_microbit);
		devicesButton.setOnClickListener(this);

		pairingStatus = (TextView) findViewById(R.id.connectingStatus);
		pairingMessage = (TextView) findViewById(R.id.connectingMessage);
		file_to_be_downloaded = getIntent().getStringExtra("download_file");
		use_existing_device = getIntent().getBooleanExtra("use_existing_device", false);
		existing_device_name = getIntent().getStringExtra("existing_device_name");
		existing_device_address = getIntent().getStringExtra("existing_device_address");
		pairingMessage.setText("Flashing program [" + file_to_be_downloaded + "]");

		mHandler = new Handler();

		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		getActionBar().setTitle("Find microbit");
		if (use_existing_device == true) {
			displayLEDGridView();
			startFlashingExisting();
		} else {
			displayLEDGridView();
		}
	}


	@Override
	public void onClick(View v) {

		switch (state) {

			case STATE_LED_GRID_DISPLAYED:
			case STATE_DEVICE_PAIRED:
			case STATE_READY_TO_SCAN: {
				generateName();
				devicesButton.setText(R.string.devices_scanning);
				pairingStatus.setText("Searching for device");
				pairingMessage.setText("Looking for a micro:bit matching the pattern");
				devicesButton.setEnabled(false);
				state = STATE_SCANNING;
				scanLeDevice(true);
				break;
			}

           /* case STATE_READY_TO_SCAN: {
				//Call this again in case the details changed
                generateName();

                //STATE change occurs in scanLeDevice function

                devicesButton.setText(R.string.devices_scanning);
                pairingStatus.setText("Searching for device");
                pairingMessage.setText("Looking for a micro:bit matching the pattern");
                devicesButton.setEnabled(false);

                state = STATE_SCANNING;

                break;
            }*/

			case STATE_PHASE1_COMPLETE:

				displayLEDGridViewConnected();
				devicesButton.setEnabled(false);
				pairingStatus.setText("micro:bit found");
				pairingMessage.setText("Downloading file " + file_to_be_downloaded);
				final Intent service = new Intent(LEDGridActivity.this, DfuService.class);
				if (use_existing_device == true) {
					service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, existing_device_address);
					service.putExtra(DfuService.EXTRA_DEVICE_NAME, existing_device_name);

				} else {
					service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, mSelectedDevice.getAddress());
					service.putExtra(DfuService.EXTRA_DEVICE_NAME, mSelectedDevice.getName());
				}
				service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, DfuService.MIME_TYPE_OCTET_STREAM);
				service.putExtra(DfuService.EXTRA_FILE_PATH, FlashSectionFragment.BINARY_FILE_NAME); // a path or URI must be provided.
				service.putExtra(DfuService.EXTRA_KEEP_BOND, false);
				service.putExtra("com.samsung.resultReceiver", resultReceiver);
				service.putExtra("com.samsung.runonly.phase", 2);

				startService(service);
				break;
		}
	}

	private void generateName() {

		deviceName = "";
		deviceCode = "";
		//Columns
		for (int col = 0; col < 5; col++) {

			//Rows
			for (int row = 0; row < 5; row++) {
				if (deviceCodeArray[(col + (5 * row))] == "1") {

					deviceName += deviceNameMapArray[(col + (5 * row))];
					break;
				}
			}
		}

		deviceCode = deviceName;
		deviceName = "BBC microbit [" + deviceName + "]";
	}

	private void toggleLED(ImageView image, int pos) {
		if (image.getTag() != "1") {
			image.setImageResource(R.drawable.ledon);
			image.setTag("1");

			deviceCodeArray[pos] = "1";
		} else {
			image.setImageResource(R.drawable.ledoff);
			image.setTag("0");

			deviceCodeArray[pos] = "0";
		}
	}

	private void displayLEDGridView() {
		logi("displayLEDGridView() :: Start");

		imgAdapter = new ImageAdapter(this);
		preferences = MBApp.getContext().getSharedPreferences("Microbit_PairedDevices", Context.MODE_PRIVATE);

		devicesGridview = (GridView) findViewById(R.id.devicesLEDGridView);
		devicesGridview.setAdapter(imgAdapter);

		devicesGridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				ImageView imageview = (ImageView) v;
				toggleLED(imageview, position);
				logi("displayLEDGridView() :: position == " + position + " id == " + id);
			}
		});
	}

	private void displayLEDGridViewConnected() {
		logi("displayLEDGridViewConnected() :: Start");

		devicesGridview = (GridView) findViewById(R.id.devicesLEDGridView);
		imgAdapter.displayTick();
		devicesGridview.setAdapter(imgAdapter);
	}

	private void display_spinner_dialog() {
		pairingProgressDialog = new ProgressDialog(MBApp.getContext());
		pairingProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pairingProgressDialog.setTitle("Searching");
		pairingProgressDialog.setMessage("Searching for BBC Microbit");
		pairingProgressDialog.setCancelable(false);
		pairingProgressDialog.show();
	}

	private void alertView(final String message, int title_id) {

		final AlertDialog.Builder dialog = new AlertDialog.Builder(this);

		dialog.setIcon(R.drawable.ic_action_about);
		dialog.setTitle("Flashing")
			.setMessage(message)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface, int i) {
					finish();
				}
			}).show();
	}

	//BLUETOOTH CODE
	private void handle_pairing_failed() {

		logi("handle_pairing_failed() :: Start");
		SharedPreferences.Editor editor = preferences.edit();
		String pairedDeviceName = preferences.getString(PREFERENCES_NAME_KEY, "None");
		logi("handle_pairing_failed() :: PairedDevice" + pairedDeviceName);
		if (!pairedDeviceName.equals("None")) {
			// Edit the saved preferences
			editor.remove(PREFERENCES_NAME_KEY);
			editor.commit();
		}

		final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setIcon(R.drawable.ic_action_about);

		dialog.setTitle("Searching")
			.setMessage("Can not find the BBC Micro:bit")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface, int i) {
				}
			}).show();

		devicesButton.setText(R.string.devices_find_microbit);
		devicesButton.setEnabled(true);
		pairingStatus.setText("Not yet linked");
		pairingMessage.setText("Enter the pattern on your micro:bit");
	}


	private void handle_pairing_successful() {

		logi("handle_pairing_successful() :: Start");


		mHandler.post(new Runnable() {
			@Override
			public void run() {

				String pairedDeviceName = preferences.getString(PREFERENCES_NAME_KEY, "None");

				// Edit the saved preferences
				if ((!pairedDeviceName.equals("None") && !pairedDeviceName.equals(deviceName))
					|| (pairedDeviceName.equals("None"))) {

					// TODO: Should we disconnect by calling removeBond from old BLE device??
					SharedPreferences.Editor editor = preferences.edit();
					editor.putString(PREFERENCES_NAME_KEY, deviceName);
					editor.putString(PREFERENCES_ADDRESS_KEY, mSelectedDevice.getAddress());
					editor.commit();
				}

				pairingStatus.setText("micro:bit found");
				pairingMessage.setText("Starting reprogramming");

				devicesButton.setText("Connected to " + deviceName);
				devicesButton.setEnabled(false);
				startFlashing();
			}
		});


		/*
		final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setIcon(R.drawable.ic_action_about);
		dialog.setTitle("micro:bit found")
			.setMessage(getString(R.string.pairing_success_message_1))
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface, int i) {
					startFlashing();
				}
			}).show();
		*/
	}

	private volatile boolean deviceFound = false;

	private void scanningFailed() {

		logi("scanningFailed() :: scanning Failed to find a matching device");
		if (deviceFound) {
			return;
		}

		scanLeDevice(false);
		devicesButton = (Button) findViewById(R.id.devicesButton);
		if (state != STATE_DEVICE_PAIRED) {
			state = STATE_READY_TO_SCAN;

			// Close the Scanning Dialog
			handle_pairing_failed();
		}
	}

	private void scanLeDevice(final boolean enable) {

		logi("scanLeDevice() :: enable = " + enable);
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

			display_spinner_dialog();
			mHandler.postDelayed(scanFailedCallback, SCAN_PERIOD);
			deviceFound = false;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mHandler.removeCallbacks(scanFailedCallback);
			scanFailedCallback = null;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			pairingProgressDialog.dismiss();
		}
	}

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

			logi("mLeScanCallback.onLeScan() :: Start");
			if (device == null) {
				return;
			}

			if ((deviceName.isEmpty()) || (device.getName() == null)) {
				logi("mLeScanCallback.onLeScan() ::   Cannot Compare");
			} else {
				String s = device.getName().toLowerCase();
				if (deviceName.toLowerCase().equals(s)) {
					//  || (s.contains(deviceCode) && s.contains("microbit"))

					logi("mLeScanCallback.onLeScan() ::   deviceName == " + deviceName.toLowerCase());
					logi("mLeScanCallback.onLeScan() ::   device.getName() == " + device.getName().toLowerCase());

					//UPDATE THE UI TO INDICATE PAIRED DEVICE (add string to
					state = STATE_DEVICE_DISCOVERED;

					// Stop scanning as device is found.
					deviceFound = true;
					scanLeDevice(false);

					//DO BLUETOOTHY THINGS HERE!!!!
					logi("mLeScanCallback.onLeScan() ::   Matching DEVICE FOUND, Pairing");
					int bondState = device.getBondState();
					mSelectedDevice = device;
					handle_pairing_successful();
				} else {
					logi("mLeScanCallback.onLeScan() ::   non-matching - deviceName == " + deviceName.toLowerCase());
					logi("mLeScanCallback.onLeScan() ::   non-matching found - device.getName() == " + device.getName().toLowerCase());
				}
			}
		}
	};

	/* This function is used to flash program into the previously linked micro:bit
	 */
	protected void startFlashingExisting() {

		logi("startFlashingExisting() ::   Start");
		pairingStatus.setText("micro:bit found");
		pairingMessage.setText("Starting reprogramming");

		devicesButton.setText("Connected to " + existing_device_name);
		devicesButton.setEnabled(false);

		final Intent service = new Intent(this, DfuService.class);
		service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, existing_device_address);
		service.putExtra(DfuService.EXTRA_DEVICE_NAME, existing_device_name);
		service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, DfuService.MIME_TYPE_OCTET_STREAM);
		//service.putExtra(DfuService.EXTRA_FILE_TYPE, mFileType);

		service.putExtra(DfuService.EXTRA_FILE_PATH, FlashSectionFragment.BINARY_FILE_NAME); // a path or URI must be provided.
		//service.putExtra(DfuService.EXTRA_FILE_URI, mFileStreamUri);
		// Init packet is required by Bootloader/DFU from SDK 7.0+ if HEX or BIN file is given above.
		// In case of a ZIP file, the init packet (a DAT file) must be included inside the ZIP file.
		//service.putExtra(DfuService.EXTRA_INIT_FILE_PATH, mInitFilePath);
		//service.putExtra(DfuService.EXTRA_INIT_FILE_URI, mInitFileStreamUri);
		service.putExtra(DfuService.EXTRA_KEEP_BOND, false);

		service.putExtra("com.samsung.resultReceiver", resultReceiver);
		service.putExtra("com.samsung.runonly.phase", 1);

		startService(service);
	}

	/* This function is used to flash program into the new micro:bit identified
	 * by the pattern entered on LED Grid display
	 */
	protected void startFlashing() {

		final Intent service = new Intent(this, DfuService.class);
		service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, mSelectedDevice.getAddress());
		service.putExtra(DfuService.EXTRA_DEVICE_NAME, mSelectedDevice.getName());
		service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, DfuService.MIME_TYPE_OCTET_STREAM);
		//service.putExtra(DfuService.EXTRA_FILE_TYPE, mFileType);
		service.putExtra(DfuService.EXTRA_FILE_PATH, FlashSectionFragment.BINARY_FILE_NAME); // a path or URI must be provided.
		//service.putExtra(DfuService.EXTRA_FILE_URI, mFileStreamUri);
		// Init packet is required by Bootloader/DFU from SDK 7.0+ if HEX or BIN file is given above.
		// In case of a ZIP file, the init packet (a DAT file) must be included inside the ZIP file.
		//service.putExtra(DfuService.EXTRA_INIT_FILE_PATH, mInitFilePath);
		//service.putExtra(DfuService.EXTRA_INIT_FILE_URI, mInitFileStreamUri);
		service.putExtra(DfuService.EXTRA_KEEP_BOND, false);

		service.putExtra("com.samsung.resultReceiver", resultReceiver);
		service.putExtra("com.samsung.runonly.phase", 1);

		startService(service);
	}

	class DFUResultReceiver extends BroadcastReceiver {
		private ProgressDialog flashSpinnerDialog;
		private boolean dialogInitDone = false;
		private boolean isCompleted = false;
		private boolean inInit = false;
		private boolean inProgress = false;

		@Override
		public void onReceive(Context context, Intent intent) {
			String message = "Broadcast intent detected " + intent.getAction();
			logi("DFUResultReceiver.onReceive :: " + message);
			if (intent.getAction() == DfuService.BROADCAST_PROGRESS) {
				if (!dialogInitDone) {
					flashSpinnerDialog = new ProgressDialog(MBApp.getContext());
					//  flashSpinnerDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					flashSpinnerDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					flashSpinnerDialog.setTitle("Flashing");
					flashSpinnerDialog.setMessage("Flashing Program to BBC Microbit");
					flashSpinnerDialog.setCancelable(false);
					dialogInitDone = true;
				}
				int state = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
				logi("DFUResultReceiver.onReceive :: state -- " + state);

				if (state < 0) {
					switch (state) {
						case DfuService.PROGRESS_COMPLETED:
							if (!isCompleted) {
								flashSpinnerDialog.dismiss();
								alertView(getString(R.string.flashing_success_message), R.string.flashing_success_title);
								// finish();
								LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
							}

							isCompleted = true;
							inInit = false;
							inProgress = false;
							break;
						case DfuService.PROGRESS_DISCONNECTING:
							if ((isCompleted == false) && (inProgress == false))// Disconnecting event because of error
							{
								String error_message = "Flashing Error Code - [" + intent.getIntExtra(DfuService.EXTRA_DATA, 0)
									+ "] Error Type - [" + intent.getIntExtra(DfuService.EXTRA_ERROR_TYPE, 0) + "]";

								logi(error_message);
								if (flashSpinnerDialog != null) {
									flashSpinnerDialog.dismiss();
								}

								alertView(error_message, R.string.flashing_failed_title);
								LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
							}

							break;
						case DfuService.PROGRESS_CONNECTING:
							if (!inInit) {
								flashSpinnerDialog.setMessage("Initialising the connection");
								flashSpinnerDialog.show();
							}

							inInit = true;
							isCompleted = false;
							break;
					}

				} else if ((state > 0) && (state < 100)) {
					if (!inProgress) {
						flashSpinnerDialog.dismiss();
						// flashSpinnerDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						flashSpinnerDialog.setMax(100);
						flashSpinnerDialog.setIndeterminate(false);
						flashSpinnerDialog.setMessage("Transmitting program to BBC Microbit");
						flashSpinnerDialog.show();
						inProgress = true;
					}

					flashSpinnerDialog.setProgress(state);
				}
			} else if (intent.getAction() == DfuService.BROADCAST_ERROR) {
				String error_message = broadcastGetErrorMessage(intent.getIntExtra(DfuService.EXTRA_DATA, 0));

				logi("DFUResultReceiver.onReceive() :: Flashing ERROR!!  Code - [" + intent.getIntExtra(DfuService.EXTRA_DATA, 0)
					+ "] Error Type - [" + intent.getIntExtra(DfuService.EXTRA_ERROR_TYPE, 0) + "]");

				if (flashSpinnerDialog != null)
					flashSpinnerDialog.dismiss();

				alertView(error_message, R.string.flashing_failed_title);
			}
		}

		private String broadcastGetErrorMessage(int errorCode) {
			String errorMessage;

			switch (errorCode) {
				case DfuService.ERROR_DEVICE_DISCONNECTED:
					errorMessage = "micro:bit disconnected";
					break;
				case DfuService.ERROR_FILE_NOT_FOUND:
					errorMessage = "File not found";
					break;
				/**
				 * Thrown if service was unable to open the file ({@link java.io.IOException} has been thrown).
				 */
				case DfuService.ERROR_FILE_ERROR:
					errorMessage = "Unable to open file";
					break;
				/**
				 * Thrown then input file is not a valid HEX or ZIP file.
				 */
				case DfuService.ERROR_FILE_INVALID:
					errorMessage = "File not a valid HEX";
					break;
				/**
				 * Thrown when {@link java.io.IOException} occurred when reading from file.
				 */
				case DfuService.ERROR_FILE_IO_EXCEPTION:
					errorMessage = "Unable to read file";
					break;
				/**
				 * Error thrown then {@code gatt.discoverServices();} returns false.
				 */
				case DfuService.ERROR_SERVICE_DISCOVERY_NOT_STARTED:
					errorMessage = "Bluetooth Discovery not started";
					break;
				/**
				 * Thrown when the service discovery has finished but the DFU service has not been found. The device does not support DFU of is not in DFU mode.
				 */
				case DfuService.ERROR_SERVICE_NOT_FOUND:
					errorMessage = "Dfu Service not found";
					break;
				/**
				 * Thrown when the required DFU service has been found but at least one of the DFU characteristics is absent.
				 */
				case DfuService.ERROR_CHARACTERISTICS_NOT_FOUND:
					errorMessage = "Dfu Characteristics not found";
					break;
				/**
				 * Thrown when unknown response has been obtained from the target. The DFU target must follow specification.
				 */
				case DfuService.ERROR_INVALID_RESPONSE:
					errorMessage = "Invalid response from micro:bit";
					break;

				/**
				 * Thrown when the the service does not support given type or mime-type.
				 */
				case DfuService.ERROR_FILE_TYPE_UNSUPPORTED:
					errorMessage = "Unsupported file type";
					break;

				/**
				 * Thrown when the the Bluetooth adapter is disabled.
				 */
				case DfuService.ERROR_BLUETOOTH_DISABLED:
					errorMessage = "Bluetooth Disabled";
					break;
				default:
					errorMessage = "Unknown Error";
					break;
			}

			return errorMessage;
		}
	}

	private void handle_phase1_complete() {
		devicesButton.setText("OK");
		devicesButton.setEnabled(true);
		pairingStatus.setTextColor(Color.GREEN);
		pairingStatus.setText("micro:bit found");
		pairingMessage.setText("Press button on micro:bit and then select OK");
		state = STATE_PHASE1_COMPLETE;

		IntentFilter filter = new IntentFilter(DfuService.BROADCAST_PROGRESS);
		IntentFilter filter1 = new IntentFilter(DfuService.BROADCAST_ERROR);
		dfuResultReceiver = new DFUResultReceiver();
		LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(dfuResultReceiver, filter);
		LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(dfuResultReceiver, filter1);
	}

	/**
	 *
	 */
	ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {

			int phase = resultCode & 0x0ffff;

			if ((phase & 0x01) != 0) {
				if ((phase & 0x0ff00) == 0) {
					logi("resultReceiver.onReceiveResult() :: Phase 1 complete recieved ");
					handle_phase1_complete();

				} else {
					logi("resultReceiver.onReceiveResult() :: Phase 1 not complete recieved ");
					alertView("micro:bit not in correct state", R.string.flashing_failed_title);
				}
			}

			if ((phase & 0x02) != 0) {
				logi("resultReceiver.onReceiveResult() :: Phase 2 complete recieved ");
			}

			super.onReceiveResult(resultCode, resultData);
		}
	};
}
