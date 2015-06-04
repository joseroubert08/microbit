package com.samsung.microbit.ui.fragment;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.samsung.microbit.R;
import com.samsung.microbit.service.BLEService;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class MyBlankFragment extends Fragment {

	private View rootView;
	private Button initButton;
	private Button runTestCodeButton;


	private BLEService bleService;
	private boolean isBound;

	static final String TAG = "MyBlankFragment";
	private boolean debug = false;

	public static final UUID button1Service = UUID.fromString("0000a000-0000-1000-8000-00805f9b34fb");
	public static final UUID button2Service = UUID.fromString("0000b000-0000-1000-8000-00805f9b34fb");

	public static final UUID button1Characteristic = UUID.fromString("0000a001-0000-1000-8000-00805f9b34fb");
	public static final UUID button2Characteristic = UUID.fromString("0000b001-0000-1000-8000-00805f9b34fb");

	UUID callBackDescriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	void logi(String message) {
		if (debug) {
			Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
		}
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {

			BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
			bleService = binder.getService();
			runTestCodeButton.setEnabled(true);
		}

		public void onServiceDisconnected(ComponentName className) {

			bleService = null;
		}
	};


	public MyBlankFragment() {
		logi("MyBlankFragment()");
	}

	BroadcastReceiver btn5rxer;
	BroadcastReceiver btn15rxer;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (btn5rxer == null) {
			Log.i("Main", "### registering receiver");
			btn5rxer = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.i("Main", "### uBit Button detected for 5");
				}
			};

			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(BLEService.messageName + 5);
			LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(btn5rxer, intentFilter);
		}


		if (btn15rxer == null) {
			Log.i("Main", "### registering receiver");
			btn15rxer = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.i("Main", "### uBit Button detected for 15");
				}
			};

			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(BLEService.messageName + 15);
			LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(btn15rxer, intentFilter);
		}

		// Inflate the layout for this fragment
		rootView = inflater.inflate(R.layout.fragment_my_blank, container, false);
		runTestCodeButton = (Button) rootView.findViewById(R.id.runTestCode);
		runTestCodeButton.setEnabled(false);
		runTestCodeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				runTestCodeButton();
			}
		});

		initButton = (Button) rootView.findViewById(R.id.initSystem);
		initButton.setEnabled(true);
		initButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				initButtonClicked();
			}
		});

		return rootView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		doUnbindService();
		//if (updateReceiver != null) {
		//	getActivity().unregisterReceiver(updateReceiver);
		//}
	}

	void initButtonClicked() {
		logi("initButtonClicked() :: start");

		Toast.makeText(getActivity(), "Test Button clicked.", Toast.LENGTH_SHORT).show();
		Intent serviceIntent = new Intent(getActivity(), BLEService.class);
		serviceIntent.putExtra("DEVICE_ADDRESS", "F7:61:FB:87:A2:46");
		serviceIntent.putExtra("com.samsung.resultReceiver", resultReceiver);
		getActivity().startService(serviceIntent);

		logi("### initButtonClicked() :: service start called");
		initButton.setEnabled(false);
	}

	ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			runTestCode(resultCode);
		}
	};


	void runTestCode(int code) {

		bindService();
	}

	void bindService() {

		logi("bindService() :: start");
		getActivity().bindService(new Intent(getActivity(), BLEService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		isBound = true;
	}

	void doUnbindService() {
		if (isBound) {

			getActivity().unbindService(serviceConnection);
			isBound = false;
		}
	}

	void runTestCodeButton() {
		logi("runTestCodeButton() :: start");

		runTestCodeButton.setEnabled(false);

		new Thread(new Runnable() {

			@Override
			public void run() {


				int i = bleService.connect();
				logi("runTestCodeButton() :: i=" + i);
				if (i != 0) {
					return;
				}

				i = bleService.discoverServices();
				List<BluetoothGattService> services = bleService.getServices();
				if (services == null) {
					logi("runTestCodeButton() :: discoverServices services=null");
				}

				Iterator<BluetoothGattService> sitr = services.iterator();
				while (sitr.hasNext()) {
					BluetoothGattService s = sitr.next();
					logi("runTestCodeButton() :: discoverServices s = " + s.getUuid().toString());
				}

				BluetoothGattService button1s = bleService.getService(button1Service);
				BluetoothGattCharacteristic button1c = button1s.getCharacteristic(button1Characteristic);
				BluetoothGattDescriptor button1d = button1c.getDescriptor(callBackDescriptor);
				bleService.enableCharacteristicNotification(button1c, button1d, true);

				BluetoothGattService button2s = bleService.getService(button2Service);
				BluetoothGattCharacteristic button2c = button2s.getCharacteristic(button2Characteristic);
				BluetoothGattDescriptor button2d = button2c.getDescriptor(callBackDescriptor);
				bleService.enableCharacteristicNotification(button2c, button2d, true);

			}
		}).start();
	}


}
