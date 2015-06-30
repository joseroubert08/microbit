package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.Project;
import com.samsung.microbit.service.DfuService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.adapter.ProjectAdapter;
import com.samsung.microbit.ui.fragment.ProjectActivityPopupFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ProjectActivity extends Activity implements View.OnClickListener {

	List<Project> projectList = new ArrayList<Project>();
	ProjectAdapter projectAdapter;
	private ListView projectListView;
	private HashMap<String, String> prettyFileNameMap = new HashMap<String, String>();
	private ArrayList<String> list = new ArrayList<String>();

	ProjectActivityPopupFragment fragment;
	LinearLayout popupOverlay;

	Project programToSend;
	public int state;
	int STATE_START_NOFLASH = 0;
	int STATE_START_FLASH = 1;
	int STATE_PHASE1_COMPLETE = 2;


	private DFUResultReceiver dfuResultReceiver;

	// DEBUG
	protected boolean debug = true;
	protected String TAG = "ProjectActivity";

	protected void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}

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
		int cause = intent.getIntExtra(IPCService.NOTIFICATION_CAUSE, 0);
		if (cause == IPCMessageManager.IPC_NOTIFICATION_GATT_DISCONNECTED) {
			if (state == STATE_START_FLASH) {
				startFlashingPhase1();
			}
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

		MBApp.setContext(this);

		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_projects);

		LinearLayout mainContentView = (LinearLayout) findViewById(R.id.mainContentView);
		mainContentView.getBackground().setAlpha(128);

		projectListView = (ListView) findViewById(R.id.projectListView);
		TextView emptyText = (TextView) findViewById(android.R.id.empty);
		projectListView.setEmptyView(emptyText);

		Utils.findProgramsAndPopulate(prettyFileNameMap, projectList);

		projectAdapter = new ProjectAdapter(this, projectList);
		projectListView.setAdapter(projectAdapter);

		TextView connectedIndicatorText = (TextView) findViewById(R.id.connectedIndicatorText);
		ImageButton connectedIndicatorIcon = (ImageButton) findViewById(R.id.connectedIndicatorIcon);
		setText(connectedIndicatorText, connectedIndicatorIcon);


		/* *************************************************
		 * TODO setup to Handle BLE Notiification
		 */
		if (broadcastIntentFilter == null) {
			broadcastIntentFilter = new IntentFilter(IPCService.INTENT_BLE_NOTIFICATION);
			LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(broadcastReceiver, broadcastIntentFilter);
		}
		state = 0;
	}

	private void setText(TextView txt, ImageButton imgBtn) {

		ConnectedDevice device = Utils.getPairedMicrobit(this);
		if (device.mName == null) {
			imgBtn.setImageResource(R.drawable.disconnected);
			imgBtn.setBackground(MBApp.getContext().getResources().getDrawable(R.drawable.project_disconnect_btn));
			txt.setText(getString(R.string.not_connected));
		} else {
			int startIndex = getString(R.string.connected_to).length();
			int endIndex = startIndex + device.mName.length() + device.mPattern.length() + 2;

			Spannable span = new SpannableString(getString(R.string.connected_to) + "\n" + device.mName + "\n" + device.mPattern);
			span.setSpan(new AbsoluteSizeSpan(20), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			span.setSpan(new ForegroundColorSpan(Color.BLACK), 0, startIndex,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			span.setSpan(new ForegroundColorSpan(Color.BLUE), getString(R.string.connected_to).length(), endIndex,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			txt.setText(span);
		}
	}

	public void onClick(final View v) {

		switch (v.getId()) {
			case R.id.createProject: {
				Intent intent = new Intent(this, TouchDevActivity.class);
				intent.putExtra(Constants.URL, getString(R.string.touchDevURLNew));
				startActivity(intent);
				finish();
			}
			break;
			case R.id.homeBtn: {
				finish();
			}
			break;
			case R.id.sendBtn:
				int pos = (Integer) v.getTag();
				Project toSend = (Project) projectAdapter.getItem(pos);
				initiateFlashing(toSend);
				break;
		}
	}


	protected void initiateFlashing(Project toSend) {

		ConnectedDevice currentMicrobit = Utils.getPairedMicrobit(this);
		programToSend = toSend;
		if (currentMicrobit.mStatus) {
			// Disconnect Existing Gatt
			IPCService.getInstance().bleDisconnect();

			state = STATE_START_FLASH;
		} else {
			startFlashingPhase1();
		}
	}


	protected void startFlashingPhase1() {

		ConnectedDevice currentMicrobit = Utils.getPairedMicrobit(this);

		final Intent service = new Intent(this, DfuService.class);
		service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, currentMicrobit.mAddress);
		service.putExtra(DfuService.EXTRA_DEVICE_NAME, currentMicrobit.mPattern);
		service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, DfuService.MIME_TYPE_OCTET_STREAM);
		//service.putExtra(DfuService.EXTRA_FILE_TYPE, mFileType);

		service.putExtra(DfuService.EXTRA_FILE_PATH, programToSend.filePath); // a path or URI must be provided.
		//service.putExtra(DfuService.EXTRA_FILE_URI, mFileStreamUri);
		// Init packet is required by Bootloader/DFU from SDK 7.0+ if HEX or BIN file is given above.
		// In case of a ZIP file, the init packet (a DAT file) must be included inside the ZIP file.
		//service.putExtra(DfuService.EXTRA_INIT_FILE_PATH, mInitFilePath);
		//service.putExtra(DfuService.EXTRA_INIT_FILE_URI, mInitFileStreamUri);
		service.putExtra(DfuService.EXTRA_KEEP_BOND, false);
		service.putExtra(DfuService.INTENT_RESULT_RECEIVER, resultReceiver);
		service.putExtra(DfuService.INTENT_REQUESTED_PHASE, 1);
		startService(service);

	}

	protected void startFlashingPhase2() {

		ConnectedDevice currentMicrobit = Utils.getPairedMicrobit(this);
		final Intent service = new Intent(ProjectActivity.this, DfuService.class);
		service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, currentMicrobit.mAddress);
		service.putExtra(DfuService.EXTRA_DEVICE_NAME, currentMicrobit.mPattern);
		service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE,DfuService.MIME_TYPE_OCTET_STREAM);
		service.putExtra(DfuService.EXTRA_FILE_PATH,programToSend.filePath); // a path or URI must be provided.
		service.putExtra(DfuService.EXTRA_KEEP_BOND,false);
		service.putExtra(DfuService.INTENT_RESULT_RECEIVER,resultReceiver);
		service.putExtra(DfuService.INTENT_REQUESTED_PHASE,2);
		startService(service);
	}
	private void handle_phase1_complete() {
		//TODO:
	//	pairingStatus.setText("micro:bit found");
	//	pairingMessage.setText("Press button on micro:bit and then select OK");
		//state = STATE_PHASE1_COMPLETE;

		IntentFilter filter = new IntentFilter(DfuService.BROADCAST_PROGRESS);
		IntentFilter filter1 = new IntentFilter(DfuService.BROADCAST_ERROR);
		dfuResultReceiver = new DFUResultReceiver();
		LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(dfuResultReceiver, filter);
		LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(dfuResultReceiver, filter1);
		PopUp.show(this,
				"Press button on micro:bit and then select OK", //message
				"Flashing", //title
				R.drawable.exclamation, //image icon res id
				0,
				PopUp.TYPE_ALERT, //type of popup.
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						PopUp.hide();
						state = STATE_PHASE1_COMPLETE;
						startFlashingPhase2();

					}
				},//override click listener for ok button
				null);//pass null to use default listener




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
					//Todo popup
					//Toast.makeText(MBApp.getContext(), "resultReceiver.onReceiveResult() :: Phase 1 not complete recieved", Toast.LENGTH_SHORT).show();
					//alertView("micro:bit not in correct state", R.string.flashing_failed_title);


					PopUp.show(MBApp.getContext(),
							"micro:bit not in correct state", //message
							"Flashing", //title
							R.drawable.exclamation, //image icon res id
							0,
							PopUp.TYPE_ALERT, //type of popup.
							new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									PopUp.hide();
									finish();
								}
							},//override click listener for ok button
							null);//pass null to use default listener
				}
			}

			if ((phase & 0x02) != 0) {
				logi("resultReceiver.onReceiveResult() :: Phase 2 complete recieved ");
			}

			super.onReceiveResult(resultCode, resultData);
		}
	};


	class DFUResultReceiver extends BroadcastReceiver {

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
					// Todo status
					dialogInitDone = true;
				}
				int state = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
				logi("DFUResultReceiver.onReceive :: state -- " + state);

				if (state < 0) {
					switch (state) {
						case DfuService.PROGRESS_COMPLETED:
							if (!isCompleted) {
								// todo progress bar dismiss
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
								// Todo
								// Progress bar dismiss
								// popup
								//alertView(error_message, R.string.flashing_failed_title);
								LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
							}

							break;
						case DfuService.PROGRESS_CONNECTING:
							if (!inInit) {
								// Todo popup
							}

							inInit = true;
							isCompleted = false;
							break;
					}

				} else if ((state > 0) && (state < 100)) {
					if (!inProgress) {
						// todo Update progress bar
						inProgress = true;
					}

					//flashSpinnerDialog.setProgress(state);
				}
			} else if (intent.getAction() == DfuService.BROADCAST_ERROR) {
				String error_message = broadcastGetErrorMessage(intent.getIntExtra(DfuService.EXTRA_DATA, 0));

				logi("DFUResultReceiver.onReceive() :: Flashing ERROR!!  Code - [" + intent.getIntExtra(DfuService.EXTRA_DATA, 0)
						+ "] Error Type - [" + intent.getIntExtra(DfuService.EXTRA_ERROR_TYPE, 0) + "]");

				//todo dismiss progress

				//TODO popup flashing failed
				//alertView(error_message, R.string.flashing_failed_title);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		/*
		if (id == R.id.action_settings) {
			return true;
		} else if (id == R.id.load_fragment) {
			Log.i("ProjectActivity", "###### onOptionsItemSelected load_fragment");

			if (popupOverlay == null) {
				popupOverlay = (LinearLayout) findViewById(R.id.popup_overlay);
				popupOverlay.getBackground().setAlpha(224);
				popupOverlay.setOnTouchListener(new View.OnTouchListener() {
					@OverridemainContentView
					public boolean onTouch(View v, MotionEvent event) {
						return true;
					}
				});
			}

			popupOverlay.setVisibility(View.VISIBLE);

			if (fragment == null) {
				FragmentManager fragmentManager = getFragmentManager();
				FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

				fragment = new ProjectActivityPopupFragment();
				fragmentTransaction.add(R.id.popup_overlay, fragment);
				fragmentTransaction.commit();
			}
		} else if (id == R.id.hide_load_fragment) {
			Log.i("ProjectActivity", "#### onOptionsItemSelected() : id == R.id.hide_load_fragment");
			if (popupOverlay != null && popupOverlay.getVisibility() == View.VISIBLE) {
				popupOverlay.setVisibility(View.INVISIBLE);
			}

		} else if (id == R.id.change_button_meaning) {
			Log.i("ProjectActivity", "#### onOptionsItemSelected() : id == R.id.change_button_meaning");
			if (popupOverlay != null && popupOverlay.getVisibility() == View.VISIBLE) {
				fragment.changeMeaning();
			}
		}
		*/

		return super.onOptionsItemSelected(item);
	}
}
