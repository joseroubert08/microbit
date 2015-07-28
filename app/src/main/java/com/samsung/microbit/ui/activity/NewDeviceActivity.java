package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.PreviousDeviceList;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.adapter.LEDAdapter;

import java.util.Arrays;

public class NewDeviceActivity extends Activity implements View.OnClickListener {

    private enum PAIRING_STATE {
        PAIRING_STATE_CONNECT_BUTTON,
        PAIRING_STATE_TIP,
        PAIRING_STATE_PATTERN_EMPTY,
        PAIRING_STATE_SEARCHING,
        PAIRING_STATE_ERROR,
        PAIRING_STATE_NEW_NAME
    };

    private PAIRING_STATE state;

    private String newDeviceName;
    private String newDeviceCode;
    private String newDeviceDisplayName;
    ConnectedDevice newDev;

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
    RelativeLayout connectTipView;
    RelativeLayout newDeviceView;
    RelativeLayout connectSearchView;

    private Handler mHandler;
    private Runnable scanFailedCallback;
    private boolean mScanning;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 15000;
    private ProgressDialog pairingProgressDialog;
    private BluetoothAdapter mBluetoothAdapter = null;

    PreviousDeviceList prevDevList;

    // DEBUG
    protected boolean debug = true;
    protected String TAG = "NewDeviceActivity";

    protected void logi(String message) {
        Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MBApp.setContext(this);
        setContentView(R.layout.activity_new_device);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this.getApplicationContext(), R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mHandler = new Handler(Looper.getMainLooper());

        connectTipView = (RelativeLayout) findViewById(R.id.connectTipView);
        newDeviceView = (RelativeLayout) findViewById(R.id.newDeviceView);
        connectSearchView = (RelativeLayout) findViewById(R.id.connectSearchView);

        if(prevDevList == null )
            prevDevList = PreviousDeviceList.getInstance(this);

        displayConnectScreen(PAIRING_STATE.PAIRING_STATE_TIP);

        findViewById(R.id.cancel_tip_button).setOnClickListener(this);
        findViewById(R.id.ok_name_button).setOnClickListener(this);
        findViewById(R.id.cancel_name_button).setOnClickListener(this);
        findViewById(R.id.cancel_search_button).setOnClickListener(this);

        //Animation
        WebView animation = (WebView) findViewById(R.id.animationwebView);
        animation.setBackgroundColor(Color.TRANSPARENT);
        animation.loadUrl("file:///android_asset/htmls/animation.html");
    }

    private void displayLedGrid() {
        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new LEDAdapter(this));
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if (state != PAIRING_STATE.PAIRING_STATE_NEW_NAME) {

                    if ((findViewById(R.id.ok_name_button).getVisibility() != View.VISIBLE)) {
                        findViewById(R.id.ok_name_button).setVisibility(View.VISIBLE);
                        findViewById(R.id.cancel_name_button).setVisibility(View.VISIBLE);
                        ((TextView) findViewById(R.id.newDeviceTxt)).setText(R.string.new_devices);
                    }
                    boolean isOn = toggleLED((ImageView) v, position);
                    setCol(parent, position, isOn);
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
        //Toast.makeText(this, "Pattern :"+newDeviceCode, Toast.LENGTH_SHORT).show();
    }

    private void setCol(AdapterView<?> parent, int pos, boolean enabledlandscape)
    {
        int index=pos-5;
        ImageView v;

        while(index >= 0)
        {
            v = (ImageView) parent.getChildAt(index);
            v.setBackground(getApplication().getResources().getDrawable(R.drawable.white_red_led_btn));
            v.setTag("0");
            index -=5;
        }
        index = pos+5;
        while(index < 25){
            v = (ImageView) parent.getChildAt(index);
            v.setBackground(getApplication().getResources().getDrawable(R.drawable.red_white_led_btn));
            v.setTag("1");
            index +=5;
        }

    }

