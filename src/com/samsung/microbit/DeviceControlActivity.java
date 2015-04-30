package com.samsung.microbit;



import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private static final String DEFAULT_DEVICE_NAME = "DefaultApp";
    private static final String DFU_DEVICE_NAME = "DfuTarg";
    
    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

	protected int mimageSizeInBytes;

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (!checkDFUService(mBluetoothLeService.getSupportedGattServices())){
                	AlertDialog alertDialog = new AlertDialog.Builder(DeviceControlActivity.this).create();
                	alertDialog.setTitle("Error");
                	alertDialog.setMessage("No DFU Service Found");
                	alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                	    new DialogInterface.OnClickListener() {
                	        public void onClick(DialogInterface dialog, int which) {
                	            dialog.dismiss();
                	        }
                	    });
                	alertDialog.show();
                	// Show all the supported services and characteristics on the user interface.
                	displayGattServices(mBluetoothLeService.getSupportedGattServices());
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_ON_CHARACTERISTIC_WRITE.equals(action)) {
            	Toast.makeText(DeviceControlActivity.this, "MicroBit flashed", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_ON_ERROR.equals(action)) {
            	Toast.makeText(DeviceControlActivity.this, "Unknown Error", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_PROGRESS_UPDATE.equals(action)) {
            	Toast.makeText(DeviceControlActivity.this, "Uploaded " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA), Toast.LENGTH_SHORT).show();
            }
            	
        }

		private boolean checkDFUService(List<BluetoothGattService> supportedGattServices) {
			if (supportedGattServices == null) return false;
			String uuid = null ;
			for (BluetoothGattService gattService : supportedGattServices) {
				uuid = gattService.getUuid().toString();
				if (uuid.equals(SampleGattAttributes.DEVICE_FIRMWARE_UPDATE)){
		            Toast.makeText(DeviceControlActivity.this, "Found device with DFU support and connected", Toast.LENGTH_SHORT).show();
		            if (mDeviceName.equals(DEFAULT_DEVICE_NAME)){
		            	List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
			            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
			            	uuid = gattCharacteristic.getUuid().toString();
				            if (uuid.equals(SampleGattAttributes.DFU_CONTROL_POINT)){
				            	if (initiateDFU(gattCharacteristic)){
				            		Toast.makeText(DeviceControlActivity.this, "DFU control characteristic written successfully", Toast.LENGTH_LONG).show();
				            		return true;
				            	} else {
				            		Toast.makeText(DeviceControlActivity.this, "Failed writing DFU Control characteristic", Toast.LENGTH_LONG).show();
				            		return false;
				            	}
				            }
			            }
		            } else if (mDeviceName.equals(DFU_DEVICE_NAME )){
		            	List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
			            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
			            	uuid = gattCharacteristic.getUuid().toString();
			            	if (uuid.equals(SampleGattAttributes.DFU_PACKET)){
			            		if (initiateFileTransfer(gattCharacteristic)){
					            	Toast.makeText(DeviceControlActivity.this, "Initiated file transfer", Toast.LENGTH_LONG).show();
			            			return true;
			            		}
			            	}
			            }
					
				}
			  }
			}
			return false;
		}

		private boolean initiateFileTransfer(BluetoothGattCharacteristic gattCharacteristic) {
			File file = new File(FlashSectionFragment.BINARY_FILE_NAME);
			if(file.exists()){
				int imageSizeInBytes = 0 ;
				FileInputStream initIs;
				try {
					initIs = new FileInputStream(FlashSectionFragment.BINARY_FILE_NAME);
					imageSizeInBytes = mimageSizeInBytes = initIs.available();
					gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
					gattCharacteristic.setValue(new byte[4]);
					gattCharacteristic.setValue(imageSizeInBytes, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
					return  mBluetoothLeService.writeCharacteristic(gattCharacteristic) ;
				} catch (Exception e ) {
					Toast.makeText(DeviceControlActivity.this, "Unknown Error", Toast.LENGTH_LONG).show();
				}
			} else {
        		Toast.makeText(DeviceControlActivity.this, "No binary to flash", Toast.LENGTH_LONG).show();
        		return false;
			}
			return true;
		}

		private boolean initiateDFU(BluetoothGattCharacteristic gattCharacteristic) {
        	final int charaProp = gattCharacteristic.getProperties();
        	if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
	            //Write to control characteristic of DFU
        		Toast.makeText(DeviceControlActivity.this, "Wrting control characteristic of DFU service", Toast.LENGTH_LONG).show();
        		//Add Values to the characteristic
        		byte [] arrayOfByte = new byte[] {(byte)(0x01),(byte)(0x4) } ;
        		gattCharacteristic.setValue(arrayOfByte);
        		//Write them back
        		return  mBluetoothLeService.writeCharacteristic(gattCharacteristic) ;
        	} 
        	return false;
		}
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
    };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_ON_CHARACTERISTIC_WRITE);
        intentFilter.addAction(BluetoothLeService.ACTION_ON_ERROR);
        intentFilter.addAction(BluetoothLeService.ACTION_PROGRESS_UPDATE);        
        return intentFilter;
    }
}
