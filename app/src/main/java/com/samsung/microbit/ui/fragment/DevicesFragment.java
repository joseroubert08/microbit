package com.samsung.microbit.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.samsung.microbit.R;
import com.samsung.microbit.ui.ImageAdapter;

import java.util.ArrayList;


public class DevicesFragment extends Fragment implements OnClickListener, OnItemClickListener{

    private static final int SUCCESS = 0;
    private static final int FILE_NOT_FOUND = -1;
    private static final int FILE_IO_ERROR = -2;
    private static final int FAILED = -3;

    private static final int NUM_LED_ELEMENTS = 25;
    private int state=0;
    private static final int STATE_AWAITING_USER_INITIATION=0;
    private static final int STATE_LED_GRID_DISPLAYED=1;
    private static final int STATE_READY_TO_SCAN=2;
    private static final int STATE_SCANNING=3;
    private static final int STATE_DEVICE_DISCOVERED=4;
    private static final int STATE_DEVICE_PAIRED=5;
    private static SharedPreferences preferences;

    private String deviceCodeArray[] = {"0","0","0","0","0","0","0","0","0","0",
                                        "0","0","0","0","0","0","0","0","0","0",
                                        "0","0","0","0","0"};

    private String deviceNameMapArray[] = {"T","A","T","A","T","P","E","P","E","P",
                                           "G","I","G","I","G","V","O","V","O","V",
                                           "Z","U","Z","U","Z"};

    private String deviceName = "";
    private ImageAdapter imgAdapter;
    private GridView devicesGridview = null;
    private Button devicesButton = null ;

    private ListView programList = null ;
    final ArrayList<String> list = new ArrayList<String>();
    private Boolean isBLEAvailable = true;
    private Boolean isBLuetoothEnabled = false;
    private BluetoothAdapter mBluetoothAdapter = null;
    private String fileNameToFlash = null ;

    private Handler mHandler;
    private boolean mScanning;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;


    final public static String BINARY_FILE_NAME= "/sdcard/output.bin";
    private static final int REQUEST_ENABLE_BT = 1;

    View rootView;


    public DevicesFragment() {
    }


    @Override
    public void onCreate (Bundle savedInstanceState){
        super.onCreate (savedInstanceState);
        //Search BLE devices here with proper flash support

        Log.d("Microbit", "onCreate");

        mHandler = new Handler();
        //checkBluetoothSupported();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d("Microbit", "onCreateView");

        //View rootView  = inflater.inflate(R.layout.fragment_devices, container, false);
        rootView  = inflater.inflate(R.layout.fragment_devices, container, false);

        devicesButton = (Button) rootView.findViewById(R.id.devicesButton);
        devicesButton.setText(R.string.devices_initiate_process);
        preferences = rootView.getContext().getSharedPreferences("Microbit_PairedDevices", Context.MODE_PRIVATE);
        /*
        //Default Values
        if(!isBLEAvailable){
            devicesButton.setEnabled(false);
            devicesButton.setText("BLE not supported!");
            state=99;
        } else {
            devicesButton.setEnabled(true);
            devicesButton.setText("TESTBUTTON");
            if (isBLuetoothEnabled){
                devicesButton.setText("Check for Paired Devices");
                state=1;
            } else {
                devicesButton.setText(R.string.devices_bluetooth_not_enabled);
            }
        }
        */

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_single_choice, list);
        //programList.setAdapter(adapter);
        devicesButton.setOnClickListener(this);
        //programList.setOnItemClickListener(this);