    private boolean toggleLED(ImageView image, int pos) {
        boolean isOn;
        //Toast.makeText(this, "Pos :" +  pos, Toast.LENGTH_SHORT).show();
        if (image.getTag() != "1") {
            deviceCodeArray[pos] = "1";
            image.setBackground(getApplication().getResources().getDrawable(R.drawable.red_white_led_btn));
            image.setTag("1");
            isOn=true;
        } else {
            deviceCodeArray[pos] = "0";
            image.setBackground(getApplication().getResources().getDrawable(R.drawable.white_red_led_btn));
            image.setTag("0");
            isOn=false;
        }
        return isOn;
    }
    private void displayConnectScreen(PAIRING_STATE gotoState) {
        connectTipView.setVisibility(View.GONE);
        newDeviceView.setVisibility(View.GONE);
        connectSearchView.setVisibility(View.GONE);

        switch (gotoState) {
            case PAIRING_STATE_TIP:
                connectTipView.setVisibility(View.VISIBLE);
                findViewById(R.id.ok_connect_button).setOnClickListener(this);
                break;
            case PAIRING_STATE_PATTERN_EMPTY:
                findViewById(R.id.gridview).setEnabled(true);
                //findViewById(R.id.connectedDeviceList).setClickable(true);
                newDeviceView.setVisibility(View.VISIBLE);
                findViewById(R.id.cancel_name_button).setVisibility(View.VISIBLE);
                findViewById(R.id.newDeviceTxt).setVisibility(View.VISIBLE);
                //findViewById(R.id.nameNewTxt).setVisibility(View.GONE);
                findViewById(R.id.nameNewEdit).setVisibility(View.GONE);
                findViewById(R.id.ok_name_button).setVisibility(View.GONE);
                break;
			/*case PAIRING_STATE_PATTERN_CHANGED:
				//newDeviceView.setVisibility(View.VISIBLE);
				findViewById(R.id.ok_pattern_button).setVisibility(View.VISIBLE);
				break;*/
            case PAIRING_STATE_NEW_NAME:
                findViewById(R.id.gridview).setEnabled(false);
                newDeviceView.setVisibility(View.VISIBLE);
                ((EditText) findViewById(R.id.nameNewEdit)).setText(" ");
                //((TextView) findViewById(R.id.nameNewTxt)).setText(getString(R.string.name_device) + " " + newDeviceCode);
                //findViewById(R.id.nameNewTxt).setVisibility(View.VISIBLE);
                EditText editText = (EditText) findViewById(R.id.nameNewEdit);
                editText.setText(newDeviceCode);
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
            case PAIRING_STATE_CONNECT_BUTTON:
                finish();
                break;
        }
        ;
    }

    public void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public void onClick(final View v) {


        switch (v.getId()) {
            case R.id.ok_connect_button:
                state = PAIRING_STATE.PAIRING_STATE_PATTERN_EMPTY;
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_PATTERN_EMPTY);
                displayLedGrid();
                break;
            case R.id.ok_name_button:
                if(state == PAIRING_STATE.PAIRING_STATE_PATTERN_EMPTY) {
                    state = PAIRING_STATE.PAIRING_STATE_SEARCHING;
                    generateName();
                    scanLeDevice(true);
                    displayConnectScreen(state);
                    break;
                }
                EditText editText = (EditText) findViewById(R.id.nameNewEdit);
                String newname = editText.getText().toString().trim();
                if (newname.isEmpty()) {
                    editText.setText("");
                    editText.setError(getString(R.string.name_empty_error));
                }
                else {
                    hideKeyboard();
                    newDev.mName = newname;
                    prevDevList.changeMicrobitName(0, newDev);
                    state = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
                    displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                }

                break;
            case R.id.cancel_tip_button:
            case R.id.cancel_name_button:
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                state = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
                break;
            case R.id.cancel_search_button:
                scanLeDevice(false);
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                state = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
                break;
            case R.id.backBtn:
                finish();
                break;
            default:
                Toast.makeText(MBApp.getContext(), "Default Item Clicked: " + v.getId(), Toast.LENGTH_SHORT).show();
                break;

        }
    }
    private volatile boolean deviceFound = false;

    private void handle_pairing_failed() {

        if(debug) logi("handle_pairing_failed() :: Start");

        // dummy code to test addition of MBits
        /*if(debug) {
            if (!newDeviceCode.equalsIgnoreCase("vuvuv")) {

                state = PAIRING_STATE.PAIRING_STATE_NEW_NAME;
                displayConnectScreen(state);
                newDev = new ConnectedDevice(null, newDeviceCode, false, "ab.cd.ef.gh.ij.56");
                prevDevList.addMicrobit(newDev, prevDevList.PREVIOUS_DEVICES_MAX);
                return;

            }
        }*/

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

                    newDev = new ConnectedDevice(null, newDeviceCode.toUpperCase(), false, device.getAddress());
                    handle_pairing_successful(newDev);
                } else {
                    if(debug) logi("mLeScanCallback.onLeScan() ::   non-matching - deviceName == " + newDeviceName.toLowerCase());
                    if(debug) logi("mLeScanCallback.onLeScan() ::   non-matching found - device.getName() == " + device.getName().toLowerCase());
                }
            }
        }
    };
    private void handle_pairing_successful(final ConnectedDevice newDev) {

        if(debug) logi("handle_pairing_successful() :: Start");


        final Runnable task = new Runnable() {

            @Override
            public void run() {

                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                //= new ConnectedDevice(null, newDeviceCode, true, device.getAddress() );
                int oldId = prevDevList.checkDuplicateMicrobit(newDev);
                prevDevList.addMicrobit(newDev, oldId);

                if(debug) logi("mLeScanCallback.onLeScan() ::   Matching DEVICE FOUND, Pairing");
                if(debug) logi("handle_pairing_successful() :: sending intent to BLEService.class");

                state = PAIRING_STATE.PAIRING_STATE_NEW_NAME;
                displayConnectScreen(state);

            }
        };
        new Handler(Looper.getMainLooper()).post(task);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_launcher, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
