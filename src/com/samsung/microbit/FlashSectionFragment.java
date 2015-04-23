package com.samsung.microbit;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter.LeScanCallback;

public class FlashSectionFragment extends Fragment implements OnClickListener{ 
	
	private Button flashSearchButton = null ;
	private TextView deviceConnectedText = null ;
	private ListView programList = null ;
	final ArrayList<String> list = new ArrayList<String>();
	private Boolean isBLEAvailable = true;
	private Boolean isBLuetoothEnabled = false;
    private BluetoothAdapter mBluetoothAdapter = null;
    private boolean mScanning;
    private Handler mHandler ;
    private int REQUEST_ENABLE_BT = 4321 ;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                	Log.d("Microbit", "onLeScan");
		        	deviceConnectedText.setText("Analysing found devices");
                	final byte[] advertisedData = scanRecord ;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        	List<UUID> uuidsFound = parseUuids(advertisedData);
                        }
                    });
                }
      };
    
    private List<UUID> parseUuids(byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;

                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;

                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }

        return uuids;
    }
    public FlashSectionFragment() {
    }

    @Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate (savedInstanceState);
		mHandler = new Handler();
		//Search BLE devices here with proper flash support
        checkMicroBitAttached();
		//Populate program list for later use
		findProgramsAndPopulate();
	}

    private void scanLeDevice(final boolean enable) {
    	if (mLeScanCallback == null){
    		deviceConnectedText.setText("Cannot get callbacks");
    	}
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    deviceConnectedText.setText("Scan stopped ");
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            deviceConnectedText.setText("startLeScan called ");
            mBluetoothAdapter.startLeScan(mLeScanCallback);//0x1531
        } else {
            mScanning = false;
            deviceConnectedText.setText("stopLeScan called ");            
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d("Microbit", "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT){
        	Log.d("Microbit", "System replied for BT enable request");
        	switch (resultCode){
        		case Activity.RESULT_OK:
        			scanLeDevice(true);
        			break;
        		case Activity.RESULT_CANCELED:
        			Toast.makeText(getActivity(), "Please enable BLE manually and restart the application", Toast.LENGTH_LONG );
        			break;        		
        	}
        }
    }
    private void checkMicroBitAttached() {
        //Open Bluetooth connection and check if MicroBit it attached
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            isBLEAvailable = false;
            return;
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter.isEnabled()) {
        	isBLuetoothEnabled = true ;
        } 
    }

    private void findProgramsAndPopulate() {
		File sdcardDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        Log.d("Micro", "Searching files in "+ sdcardDownloads.getAbsolutePath());
        int iTotalPrograms = 0 ;
		if (sdcardDownloads.exists()){
			File files[] = sdcardDownloads.listFiles();
			for (int i=0 ; i < files.length ; i++){
                String fileName = files[i].getName() ;
				if(fileName.endsWith(".hex")){
                    list.add(fileName);
                    ++iTotalPrograms;
                }
			}
		}
        if (iTotalPrograms ==0){
            list.add("No programs found !");
        }
	}
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		View rootView  = inflater.inflate(R.layout.fragment_section_flash, container, false);
		
    	flashSearchButton = (Button) rootView.findViewById(R.id.searchButton);
    	deviceConnectedText = (TextView) rootView.findViewById(R.id.textView1);
    	programList = (ListView) rootView.findViewById(R.id.programList);
    	//Default Values
        if(!isBLEAvailable){
            flashSearchButton.setEnabled(false);
            deviceConnectedText.setText("BLE not supported. Cannot load code.");
        } else {
            flashSearchButton.setEnabled(true);
    		flashSearchButton.setText("Search");
        	if (isBLuetoothEnabled){
        		deviceConnectedText.setText("Microbit not found. Please search");
        	} else {
        		deviceConnectedText.setText("Bluetooth is not enabled");
        	}
        }
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
    	        android.R.layout.simple_list_item_1, list);
    	programList.setAdapter(adapter);
   		flashSearchButton.setOnClickListener(this);
    	return rootView;
    }

	@Override
	public void onClick(View v) {
		Log.d("Microbit", "onClick");
		switch(v.getId()){
		case R.id.searchButton:
			 if (!mBluetoothAdapter.isEnabled()) {
				 Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				 getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		      } else {
		        	Log.d("Microbit", "Bluetooth is already enabled, scanning devices");
		        	deviceConnectedText.setText("Searching");
		        	scanLeDevice(true);
		      }
			break;
		}
		
	}
}