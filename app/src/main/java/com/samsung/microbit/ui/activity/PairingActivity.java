package com.samsung.microbit.ui.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.constants.PermissionCodes;
import com.samsung.microbit.data.constants.RequestCodes;
import com.samsung.microbit.data.model.ConnectedDevice;
import com.samsung.microbit.data.model.ui.PairingActivityState;
import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.ui.BluetoothChecker;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.adapter.LEDAdapter;
import com.samsung.microbit.utils.BLEConnectionHandler;
import com.samsung.microbit.utils.ServiceUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import pl.droidsonroids.gif.GifImageView;

import static com.samsung.microbit.BuildConfig.DEBUG;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PairingActivity extends Activity implements View.OnClickListener, BluetoothAdapter.LeScanCallback,
        BLEConnectionHandler.BLEConnectionManager {

    // @formatter:off
    private static final int DEVICE_CODE_ARRAY[] = {
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0};

    private static final String DEVICE_NAME_MAP_ARRAY[] = {
            "T", "A", "T", "A", "T",
            "P", "E", "P", "E", "P",
            "G", "I", "G", "I", "G",
            "V", "O", "V", "O", "V",
            "Z", "U", "Z", "U", "Z"};
    // @formatter:on

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 15000;

    /**
     * Allows to navigate through pairing process and
     * provide appropriate action.
     */
    private enum PAIRING_STATE {
        PAIRING_STATE_CONNECT_BUTTON,
        PAIRING_STATE_STEP_1,
        PAIRING_STATE_STEP_2,
        PAIRING_STATE_STEP_3,
        PAIRING_STATE_SEARCHING,
        PAIRING_STATE_ERROR
    }

    private int mActivityState;

    private PAIRING_STATE pairingState;
    private String newDeviceName;
    private String newDeviceCode;
    private String newDeviceAddress;

    LinearLayout mPairButtonView;
    LinearLayout mPairTipView;
    View mConnectDeviceView;
    LinearLayout mNewDeviceView;
    LinearLayout mPairSearchView;
    LinearLayout mBottomPairButton;

    LinearLayout mPairTipViewScreenTwo;

    // Connected Device Status
    Button deviceConnectionStatusBtn;

    private Handler mHandler;

    private BluetoothAdapter mBluetoothAdapter;
    private volatile boolean mScanning;

    private int mCurrentOrientation;
    private BluetoothLeScanner mLEScanner;

    private List<Integer> mRequestPermissions = new ArrayList<>();

    private int mRequestingPermission = -1;

    private ScanCallback newScanCallback;

    private View.OnClickListener mSuccessFulPairingHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("======mSuccessFulPairingHandler======");
            PopUp.hide();
            displayScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
        }
    };

    private View.OnClickListener mFailedPairingHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("======mFailedPairingHandler======");
            PopUp.hide();
            displayScreen(PAIRING_STATE.PAIRING_STATE_STEP_2);
        }
    };
    private View.OnClickListener mRetryPairing = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("======mRetryPairing======");
            PopUp.hide();
            scanLeDevice(true);
            displayScreen(PAIRING_STATE.PAIRING_STATE_SEARCHING);
        }
    };

    private final BroadcastReceiver gattForceClosedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BLEService.GATT_FORCE_CLOSED)) {
                updatePairedDeviceCard();
            }
        }
    };

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                logi(" mPairReceiver - state = " + state + " prevState = " + prevState);
                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    ConnectedDevice newDev = new ConnectedDevice(newDeviceCode.toUpperCase(), newDeviceCode
                            .toUpperCase(), false, newDeviceAddress, 0, null, System.currentTimeMillis());
                    handlePairingSuccessful(newDev);
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDING) {
                    scanLeDevice(false);
                    MBApp.getApp().getEchoClientManager().sendPairingStats(false, null);
                    PopUp.show(getString(R.string.pairing_failed_message), //message
                            getString(R.string.pairing_failed_title), //title
                            R.drawable.error_face, //image icon res id
                            R.drawable.red_btn,
                            PopUp.GIFF_ANIMATION_ERROR,
                            PopUp.TYPE_CHOICE, //type of popup.
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    PopUp.hide();
                                    displayScreen(PAIRING_STATE.PAIRING_STATE_STEP_1);
                                }
                            },//override click listener for ok button
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    PopUp.hide();
                                    displayScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                                }
                            });
                }
            }
        }
    };

    private BroadcastReceiver connectionChangedReceiver = BLEConnectionHandler.bleConnectionChangedReceiver(this);

    @Override
    public void setActivityState(int baseActivityState) {
        mActivityState = baseActivityState;
    }

    @Override
    public void preUpdateUi() {
        updatePairedDeviceCard();
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PermissionChecker.PERMISSION_GRANTED ||
                    (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PermissionChecker.PERMISSION_GRANTED)) {
                mRequestingPermission = mRequestPermissions.get(0);
                mRequestPermissions.remove(0);
                PopUp.show(
                        (mRequestingPermission == EventCategories.IPC_BLE_NOTIFICATION_INCOMING_CALL) ? getString(R.string
                                .telephony_permission) : getString(R.string.sms_permission),
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

    View.OnClickListener notificationOKHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("notificationOKHandler");
            PopUp.hide();
            if (mRequestingPermission == EventCategories.IPC_BLE_NOTIFICATION_INCOMING_CALL) {
                String[] permissionsNeeded = {Manifest.permission.READ_PHONE_STATE};
                requetPermission(permissionsNeeded, PermissionCodes.INCOMING_CALL_PERMISSIONS_REQUESTED);
            }
            if (mRequestingPermission == EventCategories.IPC_BLE_NOTIFICATION_INCOMING_SMS) {
                String[] permissionsNeeded = {Manifest.permission.RECEIVE_SMS};
                requetPermission(permissionsNeeded, PermissionCodes.INCOMING_SMS_PERMISSIONS_REQUESTED);
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
            PopUp.show(
                    msg,
                    getString(R.string.permissions_needed_title),
                    R.drawable.error_face, R.drawable.red_btn,
                    PopUp.GIFF_ANIMATION_ERROR,
                    PopUp.TYPE_ALERT,
                    checkMorePermissionsNeeded, checkMorePermissionsNeeded);
        }
    };

    // *************************************************

    private static final String TAG = PairingActivity.class.getSimpleName();

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setContentView(R.layout.activity_connect);
        initViews();
        mCurrentOrientation = getResources().getConfiguration().orientation;
        displayScreen(pairingState);
    }

    /**
     * Setup font styles by setting an appropriate typefaces.
     */
    private void setupFontStyle() {
        ImageView ohPrettyImg = (ImageView) findViewById(R.id.oh_pretty_emoji);
        ohPrettyImg.setVisibility(View.INVISIBLE);

        MBApp application = MBApp.getApp();

        Typeface defaultTypeface = application.getTypeface();

        deviceConnectionStatusBtn.setTypeface(defaultTypeface);
        // Connect Screen
        TextView appBarTitle = (TextView) findViewById(R.id.flash_projects_title_txt);
        appBarTitle.setTypeface(defaultTypeface);

        TextView pairBtnText = (TextView) findViewById(R.id.custom_pair_button_text);
        pairBtnText.setTypeface(defaultTypeface);

        Typeface boldTypeface = application.getTypefaceBold();

        TextView manageMicrobit = (TextView) findViewById(R.id.title_manage_microbit);
        manageMicrobit.setTypeface(boldTypeface);

        TextView manageMicorbitStatus = (TextView) findViewById(R.id.device_status_txt);
        manageMicorbitStatus.setTypeface(boldTypeface);

        // How to pair your micro:bit - Screen #1
        TextView pairTipTitle = (TextView) findViewById(R.id.pairTipTitle);
        pairTipTitle.setTypeface(boldTypeface);

        TextView stepOneTitle = (TextView) findViewById(R.id.pair_tip_step_1_step);
        stepOneTitle.setTypeface(boldTypeface);

// How to pair your micro:bit - Screen #2
        TextView howToPairStepThreeTitle = (TextView) findViewById(R.id.how_to_pair_screen_two_title);
        howToPairStepThreeTitle.setTypeface(MBApp.getApp().getTypefaceBold());

        TextView howToPairStepThreeStep = (TextView) findViewById(R.id.pair_tip_step_3_step);
        howToPairStepThreeStep.setTypeface(MBApp.getApp().getTypefaceBold());

        TextView howToPairStepThreeText = (TextView) findViewById(R.id.pair_tip_step_3_instructions);
        howToPairStepThreeText.setTypeface(MBApp.getApp().getRobotoTypeface());

        TextView howToPairStepFourTitle = (TextView) findViewById(R.id.pair_tip_step_4_step);
        howToPairStepFourTitle.setTypeface(MBApp.getApp().getTypefaceBold());

        TextView howToPairStepFourText = (TextView) findViewById(R.id.pair_tip_step_4_instructions);
        howToPairStepFourText.setTypeface(MBApp.getApp().getRobotoTypeface());

        Button cancelPairScreenTwoButton = (Button) findViewById(R.id.cancel_tip_step_3_btn);
        cancelPairScreenTwoButton.setTypeface(MBApp.getApp().getRobotoTypeface());

        Button nextPairScreenTwoButton = (Button) findViewById(R.id.ok_tip_step_3_btn);
        nextPairScreenTwoButton.setTypeface(MBApp.getApp().getRobotoTypeface());

        // Step 2 - Enter Pattern
        TextView enterPatternTitle = (TextView) findViewById(R.id.enter_pattern_step_2_title);
        enterPatternTitle.setTypeface(boldTypeface);

        TextView stepTwoTitle = (TextView) findViewById(R.id.pair_enter_pattern_step_2);
        stepTwoTitle.setTypeface(boldTypeface);

        TextView stepTwoInstructions = (TextView) findViewById(R.id.pair_enter_pattern_step_2_instructions);
        stepTwoInstructions.setTypeface(MBApp.getApp().getRobotoTypeface());


        Button cancelEnterPattern = (Button) findViewById(R.id.cancel_enter_pattern_step_2_btn);
        cancelEnterPattern.setTypeface(MBApp.getApp().getRobotoTypeface());

        Button okEnterPatternButton = (Button) findViewById(R.id.ok_enter_pattern_step_2_btn);
        okEnterPatternButton.setTypeface(MBApp.getApp().getRobotoTypeface());

        // Step 3 - Searching for micro:bit
        TextView searchMicrobitTitle = (TextView) findViewById(R.id.search_microbit_step_3_title);
        searchMicrobitTitle.setTypeface(boldTypeface);

        TextView stepThreeTitle = (TextView) findViewById(R.id.searching_microbit_step);
        stepThreeTitle.setTypeface(boldTypeface);

        Typeface robotoTypeface = application.getRobotoTypeface();

        TextView descriptionManageMicrobit = (TextView) findViewById(R.id.description_manage_microbit);
        descriptionManageMicrobit.setTypeface(robotoTypeface);

        TextView problemsMicrobit = (TextView) findViewById(R.id.connect_microbit_problems_message);
        problemsMicrobit.setTypeface(robotoTypeface);

        TextView stepOneInstructions = (TextView) findViewById(R.id.pair_tip_step_1_instructions);
        stepOneInstructions.setTypeface(robotoTypeface);

        Button cancelPairButton = (Button) findViewById(R.id.cancel_tip_step_1_btn);
        cancelPairButton.setTypeface(robotoTypeface);

        Button nextPairButton = (Button) findViewById(R.id.ok_tip_step_1_btn);
        nextPairButton.setTypeface(robotoTypeface);

        TextView stepThreeInstructions = (TextView) findViewById(R.id.searching_microbit_step_instructions);
        stepThreeInstructions.setTypeface(robotoTypeface);

        Button cancelSearchMicroBit = (Button) findViewById(R.id.cancel_search_microbit_step_3_btn);
        cancelSearchMicroBit.setTypeface(robotoTypeface);
    }

    private void initViews() {
        deviceConnectionStatusBtn = (Button) findViewById(R.id.connected_device_status_button);
        mBottomPairButton = (LinearLayout) findViewById(R.id.ll_pairing_activity_screen);

        mPairTipViewScreenTwo = (LinearLayout) findViewById(R.id.pair_tip_screen_two);
        mPairButtonView = (LinearLayout) findViewById(R.id.pairButtonView);
        mPairTipView = (LinearLayout) findViewById(R.id.pairTipView);
        mConnectDeviceView = findViewById(R.id.connectDeviceView); // Connect device view
        mNewDeviceView = (LinearLayout) findViewById(R.id.newDeviceView);
        mPairSearchView = (LinearLayout) findViewById(R.id.pairSearchView);

        //Setup on click listeners.
        deviceConnectionStatusBtn.setOnClickListener(this);
        findViewById(R.id.pairButton).setOnClickListener(this);
        findViewById(R.id.cancel_tip_step_1_btn).setOnClickListener(this);
        findViewById(R.id.ok_enter_pattern_step_2_btn).setOnClickListener(this);
        findViewById(R.id.cancel_enter_pattern_step_2_btn).setOnClickListener(this);
        findViewById(R.id.cancel_search_microbit_step_3_btn).setOnClickListener(this);

        findViewById(R.id.cancel_tip_step_3_btn).setOnClickListener(this);
        findViewById(R.id.ok_tip_step_3_btn).setOnClickListener(this);

        setupFontStyle();
    }

    private void releaseViews() {
        deviceConnectionStatusBtn = null;
        mBottomPairButton = null;
        mPairButtonView = null;
        mPairTipView = null;
        mConnectDeviceView = null; // Connect device view
        mNewDeviceView = null;
        mPairSearchView = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePairedDeviceCard();

        // Step 1 - How to pair
        findViewById(R.id.pair_tip_step_1_giff).animate();
        // Step 3 - Searching for micro:bit

    }

    @Override
    public void onPause() {
        logi("onPause() ::");
        super.onPause();

        // Step 1 - How to pair

        // Step 3 - Stop searching for micro:bit animation
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logi("onCreate() ::");

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mActivityState = PairingActivityState.STATE_IDLE;
            pairingState = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
        }

        MBApp application = MBApp.getApp();

        // Make sure to call this before any other userActionEvent is sent
        application.getEchoClientManager().sendViewEventStats("pairingactivity");

        //Register receiver
        IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mPairReceiver, intent);

        IntentFilter broadcastIntentFilter = new IntentFilter(IPCConstants.INTENT_BLE_NOTIFICATION);
        LocalBroadcastManager.getInstance(application).registerReceiver(connectionChangedReceiver, broadcastIntentFilter);

        LocalBroadcastManager.getInstance(application).registerReceiver(gattForceClosedReceiver, new
                IntentFilter(BLEService.GATT_FORCE_CLOSED));

        setupBleController();

        // ************************************************
        //Remove title barproject_list
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_connect);

        mHandler = new Handler(Looper.getMainLooper());

        initViews();

        updatePairedDeviceCard();

        mCurrentOrientation = getResources().getConfiguration().orientation;

        // pin view
        displayScreen(pairingState);
    }

    boolean setupBleController() {
        boolean retvalue = true;

        if (mBluetoothAdapter == null) {
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            retvalue = false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mLEScanner == null) {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if (mLEScanner == null)
                retvalue = false;
        }
        return retvalue;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        logi("onActivityResult");
        if (requestCode == RequestCodes.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                if (mActivityState == PairingActivityState.STATE_ENABLE_BT_FOR_PAIRING) {
                    startWithPairing();
                } else if (mActivityState == PairingActivityState.STATE_ENABLE_BT_FOR_CONNECT) {
                    toggleConnection();
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                PopUp.show(
                        getString(R.string.bluetooth_off_cannot_continue), //message
                        "",
                        R.drawable.error_face, R.drawable.red_btn,
                        PopUp.GIFF_ANIMATION_ERROR,
                        PopUp.TYPE_ALERT,
                        null, null);
            }
            //Change state back to Idle
            mActivityState = PairingActivityState.STATE_IDLE;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void displayLedGrid() {

        final GridView gridview = (GridView) findViewById(R.id.enter_pattern_step_2_gridview);
        gridview.setAdapter(new LEDAdapter(this, DEVICE_CODE_ARRAY));
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                boolean isOn = toggleLED((ImageView) v, position);
                setCol(parent, position, isOn);

                checkPatternSuccess();
            }

        });

        checkPatternSuccess();
    }

    private void checkPatternSuccess() {
        final ImageView ohPrettyImage = (ImageView) findViewById(R.id.oh_pretty_emoji);
        if (DEVICE_CODE_ARRAY[20] == 1 && DEVICE_CODE_ARRAY[21] == 1
                && DEVICE_CODE_ARRAY[22] == 1 && DEVICE_CODE_ARRAY[23] == 1
                && DEVICE_CODE_ARRAY[24] == 1) {
            findViewById(R.id.ok_enter_pattern_step_2_btn).setVisibility(View.VISIBLE);
            ohPrettyImage.setImageResource(R.drawable.emoji_entering_pattern_valid_pattern);
        } else {
            findViewById(R.id.ok_enter_pattern_step_2_btn).setVisibility(View.INVISIBLE);
            ohPrettyImage.setImageResource(R.drawable.emoji_entering_pattern);
        }
    }

    private void generateName() {

        StringBuilder deviceNameBuilder = new StringBuilder();
        //Columns
        for (int col = 0; col < 5; col++) {
            //Rows
            for (int row = 0; row < 5; row++) {
                if (DEVICE_CODE_ARRAY[(col + (5 * row))] == 1) {
                    deviceNameBuilder.append(DEVICE_NAME_MAP_ARRAY[(col + (5 * row))]);
                    break;
                }
            }
        }

        newDeviceCode = deviceNameBuilder.toString();
        newDeviceName = "BBC microbit [" + deviceNameBuilder.toString() + "]";
        //Toast.makeText(this, "Pattern :"+newDeviceCode, Toast.LENGTH_SHORT).show();
    }

    private void setCol(AdapterView<?> parent, int pos, boolean enabledlandscape) {
        int index = pos - 5;
        ImageView v;

        while (index >= 0) {
            v = (ImageView) parent.getChildAt(index);
            v.setBackground(getApplication().getResources().getDrawable(R.drawable.white_red_led_btn));
            v.setTag(R.id.ledState, 0);
            v.setSelected(false);
            DEVICE_CODE_ARRAY[index] = 0;
            int position = (Integer) v.getTag(R.id.position);
            v.setContentDescription("" + position + getLEDStatus(index)); // TODO - calculate correct position
            index -= 5;
        }
        index = pos + 5;
        while (index < 25) {
            v = (ImageView) parent.getChildAt(index);
            v.setBackground(getApplication().getResources().getDrawable(R.drawable.red_white_led_btn));
            v.setTag(R.id.ledState, 1);
            v.setSelected(false);
            DEVICE_CODE_ARRAY[index] = 1;
            int position = (Integer) v.getTag(R.id.position);
            v.setContentDescription("" + position + getLEDStatus(index));
            index += 5;
        }

    }

    private boolean toggleLED(ImageView image, int pos) {
        boolean isOn;
        //Toast.makeText(this, "Pos :" +  pos, Toast.LENGTH_SHORT).show();
        int state = (Integer) image.getTag(R.id.ledState);
        if (state != 1) {
            DEVICE_CODE_ARRAY[pos] = 1;
            image.setBackground(getApplication().getResources().getDrawable(R.drawable.red_white_led_btn));
            image.setTag(R.id.ledState, 1);
            isOn = true;

        } else {
            DEVICE_CODE_ARRAY[pos] = 0;
            image.setBackground(getApplication().getResources().getDrawable(R.drawable.white_red_led_btn));
            image.setTag(R.id.ledState, 0);
            isOn = false;
            // Update the code to consider the still ON LED below the toggled one
            if (pos < 20) {
                DEVICE_CODE_ARRAY[pos + 5] = 1;
            }
        }

        image.setSelected(false);
        int position = (Integer) image.getTag(R.id.position);
        image.setContentDescription("" + position + getLEDStatus(pos));
        return isOn;
    }

    // To read out the status of the currently selected LED at a given position
    private String getLEDStatus(int position) {
        String statusRead;
        if (DEVICE_CODE_ARRAY[position] == 1) {
            statusRead = "on";
        } else {
            statusRead = "off";
        }
        return statusRead;
    }

    // Get the drawable for the device connection status
    private Drawable getDrawableResource(int resID) {
        return ContextCompat.getDrawable(this, resID);
    }

    private void updateConnectionStatus() {
        ConnectedDevice connectedDevice = BluetoothUtils.getPairedMicrobit(this);
        Drawable mDeviceDisconnectedImg;
        Drawable mDeviceConnectedImg;

        mDeviceDisconnectedImg = getDrawableResource(R.drawable.device_status_disconnected);
        mDeviceConnectedImg = getDrawableResource(R.drawable.device_status_connected);

        if (!connectedDevice.mStatus) {
            // Device is not connected
            deviceConnectionStatusBtn.setBackgroundResource(R.drawable.grey_btn);
            deviceConnectionStatusBtn.setTextColor(Color.WHITE);
            deviceConnectionStatusBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, mDeviceDisconnectedImg, null);
            deviceConnectionStatusBtn.setContentDescription("Micro:bit not connected " + connectedDevice.mName + "is " + getMicrobitStatusForAccessibility(connectedDevice.mStatus));

        } else {
            // Device is connected
            deviceConnectionStatusBtn.setBackgroundResource(R.drawable.white_btn_devices_status_connected);
            deviceConnectionStatusBtn.setTextColor(Color.BLACK);
            deviceConnectionStatusBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, mDeviceConnectedImg, null);
            deviceConnectionStatusBtn.setContentDescription("Currently connected Micro:bit " + connectedDevice.mName + "is " + getMicrobitStatusForAccessibility(connectedDevice.mStatus));
        }
    }

    // Retrieve Micro:bit accessibility state
    public String getMicrobitStatusForAccessibility(boolean status) {
        String statusRead;
        if (status) {
            statusRead = "on";
        } else {
            statusRead = "off";
        }
        return statusRead;
    }

    private void updatePairedDeviceCard() {
        if (deviceConnectionStatusBtn != null) {
            ConnectedDevice connectedDevice = BluetoothUtils.getPairedMicrobit(this);
            if (connectedDevice.mName == null) {
                // No device is Paired
                deviceConnectionStatusBtn.setBackgroundResource(R.drawable.grey_btn);
                deviceConnectionStatusBtn.setText("-");
                deviceConnectionStatusBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                deviceConnectionStatusBtn.setOnClickListener(null);
            } else {
                deviceConnectionStatusBtn.setText(connectedDevice.mName);
                updateConnectionStatus();
                deviceConnectionStatusBtn.setOnClickListener(this);
            }
        }
    }

    private void displayScreen(PAIRING_STATE gotoState) {
        //Reset all screens first
        mPairTipView.setVisibility(View.GONE);
        mNewDeviceView.setVisibility(View.GONE);

        mPairTipViewScreenTwo.setVisibility(View.GONE);

        mPairSearchView.setVisibility(View.GONE);
        mConnectDeviceView.setVisibility(View.GONE);

        logi("********** Connect: state from " + pairingState + " to " + gotoState);
        pairingState = gotoState;

        boolean mDeviceListAvailable = ((gotoState == PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON) ||
                (gotoState == PAIRING_STATE.PAIRING_STATE_ERROR));

        if (mDeviceListAvailable) {
            updatePairedDeviceCard();
            mConnectDeviceView.setVisibility(View.VISIBLE);
        }

        switch (gotoState) {
            case PAIRING_STATE_CONNECT_BUTTON:
                break;

            case PAIRING_STATE_STEP_3:
                mPairTipViewScreenTwo.setVisibility(View.VISIBLE);
                break;

            case PAIRING_STATE_ERROR:
                Arrays.fill(DEVICE_CODE_ARRAY, 0);
                findViewById(R.id.enter_pattern_step_2_gridview).setEnabled(true);
                newDeviceName = "";
                newDeviceCode = "";
                break;

            case PAIRING_STATE_STEP_1:
                mPairTipView.setVisibility(View.VISIBLE);
                findViewById(R.id.ok_tip_step_1_btn).setOnClickListener(this);
                break;

            case PAIRING_STATE_STEP_2:
                mNewDeviceView.setVisibility(View.VISIBLE);
                findViewById(R.id.cancel_enter_pattern_step_2_btn).setVisibility(View.VISIBLE);
                findViewById(R.id.enter_pattern_step_2_title).setVisibility(View.VISIBLE);
                findViewById(R.id.oh_pretty_emoji).setVisibility(View.VISIBLE);

                displayLedGrid();
                break;

            case PAIRING_STATE_SEARCHING:
                if (mPairSearchView != null) {
                    mPairSearchView.setVisibility(View.VISIBLE);
                    TextView tvTitle = (TextView) findViewById(R.id.search_microbit_step_3_title);
                    TextView tvSearchingStep = (TextView) findViewById(R.id.searching_microbit_step);
                    tvSearchingStep.setContentDescription(tvSearchingStep.getText());
                    TextView tvSearchingInstructions = (TextView) findViewById(R.id.searching_microbit_step_instructions);
                    if (tvTitle != null) {
                        tvTitle.setText(R.string.searchingTitle);
                        findViewById(R.id.searching_progress_spinner).setVisibility(View.VISIBLE);
                        ((GifImageView) findViewById(R.id.searching_microbit_found_giffview))
                                .setImageResource(R.drawable.pairing_pin_screen_two);
                        if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                            tvSearchingStep.setText(R.string.searching_tip_step_text_one_line);
                        } else {
                            tvSearchingStep.setText(R.string.searching_tip_step_text);
                        }
                        tvSearchingInstructions.setText(R.string.searching_tip_text_instructions);
                    }
                }
                break;
        }
    }

    private void startBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, RequestCodes.REQUEST_ENABLE_BT);
    }

    public void startWithPairing() {
        if (mBottomPairButton != null) {
            mConnectDeviceView.setVisibility(View.GONE);
        }

        if (mPairButtonView != null) {
            displayScreen(PAIRING_STATE.PAIRING_STATE_STEP_1);
        }
    }

    public void toggleConnection() {
        ConnectedDevice currentDevice = BluetoothUtils.getPairedMicrobit(this);
        if (currentDevice.mAddress != null) {
            boolean currentState = currentDevice.mStatus;
            MBApp application = MBApp.getApp();

            if (!currentState) {
                mActivityState = PairingActivityState.STATE_CONNECTING;
                mRequestPermissions.clear();
                PopUp.show(
                        getString(R.string.init_connection),
                        "",
                        R.drawable.message_face, R.drawable.blue_btn,
                        PopUp.GIFF_ANIMATION_NONE,
                        PopUp.TYPE_SPINNER,
                        null, null);

                ServiceUtils.sendConnectDisconnectMessage(true);
            } else {
                mActivityState = PairingActivityState.STATE_DISCONNECTING;
                PopUp.show(
                        getString(R.string.disconnecting),
                        "",
                        R.drawable.message_face, R.drawable.blue_btn,
                        PopUp.GIFF_ANIMATION_NONE,
                        PopUp.TYPE_SPINNER,
                        null, null);

                ServiceUtils.sendConnectDisconnectMessage(false);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionCodes.BLUETOOTH_PERMISSIONS_REQUESTED: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    proceedAfterBlePermissionGranted();
                } else {
                    PopUp.show(
                            getString(R.string.location_permission_error),
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
                    PopUp.show(
                            getString(R.string.telephony_permission_error),
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

    private void proceedAfterBlePermissionGranted() {
        if (!BluetoothChecker.getInstance().isBluetoothON()) {
            mActivityState = PairingActivityState.STATE_ENABLE_BT_FOR_PAIRING;
            startBluetooth();
            return;
        }
        startWithPairing();
    }

    private void requetPermission(String[] permissions, final int requestCode) {
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    View.OnClickListener bluetoothPermissionOKHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("bluetoothPermissionOKHandler");
            PopUp.hide();
            String[] permissionsNeeded = {Manifest.permission.ACCESS_COARSE_LOCATION};
            requetPermission(permissionsNeeded, PermissionCodes.BLUETOOTH_PERMISSIONS_REQUESTED);
        }
    };
    View.OnClickListener bluetoothPermissionCancelHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("bluetoothPermissionCancelHandler");
            PopUp.hide();
            PopUp.show(
                    getString(R.string.location_permission_error),
                    getString(R.string.permissions_needed_title),
                    R.drawable.error_face, R.drawable.red_btn,
                    PopUp.GIFF_ANIMATION_ERROR,
                    PopUp.TYPE_ALERT,
                    null, null);
        }
    };

    private void checkBluetoothPermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PermissionChecker.PERMISSION_GRANTED) {
            PopUp.show(
                    getString(R.string.location_permission_pairing),
                    getString(R.string.permissions_needed_title),
                    R.drawable.message_face, R.drawable.blue_btn, PopUp.GIFF_ANIMATION_NONE,
                    PopUp.TYPE_CHOICE,
                    bluetoothPermissionOKHandler,
                    bluetoothPermissionCancelHandler);
        } else {
            proceedAfterBlePermissionGranted();
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            // Pair a micro:bit
            case R.id.pairButton:
                logi("onClick() :: pairButton");
                checkBluetoothPermissions();
                break;
            // Proceed to Enter Pattern
            case R.id.ok_tip_step_1_btn:
                logi("onClick() :: ok_tip_screen_one_button");
                displayScreen(PAIRING_STATE.PAIRING_STATE_STEP_2);
                break;
            // Confirm pattern and begin searching for micro:bit
            case R.id.ok_enter_pattern_step_2_btn:
                logi("onClick() :: ok_tip_screen_one_button");
                displayScreen(PAIRING_STATE.PAIRING_STATE_STEP_3);
                break;

            case R.id.cancel_tip_step_1_btn:
                logi("onClick() :: cancel_tip_button");
                displayScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                break;

            case R.id.cancel_tip_step_3_btn:
                logi("onClick() :: cancel_tip_screen_two_button");
                displayScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                break;
            case R.id.ok_tip_step_3_btn:
                logi("onClick() :: ok_tip_screen_two_button");
                if (pairingState == PAIRING_STATE.PAIRING_STATE_STEP_3) {
                    generateName();
                    if (!BluetoothChecker.getInstance().checkBluetoothAndStart()) {
                        return;
                    }
                    scanLeDevice(true);
                    displayScreen(PAIRING_STATE.PAIRING_STATE_SEARCHING);
                }
                break;

            case R.id.cancel_enter_pattern_step_2_btn:
                logi("onClick() :: cancel_name_button");
                cancelPairing();
                displayScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                break;
            case R.id.cancel_search_microbit_step_3_btn:
                logi("onClick() :: cancel_search_button");
                scanLeDevice(false);
                cancelPairing();
                displayScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                break;
            case R.id.connected_device_status_button:
                logi("onClick() :: connectBtn");
                if (!BluetoothChecker.getInstance().isBluetoothON()) {
                    mActivityState = PairingActivityState.STATE_ENABLE_BT_FOR_CONNECT;
                    startBluetooth();
                    return;
                }
                toggleConnection();
                break;
            // Delete Microbit
            case R.id.deleteBtn:
                logi("onClick() :: deleteBtn");
                handleDeleteMicrobit();
                break;
            case R.id.backBtn:
                logi("onClick() :: backBtn");
                handleResetAll();
                break;

            default:
                Toast.makeText(MBApp.getApp(), "Default Item Clicked: " + v.getId(), Toast.LENGTH_SHORT).show();
                break;

        }
    }

    private void handleDeleteMicrobit() {
        PopUp.show(getString(R.string.deleteMicrobitMessage), //message
                getString(R.string.deleteMicrobitTitle), //title
                R.drawable.ic_trash, R.drawable.red_btn,
                PopUp.GIFF_ANIMATION_NONE,
                PopUp.TYPE_CHOICE, //type of popup.
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopUp.hide();
                        //Unpair the device for secure BLE
                        unpairDeivce();
                        BluetoothUtils.setPairedMicroBit(MBApp.getApp(), null);
                        updatePairedDeviceCard();
                    }
                },//override click listener for ok button
                null);//pass null to use default listener
    }

    private void unpairDeivce() {
        ConnectedDevice connectedDevice = BluetoothUtils.getPairedMicrobit(this);
        String addressToDelete = connectedDevice.mAddress;
        // Get the paired devices and put them in a Set
        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bt : pairedDevices) {
            logi("Paired device " + bt.getName());
            if (bt.getAddress().equals(addressToDelete)) {
                try {
                    Method m = bt.getClass().getMethod("removeBond", (Class[]) null);
                    m.invoke(bt, (Object[]) null);
                } catch (NoSuchMethodException | IllegalAccessException
                        | InvocationTargetException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }
    }

    private void handleResetAll() {
        Arrays.fill(DEVICE_CODE_ARRAY, 0);
        scanLeDevice(false);
        cancelPairing();

        if (pairingState == PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON) {
            finish();
        } else {
            displayScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
        }
    }

    private void handlePairingFailed() {

        logi("handlePairingFailed() :: Start");
        MBApp.getApp().getEchoClientManager().sendPairingStats(false, null);
        PopUp.show(getString(R.string.pairingErrorMessage), //message
                getString(R.string.timeOut), //title
                R.drawable.error_face, //image icon res id
                R.drawable.red_btn,
                PopUp.GIFF_ANIMATION_ERROR,
                PopUp.TYPE_CHOICE, //type of popup.
                mRetryPairing,//override click listener for ok button
                mFailedPairingHandler);
    }

    private void handlePairingSuccessful(final ConnectedDevice newDev) {
        logi("handlePairingSuccessful()");
        MBApp.getApp().getEchoClientManager().sendPairingStats(true, newDev.mfirmware_version);
        BluetoothUtils.setPairedMicroBit(MBApp.getApp(), newDev);
        updatePairedDeviceCard();
        // Pop up to show pairing successful
        PopUp.show(
                getString(R.string.pairing_successful_tip_message), // message
                getString(R.string.pairing_success_message_1), //title
                R.drawable.message_face, //image icon res id
                R.drawable.green_btn,
                PopUp.GIFF_ANIMATION_NONE,
                PopUp.TYPE_ALERT, //type of popup.
                mSuccessFulPairingHandler,
                mSuccessFulPairingHandler);

        Log.e(TAG, "Set just paired to true");
        MBApp.getApp().setJustPaired(true);
    }

    private void cancelPairing() {
        logi("###>>>>>>>>>>>>>>>>>>>>> cancelPairing");
        scanLeDevice(false);//TODO: is it really needed?
    }

    /*
     * TODO : Part of HACK 20150729
     * =================================================================
     */
    private void scanLeDevice(final boolean enable) {

        logi("scanLeDevice() :: enable = " + enable);
        if (enable) {
            if (!setupBleController()) {
                logi("scanLeDevice() :: FAILED ");
                return;
            }
            if (!mScanning) {

                boolean hasBle = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

                logi("scanLeDevice ::   Searching For " + newDeviceName.toLowerCase());
                // Stops scanning after a pre-defined scan period.
                mScanning = true;
                TextView textView = (TextView) findViewById(R.id.search_microbit_step_3_title);
                if (textView != null)
                    textView.setText(getString(R.string.searchingTitle));
                mHandler.postDelayed(scanTimedOut, SCAN_PERIOD);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { //Lollipop
                    mBluetoothAdapter.startLeScan(getOldScanCallback());
                } else {
                    List<ScanFilter> filters = new ArrayList<ScanFilter>();
                    // TODO: play with ScanSettings further to ensure the Kit kat devices connectMaybeInit with higher success rate
                    ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
                    mLEScanner.startScan(filters, settings, getNewScanCallback());
                }
            }
        } else {
            if (mScanning) {
                mScanning = false;
                mHandler.removeCallbacks(scanTimedOut);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    mBluetoothAdapter.stopLeScan(getOldScanCallback());
                } else {
                    mLEScanner.stopScan(getNewScanCallback());
                }
            }
        }
    }

    private ScanCallback getNewScanCallback() {
        if (newScanCallback == null) {
            newScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    Log.i("callbackType = ", String.valueOf(callbackType));
                    Log.i("result = ", result.toString());
                    BluetoothDevice btDevice = result.getDevice();
                    final ScanRecord scanRecord = result.getScanRecord();
                    if (scanRecord != null) {
                        onLeScan(btDevice, result.getRssi(), scanRecord.getBytes());
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                    for (ScanResult sr : results) {
                        Log.i("Scan result - Results ", sr.toString());
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    Log.i("Scan failed", "Error Code : " + errorCode);
                }
            };
        }

        return newScanCallback;
    }

    private BluetoothAdapter.LeScanCallback getOldScanCallback() {
        return this;
    }

    private Runnable scanTimedOut = new Runnable() {
        @Override
        public void run() {
            scanFailedCallbackImpl();
        }
    };

    private void scanFailedCallbackImpl() {
        boolean scanning = mScanning;
        scanLeDevice(false);

        if (scanning) { // was scanning
            handlePairingFailed();
        }
    }

    @Override
    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

        logi("mLeScanCallback.onLeScan() [+]");

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
            logi("mLeScanCallback.onLeScan() ::   Cannot Compare " + device.getAddress() + " " + rssi + " " + Arrays.toString(scanRecord));
        } else {
            String s = device.getName().toLowerCase();
            //Replace all : to blank - Fix for #64
            //TODO Use pattern recognition instead
            s = s.replaceAll(":", "");
            if (newDeviceName.toLowerCase().startsWith(s)) {
                logi("mLeScanCallback.onLeScan() ::   Found micro:bit -" + device.getName().toLowerCase() + " " + device.getAddress());
                // Stop scanning as device is found.
                scanLeDevice(false);
                newDeviceAddress = device.getAddress();
                //Rohit : Do Not Call the TextView.setText directly. It doesn't work on 4.4.4
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView) findViewById(R.id.search_microbit_step_3_title);
                        TextView tvSearchingStep = (TextView) findViewById(R.id.searching_microbit_step);
                        TextView tvSearchingInstructions = (TextView) findViewById(R.id.searching_microbit_step_instructions);
                        if (textView != null) {
                            textView.setText(getString(R.string.searchingTitle));
                            findViewById(R.id.searching_progress_spinner).setVisibility(View.GONE);
                            ((GifImageView) findViewById(R.id.searching_microbit_found_giffview))
                                    .setImageResource(R.drawable.emoji_microbit_found);
                            if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                                tvSearchingStep.setText(R.string.searching_microbit_found_message_one_line);
                            } else {
                                tvSearchingStep.setText(R.string.searching_microbit_found_message);
                            }
                            tvSearchingInstructions.setText(R.string.searching_tip_text_instructions);
                            startPairingSecureBle(device);
                        }
                    }
                });
            } else {
                logi("mLeScanCallback.onLeScan() ::   Found - device.getName() == " + device.getName().toLowerCase()
                        + " , device address - " + device.getAddress());
            }
        }
    }

    private void startPairingSecureBle(BluetoothDevice device) {
        logi("###>>>>>>>>>>>>>>>>>>>>> startPairingSecureBle");
        //Check if the device is already bonded
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            logi("Device is already bonded.");
            cancelPairing();
            //Get device name from the System settings if present and add to our list
            ConnectedDevice newDev = new ConnectedDevice(newDeviceCode.toUpperCase(),
                    newDeviceCode.toUpperCase(), false, newDeviceAddress, 0, null,
                    System.currentTimeMillis());
            handlePairingSuccessful(newDev);
            return;
        }
        logi("device.createBond returns " + device.createBond());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPairTipView.setVisibility(View.GONE);
        mNewDeviceView.setVisibility(View.GONE);
        mPairTipViewScreenTwo.setVisibility(View.GONE);
        mPairSearchView.setVisibility(View.GONE);
        mConnectDeviceView.setVisibility(View.GONE);

        releaseViews();

        unbindDrawables(findViewById(R.id.connected_device_status_button));
        unbindDrawables(findViewById(R.id.pairButtonView));
        unbindDrawables(findViewById(R.id.pairTipView));
        unbindDrawables(findViewById(R.id.connectDeviceView));
        unbindDrawables(findViewById(R.id.pairSearchView));
        unbindDrawables(findViewById(R.id.flash_projects_title_txt));
        unbindDrawables(findViewById(R.id.title_manage_microbit));
        unbindDrawables(findViewById(R.id.device_status_txt));
        unbindDrawables(findViewById(R.id.description_manage_microbit));
        unbindDrawables(findViewById(R.id.pairButton));
        unbindDrawables(findViewById(R.id.connect_microbit_problems_message));
        unbindDrawables(findViewById(R.id.pairTipTitle));
        unbindDrawables(findViewById(R.id.pairTipTitle));
        unbindDrawables(findViewById(R.id.pair_tip_step_1_step));
        unbindDrawables(findViewById(R.id.pair_tip_step_1_instructions));


        unbindDrawables(findViewById(R.id.how_to_pair_screen_two_title));
        unbindDrawables(findViewById(R.id.pair_tip_step_3_step));
        unbindDrawables(findViewById(R.id.pair_tip_step_3_instructions));
        unbindDrawables(findViewById(R.id.pair_tip_step_4_step));
        unbindDrawables(findViewById(R.id.pair_tip_step_4_instructions));
        unbindDrawables(findViewById(R.id.cancel_tip_step_3_btn));
        unbindDrawables(findViewById(R.id.ok_tip_step_3_btn));

        unbindDrawables(findViewById(R.id.cancel_tip_step_1_btn));
        unbindDrawables(findViewById(R.id.ok_tip_step_1_btn));
        unbindDrawables(findViewById(R.id.enter_pattern_step_2_title));
        unbindDrawables(findViewById(R.id.pair_enter_pattern_step_2_instructions));
        unbindDrawables(findViewById(R.id.oh_pretty_emoji));
        unbindDrawables(findViewById(R.id.cancel_enter_pattern_step_2_btn));
        unbindDrawables(findViewById(R.id.ok_enter_pattern_step_2_btn));
        unbindDrawables(findViewById(R.id.search_microbit_step_3_title));
        unbindDrawables(findViewById(R.id.searching_microbit_step));
        unbindDrawables(findViewById(R.id.searching_microbit_step_instructions));
        unbindDrawables(findViewById(R.id.cancel_search_microbit_step_3_btn));
        unbindDrawables(findViewById(R.id.searching_progress_spinner));

        unregisterReceiver(mPairReceiver);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MBApp.getApp());

        localBroadcastManager.unregisterReceiver(gattForceClosedReceiver);
        localBroadcastManager.unregisterReceiver(connectionChangedReceiver);
    }

    private void unbindDrawables(View view) {

        if (view == null)
            return;

        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
            view.setBackgroundResource(0);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            logi("onKeyDown() :: Cancel");
            handleResetAll();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_launcher, menu);
        return true;
    }
}
