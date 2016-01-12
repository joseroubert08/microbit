package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.RemoteConfig;
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

    private DFUResultReceiver dfuResultReceiver;
    private int projectListSortOrder = 0;
    private boolean connectionInitiated = false;
    // DEBUG
    protected boolean debug = true;
    protected String TAG = "ProjectActivity";

    private static ACTIVITY_STATE mActivityState = ACTIVITY_STATE.STATE_IDLE;

    private enum ACTIVITY_STATE {
        STATE_IDLE,
        STATE_ENABLE_BT_INTERNAL_FLASH_REQUEST,
        STATE_ENABLE_BT_EXTERNAL_FLASH_REQUEST,
        STATE_ENABLE_BT_CONNECT,
        FLASH_STATE_FIND_DEVICE,
        FLASH_STATE_VERIFY_DEVICE,
        FLASH_STATE_WAIT_DEVICE_REBOOT,
        FLASH_STATE_INIT_DEVICE,
        FLASH_STATE_PROGRESS
    }

    protected void logi(String message) {
        if (debug) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    /* *************************************************
     * TODO setup to Handle BLE Notifications
     */
    IntentFilter broadcastIntentFilter;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            int v = intent.getIntExtra(IPCMessageManager.BUNDLE_ERROR_CODE, 0);

            logi(" broadcastReceiver ---- v= " + v);
            if (Constants.BLE_DISCONNECTED_FOR_FLASH == v) {
                logi("Bluetooth disconnected for flashing. No need to display pop-up");
                handleBLENotification(context, intent, false);
                if (programToSend != null && programToSend.filePath != null)
                    initiateFlashing();
                return;
            }
            handleBLENotification(context, intent, true);
            if (v != 0) {
                String message = intent.getStringExtra(IPCMessageManager.BUNDLE_ERROR_MESSAGE);
                logi("broadcastReceiver Error message = " + message);
                if (message == null)
                    message = "Error";
                final String displayTitle = message;

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

    private void handleBLENotification(Context context, Intent intent, boolean hide) {
        int v = intent.getIntExtra(IPCMessageManager.BUNDLE_ERROR_CODE, 0);
        logi("broadcastReceiver Error code =" + v);
        logi("handleBLENotification()");
        final boolean popupHide = hide;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setConnectedDeviceText();
                if (popupHide && connectionInitiated) {
                    connectionInitiated = false;
                    PopUp.hide();
                }
            }
        });

        int cause = intent.getIntExtra(IPCService.NOTIFICATION_CAUSE, 0);
        if (cause == IPCMessageManager.IPC_NOTIFICATION_GATT_DISCONNECTED) {
//			if (isDisconnectedForFlash) {
//				startFlashingPhase1();
//			}
        }
    }

    private void setActivityState(ACTIVITY_STATE newState) {
        logi("Flash state old - " + mActivityState + " new - " + newState);
        mActivityState = newState;
        setConnectedDeviceText();
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

        RemoteConfig.getInstance().init();

        // Make sure to call this before any other userActionEvent is sent
        if (MBApp.getApp().getEcho() != null) {
            logi("Page View test for ProjectActivity");
            MBApp.getApp().getEcho().viewEvent("com.samsung.microbit.ui.activity.projectactivity.page", null);
        }
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_projects);

        boolean showSortMenu = false;
        try {
            showSortMenu = getResources().getBoolean(R.bool.showSortMenu);
        } catch (Exception e) {
        }
