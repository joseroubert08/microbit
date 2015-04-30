package com.samsung.microbit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;
import java.lang.Integer;

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
	
	private static final int SUCCESS = 0;
	private static final int FILE_NOT_FOUND = -1;
	private static final int FILE_IO_ERROR = -2;
	private static final int FAILED = -3;
	
	private Button flashSearchButton = null ;
	private TextView deviceConnectedText = null ;
	private ListView programList = null ;
	final ArrayList<String> list = new ArrayList<String>();
	private Boolean isBLEAvailable = true;
	private Boolean isBLuetoothEnabled = false;
    private BluetoothAdapter mBluetoothAdapter = null;
    
    final public static String BINARY_FILE_NAME= "/sdcard/output.bin";

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
        Log.d("MicroBit", "Searching files in "+ sdcardDownloads.getAbsolutePath());
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
			File file = new File(BINARY_FILE_NAME);
			if(file.exists()){      
			 Intent intent = new Intent(getActivity(), DeviceScanActivity.class);
			 startActivity(intent);
			} else {
				Toast.makeText(getActivity(), "Create the binary file first by clicking one file below" , Toast.LENGTH_LONG).show();			}
			break;
		}
		
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
		String value = (String)adapter.getItemAtPosition(position);
		Toast.makeText(getActivity(), "Preparing " + value + " ..." , Toast.LENGTH_LONG).show();
		int retValue = PrepareFile(value);
		switch (retValue){
			case SUCCESS:
				Toast.makeText(getActivity(), "Binary file ready for flashing" , Toast.LENGTH_LONG).show();
				break;
			case FILE_NOT_FOUND:
			case FILE_IO_ERROR:
			case FAILED:
				Toast.makeText(getActivity(), "Failed to create binary file" , Toast.LENGTH_LONG).show();
				break;
		}
	}

	// Creates binary buffer from ONLY the data specified by hex, spec here:
	// http://en.wikipedia.org/wiki/Intel_HEX
	private int PrepareFile(String fileName) {
    Log.d("MicroBit", "PrepareFile [+]");
	FileInputStream is = null;
	BufferedReader reader = null;
	byte [] buffer = null;
	File sdcardDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) ;
	if (sdcardDownloads.exists()){
		File hexFile = new File(sdcardDownloads, fileName);
		if (hexFile.exists()){
		    Log.d("MicroBit", "Found the Hex file");
			try {
				is = new FileInputStream(hexFile);
				reader = new BufferedReader(new InputStreamReader(is));
				String line = null;
				try {
					int byteCount = 0 ;
					while ((line = reader.readLine())!= null){
						//Calculate size of output file
			            if (line.substring(7, 9).equals("00")) { // type == data
			            	byteCount += Integer.parseInt(line.substring(1, 3), 16);
			            }
					}
				    Log.d("MicroBit", "Size of outputfile = " + byteCount);

				    // "reset" to beginning of file (discard old buffered reader)
				    is.getChannel().position(1);
				    reader = new BufferedReader(new InputStreamReader(is));
				    
					int pointer = 0 ;
					buffer = new byte [byteCount];
					while ((line = reader.readLine())!= null){
						 if (line.substring(7, 9).equals("00")) { // type == data
				                int length = Integer.parseInt(line.substring(1, 3), 16);
				                String data = line.substring(9, 9 + length * 2);
				                for (int i = 0; i < length * 2; i += 2) {
				                	buffer[pointer] = (byte) Integer.parseInt(data.substring(i, i+2), 16);
				                    pointer++;
				                }
				            }
				    }
					if (reader != null) reader.close();
					//Write file
				    Log.d("MicroBit", "Writing output file");
					FileOutputStream f = new FileOutputStream(new File(BINARY_FILE_NAME));
					f.write(buffer);
					f.flush();
					f.close();					
					return SUCCESS;
				} catch (IOException e) {
					Log.d("MicroBit", "Cannot read the file");
					e.printStackTrace();
					return FILE_IO_ERROR;
				}
			} catch (FileNotFoundException e) {
				Log.d("MicroBit", "Cannot find the file");
				return FILE_NOT_FOUND;
			} 
		}
	}
	Log.d("MicroBit", "Failed conversion");
	return FAILED;
}
} 