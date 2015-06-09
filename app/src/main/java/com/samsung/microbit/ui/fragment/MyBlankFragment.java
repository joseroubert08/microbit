package com.samsung.microbit.ui.fragment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.plugin.AlertPlugin;
import com.samsung.microbit.plugin.RemoteControlPlugin;
import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.service.PluginService;

public class MyBlankFragment extends Fragment {

	private View rootView;
	private Button initButton;
	private Button runTestCodeButton;

	private BLEService bleService;
	private boolean isBound;


	static final String TAG = "MyBlankFragment";
	private boolean debug = false;
	private boolean mIsRemoteControlPlay = false;

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

	BroadcastReceiver btnReceiver;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (btnReceiver == null) {
			Log.i(TAG, "### registering receiver");
			btnReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					int buttonPressed = intent.getIntExtra("buttonPressed", 0);
					if (buttonPressed == 0) {
						return;
					}

					if((buttonPressed  & 0x010) == 0) {
						return;
					}

					Log.i(TAG, "### uBit Button detected for button = " + buttonPressed);
					int msgService = PluginService.ALERT;
					CmdArg cmd = null;
					switch (buttonPressed) {
						case 0x011:
							msgService = PluginService.REMOTE_CONTROL;
							mIsRemoteControlPlay = !mIsRemoteControlPlay;
							cmd = new CmdArg(mIsRemoteControlPlay ? RemoteControlPlugin.PLAY : RemoteControlPlugin.PAUSE, "");
							break;
						case 0x012:
						case 0x014:
						case 0x018:
							cmd = new CmdArg(AlertPlugin.FINDPHONE, "");
							break;

						case 0x031:
							msgService = PluginService.REMOTE_CONTROL;
							cmd = new CmdArg(RemoteControlPlugin.NEXT_TRACK, "");
							break;
						case 0x032:
						case 0x034:
						case 0x038:
							cmd = new CmdArg(AlertPlugin.VIBRATE, "500");
							break;
					}

					if (cmd != null) {
						sendCommand(msgService, cmd);
					}
				}
			};

			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(BLEService.MESSAGE_NAME);
			LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(btnReceiver, intentFilter);
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

		if (messageHandler == null) {
			connectWithServer();
		}

		return rootView;
	}

	@Override
	public void onDestroy() {
		doUnbindService();
		super.onDestroy();

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
			serviceConnection = null;
			isBound = false;
		}

		if (isBoundToPluginService) {
			getActivity().unbindService(serviceConnectionPluginService);
			serviceConnectionPluginService = null;
			isBoundToPluginService = false;
		}
	}

	void runTestCodeButton() {
		logi("runTestCodeButton() :: start");

		runTestCodeButton.setEnabled(false);
		new Thread(new Runnable() {

			@Override
			public void run() {

				bleService.connect();
				bleService.discoverServices();
				bleService.registerNotifications(true);
				showToast("Notifications registered. Ready");
			}
		}).start();
	}

	void showToast(final String message) {

		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
			}
		});
	}

	// ######################################################################
	private Messenger messengerPluginService = null;
	private Messenger mClientMessenger = null;
	private ServiceConnection serviceConnectionPluginService = null;
	private IncomingHandler messageHandler = null;
	private HandlerThread messageHandlerThread = null;
	private boolean isBoundToPluginService = false;


	class IncomingHandler extends Handler {
		public IncomingHandler(HandlerThread thr) {
			super(thr.getLooper());
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		}
	}

	public void connectWithServer() {
		messageHandlerThread = new HandlerThread("BLEReceiverThread");
		messageHandlerThread.start();
		messageHandler = new IncomingHandler(messageHandlerThread);
		mClientMessenger = new Messenger(messageHandler);

		serviceConnectionPluginService = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				isBoundToPluginService = true;
				messengerPluginService = new Messenger(service);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				isBoundToPluginService = false;
				serviceConnectionPluginService = null;
			}
		};

		Intent mIntent = new Intent();
		mIntent.setAction("com.samsung.microbit.service.PluginService");
		getActivity().bindService(mIntent, serviceConnectionPluginService, Context.BIND_AUTO_CREATE);
	}

	public void sendCommand(int mbsService, CmdArg cmd) {
		if (messengerPluginService != null) {
			Message msg = Message.obtain(null, mbsService);
			Bundle bundle = new Bundle();
			bundle.putInt("cmd", cmd.getCMD());
			bundle.putString("value", cmd.getValue());
			msg.setData(bundle);
			msg.replyTo = mClientMessenger;
			try {
				messengerPluginService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	// ######################################################################

}