// TODO - KEEP sort functionality
//        Spinner sortList = (Spinner) findViewById(R.id.sortProjects);
//        if (showSortMenu) {
//
//            sortList.setPrompt("Sort by");
//            ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(this, R.array.projectListSortOrder,
//                    android.R.layout.simple_spinner_item);
//
//            sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            sortList.setAdapter(sortAdapter);
//            sortList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//                @Override
//                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                    projectListSortOrder = position;
//                    projectListSortOrderChanged();
//                }
//
//                @Override
//                public void onNothingSelected(AdapterView<?> parent) {
//                }
//            });
//        }

        // Title font
        TextView flashProjectsTitle = (TextView) findViewById(R.id.flash_projects_title_txt);
        flashProjectsTitle.setTypeface(MBApp.getApp().getTypeface());

        // Create projects
        Button createProject = (Button) findViewById(R.id.createProject);
        createProject.setTypeface(MBApp.getApp().getTypeface());

        projectListView = (ListView) findViewById(R.id.projectListView);
        updateProjectsListSortOrder(true);

		/* *************************************************
         * TODO setup to Handle BLE Notification
		 */
        if (broadcastIntentFilter == null) {
            broadcastIntentFilter = new IntentFilter(IPCService.INTENT_BLE_NOTIFICATION);
            LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(broadcastReceiver, broadcastIntentFilter);
        }
        setConnectedDeviceText();
        String fileToDownload = getIntent().getStringExtra("download_file");
        if (fileToDownload != null) {
            setActivityState(ACTIVITY_STATE.STATE_ENABLE_BT_EXTERNAL_FLASH_REQUEST);
        }
        //TODO Refactor this code here!! There must be only 1 FLASH state. Not 2 as before once we start using external Browser
        if (fileToDownload == null) {
            //Request coming because of the intent-filter
            if (getIntent() != null && getIntent().getData() != null && getIntent().getData().getEncodedPath() != null)
                fileToDownload = getIntent().getData().getEncodedPath();
            if (fileToDownload != null) {
                setActivityState(ACTIVITY_STATE.STATE_ENABLE_BT_INTERNAL_FLASH_REQUEST);
            }
        }
        if (fileToDownload != null) {
            programToSend = new Project(fileToDownload, Constants.HEX_FILE_DIR + "/" + fileToDownload, 0, null, false);
            if (!BluetoothSwitch.getInstance().isBluetoothON()) {
                startBluetooth();
            } else {
                adviceOnMicrobitState();
            }
        }

    }

    private void setConnectedDeviceText() {

        TextView connectedIndicatorText = (TextView) findViewById(R.id.connectedIndicatorText);
        connectedIndicatorText.setText(connectedIndicatorText.getText());
        connectedIndicatorText.setTypeface(MBApp.getApp().getTypeface());    // TODO - check
        TextView deviceName1 = (TextView) findViewById(R.id.deviceName);
        deviceName1.setContentDescription(deviceName1.getText()); // TODO - check
        deviceName1.setTypeface(MBApp.getApp().getTypeface());
        ImageView connectedIndicatorIcon = (ImageView) findViewById(R.id.connectedIndicatorIcon);

        if (connectedIndicatorIcon == null || connectedIndicatorText == null)
            return;

        //Override the connection Icon in case of active flashing
        if (mActivityState == ACTIVITY_STATE.FLASH_STATE_FIND_DEVICE
                || mActivityState == ACTIVITY_STATE.FLASH_STATE_VERIFY_DEVICE
                || mActivityState == ACTIVITY_STATE.FLASH_STATE_WAIT_DEVICE_REBOOT
                || mActivityState == ACTIVITY_STATE.FLASH_STATE_INIT_DEVICE
                || mActivityState == ACTIVITY_STATE.FLASH_STATE_PROGRESS

                ) {
            connectedIndicatorIcon.setImageResource(R.drawable.device_connected);
            connectedIndicatorText.setText(getString(R.string.connected_to));

            return;
        }
        int startIndex = 0;
        Spannable span = null;
        ConnectedDevice device = Utils.getPairedMicrobit(this);
        if (!device.mStatus) {
            connectedIndicatorIcon.setImageResource(R.drawable.device_status_disconnected);
            connectedIndicatorText.setText(getString(R.string.not_connected));
            if (deviceName1 != null) {
                //Mobile Device.. 2 lines of display
                if (device.mName != null)
                    deviceName1.setText(device.mName);
            } else if (deviceName1 != null) {
                if (device.mName != null)
                    deviceName1.setText(device.mName + " (" + device.mPattern + ")");
            }
        } else {
            connectedIndicatorIcon.setImageResource(R.drawable.device_connected);
            connectedIndicatorText.setText(getString(R.string.connected_to));
            if (deviceName1 != null) {
                //Mobile Device.. 2 lines of display
                if (device.mName != null)
                    deviceName1.setText(device.mName);
            } else if (deviceName1 != null) {
                if (device.mName != null)
                    deviceName1.setText(device.mName + " (" + device.mPattern + ")");
            }
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
                    message = "Rename operation failed.";
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
            int totalPrograms = Utils.findProgramsAndPopulate(prettyFileNameMap, projectList);
            //Update Stats
            if (MBApp.getApp() != null && MBApp.getApp().getEcho() != null) {
                MBApp.getApp().getEcho().userActionEvent("saved_projects", Integer.toString(totalPrograms), null);
            }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                if (mActivityState == ACTIVITY_STATE.STATE_ENABLE_BT_INTERNAL_FLASH_REQUEST ||
                        mActivityState == ACTIVITY_STATE.STATE_ENABLE_BT_EXTERNAL_FLASH_REQUEST) {
                    adviceOnMicrobitState();
                } else if (mActivityState == ACTIVITY_STATE.STATE_ENABLE_BT_CONNECT) {
                    setActivityState(ACTIVITY_STATE.STATE_IDLE);
                    toggleConnection();
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                setActivityState(ACTIVITY_STATE.STATE_IDLE);
                PopUp.show(MBApp.getContext(),
                        getString(R.string.bluetooth_off_cannot_continue), //message
                        "",
                        R.drawable.error_face, R.drawable.red_btn,
                        PopUp.TYPE_ALERT,
                        null, null);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
    }

    private void toggleConnection() {
        ConnectedDevice connectedDevice = Utils.getPairedMicrobit(this);
        if (connectedDevice.mPattern != null) {
            if (connectedDevice.mStatus) {
                IPCService.getInstance().bleDisconnect();
            } else {
                connectionInitiated = true;
                PopUp.show(MBApp.getContext(),
                        getString(R.string.init_connection),
                        "",
                        R.drawable.flash_face, R.drawable.blue_btn,
                        PopUp.TYPE_SPINNER,
                        null, null);

                IPCService.getInstance().bleConnect();
            }
        }
    }

    @Override
    public void onClick(final View v) {

        int pos;

        switch (v.getId()) {
            case R.id.createProject: {
                if (MBApp.getApp().getEcho() != null) {
                    MBApp.getApp().getEcho().userActionEvent("onClick", "myScripts", null);
                }
                String url = RemoteConfig.getInstance().getMyScriptsURL();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
            finish();
            break;

            case R.id.backBtn:
                finish();
                break;

            case R.id.sendBtn:
                pos = (Integer) v.getTag();
                programToSend = (Project) projectAdapter.getItem(pos);
                setActivityState(ACTIVITY_STATE.STATE_ENABLE_BT_INTERNAL_FLASH_REQUEST);
                if (!BluetoothSwitch.getInstance().isBluetoothON()) {
                    startBluetooth();
                } else {
                    adviceOnMicrobitState();
                }
                break;

            case R.id.connectedIndicatorIcon:
                if (!BluetoothSwitch.getInstance().isBluetoothON()) {
                    setActivityState(ACTIVITY_STATE.STATE_ENABLE_BT_CONNECT);
                    startBluetooth();
                } else {
                    toggleConnection();
                }
                break;
        }
    }

    // TODO fonts on pop up
    private void adviceOnMicrobitState() {
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
            //TODO Check if the micro:bit is reachable first
            if (programToSend == null || programToSend.filePath == null) {
                PopUp.show(MBApp.getContext(),
                        getString(R.string.internal_error_msg),
                        "",
                        R.drawable.error_face, R.drawable.red_btn,
                        PopUp.TYPE_ALERT,
                        null, null);
                return;
            }
            if (mActivityState == ACTIVITY_STATE.FLASH_STATE_FIND_DEVICE
                    || mActivityState == ACTIVITY_STATE.FLASH_STATE_VERIFY_DEVICE
                    || mActivityState == ACTIVITY_STATE.FLASH_STATE_WAIT_DEVICE_REBOOT
                    || mActivityState == ACTIVITY_STATE.FLASH_STATE_INIT_DEVICE
                    || mActivityState == ACTIVITY_STATE.FLASH_STATE_PROGRESS

                    ) {
                // Another download session is in progress
                PopUp.show(MBApp.getContext(),
                        getString(R.string.multple_flashing_session_msg),
                        "",
                        R.drawable.flash_face, R.drawable.blue_btn,
                        PopUp.TYPE_ALERT,
                        null, null);
                return;
            }
            if (mActivityState == ACTIVITY_STATE.STATE_ENABLE_BT_INTERNAL_FLASH_REQUEST) {
                //Check final device from user and start flashing
                PopUp.show(MBApp.getContext(),
                        getString(R.string.flash_start_message, currentMicrobit.mName), //message
                        getString(R.string.flashing_title), //title
                        R.drawable.flash_face, R.drawable.blue_btn, //image icon res id
                        PopUp.TYPE_CHOICE, //type of popup.
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ConnectedDevice currentMicrobit = Utils.getPairedMicrobit(MBApp.getContext());
                                PopUp.hide();
                                if (currentMicrobit.mStatus == true) {
                                    IPCService.getInstance().bleDisconnectForFlash();
                                } else {
                                    //Start flashing immediately if it is already disconnected
                                    initiateFlashing();
                                }
                            }
                        },//override click listener for ok button
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PopUp.hide();
                            }
                        });//pass null to use default listeneronClick
            } else if (mActivityState == ACTIVITY_STATE.STATE_ENABLE_BT_EXTERNAL_FLASH_REQUEST) {
                if (currentMicrobit.mStatus == true) {
                    IPCService.getInstance().bleDisconnectForFlash();
                } else {
                    //Start flashing immediately if it is already disconnected
                    initiateFlashing();
                }
            }
        }
    }

    protected void initiateFlashing() {
        if (dfuResultReceiver != null) {
            LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
            dfuResultReceiver = null;
        }
        setActivityState(ACTIVITY_STATE.FLASH_STATE_FIND_DEVICE);
        registerCallbacksForFlashing();
        startFlashing();
    }

    protected void startFlashing() {
        logi(">>>>>>>>>>>>>>>>>>> startFlashing called  >>>>>>>>>>>>>>>>>>>  ");
        ConnectedDevice currentMicrobit = Utils.getPairedMicrobit(this);
        final Intent service = new Intent(ProjectActivity.this, DfuService.class);
        service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, currentMicrobit.mAddress);
        service.putExtra(DfuService.EXTRA_DEVICE_NAME, currentMicrobit.mPattern);
        service.putExtra(DfuService.EXTRA_DEVICE_PAIR_CODE, currentMicrobit.mPairingCode);
        service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, DfuService.MIME_TYPE_OCTET_STREAM);
        service.putExtra(DfuService.EXTRA_FILE_PATH, programToSend.filePath); // a path or URI must be provided.
        service.putExtra(DfuService.EXTRA_KEEP_BOND, false);
        service.putExtra(DfuService.INTENT_RESULT_RECEIVER, resultReceiver);
        service.putExtra(DfuService.INTENT_REQUESTED_PHASE, 2);
        startService(service);
    }

    private void registerCallbacksForFlashing() {
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

            logi("resultReceiver.onReceiveResult() :: Phase = " + phase + " resultCode = " + resultCode);
            super.onReceiveResult(resultCode, resultData);
        }
    };


    View.OnClickListener popupOkHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("popupOkHandler");
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
                        case DfuService.PROGRESS_STARTING:
                            setActivityState(ACTIVITY_STATE.FLASH_STATE_INIT_DEVICE);
                            PopUp.show(MBApp.getContext(),
                                    getString(R.string.dfu_status_starting_msg), //message
                                    getString(R.string.send_project), //title
                                    R.drawable.flash_face, R.drawable.blue_btn,
                                    PopUp.TYPE_SPINNER_NOT_CANCELABLE, //type of popup.
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            //Do nothing. As this is non-cancellable pop-up

                                        }
                                    },//override click listener for ok button
                                    null);//pass null to use default listener
                            break;
                        case DfuService.PROGRESS_COMPLETED:
                            if (!isCompleted) {
                                setActivityState(ACTIVITY_STATE.STATE_IDLE);
                                LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
                                dfuResultReceiver = null;
                                //Update Stats
                                if (MBApp.getApp().getEcho() != null) {
                                    //TODO add more data action_location (app/web), hex_file_size, binary_size, firmware <Micro:bit firmware version >
                                    MBApp.getApp().getEcho().userActionEvent("hex_file_flash", "success", null);
                                }
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
                            String error_message = "Error Code - [" + intent.getIntExtra(DfuService.EXTRA_DATA, 0)
                                    + "] \n Error Type - [" + intent.getIntExtra(DfuService.EXTRA_ERROR_TYPE, 0) + "]";

/*                            if ((isCompleted == false) && (inProgress == false))// Disconnecting event because of error
                            {
                                logi(error_message);
                                setActivityState(ACTIVITY_STATE.STATE_IDLE);
                                PopUp.show(MBApp.getContext(),
                                        error_message, //message
                                        getString(R.string.flashing_failed_title), //title
                                        R.drawable.error_face, R.drawable.red_btn,
                                        PopUp.TYPE_ALERT, //type of popup.
                                        popupOkHandler,//override click listener for ok button
                                        popupOkHandler);//pass null to use default listener

                                LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
                            }*/
                            break;

                        case DfuService.PROGRESS_CONNECTING:
                            if ((!inInit) && (!isCompleted)) {
                                setActivityState(ACTIVITY_STATE.FLASH_STATE_INIT_DEVICE);
                                PopUp.show(MBApp.getContext(),
                                        getString(R.string.init_connection), //message
                                        getString(R.string.send_project), //title
                                        R.drawable.flash_face, R.drawable.blue_btn,
                                        PopUp.TYPE_SPINNER_NOT_CANCELABLE, //type of popup.
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                //Do nothing. As this is non-cancellable pop-up
                                            }
                                        },//override click listener for ok button
                                        null);//pass null to use default listener
                            }

                            inInit = true;
                            isCompleted = false;
                            break;
                        case DfuService.PROGRESS_VALIDATING:
                            setActivityState(ACTIVITY_STATE.FLASH_STATE_VERIFY_DEVICE);
                            PopUp.show(MBApp.getContext(),
                                    getString(R.string.validating_microbit), //message
                                    getString(R.string.send_project), //title
                                    R.drawable.flash_face, R.drawable.blue_btn,
                                    PopUp.TYPE_SPINNER_NOT_CANCELABLE, //type of popup.
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            //Do nothing. As this is non-cancellable pop-up

                                        }
                                    },//override click listener for ok button
                                    null);//pass null to use default listener
                            break;
                        case DfuService.PROGRESS_WAITING_REBOOT:
                            setActivityState(ACTIVITY_STATE.FLASH_STATE_WAIT_DEVICE_REBOOT);
                            PopUp.show(MBApp.getContext(),
                                    getString(R.string.waiting_reboot), //message
                                    getString(R.string.send_project), //title
                                    R.drawable.flash_face, R.drawable.blue_btn,
                                    PopUp.TYPE_SPINNER_NOT_CANCELABLE, //type of popup.
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            //Do nothing. As this is non-cancellable pop-up

                                        }
                                    },//override click listener for ok button
                                    null);//pass null to use default listener
                            break;
                        case DfuService.PROGRESS_VALIDATION_FAILED:
                            setActivityState(ACTIVITY_STATE.STATE_IDLE);
                            //Update Stats
                            if (MBApp.getApp().getEcho() != null) {
                                //TODO add more data action_location (app/web), hex_file_size, binary_size, firmware <Micro:bit firmware version >
                                MBApp.getApp().getEcho().userActionEvent("hex_file_flash", "fail", null);
                            }
                            PopUp.show(MBApp.getContext(),
                                    getString(R.string.flashing_verifcation_failed), //message
                                    getString(R.string.flashing_verifcation_failed_title),
                                    R.drawable.error_face, R.drawable.red_btn,
                                    PopUp.TYPE_ALERT, //type of popup.
                                    popupOkHandler,//override click listener for ok button
                                    popupOkHandler);//pass null to use default listener

                            LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
                            dfuResultReceiver = null;
                            break;
                        case DfuService.PROGRESS_ABORTED:
                            setActivityState(ACTIVITY_STATE.STATE_IDLE);
                            //Update Stats
                            if (MBApp.getApp().getEcho() != null) {
                                //TODO add more data action_location (app/web), hex_file_size, binary_size, firmware <Micro:bit firmware version >
                                MBApp.getApp().getEcho().userActionEvent("hex_file_flash", "fail", null);
                            }
                            PopUp.show(MBApp.getContext(),
                                    getString(R.string.flashing_aborted), //message
                                    getString(R.string.flashing_aborted_title),
                                    R.drawable.error_face, R.drawable.red_btn,
                                    PopUp.TYPE_ALERT, //type of popup.
                                    popupOkHandler,//override click listener for ok button
                                    popupOkHandler);//pass null to use default listener

                            LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
                            dfuResultReceiver = null;
                            break;

                    }
                } else if ((state > 0) && (state < 100)) {
                    if (!inProgress) {
                        setActivityState(ACTIVITY_STATE.FLASH_STATE_PROGRESS);
                        PopUp.show(MBApp.getContext(),
                                MBApp.getContext().getString(R.string.flashing_progress_message),
                                String.format(MBApp.getContext().getString(R.string.flashing_project), programToSend.name),
                                R.drawable.flash_modal_emoji, R.drawable.transparent_btn,
                                PopUp.TYPE_PROGRESS_NOT_CANCELABLE, null, null);

                        inProgress = true;
                    }

                    PopUp.updateProgressBar(state);

                }
            } else if (intent.getAction() == DfuService.BROADCAST_ERROR) {
                String error_message = Utils.broadcastGetErrorMessage(intent.getIntExtra(DfuService.EXTRA_DATA, 0));
                setActivityState(ACTIVITY_STATE.STATE_IDLE);
                logi("DFUResultReceiver.onReceive() :: Flashing ERROR!!  Code - [" + intent.getIntExtra(DfuService.EXTRA_DATA, 0)
                        + "] Error Type - [" + intent.getIntExtra(DfuService.EXTRA_ERROR_TYPE, 0) + "]");
                //Update Stats
                if (MBApp.getApp().getEcho() != null) {
                    //TODO add more data action_location (app/web), hex_file_size, binary_size, firmware <Micro:bit firmware version >
                    MBApp.getApp().getEcho().userActionEvent("hex_file_flash", "fail", null);
                }
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
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
