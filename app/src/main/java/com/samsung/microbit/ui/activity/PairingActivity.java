package com.samsung.microbit.ui.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.PreviousDeviceList;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.service.DfuService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.ui.BluetoothSwitch;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.adapter.ConnectedDeviceAdapter;
import com.samsung.microbit.ui.adapter.LEDAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PairingActivity extends Activity implements View.OnClickListener {

    private static boolean DISABLE_DEVICE_LIST = false;

    ConnectedDevice[] mPrevDeviceArray;
    PreviousDeviceList mPrevDevList;
    ConnectedDevice mCurrentDevice;


    private enum PAIRING_STATE {
        PAIRING_STATE_CONNECT_BUTTON,
        PAIRING_STATE_TIP,
        PAIRING_STATE_PATTERN_EMPTY,
        PAIRING_STATE_SEARCHING,
        PAIRING_STATE_ERROR,
        PAIRING_STATE_NEW_NAME
    }

    private static PAIRING_STATE mState = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
    private static String mNewDeviceName;
    private static String mNewDeviceCode;
    private static String mNewDeviceAddress;

    // @formatter:off
    private static String deviceCodeArray[] = {
            "0", "0", "0", "0", "0",
            "0", "0", "0", "0", "0",
            "0", "0", "0", "0", "0",
            "0", "0", "0", "0", "0",
            "0", "0", "0", "0", "0"};

    private String deviceNameMapArray[] = {
            "T", "A", "T", "A", "T",
            "P", "E", "P", "E", "P",
            "G", "I", "G", "I", "G",
            "V", "O", "V", "O", "V",
            "Z", "U", "Z", "U", "Z"};
    // @formatter:on

    LinearLayout mPairButtonView;
    LinearLayout mPairTipView;
    LinearLayout mConnectDeviceView; // new layout
    LinearLayout mNewDeviceView;
    LinearLayout mPairSearchView;
    LinearLayout mBottomPairButton;
    LinearLayout mEnterPinView; // pin view
    List<ConnectedDevice> connectedDeviceList = new ArrayList<ConnectedDevice>();
    ConnectedDeviceAdapter connectedDeviceAdapter;
    private ListView lvConnectedDevice;

    private Handler mHandler;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 15000;

    private DFUResultReceiver dfuResultReceiver;
    /*
     * TODO : HACK 20150729
     * A bit of a hack to make sure the scan finishes properly.  Needs top be done properly
     * =================================================================
     */
    private static PairingActivity instance;
    private static BluetoothAdapter mBluetoothAdapter = null;
    private static volatile boolean mScanning = false;
    private static volatile boolean mPairing = false;
    //private Runnable scanFailedCallback;
    private static BluetoothLeScanner mLEScanner = null;


    private enum ACTIVITY_STATE {
        STATE_IDLE,
        STATE_ENABLE_BT_FOR_CONNECT,
        STATE_ENABLE_BT_FOR_PAIRING,
    }


    private static ACTIVITY_STATE mActivityState = ACTIVITY_STATE.STATE_IDLE;
    private int selectedDeviceForConnect = 0;

	/*
     * =================================================================
	 */

    class DFUResultReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = "Broadcast intent detected " + intent.getAction();
            logi("DFUResultReceiver.onReceive :: " + message);
            if (intent.getAction() == DfuService.BROADCAST_ERROR) {
                String error_message = Utils.broadcastGetErrorMessage(intent.getIntExtra(DfuService.EXTRA_DATA, 0));
                logi("DFUResultReceiver.onReceive() :: Pairing ERROR!!  Code - [" + intent.getIntExtra(DfuService.EXTRA_DATA, 0)
                        + "] Error Type - [" + intent.getIntExtra(DfuService.EXTRA_ERROR_TYPE, 0) + "]");

                if (mPairing) {
                    cancelPairing();
                    PopUp.show(MBApp.getContext(),
                            error_message, //message
                            getString(R.string.pairing_failed_title), //title
                            R.drawable.error_face, //image icon res id
                            R.drawable.red_btn,
                            PopUp.TYPE_ALERT, //type of popup.
                            null,//override click listener for ok button
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    PopUp.hide();
                                    displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                                }
                            });//pass null to use default listener
                }
            } else if (intent.getAction() == DfuService.BROADCAST_LOG) {
                String log_message = intent.getStringExtra(DfuService.EXTRA_LOG_MESSAGE);
                logi("DFUResultReceiver.onReceive() :: BROADCAST_LOG  Message - [" + log_message
                        + "] Log Level - [" + intent.getIntExtra(DfuService.EXTRA_LOG_LEVEL, 0) + "]");
            }
        }
    }

    static IntentFilter broadcastIntentFilter;

    ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            int phase = resultCode & 0x0ffff;

            if (phase == Constants.FLASHING_PAIRING_CODE_CHARACTERISTIC_RECIEVED) {
                logi("resultReceiver.onReceiveResult() :: FLASHING_PAIRING_CODE_CHARACTERISTIC_RECIEVED ");
                if (mPairing) {
                    int pairing_code = resultData.getInt("pairing_code");
                    logi("-----------> Pairing Code is " + pairing_code + " for device " + mNewDeviceCode.toUpperCase());
                    ConnectedDevice newDev = new ConnectedDevice(mNewDeviceCode.toUpperCase(), mNewDeviceCode.toUpperCase(), false, mNewDeviceAddress, pairing_code);
                    handlePairingSuccessful(newDev);
                    return;
                }

            } else if ((phase & Constants.PAIRING_CONTROL_CODE_REQUESTED) != 0) {
                if ((phase & 0x0ff00) == 0) {
                    if (mPairing) {
                        logi("resultReceiver.onReceiveResult() :: PAIRING_CONTROL_CODE_REQUESTED ");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Initiate bluetooth pairing request
                                if (mEnterPinView != null) {
                                    mPairSearchView.setVisibility(View.GONE);
                                    mEnterPinView.setVisibility(View.VISIBLE);
                                }
//                                TextView textView = (TextView) findViewById(R.id.pairSearchTitle);
//                                if (textView != null)
//                                    textView.setText(getString(R.string.pairing_phase2_msg_New));
                            }
                        });
                    }
                } /*else {
                    logi("resultReceiver.onReceiveResult() :: Phase 1 not complete recieved ");
                    if (mPairing) {
                        cancelPairing();
                        //Get the error message
                        PopUp.show(MBApp.getContext(),
                                getString(R.string.pairing_failed_message), //message
                                getString(R.string.pairing_failed_title), //title
                                R.drawable.error_face, //image icon res id
                                R.drawable.red_btn,
                                PopUp.TYPE_ALERT, //type of popup.
                                null,//override click listener for ok button
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        PopUp.hide();
                                        displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                                    }
                                });//pass null to use default listener
                    }
				}*/
            }
            super.onReceiveResult(resultCode, resultData);
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            int v = intent.getIntExtra(IPCMessageManager.BUNDLE_ERROR_CODE, 0);
            if (Constants.BLE_DISCONNECTED_FOR_FLASH == v) {
                logi("Bluetooth disconnected for flashing. No need to display pop-up");
                handleBLENotification(context, intent, false);
                return;
            }
            handleBLENotification(context, intent, true);
            if (v != 0) {
                logi("broadcastReceiver Error code =" + v);
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

    private void handleBLENotification(Context context, Intent intent, boolean popupHide) {

        mCurrentDevice = Utils.getPairedMicrobit(this);
        logi("handleBLENotification() " + mCurrentDevice.mPattern + "[" + mCurrentDevice.mStatus + "]");
        if (mPrevDevList == null) {
            mPrevDevList = PreviousDeviceList.getInstance(this);
        }
        mPrevDeviceArray = mPrevDevList.loadPrevMicrobits();

        if (mCurrentDevice.mPattern != null && mPrevDeviceArray != null && mCurrentDevice.mPattern.equals(mPrevDeviceArray[0].mPattern)) {
            mPrevDeviceArray[0].mStatus = mCurrentDevice.mStatus;
            mPrevDevList.changeMicrobitState(0, mPrevDeviceArray[0], mPrevDeviceArray[0].mStatus, true);
            populateConnectedDeviceList(false);

        }

        if (popupHide)
            PopUp.hide();
    }

    // *************************************************

    // DEBUG
    protected boolean debug = true;
    protected String TAG = "PairingActivity";

    protected void logi(String message) {
        Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    @Override
    public void onResume() {
        super.onResume();
        MBApp.setContext(this);
        populateConnectedDeviceList(false);
    }

    @Override
    public void onPause() {
        logi("onPause() ::");
        super.onPause();

    }

    public PairingActivity() {
        logi("PairingActivity() ::");
        instance = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        logi("onCreate() ::");

        super.onCreate(savedInstanceState);

        MBApp.setContext(this);

        // Make sure to call this before any other userActionEvent is sent
        if (MBApp.getApp().getEcho() != null) {
            logi("Page View test for PairingActivity");
            MBApp.getApp().getEcho().viewEvent("com.samsung.microbit.ui.activity.pairingactivity.page", null);
        }

        if (broadcastIntentFilter == null) {
            broadcastIntentFilter = new IntentFilter(IPCService.INTENT_BLE_NOTIFICATION);
            LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(broadcastReceiver, broadcastIntentFilter);
        }

        setupBleController();

        // ************************************************
        //Remove title barproject_list
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_connect);

        mHandler = new Handler(Looper.getMainLooper());
        if (mPrevDevList == null) {
            mPrevDevList = PreviousDeviceList.getInstance(this);
            mPrevDeviceArray = mPrevDevList.loadPrevMicrobits();
        }
        //mPrevDeviceArray = new ConnectedDevice[PREVIOUS_DEVICES_MAX];
        lvConnectedDevice = (ListView) findViewById(R.id.connectedDeviceList);
        TextView emptyText = (TextView) findViewById(android.R.id.empty);
        lvConnectedDevice.setEmptyView(emptyText);
        populateConnectedDeviceList(false);

        mBottomPairButton = (LinearLayout) findViewById(R.id.ll_pairing_activity_screen);
        mPairButtonView = (LinearLayout) findViewById(R.id.pairButtonView);
        mPairTipView = (LinearLayout) findViewById(R.id.pairTipView);
        mConnectDeviceView = (LinearLayout) findViewById(R.id.connectDeviceView); // Connect device view
        mNewDeviceView = (LinearLayout) findViewById(R.id.newDeviceView);
        mPairSearchView = (LinearLayout) findViewById(R.id.pairSearchView);
        mEnterPinView = (LinearLayout) findViewById(R.id.enterPinView);

        /* Font type */
        // Connect Screen
        TextView appBarTitle = (TextView) findViewById(R.id.flash_projects_title_txt);
        appBarTitle.setTypeface(MBApp.getApp().getTypeface());

        TextView manageMicrobit = (TextView) findViewById(R.id.title_manage_microbit);
        manageMicrobit.setTypeface(MBApp.getApp().getTypeface());

        TextView descriptionManageMicrobit = (TextView) findViewById(R.id.description_manage_microbit);
        descriptionManageMicrobit.setTypeface(MBApp.getApp().getTypeface());

        Button bluetoothSettings = (Button) findViewById(R.id.go_bluetooth_settings);
        bluetoothSettings.setTypeface(MBApp.getApp().getTypeface());

        Button pairButton = (Button) findViewById(R.id.pairButton);
        pairButton.setTypeface(MBApp.getApp().getTypeface());

        // Step 1 - How to pair your micro:bit
        TextView pairTipTitle = (TextView) findViewById(R.id.pairTipTitle);
        pairTipTitle.setTypeface(MBApp.getApp().getTypeface());

        TextView stepOneTitle = (TextView) findViewById(R.id.pair_tip_step_1_step);
        stepOneTitle.setTypeface(MBApp.getApp().getTypeface());

        TextView stepOneInstructions = (TextView) findViewById(R.id.pair_tip_step_1_instructions);
        stepOneInstructions.setTypeface(MBApp.getApp().getTypeface());

        Button cancelPairButton = (Button) findViewById(R.id.cancel_tip_step_1_btn);
        cancelPairButton.setTypeface(MBApp.getApp().getTypeface());

        Button nextPairButton = (Button) findViewById(R.id.ok_tip_step_1_btn);
        nextPairButton.setTypeface(MBApp.getApp().getTypeface());

        // Step 2 - Enter Pattern
        TextView enterPatternTitle = (TextView) findViewById(R.id.enter_pattern_step_2_title);
        enterPatternTitle.setTypeface(MBApp.getApp().getTypeface());

        TextView stepTwoTitle = (TextView) findViewById(R.id.pair_enter_pattern_step_2);
        stepTwoTitle.setTypeface(MBApp.getApp().getTypeface());

        TextView stepTwoInstructions = (TextView) findViewById(R.id.pair_enter_pattern_step_2_instructions);
        stepTwoInstructions.setTypeface(MBApp.getApp().getTypeface());

        Button cancelEnterPattern = (Button) findViewById(R.id.cancel_enter_pattern_step_2_btn);
        cancelEnterPattern.setTypeface(MBApp.getApp().getTypeface());

        Button okEnterPatternButton = (Button) findViewById(R.id.ok_enter_pattern_step_2_btn);
        okEnterPatternButton.setTypeface(MBApp.getApp().getTypeface());

        // Step 3 - Searching for micro:bit
        TextView searchMicrobitTitle = (TextView) findViewById(R.id.search_microbit_step_3_title);
        searchMicrobitTitle.setTypeface(MBApp.getApp().getTypeface());

        TextView stepThreeTitle = (TextView) findViewById(R.id.searching_microbit_step_3_step);
        stepThreeTitle.setTypeface(MBApp.getApp().getTypeface());

        TextView stepThreeInstructions = (TextView) findViewById(R.id.searching_microbit_step_3_instructions);
        stepThreeInstructions.setTypeface(MBApp.getApp().getTypeface());

        Button cancelSearchMicroBit = (Button) findViewById(R.id.cancel_search_microbit_step_3_btn);
        cancelSearchMicroBit.setTypeface(MBApp.getApp().getTypeface());

        // Step 4 - Enter Pin
        TextView enterPinTitle = (TextView) findViewById(R.id.enter_pin_step_4_title);
        enterPinTitle.setTypeface(MBApp.getApp().getTypeface());

        TextView stepFourTitle = (TextView) findViewById(R.id.enter_pin_step_4_step);
        stepFourTitle.setTypeface(MBApp.getApp().getTypeface());

        TextView stepFourInstructions = (TextView) findViewById(R.id.enter_pin_step_4_instructions);
        stepFourInstructions.setTypeface(MBApp.getApp().getTypeface());

        Button cancelEnterPin = (Button) findViewById(R.id.cancel_enter_pin_step_4_btn);
        cancelEnterPin.setTypeface(MBApp.getApp().getTypeface());

        // pin view
        displayConnectScreen(mState);
        findViewById(R.id.pairButton).setOnClickListener(this);
        findViewById(R.id.cancel_tip_step_1_btn).setOnClickListener(this);
        findViewById(R.id.ok_enter_pattern_step_2_btn).setOnClickListener(this);
        findViewById(R.id.cancel_enter_pattern_step_2_btn).setOnClickListener(this);
        findViewById(R.id.cancel_search_microbit_step_3_btn).setOnClickListener(this);
        findViewById(R.id.go_bluetooth_settings).setOnClickListener(this);
        findViewById(R.id.cancel_enter_pin_step_4_btn).setOnClickListener(this);

        // TODO - change animation
        // Animation
        WebView animation = (WebView) findViewById(R.id.animationwebView);
        animation.setBackgroundColor(Color.TRANSPARENT);
        animation.loadUrl("file:///android_asset/htmls/animation.html");
    }

    boolean setupBleController() {
        boolean retvalue = true;


        if (mBluetoothAdapter == null) {
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        if (mBluetoothAdapter == null) {
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

        Log.d("Microbit", "onActivityResult");
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                if (mActivityState == ACTIVITY_STATE.STATE_ENABLE_BT_FOR_PAIRING) {
                    startWithPairing();
                } else if (mActivityState == ACTIVITY_STATE.STATE_ENABLE_BT_FOR_CONNECT) {
                    toggleConnection(selectedDeviceForConnect);
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                PopUp.show(MBApp.getContext(),
                        getString(R.string.bluetooth_off_cannot_continue), //message
                        "",
                        R.drawable.error_face, R.drawable.red_btn,
                        PopUp.TYPE_ALERT,
                        null, null);
            }
            //Change state back to Idle
            mActivityState = ACTIVITY_STATE.STATE_IDLE;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void displayLedGrid() {
        GridView gridview = (GridView) findViewById(R.id.enter_pattern_step_2_gridview);
        gridview.setAdapter(new LEDAdapter(this, deviceCodeArray));
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if (mState != PAIRING_STATE.PAIRING_STATE_NEW_NAME) {

                    if ((findViewById(R.id.ok_enter_pattern_step_2_btn).getVisibility() != View.VISIBLE)) {
                        findViewById(R.id.ok_enter_pattern_step_2_btn).setVisibility(View.VISIBLE);
                    }

                    boolean isOn = toggleLED((ImageView) v, position);
                    setCol(parent, position, isOn);
                    //Toast.makeText(MBApp.getContext(), "LED Clicked: " + position, Toast.LENGTH_SHORT).show();

                    if (!Arrays.asList(deviceCodeArray).contains("1")) {
                        findViewById(R.id.ok_enter_pattern_step_2_btn).setVisibility(View.INVISIBLE);
                    }
                }
                //TODO KEEP TRACK OF ALL LED STATUS AND TOGGLE COLOR

            }
        });

        if (!Arrays.asList(deviceCodeArray).contains("1")) {
            findViewById(R.id.ok_enter_pattern_step_2_btn).setVisibility(View.INVISIBLE);
        } else
            findViewById(R.id.ok_enter_pattern_step_2_btn).setVisibility(View.VISIBLE);
    }

    private void generateName() {

        mNewDeviceName = "";
        mNewDeviceCode = "";
        //Columns
        for (int col = 0; col < 5; col++) {
            //Rows
            for (int row = 0; row < 5; row++) {
                if (deviceCodeArray[(col + (5 * row))] == "1") {
                    mNewDeviceName += deviceNameMapArray[(col + (5 * row))];
                    break;
                }
            }
        }
        mNewDeviceCode = mNewDeviceName;
        mNewDeviceName = "BBC microbit [" + mNewDeviceName + "]";
        //Toast.makeText(this, "Pattern :"+mNewDeviceCode, Toast.LENGTH_SHORT).show();
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
        mPrevDeviceArray = mPrevDevList.loadPrevMicrobits();
        if (mPrevDevList != null)
            numOfPreviousItems = mPrevDevList.size();

        if ((numOfPreviousItems > 0) && (mCurrentDevice != null) &&
                (mPrevDeviceArray[0].mPattern.equals(mCurrentDevice.mPattern))) {
            mPrevDeviceArray[0].mStatus = mCurrentDevice.mStatus;
        }

        for (int i = 0; i < numOfPreviousItems; i++) {
            connectedDeviceList.add(mPrevDeviceArray[i]);
        }
        for (int i = numOfPreviousItems; i < 1; i++) {
            connectedDeviceList.add(new ConnectedDevice(null, null, false, null, 0));
        }


        if (isupdate) {
            connectedDeviceAdapter.updateAdapter(connectedDeviceList);

        } else {
            connectedDeviceAdapter = new ConnectedDeviceAdapter(this, connectedDeviceList);
            lvConnectedDevice.setAdapter(connectedDeviceAdapter);
        }

    }

    public static boolean disableListView() {
        return DISABLE_DEVICE_LIST;
    }

    private boolean isPortraitMode() {
        return (mBottomPairButton != null);
    }

    private void displayConnectScreen(PAIRING_STATE gotoState) {
        mPairTipView.setVisibility(View.GONE);
        mNewDeviceView.setVisibility(View.GONE);
        mPairSearchView.setVisibility(View.GONE);
        mEnterPinView.setVisibility(View.GONE); // disable
        Log.d("Microbit", "********** Connect: state from " + mState + " to " + gotoState);
        mState = gotoState;

        if ((gotoState == PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON) ||
                (gotoState == PAIRING_STATE.PAIRING_STATE_ERROR))
            DISABLE_DEVICE_LIST = false;
        else
            DISABLE_DEVICE_LIST = true;

        if (isPortraitMode() && (disableListView())) {
            //
        } else {
            populateConnectedDeviceList(true);
            mConnectDeviceView.setVisibility(View.VISIBLE);
        }

        switch (gotoState) {
            case PAIRING_STATE_CONNECT_BUTTON:
                break;

            case PAIRING_STATE_ERROR:
                //   mPairButtonView.setVisibility(View.VISIBLE);// TODO debug - error case
                lvConnectedDevice.setEnabled(true);
                Arrays.fill(deviceCodeArray, "0");
                findViewById(R.id.enter_pattern_step_2_gridview).setEnabled(true);
                mNewDeviceName = "";
                mNewDeviceCode = "";
                break;

            case PAIRING_STATE_TIP:
                mPairTipView.setVisibility(View.VISIBLE);
                mConnectDeviceView.setVisibility(View.GONE);
                findViewById(R.id.ok_tip_step_1_btn).setOnClickListener(this);
                break;

            case PAIRING_STATE_PATTERN_EMPTY:
                findViewById(R.id.enter_pattern_step_2_gridview).setEnabled(true);
                findViewById(R.id.connectedDeviceList).setClickable(true);
                mNewDeviceView.setVisibility(View.VISIBLE);
                findViewById(R.id.cancel_enter_pattern_step_2_btn).setVisibility(View.VISIBLE);
                findViewById(R.id.enter_pattern_step_2_title).setVisibility(View.VISIBLE);

                // test
                findViewById(R.id.ok_enter_pattern_step_2_btn).setVisibility(View.GONE);

                displayLedGrid();
                break;

            case PAIRING_STATE_NEW_NAME:
                findViewById(R.id.enter_pattern_step_2_gridview).setEnabled(false);
                findViewById(R.id.connectedDeviceList).setClickable(false);
                mNewDeviceView.setVisibility(View.VISIBLE);

                if ((mPrevDeviceArray == null) || (mPrevDeviceArray[0].mName == null) || (mPrevDeviceArray[0].mName.equals(""))) {

                } else {

                }

                findViewById(R.id.ok_enter_pattern_step_2_btn).setVisibility(View.VISIBLE);
                findViewById(R.id.cancel_enter_pattern_step_2_btn).setVisibility(View.VISIBLE);
                displayLedGrid();
                break;

            case PAIRING_STATE_SEARCHING:
                mPairSearchView.setVisibility(View.VISIBLE);
                break;
        }
    }

    private TextView.OnEditorActionListener editorOnActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            boolean handled = true;
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dismissKeyBoard(v, true, true);
            } else if (actionId == -1) {
                dismissKeyBoard(v, true, false);
            }
            return handled;
        }
    };

    private void dismissKeyBoard(View v, boolean hide, boolean done) {
        if (done) {
            EditText ed = (EditText) v;
            String newName = ed.getText().toString().trim();
            if (newName.isEmpty()) {
                ed.setText("");
                ed.setError(getString(R.string.name_empty_error));
            } else {
                hideKeyboard(v);
                mPrevDeviceArray[0].mName = newName;
                mPrevDevList.changeMicrobitName(0, mPrevDeviceArray[0]);
                populateConnectedDeviceList(true);
                ed.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void startBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
    }

    public void startWithPairing() {
        if (mBottomPairButton != null) {
            mConnectDeviceView.setVisibility(View.GONE);
        }

        if (mPairButtonView != null) {
            displayConnectScreen(PAIRING_STATE.PAIRING_STATE_TIP);
        }
    }

    public void toggleConnection(int pos) {
        boolean currentState = mPrevDeviceArray[pos].mStatus;
        if (!currentState) {
            PopUp.show(MBApp.getContext(),
                    getString(R.string.init_connection),
                    "",
                    R.drawable.message_face, R.drawable.blue_btn,
                    PopUp.TYPE_SPINNER,
                    null, null);
            mPrevDevList.changeMicrobitState(pos, mPrevDeviceArray[pos], true, false);
            IPCService.getInstance().bleConnect();
        } else {
            mPrevDeviceArray[pos].mStatus = !currentState;
            mPrevDevList.changeMicrobitState(pos, mPrevDeviceArray[pos], false, false);
            populateConnectedDeviceList(true);
        }
    }

    public void onClick(final View v) {
        int pos;

        switch (v.getId()) {
            case R.id.pairButton:
                if (debug) logi("onClick() :: pairButton");
                if (!BluetoothSwitch.getInstance().isBluetoothON()) {
                    mActivityState = ACTIVITY_STATE.STATE_ENABLE_BT_FOR_PAIRING;
                    startBluetooth();
                    return;
                }
                startWithPairing();
                break;
            case R.id.go_bluetooth_settings: //Bluetooth
                Intent goToBlueToothIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(goToBlueToothIntent);
                break;
            case R.id.ok_tip_step_1_btn:
                if (debug) logi("onClick() :: ok_pair_button");
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_PATTERN_EMPTY);
                break;

            case R.id.ok_enter_pattern_step_2_btn:
                if (debug) logi("onClick() :: ok_name_button");
                if (mState == PAIRING_STATE.PAIRING_STATE_PATTERN_EMPTY) {
                    generateName();
                    if (!BluetoothSwitch.getInstance().checkBluetoothAndStart()) {
                        return;
                    }
                    scanLeDevice(true);
                    displayConnectScreen(PAIRING_STATE.PAIRING_STATE_SEARCHING);
                    break;
                }
                break;

            case R.id.cancel_tip_step_1_btn:
                if (debug) logi("onClick() :: cancel_tip_button");
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                break;

            case R.id.cancel_enter_pattern_step_2_btn:
                if (debug) logi("onClick() :: cancel_name_button");
                cancelPairing();
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                break;

            case R.id.cancel_search_microbit_step_3_btn:
                if (debug) logi("onClick() :: cancel_search_button");
                scanLeDevice(false);
                cancelPairing();
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                break;

            case R.id.connectBtn:
                if (debug) logi("onClick() :: connectBtn");
                pos = (Integer) v.getTag();
                if (!BluetoothSwitch.getInstance().isBluetoothON()) {
                    selectedDeviceForConnect = pos;
                    mActivityState = ACTIVITY_STATE.STATE_ENABLE_BT_FOR_CONNECT;
                    startBluetooth();
                    return;
                }
                toggleConnection(pos);
                break;

            case R.id.deleteBtn:
                if (debug) logi("onClick() :: deleteBtn");
                pos = (Integer) v.getTag();
                handleDeleteMicrobit(pos);
                break;
            case R.id.cancel_enter_pin_step_4_btn:
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                break;
            case R.id.backBtn:
                if (debug) logi("onClick() :: backBtn");
                handleResetAll();
                break;

            default:
                Toast.makeText(MBApp.getContext(), "Default Item Clicked: " + v.getId(), Toast.LENGTH_SHORT).show();
                break;

        }
    }

    public void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
    }

    private void handleDeleteMicrobit(final int pos) {
        PopUp.show(this,
                getString(R.string.deleteMicrobitMessage), //message
                getString(R.string.deleteMicrobitTitle), //title
                R.drawable.delete_project, R.drawable.red_btn,
                PopUp.TYPE_CHOICE, //type of popup.
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopUp.hide();
                        mPrevDevList.removeMicrobit(pos);
                        populateConnectedDeviceList(true);
                    }
                },//override click listener for ok button
                null);//pass null to use default listener
    }

    private void handleResetAll() {
        Arrays.fill(deviceCodeArray, "0");
        scanLeDevice(false);
        cancelPairing();

        if (!isPortraitMode()) {
            mState = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
            finish();
        } else if (isPortraitMode() && mState == PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON) {
            finish();
        } else {
            displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
        }
    }

    private void handlePairingFailed() {

        if (debug) logi("handlePairingFailed() :: Start");
        mPairing = false;

        //displayConnectScreen(PAIRING_STATE.PAIRING_STATE_ERROR);

        PopUp.show(this,
                getString(R.string.pairingErrorMessage), //message
                getString(R.string.timeOut), //title
                R.drawable.error_face, //image icon res id
                R.drawable.red_btn,
                PopUp.TYPE_ALERT, //type of popup.
                null,//override click listener for ok button
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopUp.hide();
                        displayConnectScreen(PAIRING_STATE.PAIRING_STATE_PATTERN_EMPTY);
                    }
                });//pass null to use default listener

    }

    private void handlePairingSuccessful(final ConnectedDevice newDev) {
        mPairing = false;

        final Runnable task = new Runnable() {

            @Override
            public void run() {

                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                //= new ConnectedDevice(null, mNewDeviceCode, true, device.getAddress() );
                int oldId = mPrevDevList.checkDuplicateMicrobit(newDev);
                mPrevDevList.addMicrobit(newDev, oldId);
                populateConnectedDeviceList(true);

                if (debug) logi("handlePairingSuccessful() :: sending intent to BLEService.class");
                // Pairing successful hide enter pin screen
                if (mEnterPinView != null) {
                    mEnterPinView.setVisibility(View.GONE);
                }
                runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      Toast.makeText(MBApp.getContext(), "Paired successfully", Toast.LENGTH_LONG).show();
                                  }
                              }
                );
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                // Pop up to show pairing successful
/*                PopUp.show(MBApp.getContext(),
                        " Micro:bit paired successfully", // message
                        getString(R.string.pairing_success_message_1), //title
                        R.drawable.message_face, //image icon res id
                        R.drawable.green_btn,
                        PopUp.TYPE_ALERT, //type of popup.
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PopUp.hide();
                                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                            }

                        }, null);*/
                //pass null to use default listener
                //displayConnectScreen(PAIRING_STATE.PAIRING_STATE_NEW_NAME);

            }
        };

        new Handler(Looper.getMainLooper()).post(task);
    }

    private void registerCallBacksForPairing() {
        IntentFilter filter = new IntentFilter(DfuService.BROADCAST_LOG);
        IntentFilter filter1 = new IntentFilter(DfuService.BROADCAST_ERROR);
        dfuResultReceiver = new DFUResultReceiver();
        LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(dfuResultReceiver, filter);
        LocalBroadcastManager.getInstance(MBApp.getContext()).registerReceiver(dfuResultReceiver, filter1);
    }

    private void startPairing(String deviceAddress) {

        if (dfuResultReceiver != null) {
            LocalBroadcastManager.getInstance(MBApp.getContext()).unregisterReceiver(dfuResultReceiver);
            dfuResultReceiver = null;
        }
        registerCallBacksForPairing();
        logi("###>>>>>>>>>>>>>>>>>>>>> startPairing");
        mPairing = true;
        final Intent service = new Intent(this, DfuService.class);
        service.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, deviceAddress);
        service.putExtra(DfuService.EXTRA_KEEP_BOND, false);
        service.putExtra(DfuService.INTENT_RESULT_RECEIVER, resultReceiver);
        service.putExtra(DfuService.INTENT_REQUESTED_PHASE, 1);
        startService(service);
    }

    private void cancelPairing() {
        logi("###>>>>>>>>>>>>>>>>>>>>> cancelPairing");
        scanLeDevice(false);//TODO: is it really needed?

        if (mPairing) {
            final Intent abortIntent = new Intent(this, DfuService.class);
            abortIntent.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_ABORT);
            startService(abortIntent);
            mPairing = false;
        }
    }

    /*
     * TODO : Part of HACK 20150729
     * =================================================================
     */
    private void scanLeDevice(final boolean enable) {

        if (debug) logi("scanLeDevice() :: enable = " + enable);
        if (enable) {
            if (!setupBleController()) {
                if (debug) logi("scanLeDevice() :: FAILED ");
                return;
            }
            if (!mScanning && !mPairing) {
                if (debug) logi("scanLeDevice ::   Searching For " + mNewDeviceName.toLowerCase());
                // Stops scanning after a pre-defined scan period.
                mScanning = true;
                TextView textView = (TextView) findViewById(R.id.search_microbit_step_3_title);
                if (textView != null)
                    textView.setText(getString(R.string.searchingTitle));

                mHandler.postDelayed(scanTimedOut, SCAN_PERIOD);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { //Lollipop
                    mBluetoothAdapter.startLeScan((BluetoothAdapter.LeScanCallback) getBlueToothCallBack());
                } else {
                    List<ScanFilter> filters = new ArrayList<ScanFilter>();
                    // TODO: play with ScanSettings further to ensure the Kit kat devices connect with higher success rate
                    ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                    mLEScanner.startScan(filters, settings, (ScanCallback) getBlueToothCallBack());
                }
            }
        } else {
            if (mScanning) {
                mScanning = false;
                mHandler.removeCallbacks(scanTimedOut);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    mBluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback) getBlueToothCallBack());
                } else {
                    mLEScanner.stopScan((ScanCallback) getBlueToothCallBack());
                }
            }
        }
    }

    private static Runnable scanTimedOut = new Runnable() {
        @Override
        public void run() {
            PairingActivity.instance.scanFailedCallbackImpl();
        }
    };

    private void scanFailedCallbackImpl() {
        if (mPairing) {
            return;
        }

        boolean scanning = mScanning;
        scanLeDevice(false);

        if (scanning) { // was scanning
            handlePairingFailed();
        }
    }

    private static Object mBluetoothScanCallBack = null;

    private static Object getBlueToothCallBack() {
        if (mBluetoothScanCallBack == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBluetoothScanCallBack = (ScanCallback) new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        Log.i("callbackType = ", String.valueOf(callbackType));
                        Log.i("result = ", result.toString());
                        BluetoothDevice btDevice = result.getDevice();
                        PairingActivity.instance.onLeScan(btDevice, result.getRssi(), result.getScanRecord().getBytes());
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

                return (mBluetoothScanCallBack);
            } else {
                mBluetoothScanCallBack = new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                        PairingActivity.instance.onLeScan(device, rssi, scanRecord);
                    }
                };
            }
        }
        return mBluetoothScanCallBack;
    }

    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

        if (debug) logi("mLeScanCallback.onLeScan() [+]");

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

        if ((mNewDeviceName.isEmpty()) || (device.getName() == null)) {
            if (debug)
                logi("mLeScanCallback.onLeScan() ::   Cannot Compare " + device.getAddress() + " " + rssi + " " + scanRecord.toString());
        } else {
            String s = device.getName().toLowerCase();
            //Replace all : to blank - Fix for #64
            //TODO Use pattern recognition instead
            s = s.replaceAll(":", "");
            if (mNewDeviceName.toLowerCase().equals(s)) {

                if (debug)
                    logi("mLeScanCallback.onLeScan() ::   Found micro:bit -" + device.getName().toLowerCase() + " " + device.getAddress());
                // Stop scanning as device is found.
                scanLeDevice(false);
                mNewDeviceAddress = device.getAddress();
                //Rohit : Do Not Call the TextView.setText directly. It doesn't work on 4.4.4
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView) findViewById(R.id.search_microbit_step_3_title);
                        if (textView != null)
                            textView.setText(getString(R.string.pairing_msg_1));
                        startPairing(mNewDeviceAddress);
                    }
                });
            } else {

                if (debug)
                    logi("mLeScanCallback.onLeScan() ::   Found - device.getName() == " + device.getName().toLowerCase());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPairTipView.setVisibility(View.GONE);
        mNewDeviceView.setVisibility(View.GONE);
        mPairSearchView.setVisibility(View.GONE);
        mConnectDeviceView.setVisibility(View.GONE); // TODO check this
        mEnterPinView.setVisibility(View.GONE); // TODO check this
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (debug) logi("onKeyDown() :: Cancel");
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
