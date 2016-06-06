package com.samsung.microbit.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
import android.text.Spannable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.samsung.microbit.BuildConfig;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.Project;
import com.samsung.microbit.presentation.AppInfoPresenter;
import com.samsung.microbit.service.DfuService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.ui.BluetoothSwitch;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.adapter.ProjectAdapter;
import com.samsung.microbit.utils.ErrorUtils;
import com.samsung.microbit.utils.FileUtils;
import com.samsung.microbit.utils.PreferenceUtils;
import com.samsung.microbit.utils.UnpackUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ProjectActivity extends Activity implements View.OnClickListener {

    List<Project> projectList = new ArrayList<Project>();
    ProjectAdapter projectAdapter;
    private ListView projectListView;
    private HashMap<String, String> prettyFileNameMap = new HashMap<String, String>();

    Project programToSend;

    private String m_HexFileSizeStats = "0";
    private String m_BinSizeStats = "0";
    private String m_MicroBitFirmware = "0.0";


    private DFUResultReceiver dfuResultReceiver;
    private int projectListSortOrder = 0;

    protected boolean debug = BuildConfig.DEBUG;
    protected String TAG = "ProjectActivity";

    private static ACTIVITY_STATE mActivityState = ACTIVITY_STATE.STATE_IDLE;

    private List<Integer> mRequestPermission = new ArrayList<Integer>();

    private int mRequestingPermission = -1;


    private enum ACTIVITY_STATE {
        STATE_IDLE,
        STATE_ENABLE_BT_INTERNAL_FLASH_REQUEST,
        STATE_ENABLE_BT_EXTERNAL_FLASH_REQUEST,
        STATE_ENABLE_BT_CONNECT,
        FLASH_STATE_FIND_DEVICE,
        FLASH_STATE_VERIFY_DEVICE,
        FLASH_STATE_WAIT_DEVICE_REBOOT,
        FLASH_STATE_INIT_DEVICE,
        FLASH_STATE_PROGRESS,
        MICROBIT_CONNECTING,
        MICROBIT_DISCONNECTING
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
    private BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            int error = intent.getIntExtra(IPCMessageManager.BUNDLE_ERROR_CODE, 0);
            String firmware = intent.getStringExtra(IPCMessageManager.BUNDLE_MICROBIT_FIRMWARE);
            int getNotification = intent.getIntExtra(IPCMessageManager.BUNDLE_MICROBIT_REQUESTS, -1);

            setConnectedDeviceText();
            if (firmware != null && !firmware.isEmpty()) {
                BluetoothUtils.updateFirmwareMicrobit(context, firmware);
                return;
            }

            if (mActivityState == ACTIVITY_STATE.MICROBIT_CONNECTING || mActivityState == ACTIVITY_STATE.MICROBIT_DISCONNECTING) {

                if (getNotification == IPCMessageManager.IPC_NOTIFICATION_INCOMING_CALL_REQUESTED ||
                        getNotification == IPCMessageManager.IPC_NOTIFICATION_INCOMING_SMS_REQUESTED) {
                    logi("micro:bit application needs more permissions");
                    mRequestPermission.add(getNotification);
                    return;
                }
                ConnectedDevice device = BluetoothUtils.getPairedMicrobit(context);
                if (mActivityState == ACTIVITY_STATE.MICROBIT_CONNECTING) {
                    if (error == 0) {
                        MBApp.getApp().getEchoClientManager().sendConnectStats(Constants.CONNECTION_STATE.SUCCESS, device.mfirmware_version, null);
                        BluetoothUtils.updateConnectionStartTime(context, System.currentTimeMillis());
                        //Check if more permissions were needed and request in the Application
                        if (!mRequestPermission.isEmpty()) {
                            setActivityState(ACTIVITY_STATE.STATE_IDLE);
                            PopUp.hide();
                            checkTelephonyPermissions();
                            return;
                        }
                    } else {
                        MBApp.getApp().getEchoClientManager().sendConnectStats(Constants.CONNECTION_STATE.FAIL, null, null);
                    }
                }
                if (error == 0 && mActivityState == ACTIVITY_STATE.MICROBIT_DISCONNECTING) {
                    long now = System.currentTimeMillis();
                    long connectionTime = (now - device.mlast_connection_time) / 1000; //Time in seconds
                    MBApp.getApp().getEchoClientManager().sendConnectStats(Constants.CONNECTION_STATE.DISCONNECT, device.mfirmware_version, Long.toString(connectionTime));
                }

                setActivityState(ACTIVITY_STATE.STATE_IDLE);
                PopUp.hide();

                if (error != 0) {
                    String message = intent.getStringExtra(IPCMessageManager.BUNDLE_ERROR_MESSAGE);
                    logi("localBroadcastReceiver Error message = " + message);
                    if (message == null)
                        message = "Error";
                    final String displayTitle = message;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            PopUp.show(MBApp.getContext(),
                                    MBApp.getContext().getString(R.string.micro_bit_reset_msg),
                                    MBApp.getContext().getString(R.string.general_error_title),
                                    R.drawable.error_face, R.drawable.red_btn,
                                    PopUp.GIFF_ANIMATION_ERROR,
                                    PopUp.TYPE_ALERT, null, null);
                        }
                    });
                }
            }
        }
    };

    View.OnClickListener notificationOKHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("notificationOKHandler");
            PopUp.hide();
            if (mRequestingPermission == IPCMessageManager.IPC_NOTIFICATION_INCOMING_CALL_REQUESTED) {
                String[] permissionsNeeded = {Manifest.permission.READ_PHONE_STATE};
                requetPermission(permissionsNeeded, Constants.INCOMING_CALL_PERMISSIONS_REQUESTED);
            }
            if (mRequestingPermission == IPCMessageManager.IPC_NOTIFICATION_INCOMING_SMS_REQUESTED) {
                String[] permissionsNeeded = {Manifest.permission.RECEIVE_SMS};
                requetPermission(permissionsNeeded, Constants.INCOMING_SMS_PERMISSIONS_REQUESTED);
            }
        }
    };

    View.OnClickListener checkMorePermissionsNeeded = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mRequestPermission.isEmpty()) {
                checkTelephonyPermissions();
            } else {
                PopUp.hide();
            }
        }
    };

    View.OnClickListener notificationCancelHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("notificationCancelHandler");
            String msg = "Your program might not run properly";
            if (mRequestingPermission == IPCMessageManager.IPC_NOTIFICATION_INCOMING_CALL_REQUESTED) {
                msg = getString(R.string.telephony_permission_error);
            } else if (mRequestingPermission == IPCMessageManager.IPC_NOTIFICATION_INCOMING_SMS_REQUESTED) {
                msg = getString(R.string.sms_permission_error);
            }
            PopUp.hide();
            PopUp.show(MBApp.getContext(),
                    msg,
                    getString(R.string.permissions_needed_title),
                    R.drawable.error_face, R.drawable.red_btn,
                    PopUp.GIFF_ANIMATION_ERROR,
                    PopUp.TYPE_ALERT,
                    checkMorePermissionsNeeded, checkMorePermissionsNeeded);
        }
    };

    View.OnClickListener diskStoragePermissionOKHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("diskStoragePermissionOKHandler");
            PopUp.hide();
            String[] permissionsNeeded = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            requetPermission(permissionsNeeded, Constants.APP_STORAGE_PERMISSIONS_REQUESTED);
        }
    };

    private AppInfoPresenter appInfoPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logi("onCreate() :: ");
        MBApp.setContext(this);

        appInfoPresenter = new AppInfoPresenter();

        appInfoPresenter.start();

        // Make sure to call this before any other userActionEvent is sent
        MBApp.getApp().getEchoClientManager().sendViewEventStats("projectactivity");

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
        TextView createProjectText = (TextView) findViewById(R.id.custom_button_text);
        createProjectText.setTypeface(MBApp.getApp().getTypeface());

        projectListView = (ListView) findViewById(R.id.projectListView);
        checkMinimumPermissionsForThisScreen();

		/* *************************************************
         * TODO setup to Handle BLE Notification
		 */
        if (broadcastIntentFilter == null) {
            broadcastIntentFilter = new IntentFilter(IPCService.INTENT_BLE_NOTIFICATION);
            LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(localBroadcastReceiver, broadcastIntentFilter);
        }
        setConnectedDeviceText();
        String fullpathoffile = null;
        String fileName = null;
        if (getIntent() != null && getIntent().getData() != null && getIntent().getData().getEncodedPath() != null) {
            fullpathoffile = getIntent().getData().getEncodedPath();
            String path[] = fullpathoffile.split("/");
            fileName = path[path.length - 1];
            setActivityState(ACTIVITY_STATE.STATE_ENABLE_BT_EXTERNAL_FLASH_REQUEST);
        }
        if (fullpathoffile != null) {
            programToSend = new Project(fileName, fullpathoffile, 0, null, false);
            if (!BluetoothSwitch.getInstance().isBluetoothON()) {
                startBluetooth();
            } else {
                adviceOnMicrobitState();
            }
        }
    }

    @Override
    protected void onDestroy() {
        appInfoPresenter.destroy();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        MBApp.setContext(this);
    }

    private void checkTelephonyPermissions() {
        if (!mRequestPermission.isEmpty()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PermissionChecker.PERMISSION_GRANTED ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PermissionChecker.PERMISSION_GRANTED)) {
                mRequestingPermission = mRequestPermission.get(0);
                mRequestPermission.remove(0);
                PopUp.show(MBApp.getContext(),
                        (mRequestingPermission == IPCMessageManager.IPC_NOTIFICATION_INCOMING_CALL_REQUESTED) ? getString(R.string.telephony_permission) : getString(R.string.sms_permission),
                        getString(R.string.permissions_needed_title),
                        R.drawable.message_face, R.drawable.blue_btn, PopUp.GIFF_ANIMATION_NONE,
                        PopUp.TYPE_CHOICE,
                        notificationOKHandler,
                        notificationCancelHandler);
            }
        }
    }

    private void setActivityState(ACTIVITY_STATE newState) {
        logi("Flash state old - " + mActivityState + " new - " + newState);
        mActivityState = newState;
        setConnectedDeviceText();
    }

    private void requetPermission(String[] permissions, final int requestCode) {
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.APP_STORAGE_PERMISSIONS_REQUESTED: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    updateProjectsListSortOrder(true);
                } else {
                    PopUp.show(MBApp.getContext(),
                            getString(R.string.storage_permission_for_programs_error),
                            getString(R.string.permissions_needed_title),
                            R.drawable.error_face, R.drawable.red_btn,
                            PopUp.GIFF_ANIMATION_ERROR,
                            PopUp.TYPE_ALERT,
                            null, null);
                }
            }
            case Constants.INCOMING_CALL_PERMISSIONS_REQUESTED: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    PopUp.show(MBApp.getContext(),
                            getString(R.string.telephony_permission_error),
                            getString(R.string.permissions_needed_title),
                            R.drawable.error_face, R.drawable.red_btn,
                            PopUp.GIFF_ANIMATION_ERROR,
                            PopUp.TYPE_ALERT,
                            checkMorePermissionsNeeded, checkMorePermissionsNeeded);
                } else {
                    if (!mRequestPermission.isEmpty()) {
                        checkTelephonyPermissions();
                    }
                }
            }
            break;
            case Constants.INCOMING_SMS_PERMISSIONS_REQUESTED: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    PopUp.show(MBApp.getContext(),
                            getString(R.string.sms_permission_error),
                            getString(R.string.permissions_needed_title),
                            R.drawable.error_face, R.drawable.red_btn,
                            PopUp.GIFF_ANIMATION_ERROR,
                            PopUp.TYPE_ALERT,
                            checkMorePermissionsNeeded, checkMorePermissionsNeeded);
                } else {
                    if (!mRequestPermission.isEmpty()) {
                        checkTelephonyPermissions();
                    }
                }
            }
            break;
        }
    }

    View.OnClickListener diskStoragePermissionCancelHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("diskStoragePermissionCancelHandler");
            PopUp.hide();
            PopUp.show(MBApp.getContext(),
                    getString(R.string.storage_permission_for_programs_error),
                    getString(R.string.permissions_needed_title),
                    R.drawable.error_face, R.drawable.red_btn,
                    PopUp.GIFF_ANIMATION_ERROR,
                    PopUp.TYPE_ALERT,
                    null, null);
        }
    };

    private void checkMinimumPermissionsForThisScreen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED)) {
            PopUp.show(MBApp.getContext(),
                    getString(R.string.storage_permission_for_programs),
                    getString(R.string.permissions_needed_title),
                    R.drawable.message_face, R.drawable.blue_btn, PopUp.GIFF_ANIMATION_NONE,
                    PopUp.TYPE_CHOICE,
                    diskStoragePermissionOKHandler,
                    diskStoragePermissionCancelHandler);
        } else {
            //We have required permission. Update the list directly
            updateProjectsListSortOrder(true);
        }
    }

    private void setConnectedDeviceText() {

        TextView connectedIndicatorText = (TextView) findViewById(R.id.connectedIndicatorText);
        connectedIndicatorText.setText(connectedIndicatorText.getText());
        connectedIndicatorText.setTypeface(MBApp.getApp().getTypeface());
        TextView deviceName1 = (TextView) findViewById(R.id.deviceName);
        deviceName1.setContentDescription(deviceName1.getText());
        deviceName1.setTypeface(MBApp.getApp().getTypeface());
        deviceName1.setOnClickListener(this);
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
            connectedIndicatorIcon.setImageResource(R.drawable.device_status_connected);
            connectedIndicatorText.setText(getString(R.string.connected_to));

            return;
        }
        int startIndex = 0;
        Spannable span = null;
        ConnectedDevice device = BluetoothUtils.getPairedMicrobit(this);
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
            connectedIndicatorIcon.setImageResource(R.drawable.device_status_connected);
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

        FileUtils.RenameResult renameResult = FileUtils.renameFile(filePath, newName);
        if (renameResult != FileUtils.RenameResult.SUCCESS) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Alert");

            String message = "OOPS!";
            switch (renameResult) {
                case NEW_PATH_ALREADY_EXIST:
                    message = "Cannot rename, destination file already exists.";
                    break;

                case OLD_PATH_NOT_CORRECT:
                    message = "Cannot rename, source file not exist.";
                    break;

                case RENAME_ERROR:
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
            int totalPrograms = UnpackUtils.findProgramsAndPopulate(prettyFileNameMap, projectList);
        }

        projectListSortOrder = PreferenceUtils.getListOrderPrefs();
        int sortBy = (projectListSortOrder >> 1);
        int sortOrder = projectListSortOrder & 0x01;
        com.samsung.microbit.utils.Utils.sortProjectList(projectList, sortBy, sortOrder);

        projectAdapter = new ProjectAdapter(this, projectList);
        projectListView.setAdapter(projectAdapter);
        projectListView.setItemsCanFocus(true);
    }

    void projectListSortOrderChanged() {
        PreferenceUtils.setListOrderPrefs(projectListSortOrder);
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
                        PopUp.GIFF_ANIMATION_ERROR,
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
        ConnectedDevice connectedDevice = BluetoothUtils.getPairedMicrobit(this);
        if (connectedDevice.mPattern != null) {
            if (connectedDevice.mStatus) {
                setActivityState(ACTIVITY_STATE.MICROBIT_DISCONNECTING);
                PopUp.show(MBApp.getContext(),
                        getString(R.string.disconnecting),
                        "",
                        R.drawable.flash_face, R.drawable.blue_btn,
                        PopUp.GIFF_ANIMATION_NONE,
                        PopUp.TYPE_SPINNER_NOT_CANCELABLE,
                        null, null);
                IPCService.getInstance().bleDisconnect();
            } else {
                mRequestPermission.clear();
                setActivityState(ACTIVITY_STATE.MICROBIT_CONNECTING);
                PopUp.show(MBApp.getContext(),
                        getString(R.string.init_connection),
                        "",
                        R.drawable.flash_face, R.drawable.blue_btn,
                        PopUp.GIFF_ANIMATION_NONE,
                        PopUp.TYPE_SPINNER_NOT_CANCELABLE,
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
                MBApp.getApp().getEchoClientManager().sendNavigationStats("home", "my-scripts");
                String url = MBApp.getApp().getAppInfo().getMyScriptsURL();
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
            case R.id.deviceName:
                // Toast.makeText(this, "Back to connect screen", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, PairingActivity.class);
                startActivity(intent);
                break;

        }
    }

    // TODO fonts on pop up
    private void adviceOnMicrobitState() {
        ConnectedDevice currentMicrobit = BluetoothUtils.getPairedMicrobit(this);

        if (currentMicrobit.mPattern == null) {
            PopUp.show(MBApp.getContext(),
                    getString(R.string.flashing_failed_no_microbit), //message
                    getString(R.string.flashing_error), //title
                    R.drawable.error_face,//image icon res id
                    R.drawable.red_btn,
                    PopUp.GIFF_ANIMATION_ERROR,
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
                        PopUp.GIFF_ANIMATION_ERROR,
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
                // Another download session is in progress.xml
                PopUp.show(MBApp.getContext(),
                        getString(R.string.multple_flashing_session_msg),
                        "",
                        R.drawable.flash_face, R.drawable.blue_btn,
                        PopUp.GIFF_ANIMATION_FLASH,
                        PopUp.TYPE_ALERT,
                        null, null);
                return;
            }
            if (mActivityState == ACTIVITY_STATE.STATE_ENABLE_BT_INTERNAL_FLASH_REQUEST ||
                    mActivityState == ACTIVITY_STATE.STATE_ENABLE_BT_EXTERNAL_FLASH_REQUEST) {
                //Check final device from user and start flashing
                PopUp.show(MBApp.getContext(),
                        getString(R.string.flash_start_message, currentMicrobit.mName), //message
                        getString(R.string.flashing_title), //title
                        R.drawable.flash_face, R.drawable.blue_btn, //image icon res id
                        PopUp.GIFF_ANIMATION_NONE,
                        PopUp.TYPE_CHOICE, //type of popup.
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ConnectedDevice currentMicrobit = BluetoothUtils.getPairedMicrobit(MBApp.getContext());
                                PopUp.hide();
                                initiateFlashing();
                            }
                        },//override click listener for ok button
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PopUp.hide();
                            }
                        });//pass null to use default listeneronClick
            } else if (mActivityState == ACTIVITY_STATE.STATE_ENABLE_BT_EXTERNAL_FLASH_REQUEST) {
                initiateFlashing();
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
        //Reset all stats value
        m_BinSizeStats = "0";
        m_MicroBitFirmware = "0.0";
        m_HexFileSizeStats = FileUtils.getFileSize(programToSend.filePath);

        ConnectedDevice currentMicrobit = BluetoothUtils.getPairedMicrobit(this);
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
        IntentFilter filter2 = new IntentFilter(DfuService.BROADCAST_LOG);
        dfuResultReceiver = new DFUResultReceiver();
        LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(dfuResultReceiver, filter);
        LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(dfuResultReceiver, filter1);
        LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(dfuResultReceiver, filter2);
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
            PopUp.hide();
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
                                    PopUp.GIFF_ANIMATION_FLASH,
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
                                MBApp.getApp().getEchoClientManager().sendFlashStats(true, programToSend.name, m_HexFileSizeStats, m_BinSizeStats, m_MicroBitFirmware);
                                PopUp.show(MBApp.getContext(),
                                        getString(R.string.flashing_success_message), //message
                                        getString(R.string.flashing_success_title), //title
                                        R.drawable.message_face, R.drawable.blue_btn,
                                        PopUp.GIFF_ANIMATION_NONE,
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
                            break;

                        case DfuService.PROGRESS_CONNECTING:
                            if ((!inInit) && (!isCompleted)) {
                                setActivityState(ACTIVITY_STATE.FLASH_STATE_INIT_DEVICE);
                                PopUp.show(MBApp.getContext(),
                                        getString(R.string.init_connection), //message
                                        getString(R.string.send_project), //title
                                        R.drawable.flash_face, R.drawable.blue_btn,
                                        PopUp.GIFF_ANIMATION_FLASH,
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
                                    PopUp.GIFF_ANIMATION_FLASH,
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
                                    PopUp.GIFF_ANIMATION_FLASH,
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
                            MBApp.getApp().getEchoClientManager().sendFlashStats(false, programToSend.name, m_HexFileSizeStats, m_BinSizeStats, m_MicroBitFirmware);
                            PopUp.show(MBApp.getContext(),
                                    getString(R.string.flashing_verifcation_failed), //message
                                    getString(R.string.flashing_verifcation_failed_title),
                                    R.drawable.error_face, R.drawable.red_btn,
                                    PopUp.GIFF_ANIMATION_ERROR,
                                    PopUp.TYPE_ALERT, //type of popup.
                                    popupOkHandler,//override click listener for ok button
                                    popupOkHandler);//pass null to use default listener

                            LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
                            dfuResultReceiver = null;
                            break;
                        case DfuService.PROGRESS_ABORTED:
                            setActivityState(ACTIVITY_STATE.STATE_IDLE);
                            //Update Stats
                            MBApp.getApp().getEchoClientManager().sendFlashStats(false, programToSend.name, m_HexFileSizeStats, m_BinSizeStats, m_MicroBitFirmware);
                            PopUp.show(MBApp.getContext(),
                                    getString(R.string.flashing_aborted), //message
                                    getString(R.string.flashing_aborted_title),
                                    R.drawable.error_face, R.drawable.red_btn,
                                    PopUp.GIFF_ANIMATION_ERROR,
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
                                R.drawable.flash_modal_emoji, 0,
                                PopUp.GIFF_ANIMATION_FLASH,
                                PopUp.TYPE_PROGRESS_NOT_CANCELABLE, null, null);

                        inProgress = true;
                    }

                    PopUp.updateProgressBar(state);

                }
            } else if (intent.getAction() == DfuService.BROADCAST_ERROR) {
                String error_message = ErrorUtils.broadcastGetErrorMessage(intent.getIntExtra(DfuService.EXTRA_DATA, 0));

                logi("DFUResultReceiver.onReceive() :: Flashing ERROR!!  Code - [" + intent.getIntExtra(DfuService.EXTRA_DATA, 0)
                        + "] Error Type - [" + intent.getIntExtra(DfuService.EXTRA_ERROR_TYPE, 0) + "]");

                setActivityState(ACTIVITY_STATE.STATE_IDLE);
                LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
                dfuResultReceiver = null;
                //Update Stats
                MBApp.getApp().getEchoClientManager().sendFlashStats(false, programToSend.name, m_HexFileSizeStats, m_BinSizeStats, m_MicroBitFirmware);
                PopUp.show(MBApp.getContext(),
                        error_message, //message
                        getString(R.string.flashing_failed_title), //title
                        R.drawable.error_face, R.drawable.red_btn,
                        PopUp.GIFF_ANIMATION_ERROR,
                        PopUp.TYPE_ALERT, //type of popup.
                        popupOkHandler,//override click listener for ok button
                        popupOkHandler);//pass null to use default listener
            } else if (intent.getAction() == DfuService.BROADCAST_LOG) {
                //Only used for Stats at the moment
                String data = "";
                int logLevel = intent.getIntExtra(DfuService.EXTRA_LOG_LEVEL, 0);
                switch (logLevel) {
                    case DfuService.LOG_LEVEL_BINARY_SIZE:
                        data = intent.getStringExtra(DfuService.EXTRA_DATA);
                        m_BinSizeStats = data;
                        break;
                    case DfuService.LOG_LEVEL_FIRMWARE:
                        data = intent.getStringExtra(DfuService.EXTRA_DATA);
                        m_MicroBitFirmware = data;
                        break;
                }
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
