package com.samsung.microbit;

import java.io.File;
import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FlashSectionFragment extends Fragment implements OnClickListener, OnItemClickListener{ 
	
	private Button flashSearchButton = null ;
	private TextView deviceConnectedText = null ;
	private ListView programList = null ;
	final ArrayList<String> list = new ArrayList<String>();
	private Boolean isBLEAvailable = true;
	private Boolean isBLuetoothEnabled = false;
    private BluetoothAdapter mBluetoothAdapter = null;

    public FlashSectionFragment() {
    }

    @Override
	public void onCreate (Bundle savedInstanceState){
		super.onCreate (savedInstanceState);
		//Search BLE devices here with proper flash support
        checkMicroBitAttached();
		//Populate program list for later use
		findProgramsAndPopulate();
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
        		deviceConnectedText.setText(R.string.microbit_not_found);
        	} else {
        		deviceConnectedText.setText(R.string.error_bluetooth_not_enabled);
        	}
        }
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
    	        android.R.layout.simple_list_item_1, list);
    	programList.setAdapter(adapter);
   		flashSearchButton.setOnClickListener(this);
   		programList.setOnItemClickListener(this);
    	return rootView;
    }

	@Override
	public void onClick(View v) {
		Log.d("Microbit", "onClick");
		switch(v.getId()){
		case R.id.searchButton:
			 Intent intent = new Intent(getActivity(), DeviceScanActivity.class);
			 startActivity(intent);
			break;
		}
		
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
		String value = (String)adapter.getItemAtPosition(position);
		Toast.makeText(getActivity(), value, Toast.LENGTH_SHORT).show();
	}
}