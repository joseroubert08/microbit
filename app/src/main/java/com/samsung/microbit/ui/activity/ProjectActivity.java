package com.samsung.microbit.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.PermissionCodes;
import com.samsung.microbit.data.constants.RequestCodes;
import com.samsung.microbit.data.model.ConnectedDevice;
import com.samsung.microbit.data.model.Project;
import com.samsung.microbit.data.model.ui.FlashActivityState;
import com.samsung.microbit.presentation.ConfigInfoPresenter;
import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.service.DfuService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.ui.BluetoothChecker;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.adapter.ProjectAdapter;
import com.samsung.microbit.utils.FileUtils;
import com.samsung.microbit.utils.BLEConnectionHandler;
import com.samsung.microbit.utils.PreferenceUtils;
import com.samsung.microbit.utils.UnpackUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import no.nordicsemi.android.error.GattError;

import static com.samsung.microbit.BuildConfig.DEBUG;


public class ProjectActivity extends Activity implements View.OnClickListener, BLEConnectionHandler.BLEConnectionManager {
    private static final String TAG = ProjectActivity.class.getSimpleName();

    private static final int ALERT_DIALOG_RECONNECT = 1;

    private List<Project> mProjectList = new ArrayList<>();
    private ListView mProjectListView;
    private ListView mProjectListViewRight;
    private HashMap<String, String> mPrettyFileNameMap = new HashMap<>();

    private Project mProgramToSend;

    private String m_HexFileSizeStats = "0";
    private String m_BinSizeStats = "0";
    private String m_MicroBitFirmware = "0.0";

    private DFUResultReceiver dfuResultReceiver;

    private List<Integer> mRequestPermissions = new ArrayList<>();

    private int mRequestingPermission = -1;

    private int mActivityState;

    private BroadcastReceiver connectionChangedReceiver = BLEConnectionHandler.bleConnectionChangedReceiver(this);

