package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.Project;
import com.samsung.microbit.service.DfuService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.ui.BluetoothSwitch;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.adapter.ProjectAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ProjectActivity extends Activity implements View.OnClickListener {

	List<Project> projectList = new ArrayList<Project>();
	ProjectAdapter projectAdapter;
	private ListView projectListView;
	private HashMap<String, String> prettyFileNameMap = new HashMap<String, String>();

	Project programToSend;
	public int state;

	int STATE_START_NOFLASH = 0;
	int STATE_PHASE1_FLASH = 1;
    int STATE_PHASE2_FLASH = 2;


    boolean  isDisconnectedForFlash=false;
	Handler mHandler;
	private Runnable handleResetMicrobit;

	private DFUResultReceiver dfuResultReceiver;
	private int projectListSortOrder = 0;

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
            int v = intent.getIntExtra(IPCMessageManager.BUNDLE_ERROR_CODE, 0);

            logi(" broadcastReceiver ---- v= " + v);
            if (Constants.BLE_DISCONNECTED_FOR_FLASH == v){
                logi("Bluetooth disconnected for flashing. No need to display pop-up");
                return;
            }
			if (v != 0 ) {
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

	private void handleBLENotification(Context context, Intent intent) {

		logi("handleBLENotification()");

		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setConnectedDeviceText();
                PopUp.hide();
            }
        });

		int cause = intent.getIntExtra(IPCService.NOTIFICATION_CAUSE, 0);
		if (cause == IPCMessageManager.IPC_NOTIFICATION_GATT_DISCONNECTED) {
			if (isDisconnectedForFlash) {
				startFlashingPhase1();
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		MBApp.setContext(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		logi("onCreate() :: ");
		MBApp.setContext(this);

		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_projects);

		RelativeLayout layout = (RelativeLayout) findViewById(R.id.layout);
		/*
		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			layout.setBackground(getResources().getDrawable(R.drawable.bg_port));
		} else {
			layout.setBackground(getResources().getDrawable(R.drawable.bg_land));
		}
		*/

		boolean showSortMenu = false;
		try {
			showSortMenu = getResources().getBoolean(R.bool.showSortMenu);
		} catch (Exception e) {
		}

		Spinner sortList = (Spinner) findViewById(R.id.sortProjects);
		if (showSortMenu) {

			sortList.setPrompt("Sort by");
			ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(this, R.array.projectListSortOrder,
				android.R.layout.simple_spinner_item);

			sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sortList.setAdapter(sortAdapter);
			sortList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					projectListSortOrder = position;
					projectListSortOrderChanged();
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		}

		projectListView = (ListView) findViewById(R.id.projectListView);
		updateProjectsListSortOrder(true);

		/* *************************************************
		 * TODO setup to Handle BLE Notiification
		 */
		if (broadcastIntentFilter == null) {
			broadcastIntentFilter = new IntentFilter(IPCService.INTENT_BLE_NOTIFICATION);
			LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(broadcastReceiver, broadcastIntentFilter);
		}

		state = STATE_START_NOFLASH;
		setConnectedDeviceText();
		String fileToDownload = getIntent().getStringExtra("download_file");

		if (fileToDownload != null) {
			programToSend = new Project(fileToDownload, Constants.HEX_FILE_DIR + "/" + fileToDownload, 0, null, false);
			adviceOnMicrobitState(programToSend);
        }
	}

	private void setConnectedDeviceText() {

		TextView connectedIndicatorText = (TextView) findViewById(R.id.connectedIndicatorText);
		ImageButton connectedIndicatorIcon = (ImageButton) findViewById(R.id.connectedIndicatorIcon);

		if (connectedIndicatorIcon == null || connectedIndicatorText == null)
			return;

		int startIndex = 0;
		Spannable span = null;
		ConnectedDevice device = Utils.getPairedMicrobit(this);
		if (!device.mStatus) {
			connectedIndicatorIcon.setImageResource(R.drawable.disconnect_device);
			connectedIndicatorIcon.setBackground(MBApp.getContext().getResources().getDrawable(R.drawable.project_disconnect_btn));
			connectedIndicatorText.setText(getString(R.string.not_connected));
			startIndex = getString(R.string.not_connected).length();

			//TODO Add a formatted string in string resource KK
			span = new SpannableString(getString(R.string.not_connected) + "\n" + device.mName + "\n(" + device.mPattern + ")");
		} else {
			startIndex = getString(R.string.connected_to).length();
			connectedIndicatorIcon.setImageResource(R.drawable.device_connected);
			connectedIndicatorIcon.setBackground(MBApp.getContext().getResources().getDrawable(R.drawable.project_connect_btn));

			//TODO Add a formatted string in string resource KK
			span = new SpannableString(getString(R.string.connected_to) + "\n" + device.mName + "\n(" + device.mPattern + ")");
		}

		if (device.mPattern != null) {
			int endIndex = startIndex + 4;
			if (device.mName != null)
				endIndex += device.mName.length();

			if (device.mPattern != null)
				endIndex += device.mPattern.length();

			span.setSpan(new AbsoluteSizeSpan(20), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			span.setSpan(new ForegroundColorSpan(Color.BLACK), 0, startIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			span.setSpan(new ForegroundColorSpan(Color.BLUE), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			connectedIndicatorText.setText(span);
		}
	}

	public void renameFile(String filePath, String newName) {

		int rc = Utils.renameFile(filePath, newName);
		if (rc != 0) {
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("Alert");

			String message = "OOPS!";
			switch (rc) {
				case 1:
					message = "Cannot rename, destination file already exists.";
					break;

				case 2:
					message = "Cannot rename, source file not exist.";
					break;

				case 3:
					message = "Rename opertaion failed.";
					break;
			}

			alertDialog.setMessage(message);
			alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

			alertDialog.show();
		} else {
			updateProjectsListSortOrder(true);
		}
	}

	void updateProjectsListSortOrder(boolean reReadFS) {

		TextView emptyText = (TextView) findViewById(android.R.id.empty);
		projectListView.setEmptyView(emptyText);
		if (reReadFS) {
			projectList.clear();
			Utils.findProgramsAndPopulate(prettyFileNameMap, projectList);
		}

		projectListSortOrder = Utils.getListOrderPrefs(this);
		int sortBy = (projectListSortOrder >> 1);
		int sortOrder = projectListSortOrder & 0x01;
		Utils.sortProjectList(projectList, sortBy, sortOrder);

		projectAdapter = new ProjectAdapter(this, projectList);
		projectListView.setAdapter(projectAdapter);
		projectListView.setItemsCanFocus(true);
	}

	void projectListSortOrderChanged() {
		Utils.setListOrderPrefs(this, projectListSortOrder);
		updateProjectsListSortOrder(true);
	}

	public void onClick(final View v) {

		int pos;
		Intent intent;

		switch (v.getId()) {
			/*
			case R.id.preferences:
				Toast.makeText(MBApp.getContext(), "preferences", Toast.LENGTH_SHORT).show();
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Pick display order of projects")
					.setSingleChoiceItems(R.array.projectListSortOrder, projectListSortOrder, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							projectListSortOrder = which;
							projectListSortOrderChanged();
						}
					})
					.setNegativeButton("Cancel", null);

				builder.create();
				builder.show();

				break;
			*/

			case R.id.createProject:
				intent = new Intent(this, TouchDevActivity.class);
				intent.putExtra(Constants.URL, getString(R.string.touchDevURLNew));
				startActivity(intent);
				finish();
				break;

			case R.id.backBtn:
				finish();
				break;

			case R.id.sendBtn:
				if (!BluetoothSwitch.getInstance().checkBluetoothAndStart()){
					return;
				}
				pos = (Integer) v.getTag();
				Project toSend = (Project) projectAdapter.getItem(pos);
				adviceOnMicrobitState(toSend);
				break;

			case R.id.connectedIndicatorIcon:
                if (!BluetoothSwitch.getInstance().checkBluetoothAndStart()){
                    return;
                }

				ConnectedDevice connectedDevice = Utils.getPairedMicrobit(this);
				if (connectedDevice.mPattern != null) {
					if (connectedDevice.mStatus) {
						IPCService.getInstance().bleDisconnect();
					} else {

						PopUp.show(MBApp.getContext(),
							getString(R.string.init_connection),
							"",
							R.drawable.flash_face, R.drawable.blue_btn,
							PopUp.TYPE_SPINNER,
							null, null);

						IPCService.getInstance().bleConnect();
					}
				}

				break;
		}
	}

	private void adviceOnMicrobitState(final Project toSend) {
        ConnectedDevice currentMicrobit = Utils.getPairedMicrobit(this);
        if (currentMicrobit.mPattern == null) {
            PopUp.show(MBApp.getContext(),
                    getString(R.string.flashing_failed_no_microbit), //message
                    getString(R.string.flashing_error), //title
                    R.drawable.error_face,//image icon res id
                    R.drawable.red_btn,
                    PopUp.TYPE_ALERT, //type of popup.
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            PopUp.hide();
                        }
                    },//override click listener for ok button
                    null);//pass null to use default listeneronClick
        } else {
		PopUp.show(MBApp.getContext(),
                getString(R.string.flashing_tip), //message
                getString(R.string.flashing_tip_title), //title
                R.drawable.flash_face, R.drawable.blue_btn, //image icon res id
                PopUp.TYPE_CHOICE, //type of popup.
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopUp.hide();
                        initiateFlashing(toSend);
                    }
                },//override click listener for ok button
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopUp.hide();
                    }
                });//pass null to use default listeneronClick
	}
	}

	protected void initiateFlashing(Project toSend) {

		ConnectedDevice currentMicrobit = Utils.getPairedMicrobit(this);
		if (dfuResultReceiver != null) {
			LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
			dfuResultReceiver = null;
		}

		programToSend = toSend;

		if (currentMicrobit.mStatus) {
			// Disconnect Existing Gatt
			IPCService.getInstance().bleDisconnectForFlash();
            isDisconnectedForFlash = true;
		} else {
			startFlashingPhase1();
		}
	}

	protected void startFlashingPhase1() {

        if (STATE_PHASE1_FLASH == state){
            logi(">>>>>>>>>>>>>>>>>>> startFlashingPhase1 called again >>>>>>>>>>>>>>>>>>>  ");
            return; //Do nothing
        }

        logi(">>>>>>>>>>>>>>>>>>> startFlashingPhase1 called  >>>>>>>>>>>>>>>>>>>  ");
        state = STATE_PHASE1_FLASH;
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

		PopUp.show(this,
			getString(R.string.flashing_phase1_msg), //message
			String.format(MBApp.getContext().getString(R.string.flashing_project), programToSend.name), //title
			R.drawable.flash_face, R.drawable.blue_btn,
			PopUp.TYPE_SPINNER, //type of popup.
			null,//override click listener for ok button,
			null);//pass null to use default listener
	}

	protected void startFlashingPhase2() {

        if (STATE_PHASE2_FLASH == state){
            logi(">>>>>>>>>>>>>>>>>>> startFlashingPhase2 called again >>>>>>>>>>>>>>>>>>>  ");
            return; //Do nothing
        }
        logi(">>>>>>>>>>>>>>>>>>> startFlashingPhase2 called  >>>>>>>>>>>>>>>>>>>  ");
        state = STATE_PHASE2_FLASH;
		ConnectedDevice currentMicrobit = Utils.getPairedMicrobit(this);
		final Intent service = new Intent(ProjectActivity.this, DfuService.class);
		service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, currentMicrobit.mAddress);
		service.putExtra(DfuService.EXTRA_DEVICE_NAME, currentMicrobit.mPattern);
		service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, DfuService.MIME_TYPE_OCTET_STREAM);
		service.putExtra(DfuService.EXTRA_FILE_PATH, programToSend.filePath); // a path or URI must be provided.
		service.putExtra(DfuService.EXTRA_KEEP_BOND, false);
		service.putExtra(DfuService.INTENT_RESULT_RECEIVER, resultReceiver);
		service.putExtra(DfuService.INTENT_REQUESTED_PHASE, 2);
		startService(service);
	}

	private void handle_phase1_complete() {
		//TODO:
		//	pairingStatus.setText("micro:bit found");Phase 2 complete recieved
		//	pairingMessage.setText("Press button on micro:bit and then select OK");
		//state = STATE_PHASE1_COMPLETE;

		IntentFilter filter = new IntentFilter(DfuService.BROADCAST_PROGRESS);
		IntentFilter filter1 = new IntentFilter(DfuService.BROADCAST_ERROR);
		dfuResultReceiver = new DFUResultReceiver();
		LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(dfuResultReceiver, filter);
		LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(dfuResultReceiver, filter1);
		mHandler = new Handler();
		handleResetMicrobit = new Runnable() {
			@Override
			public void run() {
				handle_reset_microbit();
			}
		};
		PopUp.show(this,
			getString(R.string.flashing_phase2_msg), //message
			getString(R.string.flashing_title), //title
			R.drawable.flash_face, //image icon res id
			R.drawable.blue_btn,
			PopUp.TYPE_NOBUTTON, //type of popup.
			null,//override click listener for ok button
			null);//pass null to use default listener

		//mHandler.postDelayed(handleResetMicrobit, 30000);

	}


	void handle_reset_microbit() {
		logi("handle_reset_microbit");
		PopUp.show(MBApp.getContext(),
			getString(R.string.flashing_error_msg), //message
			getString(R.string.flashing_failed_title), //title
			R.drawable.error_face, //image icon res id
			R.drawable.red_btn,
			PopUp.TYPE_ALERT, //type of popup.
			popupOkHandler,//override click listener for ok button
			popupOkHandler);//pass null to use default listener
		LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
	}

	/**
	 *
	 */
	ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {

			int phase = resultCode & 0x0ffff;

			if (phase == Constants.FLASHING_PAIRING_CODE_CHARACTERISTIC_RECIEVED) {
				logi("resultReceiver.onReceiveResult() :: Phase 2 start recieved ");
				PopUp.hide();
				mHandler.removeCallbacks(handleResetMicrobit);
				handleResetMicrobit = null;
				startFlashingPhase2();

			} else if ((phase & Constants.FLASHING_PHASE_1_COMPLETE) != 0) {
				if ((phase & 0x0ff00) == 0) {
					logi("resultReceiver.onReceiveResult() :: Phase 1 complete recieved ");
					handle_phase1_complete();

				} else {
					logi("resultReceiver.onReceiveResult() :: Phase 1 not complete recieved ");
					PopUp.show(MBApp.getContext(),
						getString(R.string.flashing_error_msg), //message
						getString(R.string.flashing_failed_title), //title
						R.drawable.error_face, //image icon res id
						R.drawable.red_btn,
						PopUp.TYPE_ALERT, //type of popup.
                        popupOkHandler,//override click listener for ok button
                        popupOkHandler);//pass null to use default listener
				}
			}

			if ((phase & Constants.FLASHING_PHASE_2_COMPLETE) != 0) {
				logi("resultReceiver.onReceiveResult() :: Phase 2 complete recieved ");
			}

			super.onReceiveResult(resultCode, resultData);
		}
	};


    View.OnClickListener popupOkHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("popupOkHandler");
            state = STATE_START_NOFLASH;
            if(isDisconnectedForFlash) {
                IPCService.getInstance().bleConnect();
                isDisconnectedForFlash = false;
            }
        }
    };

	class DFUResultReceiver extends BroadcastReceiver {

		private boolean isCompleted = false;
		private boolean inInit = false;
		private boolean inProgress = false;

		@Override
		public void onReceive(Context context, Intent intent) {
			String message = "Broadcast intent detected " + intent.getAction();
			logi("DFUResultReceiver.onReceive :: " + message);
			if (intent.getAction() == DfuService.BROADCAST_PROGRESS) {

				int state = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
				if (state < 0) {
					logi("DFUResultReceiver.onReceive :: state -- " + state);
					switch (state) {
						case DfuService.PROGRESS_COMPLETED:
							if (!isCompleted) {
								// todo progress bar dismiss
								PopUp.hide();
								LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
								dfuResultReceiver = null;
								PopUp.show(MBApp.getContext(),
									getString(R.string.flashing_success_message), //message
									getString(R.string.flashing_success_title), //title
									R.drawable.message_face, R.drawable.blue_btn,
									PopUp.TYPE_ALERT, //type of popup.
									popupOkHandler,//override click listener for ok button
                                    popupOkHandler);//pass null to use default listener
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
								//PopUp.hide();
								PopUp.show(MBApp.getContext(),
									error_message, //message
									getString(R.string.flashing_failed_title), //title
									R.drawable.error_face, R.drawable.red_btn,
									PopUp.TYPE_ALERT, //type of popup.
                                    popupOkHandler,//override click listener for ok button
                                    popupOkHandler);//pass null to use default listener

								LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
							}

							break;

						case DfuService.PROGRESS_CONNECTING:
							if ((!inInit) && (!isCompleted)) {
								PopUp.show(MBApp.getContext(),
									getString(R.string.init_connection), //message
									getString(R.string.send_project), //title
									R.drawable.flash_face, R.drawable.blue_btn,
									PopUp.TYPE_SPINNER, //type of popup.
									new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											PopUp.hide();

										}
									},//override click listener for ok button
									null);//pass null to use default listener
							}

							inInit = true;
							isCompleted = false;
							break;
					}
				} else if ((state > 0) && (state < 100)) {
					if (!inProgress) {
						// TODO Update progress bar check if correct.(my3)
						PopUp.show(MBApp.getContext(),
							MBApp.getContext().getString(R.string.flashing_progress_message),
							String.format(MBApp.getContext().getString(R.string.flashing_project), programToSend.name),
							R.drawable.flash_face, R.drawable.blue_btn,
							PopUp.TYPE_PROGRESS, null, null);

						inProgress = true;
					}

					PopUp.updateProgressBar(state);

				}
			} else if (intent.getAction() == DfuService.BROADCAST_ERROR) {
				String error_message = broadcastGetErrorMessage(intent.getIntExtra(DfuService.EXTRA_DATA, 0));

				logi("DFUResultReceiver.onReceive() :: Flashing ERROR!!  Code - [" + intent.getIntExtra(DfuService.EXTRA_DATA, 0)
					+ "] Error Type - [" + intent.getIntExtra(DfuService.EXTRA_ERROR_TYPE, 0) + "]");

				//todo dismiss progress
				PopUp.hide();

				//TODO popup flashing failed
				PopUp.show(MBApp.getContext(),
					error_message, //message
					getString(R.string.flashing_failed_title), //title
					R.drawable.error_face, R.drawable.red_btn,
					PopUp.TYPE_ALERT, //type of popup.
                    popupOkHandler,//override click listener for ok button
                    popupOkHandler);//pass null to use default listener

				LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
				dfuResultReceiver = null;

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
}