        return rootView;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("Microbit", "onActivityResult");

        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {

            Toast.makeText(getActivity(), "You must enable Bluetooth to continue", Toast.LENGTH_LONG).show();
            state = STATE_AWAITING_USER_INITIATION;
        }
        else
        {
            devicesButton = (Button) rootView.findViewById(R.id.devicesButton);
            devicesButton.setText(R.string.devices_find_microbit);
            displayLEDGridView();
            state = STATE_LED_GRID_DISPLAYED;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onClick(View v) {

        switch(state)
        {
            case STATE_AWAITING_USER_INITIATION:
            {
                checkBluetoothSupported();
                break;
            }

            case STATE_LED_GRID_DISPLAYED:
            {
                //generateCode();
                generateName();

                devicesButton = (Button) rootView.findViewById(R.id.devicesButton);
                devicesButton.setText(R.string.devices_scanning);

                state=STATE_SCANNING;
                scanLeDevice(true);
                break;
            }

            case STATE_READY_TO_SCAN:
            {
                //Call this again in case the details changed
                generateName();

                //STATE change occurs in scanLeDevice function
                devicesButton = (Button) rootView.findViewById(R.id.devicesButton);
                devicesButton.setText(R.string.devices_scanning);

                state=STATE_SCANNING;
                scanLeDevice(true);
                break;
            }

            case STATE_SCANNING:
            {
                //User has requested a stop.
                //STATE change occurs in scanLeDevice function
                scanLeDevice(false);
                state=STATE_READY_TO_SCAN;

                devicesButton = (Button) rootView.findViewById(R.id.devicesButton);
                devicesButton.setText(R.string.devices_find_microbit);
                break;
            }
        }


        /*
        Log.d("Microbit", "onClick - state == " + state);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();

            //WE SHOULD LISTEN TO ENSURE BLUETOOTH SWITCHED ON.
            //ASSUMING AND FORCING STATE CHANGE FOR NOW
            state=1;
        }
        else
        {
            switch(state)
            {
                case 1:
                {
                    //Check for linked Microbit device
                    checkForPairedDevices();

                    break;
                }

                case 2:
                {
                    View p = (View) v.getRootView();
                    if (p != null) {
                        devicesButton = (Button) p.findViewById(R.id.devicesButton);
                        devicesButton.setText(R.string.devices_find_microbit);
                        state = 3;
                    }
                    break;
                }

                case 3:
                {
                    Log.d("Microbit","SEARCHING FOR MICROBIT");
                    //displayLEDGrid(v);
                    View p = (View) v.getRootView();
                    if (p != null) {
                        devicesButton = (Button) p.findViewById(R.id.devicesButton);
                        devicesButton.setText(R.string.devices_generate_code);
                        displayLEDGridView(v);
                        state = 4;
                    }
                    break;
                }

                case 4:
                {
                    generateCode();
                    generateName();
                    Log.d("Microbit", "CODE GENERATED");

                    View p = (View) v.getRootView();
                    if (p != null) {
                        devicesButton = (Button) p.findViewById(R.id.devicesButton);
                        devicesButton.setText("Click to initiate Pairing");
                    }

                    state = 5;

                    break;
                }

                case 5:
                {
                    //Send to the layer below so that it can begin pairing
                    //SHOULD WE USE A MESSAGING INTERFACE SO UI IS SEPARATE?

                    Intent intent = new Intent(getActivity(), DeviceScanActivity.class);
                    startActivity(intent);
                    break;
                }

                case 6:
                {
                    View p = (View) v.getRootView();
                    if (p != null) {
                        devicesButton = (Button) p.findViewById(R.id.devicesButton);
                        devicesButton.setText("Paired Device Found");
                    }

                    Toast.makeText(getActivity(), "PAIRED DEVICE FOUND...:)", Toast.LENGTH_LONG).show();
                    break;
                }

                case 99:
                {
                    Toast.makeText(getActivity(), R.string.devices_ble_unsupported, Toast.LENGTH_LONG).show();
                    break;
                }
            }

        }
        */

    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {

    }


    private void checkBluetoothSupported() {
        //Open Bluetooth connection and check if MicroBit it attached
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.

        //Check if BLE is supported
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            isBLEAvailable = false;
            return;
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        else
        {
            isBLuetoothEnabled = true ;

            devicesButton = (Button) rootView.findViewById(R.id.devicesButton);
            devicesButton.setText(R.string.devices_find_microbit);
            displayLEDGridView();
            state = STATE_LED_GRID_DISPLAYED;
        }
    }


    private void checkForPairedDevices()
    {
        //Check for paired Microbit device here

        //This is a temporary means of checking for a device.. :)
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("TEMP - Should I pretend to have a stored device?");
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d("Microbit", "WE HAVE A PAIRED DEVICE");
                state=6;
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d("Microbit", "NO PAIRED DEVICE FOUND");
                state=2;
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void generateName() {

        deviceName="";
        //Columns
        for (int col = 0; col < 5; col++) {

            //Rows
            for (int row=0; row<5; row++)
            {
                if (deviceCodeArray[(col+(5*row))] == "1") {

                    deviceName += deviceNameMapArray[(col+(5*row))];
                    break;
                }
            }
        }

        Toast.makeText(getActivity(), deviceName, Toast.LENGTH_SHORT).show();
    }


    private String generateCode()
    {
        String deviceCode = "";

        for (int i=0; i<NUM_LED_ELEMENTS; i++)
        {
            deviceCode += deviceCodeArray[i];
        }

        Toast.makeText(getActivity(), deviceCode, Toast.LENGTH_LONG).show();

        return deviceCode;
    }


    private void toggleLED(ImageView image, int pos)
    {
        if(image.getTag()!="1")
        {
            image.setImageResource(R.drawable.ledon);
            image.setTag("1");

            deviceCodeArray[pos]="1";
        }
        else
        {
            image.setImageResource(R.drawable.ledoff);
            image.setTag("0");

            deviceCodeArray[pos]="0";
        }
    }


    private void displayLEDGridView()
    {
        Log.d("Microbit","displayLEDGridView");

        imgAdapter=new ImageAdapter(getActivity());
        devicesGridview = (GridView) rootView.findViewById(R.id.devicesLEDGridView);
        devicesGridview.setAdapter(imgAdapter);

        devicesGridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {

                ImageView imageview = (ImageView) v;
                toggleLED(imageview, position);

                Log.d("Microbit", "position == " + position + " id == " + id);
            }
        });
    }


    //BLUETOOTH CODE

    private void scanningFailed()
    {
        Log.d("Microbit", "scanning Failed to find a matching device");

        scanLeDevice(false);
        state=STATE_READY_TO_SCAN;

        devicesButton = (Button) rootView.findViewById(R.id.devicesButton);
        devicesButton.setText(R.string.devices_find_microbit);
    }

    private void scanLeDevice(final boolean enable) {

        Log.d("Microbit", "scanLeDevice DEVICE");

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    scanningFailed();
                    //invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            rootView.getContext().registerReceiver(bBondStateReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

            //Button text occurs in state handler function
        }
        //invalidateOptionsMenu();
    }