    private final BroadcastReceiver gattForceClosedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BLEService.GATT_FORCE_CLOSED)) {
                setConnectedDeviceText();
            }
        }
    };

    View.OnClickListener notificationOKHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("notificationOKHandler");
            PopUp.hide();
            if (mRequestingPermission == EventCategories.IPC_BLE_NOTIFICATION_INCOMING_CALL) {
                String[] permissionsNeeded = {Manifest.permission.READ_PHONE_STATE};
                requestPermission(permissionsNeeded, PermissionCodes.INCOMING_CALL_PERMISSIONS_REQUESTED);
            }
            if (mRequestingPermission == EventCategories.IPC_BLE_NOTIFICATION_INCOMING_SMS) {
                String[] permissionsNeeded = {Manifest.permission.RECEIVE_SMS};
                requestPermission(permissionsNeeded, PermissionCodes.INCOMING_SMS_PERMISSIONS_REQUESTED);
            }
        }
    };

    View.OnClickListener checkMorePermissionsNeeded = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mRequestPermissions.isEmpty()) {
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
            if (mRequestingPermission == EventCategories.IPC_BLE_NOTIFICATION_INCOMING_CALL) {
                msg = getString(R.string.telephony_permission_error);
            } else if (mRequestingPermission == EventCategories.IPC_BLE_NOTIFICATION_INCOMING_SMS) {
                msg = getString(R.string.sms_permission_error);
            }
            PopUp.hide();
            PopUp.show(msg,
                    getString(R.string.permissions_needed_title),
                    R.drawable.error_face, R.drawable.red_btn,
                    PopUp.GIFF_ANIMATION_ERROR,
                    PopUp.TYPE_ALERT,
                    checkMorePermissionsNeeded, checkMorePermissionsNeeded);
        }
    };

    @Override
    public void setActivityState(int baseActivityState) {
        mActivityState = baseActivityState;
        setConnectedDeviceText();
    }

    @Override
    public void preUpdateUi() {
        setConnectedDeviceText();
    }

    @Override
    public int getActivityState() {
        return mActivityState;
    }

    @Override
    public void logi(String message) {
        if (DEBUG) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    @Override
    public void checkTelephonyPermissions() {
        if (!mRequestPermissions.isEmpty()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                    != PermissionChecker.PERMISSION_GRANTED ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                            != PermissionChecker.PERMISSION_GRANTED)) {
                mRequestingPermission = mRequestPermissions.get(0);
                mRequestPermissions.remove(0);
                PopUp.show((mRequestingPermission == EventCategories.IPC_BLE_NOTIFICATION_INCOMING_CALL)
                                ? getString(R.string.telephony_permission)
                                : getString(R.string.sms_permission),
                        getString(R.string.permissions_needed_title),
                        R.drawable.message_face, R.drawable.blue_btn, PopUp.GIFF_ANIMATION_NONE,
                        PopUp.TYPE_CHOICE,
                        notificationOKHandler,
                        notificationCancelHandler);
            }
        }
    }

    @Override
    public void addPermissionRequest(int permission) {
        mRequestPermissions.add(permission);
    }

    @Override
    public boolean arePermissionsGranted() {
        return mRequestPermissions.isEmpty();
    }

    private ConfigInfoPresenter configInfoPresenter;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setContentView(R.layout.activity_projects);
        initViews();
        setupFontStyle();
        setConnectedDeviceText();
        setupListAdapter();
    }

    /**
     * Setup font style by setting an appropriate typeface to needed views.
     */
    private void setupFontStyle() {
        // Title font
        TextView flashProjectsTitle = (TextView) findViewById(R.id.flash_projects_title_txt);
        flashProjectsTitle.setTypeface(MBApp.getApp().getTypeface());

        // Create projects
        TextView createProjectText = (TextView) findViewById(R.id.custom_button_text);
        createProjectText.setTypeface(MBApp.getApp().getRobotoTypeface());
    }

    private void initViews() {
        mProjectListView = (ListView) findViewById(R.id.projectListView);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mProjectListViewRight = (ListView) findViewById(R.id.projectListViewRight);
        }
    }

    private void releaseViews() {
        mProjectListView = null;
        mProjectListViewRight = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mActivityState = FlashActivityState.STATE_IDLE;
        }

        logi("onCreate() :: ");

        configInfoPresenter = new ConfigInfoPresenter();

        configInfoPresenter.start();

        MBApp application = MBApp.getApp();

        // Make sure to call this before any other userActionEvent is sent
        application.getEchoClientManager().sendViewEventStats("projectactivity");

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_projects);
        initViews();
        setupFontStyle();
        checkMinimumPermissionsForThisScreen();

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(application);

        IntentFilter broadcastIntentFilter = new IntentFilter(IPCService.INTENT_BLE_NOTIFICATION);
        localBroadcastManager.registerReceiver(connectionChangedReceiver, broadcastIntentFilter);

        localBroadcastManager.registerReceiver(gattForceClosedReceiver, new IntentFilter(BLEService.GATT_FORCE_CLOSED));

        setConnectedDeviceText();
        String fullPathOfFile = null;
        String fileName = null;
        if (getIntent() != null && getIntent().getData() != null && getIntent().getData().getEncodedPath() != null) {
            fullPathOfFile = getIntent().getData().getEncodedPath();
            String path[] = fullPathOfFile.split("/");
            fileName = path[path.length - 1];
            setActivityState(FlashActivityState.STATE_ENABLE_BT_EXTERNAL_FLASH_REQUEST);
        }
        if (fullPathOfFile != null) {
            mProgramToSend = new Project(fileName, fullPathOfFile, 0, null, false);
            if (!BluetoothChecker.getInstance().isBluetoothON()) {
                startBluetooth();
            } else {
                adviceOnMicrobitState();
            }
        }
    }

    @Override
    protected void onDestroy() {
        configInfoPresenter.destroy();

        MBApp application = MBApp.getApp();

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(application);

        localBroadcastManager.unregisterReceiver(gattForceClosedReceiver);
        localBroadcastManager.unregisterReceiver(connectionChangedReceiver);

        application.stopService(new Intent(application, DfuService.class));

        super.onDestroy();
        releaseViews();
    }

    private void requestPermission(String[] permissions, final int requestCode) {
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    View.OnClickListener diskStoragePermissionOKHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("diskStoragePermissionOKHandler");
            PopUp.hide();
            String[] permissionsNeeded = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            requestPermission(permissionsNeeded, PermissionCodes.APP_STORAGE_PERMISSIONS_REQUESTED);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionCodes.APP_STORAGE_PERMISSIONS_REQUESTED: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    updateProjectsListSortOrder(true);
                } else {
                    PopUp.show(getString(R.string.storage_permission_for_programs_error),
                            getString(R.string.permissions_needed_title),
                            R.drawable.error_face, R.drawable.red_btn,
                            PopUp.GIFF_ANIMATION_ERROR,
                            PopUp.TYPE_ALERT,
                            null, null);
                }
            }
            break;
            case PermissionCodes.INCOMING_CALL_PERMISSIONS_REQUESTED: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    PopUp.show(getString(R.string.telephony_permission_error),
                            getString(R.string.permissions_needed_title),
                            R.drawable.error_face, R.drawable.red_btn,
                            PopUp.GIFF_ANIMATION_ERROR,
                            PopUp.TYPE_ALERT,
                            checkMorePermissionsNeeded, checkMorePermissionsNeeded);
                } else {
                    if (!mRequestPermissions.isEmpty()) {
                        checkTelephonyPermissions();
                    }
                }
            }
            break;
            case PermissionCodes.INCOMING_SMS_PERMISSIONS_REQUESTED: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    PopUp.show(
                            getString(R.string.sms_permission_error),
                            getString(R.string.permissions_needed_title),
                            R.drawable.error_face, R.drawable.red_btn,
                            PopUp.GIFF_ANIMATION_ERROR,
                            PopUp.TYPE_ALERT,
                            checkMorePermissionsNeeded, checkMorePermissionsNeeded);
                } else {
                    if (!mRequestPermissions.isEmpty()) {
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
            PopUp.show(
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
            PopUp.show(
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
        connectedIndicatorText.setTypeface(MBApp.getApp().getRobotoTypeface());
        TextView deviceName = (TextView) findViewById(R.id.deviceName);
        deviceName.setContentDescription(deviceName.getText());
        deviceName.setTypeface(MBApp.getApp().getRobotoTypeface());
        deviceName.setOnClickListener(this);
        ImageView connectedIndicatorIcon = (ImageView) findViewById(R.id.connectedIndicatorIcon);

        //Override the connection Icon in case of active flashing
        if (mActivityState == FlashActivityState.FLASH_STATE_FIND_DEVICE
                || mActivityState == FlashActivityState.FLASH_STATE_VERIFY_DEVICE
                || mActivityState == FlashActivityState.FLASH_STATE_WAIT_DEVICE_REBOOT
                || mActivityState == FlashActivityState.FLASH_STATE_INIT_DEVICE
                || mActivityState == FlashActivityState.FLASH_STATE_PROGRESS

                ) {
            connectedIndicatorIcon.setImageResource(R.drawable.device_status_connected);
            connectedIndicatorText.setText(getString(R.string.connected_to));

            return;
        }
        ConnectedDevice device = BluetoothUtils.getPairedMicrobit(this);
        if (!device.mStatus) {
            connectedIndicatorIcon.setImageResource(R.drawable.device_status_disconnected);
            connectedIndicatorText.setText(getString(R.string.not_connected));
            if (device.mName != null) {
                deviceName.setText(device.mName);
            } else {
                deviceName.setText("");
            }
        } else {
            connectedIndicatorIcon.setImageResource(R.drawable.device_status_connected);
            connectedIndicatorText.setText(getString(R.string.connected_to));
            if (device.mName != null) {
                deviceName.setText(device.mName);
            } else {
                deviceName.setText("");
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

    public void updateProjectsListSortOrder(boolean reReadFS) {
        if (reReadFS) {
            mProjectList.clear();
            UnpackUtils.findProgramsAndPopulate(mPrettyFileNameMap, mProjectList);
        }

        int projectListSortOrder = PreferenceUtils.getListOrderPrefs();
        int sortBy = (projectListSortOrder >> 1);
        int sortOrder = projectListSortOrder & 0x01;
        com.samsung.microbit.utils.Utils.sortProjectList(mProjectList, sortBy, sortOrder);

        setupListAdapter();
    }

    /**
     * Sets a list adapter for a list view. If orientation is a landscape then
     * list of items are split up on two lists that will be displayed in two different columns.
     */
    private void setupListAdapter() {
        ProjectAdapter projectAdapter;
        TextView emptyText = (TextView) findViewById(R.id.project_list_empty);
        emptyText.setVisibility(View.GONE);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            List<Project> leftList = new ArrayList<>();
            List<Project> rightList = new ArrayList<>();
            for (int i = 0; i < mProjectList.size(); i++) {
                if (i % 2 == 0) {
                    leftList.add(mProjectList.get(i));
                } else {
                    rightList.add(mProjectList.get(i));
                }
            }
            projectAdapter = new ProjectAdapter(this, leftList);
            ProjectAdapter projectAdapterRight = new ProjectAdapter(this, rightList);
            mProjectListViewRight.setAdapter(projectAdapterRight);
            if (projectAdapter.isEmpty() && projectAdapterRight.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
            }
        } else {
            projectAdapter = new ProjectAdapter(this, mProjectList);
            if (projectAdapter.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
            }
        }
        mProjectListView.setAdapter(projectAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RequestCodes.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                if (mActivityState == FlashActivityState.STATE_ENABLE_BT_INTERNAL_FLASH_REQUEST ||
                        mActivityState == FlashActivityState.STATE_ENABLE_BT_EXTERNAL_FLASH_REQUEST) {
                    adviceOnMicrobitState();
                } else if (mActivityState == FlashActivityState.STATE_ENABLE_BT_FOR_CONNECT) {
                    setActivityState(FlashActivityState.STATE_IDLE);
                    toggleConnection();
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                setActivityState(FlashActivityState.STATE_IDLE);
                PopUp.show(
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
        startActivityForResult(enableBtIntent, RequestCodes.REQUEST_ENABLE_BT);
    }

    private void toggleConnection() {
        ConnectedDevice connectedDevice = BluetoothUtils.getPairedMicrobit(this);
        if (connectedDevice.mPattern != null) {
            if (connectedDevice.mStatus) {
                setActivityState(FlashActivityState.STATE_DISCONNECTING);
                PopUp.show(
                        getString(R.string.disconnecting),
                        "",
                        R.drawable.flash_face, R.drawable.blue_btn,
                        PopUp.GIFF_ANIMATION_NONE,
                        PopUp.TYPE_SPINNER,
                        null, null);
                IPCService.bleDisconnect();
            } else {
                mRequestPermissions.clear();
                setActivityState(FlashActivityState.STATE_CONNECTING);
                PopUp.show(
                        getString(R.string.init_connection),
                        "",
                        R.drawable.flash_face, R.drawable.blue_btn,
                        PopUp.GIFF_ANIMATION_NONE,
                        PopUp.TYPE_SPINNER,
                        null, null);

                IPCService.bleConnect();
            }
        }
    }

    /**
     * Sends a project to flash on a mi0crobit board. If bluetooth is off then turn it on.
     *
     * @param project Project to flash.
     */
    public void sendProject(final Project project) {
        mProgramToSend = project;
        setActivityState(FlashActivityState.STATE_ENABLE_BT_INTERNAL_FLASH_REQUEST);
        if (!BluetoothChecker.getInstance().isBluetoothON()) {
            startBluetooth();
        } else {
            adviceOnMicrobitState();
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.createProject: {
                MBApp.getApp().getEchoClientManager().sendNavigationStats("home", "my-scripts");
                String url = MBApp.getApp().getConfigInfo().getMyScriptsURL();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
            finish();
            break;

            case R.id.backBtn:
                finish();
                break;

            case R.id.connectedIndicatorIcon:
                if (!BluetoothChecker.getInstance().isBluetoothON()) {
                    setActivityState(FlashActivityState.STATE_ENABLE_BT_FOR_CONNECT);
                    startBluetooth();
                } else {
                    toggleConnection();
                }
                break;
            case R.id.deviceName:
                // Toast.makeText(this, "Back to connectMaybeInit screen", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, PairingActivity.class);
                startActivity(intent);
                break;

        }
    }

    // TODO fonts on pop up
    private void adviceOnMicrobitState() {
        ConnectedDevice currentMicrobit = BluetoothUtils.getPairedMicrobit(this);

        if (currentMicrobit.mPattern == null) {
            PopUp.show(
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
            if (mProgramToSend == null || mProgramToSend.filePath == null) {
                PopUp.show(
                        getString(R.string.internal_error_msg),
                        "",
                        R.drawable.error_face, R.drawable.red_btn,
                        PopUp.GIFF_ANIMATION_ERROR,
                        PopUp.TYPE_ALERT,
                        null, null);
                return;
            }
            if (mActivityState == FlashActivityState.FLASH_STATE_FIND_DEVICE
                    || mActivityState == FlashActivityState.FLASH_STATE_VERIFY_DEVICE
                    || mActivityState == FlashActivityState.FLASH_STATE_WAIT_DEVICE_REBOOT
                    || mActivityState == FlashActivityState.FLASH_STATE_INIT_DEVICE
                    || mActivityState == FlashActivityState.FLASH_STATE_PROGRESS

                    ) {
                // Another download session is in progress.xml
                PopUp.show(
                        getString(R.string.multple_flashing_session_msg),
                        "",
                        R.drawable.flash_face, R.drawable.blue_btn,
                        PopUp.GIFF_ANIMATION_FLASH,
                        PopUp.TYPE_ALERT,
                        null, null);
                return;
            }
            if (mActivityState == FlashActivityState.STATE_ENABLE_BT_INTERNAL_FLASH_REQUEST ||
                    mActivityState == FlashActivityState.STATE_ENABLE_BT_EXTERNAL_FLASH_REQUEST) {
                //Check final device from user and start flashing
                PopUp.show(
                        getString(R.string.flash_start_message, currentMicrobit.mName), //message
                        getString(R.string.flashing_title), //title
                        R.drawable.flash_face, R.drawable.blue_btn, //image icon res id
                        PopUp.GIFF_ANIMATION_NONE,
                        PopUp.TYPE_CHOICE, //type of popup.
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ConnectedDevice currentMicrobit = BluetoothUtils.getPairedMicrobit(MBApp.getApp());
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
            } else {
                initiateFlashing();
            }
        }
    }

    protected void initiateFlashing() {
        if (dfuResultReceiver != null) {
            LocalBroadcastManager.getInstance(MBApp.getApp()).unregisterReceiver(dfuResultReceiver);
            dfuResultReceiver = null;
        }
        setActivityState(FlashActivityState.FLASH_STATE_FIND_DEVICE);
        registerCallbacksForFlashing();
        startFlashing();
    }

    protected void startFlashing() {
        logi(">>>>>>>>>>>>>>>>>>> startFlashing called  >>>>>>>>>>>>>>>>>>>  ");
        //Reset all stats value
        m_BinSizeStats = "0";
        m_MicroBitFirmware = "0.0";
        m_HexFileSizeStats = FileUtils.getFileSize(mProgramToSend.filePath);

        ConnectedDevice currentMicrobit = BluetoothUtils.getPairedMicrobit(this);

        MBApp application = MBApp.getApp();

        final Intent service = new Intent(application, DfuService.class);
        service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, currentMicrobit.mAddress);
        service.putExtra(DfuService.EXTRA_DEVICE_NAME, currentMicrobit.mPattern);
        service.putExtra(DfuService.EXTRA_DEVICE_PAIR_CODE, currentMicrobit.mPairingCode);
        service.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, DfuService.MIME_TYPE_OCTET_STREAM);
        service.putExtra(DfuService.EXTRA_FILE_PATH, mProgramToSend.filePath); // a path or URI must be provided.
        service.putExtra(DfuService.EXTRA_KEEP_BOND, false);
        service.putExtra(DfuService.INTENT_RESULT_RECEIVER, resultReceiver);
        service.putExtra(DfuService.INTENT_REQUESTED_PHASE, 2);

        application.stopService(service);
        application.startService(service);
    }

    private void registerCallbacksForFlashing() {
        IntentFilter filter = new IntentFilter(DfuService.BROADCAST_PROGRESS);
        IntentFilter filter1 = new IntentFilter(DfuService.BROADCAST_ERROR);
        IntentFilter filter2 = new IntentFilter(DfuService.BROADCAST_LOG);
        dfuResultReceiver = new DFUResultReceiver();

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MBApp.getApp());

        localBroadcastManager.registerReceiver(dfuResultReceiver, filter);
        localBroadcastManager.registerReceiver(dfuResultReceiver, filter1);
        localBroadcastManager.registerReceiver(dfuResultReceiver, filter2);
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

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ALERT_DIALOG_RECONNECT) {
            //Create dialog to reconnect to a micro:bit board after successful flashing.
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            /*dialogBuilder.setTitle(R.string.reconnect_title);
            dialogBuilder.setMessage(R.string.reconnect_text);
            dialogBuilder.setPositiveButton(R.string.reconnect_ok_button, reconnectOnClickListener);*/
            dialogBuilder.setNegativeButton(android.R.string.cancel, reconnectOnClickListener);
            return dialogBuilder.create();
        }
        return super.onCreateDialog(id);
    }

    private DialogInterface.OnClickListener reconnectOnClickListener
            = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    //Do reconnect.
                    toggleConnection();
                    break;
                case Dialog.BUTTON_NEGATIVE:
                case Dialog.BUTTON_NEUTRAL:
                    break;
            }
        }
    };

    class DFUResultReceiver extends BroadcastReceiver {

        private boolean isCompleted = false;
        private boolean inInit = false;
        private boolean inProgress = false;

        private View.OnClickListener popupFinishFlashingHandler = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logi("popupOkHandler");
                PopUp.hide();

                //Show dialog to reconnect to a board.
                showDialog(ALERT_DIALOG_RECONNECT);
            }
        };

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = "Broadcast intent detected " + intent.getAction();
            logi("DFUResultReceiver.onReceive :: " + message);
            if (intent.getAction().equals(DfuService.BROADCAST_PROGRESS)) {

                int state = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
                if (state < 0) {
                    logi("DFUResultReceiver.onReceive :: state -- " + state);
                    switch (state) {
                        case DfuService.PROGRESS_STARTING:
                            setActivityState(FlashActivityState.FLASH_STATE_INIT_DEVICE);
                            PopUp.show(
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
                                setActivityState(FlashActivityState.STATE_IDLE);

                                MBApp application = MBApp.getApp();

                                LocalBroadcastManager.getInstance(application).unregisterReceiver(dfuResultReceiver);
                                dfuResultReceiver = null;
                                //Update Stats
                                application.getEchoClientManager().sendFlashStats(true, mProgramToSend.name,
                                        m_HexFileSizeStats,
                                        m_BinSizeStats, m_MicroBitFirmware);
                                PopUp.show(getString(R.string.flashing_success_message), //message
                                        getString(R.string.flashing_success_title), //title
                                        R.drawable.message_face, R.drawable.blue_btn,
                                        PopUp.GIFF_ANIMATION_NONE,
                                        PopUp.TYPE_ALERT, //type of popup.
                                        popupFinishFlashingHandler,//override click listener for ok button
                                        popupFinishFlashingHandler);//pass null to use default listener
                            }

                            isCompleted = true;
                            inInit = false;
                            inProgress = false;

                            break;
                        case DfuService.PROGRESS_DISCONNECTING:
                            break;

                        case DfuService.PROGRESS_CONNECTING:
                            if ((!inInit) && (!isCompleted)) {
                                setActivityState(FlashActivityState.FLASH_STATE_INIT_DEVICE);
                                PopUp.show(
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
                            setActivityState(FlashActivityState.FLASH_STATE_VERIFY_DEVICE);
                            PopUp.show(
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
                            setActivityState(FlashActivityState.FLASH_STATE_WAIT_DEVICE_REBOOT);
                            PopUp.show(
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
                            setActivityState(FlashActivityState.STATE_IDLE);

                            MBApp application = MBApp.getApp();

                            //Update Stats
                            application.getEchoClientManager().sendFlashStats(false, mProgramToSend.name,
                                    m_HexFileSizeStats,
                                    m_BinSizeStats, m_MicroBitFirmware);
                            PopUp.show(getString(R.string.flashing_verifcation_failed), //message
                                    getString(R.string.flashing_verifcation_failed_title),
                                    R.drawable.error_face, R.drawable.red_btn,
                                    PopUp.GIFF_ANIMATION_ERROR,
                                    PopUp.TYPE_ALERT, //type of popup.
                                    popupOkHandler,//override click listener for ok button
                                    popupOkHandler);//pass null to use default listener

                            LocalBroadcastManager.getInstance(application).unregisterReceiver(dfuResultReceiver);
                            dfuResultReceiver = null;
                            break;
                        case DfuService.PROGRESS_ABORTED:
                            setActivityState(FlashActivityState.STATE_IDLE);

                            application = MBApp.getApp();

                            //Update Stats
                            application.getEchoClientManager().sendFlashStats(false, mProgramToSend.name,
                                    m_HexFileSizeStats,
                                    m_BinSizeStats, m_MicroBitFirmware);
                            PopUp.show(getString(R.string.flashing_aborted), //message
                                    getString(R.string.flashing_aborted_title),
                                    R.drawable.error_face, R.drawable.red_btn,
                                    PopUp.GIFF_ANIMATION_ERROR,
                                    PopUp.TYPE_ALERT, //type of popup.
                                    popupOkHandler,//override click listener for ok button
                                    popupOkHandler);//pass null to use default listener

                            LocalBroadcastManager.getInstance(application).unregisterReceiver(dfuResultReceiver);
                            dfuResultReceiver = null;
                            break;

                    }
                } else if ((state > 0) && (state < 100)) {
                    if (!inProgress) {
                        setActivityState(FlashActivityState.FLASH_STATE_PROGRESS);

                        MBApp application = MBApp.getApp();

                        PopUp.show(application.getString(R.string.flashing_progress_message),
                                String.format(application.getString(R.string.flashing_project), mProgramToSend.name),
                                R.drawable.flash_modal_emoji, 0,
                                PopUp.GIFF_ANIMATION_FLASH,
                                PopUp.TYPE_PROGRESS_NOT_CANCELABLE, null, null);

                        inProgress = true;
                    }

                    PopUp.updateProgressBar(state);

                }
            } else if (intent.getAction().equals(DfuService.BROADCAST_ERROR)) {
                String error_message = GattError.parse(intent.getIntExtra(DfuService.EXTRA_DATA, 0));

                logi("DFUResultReceiver.onReceive() :: Flashing ERROR!!  Code - [" + intent.getIntExtra(DfuService.EXTRA_DATA, 0)
                        + "] Error Type - [" + intent.getIntExtra(DfuService.EXTRA_ERROR_TYPE, 0) + "]");

                setActivityState(FlashActivityState.STATE_IDLE);

                MBApp application = MBApp.getApp();

                LocalBroadcastManager.getInstance(application).unregisterReceiver(dfuResultReceiver);
                dfuResultReceiver = null;
                //Update Stats
                application.getEchoClientManager().sendFlashStats(false, mProgramToSend.name, m_HexFileSizeStats,
                        m_BinSizeStats, m_MicroBitFirmware);
                PopUp.show(error_message, //message
                        getString(R.string.flashing_failed_title), //title
                        R.drawable.error_face, R.drawable.red_btn,
                        PopUp.GIFF_ANIMATION_ERROR,
                        PopUp.TYPE_ALERT, //type of popup.
                        popupOkHandler,//override click listener for ok button
                        popupOkHandler);//pass null to use default listener
            } else if (intent.getAction().equals(DfuService.BROADCAST_LOG)) {
                //Only used for Stats at the moment
                String data;
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
