package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.adapter.ConnectedDeviceAdapter;
import com.samsung.microbit.ui.adapter.LEDAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class ConnectActivity extends Activity implements View.OnClickListener  {
    private SharedPreferences preferences;
    private final String PREFERENCES_PREVDEV_PREFNAME = "PreviousDevices";
    private final String PREFERENCES_PREVDEV_KEY = "PreviousDevicesKey";
    private final int PREVIOUS_DEVICES_MAX=3;

    StoredDevice []prevDeviceArray;
    ArrayList prevMicrobitList;
    private enum PAIRING_STATE {
        PAIRING_STATE_CONNECT_BUTTON,
        PAIRING_STATE_TIP,
        PAIRING_STATE_PATTERN_EMPTY,
        PAIRING_STATE_PATTERN_CHANGED,
        PAIRING_STATE_SEARCHING,
        PAIRING_STATE_ERROR,
        PAIRING_STATE_NEW_NAME
    };

    private PAIRING_STATE state;

    private String newDeviceName;
    private String newDeviceCode;
    private String newDeviceDisplayName;

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


    RelativeLayout connectButtonView;
    RelativeLayout connectTipView;
    RelativeLayout newDeviceView;
    RelativeLayout connectSearchView;


    List<ConnectedDevice> connectedDeviceList = new ArrayList<ConnectedDevice>();
    ConnectedDeviceAdapter connectedDeviceAdapter;
    private ListView lvConnectedDevice;



    private Handler mHandler;
    private Runnable scanFailedCallback;
    private boolean mScanning;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 1000; //15000;
    private ProgressDialog pairingProgressDialog;
    private BluetoothAdapter mBluetoothAdapter = null;
    private Boolean isBLuetoothEnabled = false;
    final private int REQUEST_BT_ENABLE=1;

    // DEBUG
    protected boolean debug = true;
    protected String TAG = "ConnectActivity";
    protected void logi(String message) {
        if (debug) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MBApp.setContext(this);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_connect);

        LinearLayout mainContentView = (LinearLayout) findViewById(R.id.mainContentView);
        mainContentView.getBackground().setAlpha(128);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this.getApplicationContext(), R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);
        }

        mHandler = new Handler();

        prevDeviceArray = new StoredDevice[3];
        lvConnectedDevice = (ListView) findViewById(R.id.connectedDeviceList);
        populateConnectedDeviceList(false);


        connectButtonView = (RelativeLayout) findViewById(R.id.connectButtonView);
        connectTipView = (RelativeLayout) findViewById(R.id.connectTipView);
        newDeviceView = (RelativeLayout) findViewById(R.id.newDeviceView);
        connectSearchView = (RelativeLayout) findViewById(R.id.connectSearchView);

        displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
        findViewById(R.id.connectButton).setOnClickListener(this);
        findViewById(R.id.ok_pattern_button).setOnClickListener(this);
        findViewById(R.id.ok_name_button).setOnClickListener(this);
        findViewById(R.id.cancel_name_button).setOnClickListener(this);
        findViewById(R.id.cancel_search_button).setOnClickListener(this);
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

    private void  displayLedGrid()
    {
        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new LEDAdapter(this));
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if (state != PAIRING_STATE.PAIRING_STATE_NEW_NAME) {

                    if ((findViewById(R.id.ok_pattern_button).getVisibility() != View.VISIBLE)
                            && (findViewById(R.id.ok_name_button).getVisibility() != View.VISIBLE)) {
                        findViewById(R.id.ok_pattern_button).setVisibility(View.VISIBLE);
                        ((TextView) findViewById(R.id.newDeviceTxt)).setText(R.string.new_devices);
                    }
                    toggleLED((ImageView) v, position);
                    Toast.makeText(MBApp.getContext(), "LED Clicked: " + position, Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Pattern :"+newDeviceCode, Toast.LENGTH_SHORT).show();
    }

    private void toggleLED(ImageView image, int pos) {
        if (image.getTag() != "1") {
            deviceCodeArray[pos] = "1";
            image.setBackground(getApplication().getResources().getDrawable(R.drawable.red_white_led_btn));
            image.setTag("1");
        } else {
            deviceCodeArray[pos] = "0";
            image.setBackground(getApplication().getResources().getDrawable(R.drawable.white_red_led_btn));
            image.setTag("0");
        }
    }

    private void populateConnectedDeviceList(boolean isupdate)
    {
        connectedDeviceList.clear();
        int numOfPreviousItems=0;
        /* Get Previous connected devices */
        prevMicrobitList = loadPrevMicrobits();
        if(prevMicrobitList != null)
            numOfPreviousItems = prevMicrobitList.size();


        for(int i=0; i< numOfPreviousItems; i++) {
            connectedDeviceList.add(new ConnectedDevice(prevDeviceArray[i].deviceDisplayName,
                    prevDeviceArray[i].deviceName, prevDeviceArray[i].isConnected));
        }
        for(int i=numOfPreviousItems; i<PREVIOUS_DEVICES_MAX; i++) {
            connectedDeviceList.add(new ConnectedDevice(null, null, false));
        }

     //   connectedDeviceList.add(new ConnectedDevice("Kuhee MB 0", "VUVUVU", true));


        if(isupdate) {
            connectedDeviceAdapter.updateAdapter(connectedDeviceList);
            lvConnectedDevice.setAdapter(connectedDeviceAdapter);

        } else {
            connectedDeviceAdapter = new ConnectedDeviceAdapter(this, connectedDeviceList);
            lvConnectedDevice.setAdapter(connectedDeviceAdapter);
        }
    }


    private void displayConnectScreen(PAIRING_STATE gotoState)
    {
        connectButtonView.setVisibility(View.GONE);
        connectTipView.setVisibility(View.GONE);
        newDeviceView.setVisibility(View.GONE);
        connectSearchView.setVisibility(View.GONE);

        switch(gotoState)
        {
            case PAIRING_STATE_CONNECT_BUTTON:
                connectButtonView.setVisibility(View.VISIBLE);
                break;
            case PAIRING_STATE_TIP:
                connectTipView.setVisibility(View.VISIBLE);
                findViewById(R.id.ok_connect_button).setOnClickListener(this);
                break;
            case PAIRING_STATE_PATTERN_EMPTY:
                newDeviceView.setVisibility(View.VISIBLE);
                findViewById(R.id.ok_pattern_button).setVisibility(View.GONE);
                findViewById(R.id.ok_name_button).setVisibility(View.GONE);
                findViewById(R.id.newDeviceTxt).setVisibility(View.VISIBLE);
                findViewById(R.id.nameNewTxt).setVisibility(View.GONE);
                findViewById(R.id.nameNewEdit).setVisibility(View.GONE);
                findViewById(R.id.ok_name_button).setVisibility(View.GONE);
                findViewById(R.id.cancel_name_button).setVisibility(View.GONE);
                break;
            case PAIRING_STATE_PATTERN_CHANGED:
                //newDeviceView.setVisibility(View.VISIBLE);
                findViewById(R.id.ok_pattern_button).setVisibility(View.VISIBLE);
                break;
            case PAIRING_STATE_NEW_NAME:
                newDeviceView.setVisibility(View.VISIBLE);
                findViewById(R.id.ok_pattern_button).setVisibility(View.GONE);
                ((EditText)findViewById(R.id.nameNewEdit)).setText(" ");
                findViewById(R.id.nameNewTxt).setVisibility(View.VISIBLE);
                findViewById(R.id.nameNewEdit).setVisibility(View.VISIBLE);
                findViewById(R.id.ok_name_button).setVisibility(View.VISIBLE);
                findViewById(R.id.cancel_name_button).setVisibility(View.VISIBLE);
                break;
            case PAIRING_STATE_SEARCHING:
                connectSearchView.setVisibility(View.VISIBLE);
                break;
            case PAIRING_STATE_ERROR:
                connectSearchView.setVisibility(View.VISIBLE);
                newDeviceView.setVisibility(View.VISIBLE);
                break;
        };
    }

    public void onClick(final View v) {

        int pos;

        switch (v.getId()) {
            case R.id.connectButton:
                state = PAIRING_STATE.PAIRING_STATE_TIP;
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_TIP);
                break;
            case R.id.ok_connect_button:
                state = PAIRING_STATE.PAIRING_STATE_PATTERN_EMPTY;
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_PATTERN_EMPTY);
                displayLedGrid();
                break;
            case R.id.ok_pattern_button:
                state = PAIRING_STATE.PAIRING_STATE_SEARCHING;
                generateName();
                scanLeDevice(true);
                displayConnectScreen(state);

                break;
            case  R.id.ok_name_button:
                String newname = ((EditText)findViewById(R.id.nameNewEdit)).getText().toString();
                if(!newname.isEmpty()) {
                    prevDeviceArray[0].deviceDisplayName = newname;
                    changeMicrobitName(0, prevDeviceArray[0]);
                    displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);

                } else
                    Toast.makeText(MBApp.getContext(), "Enter Name ", Toast.LENGTH_SHORT).show();
                break;
            case  R.id.cancel_name_button:
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                state = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
                break;
            case  R.id.cancel_search_button:
                scanLeDevice(false);
                displayConnectScreen(PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON);
                state = PAIRING_STATE.PAIRING_STATE_CONNECT_BUTTON;
                break;
            case R.id.connectBtn:
                pos = (Integer)v.getTag();
                boolean toTurnON=false;
                if (!prevDeviceArray[pos].isConnected)
                    toTurnON=true;
                prevDeviceArray[pos].isConnected = !prevDeviceArray[pos].isConnected;
                changeMicrobitState(pos, prevDeviceArray[pos], toTurnON);
                break;
            case R.id.deleteBtn:
                pos = (Integer)v.getTag();
                removeMicrobit(pos);
                break;
            default:
                Toast.makeText(MBApp.getContext(), "Default Item Clicked: " + v.getId(), Toast.LENGTH_SHORT).show();
                break;

        }
    }

    public void onHomeBtnClicked(View v){
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);

    }

    private volatile boolean deviceFound = false;

    private void handle_pairing_failed() {

        logi("handle_pairing_failed() :: Start");

        // for success
        if(debug) {
            if (!newDeviceCode.equalsIgnoreCase("vuvuv")) {

                state = PAIRING_STATE.PAIRING_STATE_NEW_NAME;
                displayConnectScreen(state);
                StoredDevice newDev = new StoredDevice(null, newDeviceCode, "ab.cd.ef.gh");
                addMicrobit(newDev);
                return;

            }
        }

        displayConnectScreen(PAIRING_STATE.PAIRING_STATE_NEW_NAME);

        PopUp.show(this,
                "We cannot find that Micro:Bit\nPlease try again", //message
                "Error", //title
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

        logi("scanningFailed() :: scanning Failed to find a matching device");
        if (deviceFound) {
            return;
        }
        scanLeDevice(false);
        handle_pairing_failed();
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

           // display_spinner_dialog();
            mHandler.postDelayed(scanFailedCallback, SCAN_PERIOD);
            deviceFound = false;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mHandler.removeCallbacks(scanFailedCallback);
            scanFailedCallback = null;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
           // pairingProgressDialog.dismiss();
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

            if ((newDeviceName.isEmpty()) || (device.getName() == null)) {
                logi("mLeScanCallback.onLeScan() ::   Cannot Compare");
            } else {
                String s = device.getName().toLowerCase();
                if (newDeviceName.toLowerCase().equals(s)
                        || (s.contains(newDeviceCode.toLowerCase()) && s.contains("microbit"))) {

                    logi("mLeScanCallback.onLeScan() ::   deviceName == " + newDeviceName.toLowerCase());
                    logi("mLeScanCallback.onLeScan() ::   device.getName() == " + device.getName().toLowerCase());

                    // Stop scanning as device is found.
                    deviceFound = true;
                    scanLeDevice(false);
                    state = PAIRING_STATE.PAIRING_STATE_NEW_NAME;
                    displayConnectScreen(state);
                    StoredDevice newDev = new StoredDevice(null, newDeviceCode,device.getAddress() );
                    addMicrobit(newDev);
                    connectBluetoothDevice();

                    //DO BLUETOOTHY THINGS HERE!!!!
                    logi("mLeScanCallback.onLeScan() ::   Matching DEVICE FOUND, Pairing");
                  //  mSelectedDevice = device;
                  //  handle_pairing_successful();
                } else {
                    logi("mLeScanCallback.onLeScan() ::   non-matching - deviceName == " + newDeviceName.toLowerCase());
                    logi("mLeScanCallback.onLeScan() ::   non-matching found - device.getName() == " + device.getName().toLowerCase());
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_launcher, menu);
        return true;
    }

    void disconnectBluetooth()
    {
        //TODO

    }
    void connectBluetoothDevice()
    {
        //TODO
    }

    public class StoredDevice
    {
        public String deviceDisplayName;
        private String deviceName;
        private String deviceAddress;
        private boolean isConnected;

        StoredDevice(String displayName, String name, String address)
        {
            deviceDisplayName = displayName;
            deviceName = name;
            deviceAddress = address;
            isConnected=true;
        }
    };

    private void storeMicrobits(List prevDevList)
    {
        // used for store arrayList in json format
        SharedPreferences settings;
        SharedPreferences.Editor editor;
        settings = getSharedPreferences(PREFERENCES_PREVDEV_PREFNAME,MODE_PRIVATE);
        editor = settings.edit();
        Gson gson = new Gson();
        String jsonPrevDevices = gson.toJson(prevDevList);
        editor.putString(PREFERENCES_PREVDEV_KEY, jsonPrevDevices);
        editor.commit();
        populateConnectedDeviceList(true);
    }

    private ArrayList loadPrevMicrobits()
    {
        // used for retrieving arraylist from json formatted string
        SharedPreferences settings;
        List prevMicrobitTemp;
        settings = getSharedPreferences(PREFERENCES_PREVDEV_PREFNAME,MODE_PRIVATE);
        if (settings.contains(PREFERENCES_PREVDEV_KEY)) {
            String jsonFavorites = settings.getString(PREFERENCES_PREVDEV_KEY, null);
            Gson gson = new Gson();
            StoredDevice[] prevMicrobitItems = gson.fromJson(jsonFavorites,StoredDevice[].class);
            prevMicrobitTemp = Arrays.asList(prevMicrobitItems);
            prevMicrobitList = new ArrayList(prevMicrobitTemp);

            String dbgDevices="L ";
            int ind=0;
            for(Iterator<StoredDevice> it=prevMicrobitList.iterator();it.hasNext();)
            {
                StoredDevice st = it.next();
                prevDeviceArray[ind++] = st;
                if(debug)
                    dbgDevices =dbgDevices+ "["+st.deviceDisplayName + " "+st.deviceName + " " + st.deviceAddress + "] ";

            }
            if(debug)
                Toast.makeText(this, dbgDevices, Toast.LENGTH_LONG).show();


        } else
            return null;

        return (ArrayList) prevMicrobitList;
    }
    public void addMicrobit(StoredDevice newMicrobit)
    {
        if (prevMicrobitList== null)
            prevMicrobitList = new ArrayList(PREVIOUS_DEVICES_MAX);
        if(prevMicrobitList.size() == PREVIOUS_DEVICES_MAX)
            prevMicrobitList.remove(PREVIOUS_DEVICES_MAX-1);

        prevMicrobitList.add(0, newMicrobit); // new devices added to beginning

        String dbgDevices="A ";
        int ind=0;
        for(Iterator<StoredDevice> it=prevMicrobitList.iterator();it.hasNext();)
        {
            StoredDevice st = it.next();
            if(ind != 0)
                st.isConnected=false;
            prevDeviceArray[ind++] = st;
            if(debug)
                dbgDevices =dbgDevices+ "["+st.deviceDisplayName + " "+st.deviceName + " " + st.deviceAddress + "] ";

        }
        if(debug)
            Toast.makeText(this, dbgDevices, Toast.LENGTH_LONG).show();
        storeMicrobits(prevMicrobitList);
    }

    public void changeMicrobitName(int index, StoredDevice modMicrobit)
    {
        prevMicrobitList.remove(index);
        prevMicrobitList.add(index, modMicrobit);

        String dbgDevices="REN ";
        int ind=0;
        for(Iterator<StoredDevice> it=prevMicrobitList.iterator();it.hasNext();)
        {
            StoredDevice st = it.next();
       //     prevDeviceArray[ind++] = st;

            if(debug)
                dbgDevices =dbgDevices+ "["+st.deviceDisplayName + " "+st.deviceName + " " + st.deviceAddress + "] ";

        }
        if(debug)
            Toast.makeText(this, dbgDevices, Toast.LENGTH_LONG).show();
        storeMicrobits(prevMicrobitList);
    }
    public void changeMicrobitState(int index, StoredDevice modMicrobit, boolean isTurnedOn)
    {
        prevMicrobitList.remove(index);
        prevMicrobitList.add(index, modMicrobit);

        String dbgDevices="C ";
        int ind=0;
        for(Iterator<StoredDevice> it=prevMicrobitList.iterator();it.hasNext();)
        {
            StoredDevice st = it.next();
            if(isTurnedOn && (ind!=index)) {
                prevDeviceArray[ind].isConnected = false; // toggle previously connected BT OFF
                disconnectBluetooth();
            }
            ind++;
            if(debug)
                dbgDevices =dbgDevices+ "["+st.deviceDisplayName + " "+st.deviceName + " " + st.deviceAddress + "] ";

        }
        if(debug)
            Toast.makeText(this, dbgDevices, Toast.LENGTH_LONG).show();
        storeMicrobits(prevMicrobitList);
    }
    public void removeMicrobit(int index )
    {
        if (prevMicrobitList != null) {
            prevMicrobitList.remove(index);

            String dbgDevices="R ";
            int ind=0;
            for(Iterator<StoredDevice> it=prevMicrobitList.iterator();it.hasNext();)
            {
                StoredDevice st = it.next();
                prevDeviceArray[ind++] = st;
                if(debug)
                    dbgDevices =dbgDevices+ "["+st.deviceDisplayName + " "+st.deviceName + " " + st.deviceAddress + "] ";

            }
            if(debug)
                Toast.makeText(this, dbgDevices, Toast.LENGTH_LONG).show();

            storeMicrobits(prevMicrobitList);
        }
    }

}