    final BroadcastReceiver bBondStateReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch (device.getBondState()) {
                        case BluetoothDevice.BOND_BONDING:
                            Log.d("Microbit", "BOND_BONDING - it is pairing");
                            Toast.makeText(getActivity(), "BOND_BONDING - RECEIVED", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            state=STATE_DEVICE_PAIRED;
                            Log.d("Microbit", "BOND_BONDED - Pairing finished successfully");
                            // The SharedPreferences editor - must use commit() to submit changes
                            SharedPreferences.Editor editor = preferences.edit();

                            // Edit the saved preferences
                            editor.putString("PairedDevice", deviceName);
                            editor.commit();
                            Toast.makeText(getActivity(), "Partner Found - Enter both buttons on device", Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothDevice.BOND_NONE:
                            Log.d("Microbit", "BOND_NONE - cancel");
                            Toast.makeText(getActivity(), "BONDING FAILED", Toast.LENGTH_SHORT).show();
                        default:
                            break;
                    }
                }
            }
        };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

                    Log.d("Microbit", "scanLeDevice DEVICE FOUND");

                    if (device == null)
                    {
                        return;
                    }

                    if ((deviceName.isEmpty()) || (device.getName() == null))
                    {
                        Log.d("Microbit", "Cannot Compare");
                    }
                    else if (deviceName.toLowerCase().equals(device.getName().toLowerCase()))
                    {
                        Toast.makeText(getActivity(), "Partner Found - " + device.getName(), Toast.LENGTH_SHORT).show();

                        Log.d("Microbit", " deviceName == " + deviceName.toLowerCase());
                        Log.d("Microbit", " device.getName() == " + device.getName().toLowerCase());

                        //UPDATE THE UI TO INDICATE PAIRED DEVICE (add string to
                        //strings.xml and update the button)

                        state=STATE_DEVICE_DISCOVERED;
                        // Stop scanning as device is found.
                        scanLeDevice(false);

                        //DO BLUETOOTHY THINGS HERE!!!!

                        Log.d("Microbit", "Matching DEVICE FOUND, Pairing");
                        Boolean isBonded = false;
                        isBonded = device.createBond();

                        /*
                        if (!isBonded) {
                            Toast.makeText(getActivity(), "Pairing Failed", Toast.LENGTH_SHORT).show();
                        }*/
                    }
                    else
                    {
                        Log.d("Microbit", " deviceName == " + deviceName.toLowerCase());
                        Log.d("Microbit", " device.getName() == " + device.getName().toLowerCase());

                        Toast.makeText(getActivity(), "NO MATCH  - " + device.getName(), Toast.LENGTH_SHORT).show();
                    }


                    /*
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                    */
                }
            };
} 