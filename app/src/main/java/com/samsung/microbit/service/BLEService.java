package com.samsung.microbit.service;

import android.app.NotificationManager;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.BuildConfig;
import com.samsung.microbit.R;
import com.samsung.microbit.core.bluetooth.BLEManager;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.NameValuePair;
import com.samsung.microbit.utils.ErrorUtils;

import java.util.UUID;

public class BLEService extends BLEBaseService {

	protected String TAG = "BLEService";
	protected boolean debug = BuildConfig.DEBUG;
	protected boolean firstRun = true;

	protected void logi(String message) {
        if (debug) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
	}

	NotificationManager notifyMgr = null;
	int notificationId = 1010;

	public BLEService() {
		startIPCListener();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		int rc = 0;
		logi("onStartCommand()");
		rc = super.onStartCommand(intent, flags, startId);
		/*
		 * Make the initial connection to other processes
		 */
		new Thread(new Runnable() {
			@Override
			public void run() {

                //This code is necessary to prevent disconnection when there is an orientation change
				if(firstRun) {
                    firstRun = false;
					logi("First run!");
					try {
						Thread.sleep(IPCMessageManager.STARTUP_DELAY + 500L);
						sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CODE_INIT, null, null);
						sendtoPluginService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_FUNCTION_CODE_INIT, null, null);
						setNotification(false, 0);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				else {
					logi("Not first run!");
				}

			}
		}).start();

		return rc;
	}

	protected String getDeviceAddress() {

		logi("getDeviceAddress()");
		ConnectedDevice currentDevice = BluetoothUtils.getPairedMicrobit(this);
		String pairedDeviceName = currentDevice.mAddress;
		if (pairedDeviceName == null) {
			setNotification(false, 2);
		}

		return pairedDeviceName;
	}

	protected void startupConnection() {

		logi("startupConnection() bleManager=" + bleManager);
		boolean success = true;
		int rc = connect();
		if (rc == 0) {
			logi("startupConnection() :: connect() == 0");
			rc = discoverServices();
			if (rc == 0) {

				logi("startupConnection() :: discoverServices() == 0");
				if (registerNotifications(true)) {
					setNotification(true, 0);
				} else {
					rc = 1;
					success = false;
				}
			} else {
				success = false;
				logi("startupConnection() :: discoverServices() != 0");
			}
		} else {
			success = false;
		}

		if (!success) {
			logi("startupConnection() :: Failed ErrorCode = " + rc);
			if (bleManager != null) {
				reset();
				setNotification(false, rc);
			}
		}

		logi("startupConnection() :: end");
	}

	@Override
	public void onDestroy() {
		logi("onDestroy()");
	}

    public boolean registerMicrobitRequirements(BluetoothGattService eventService, boolean enable){
        /*
        Register to know about the micro:bit requirements. What events does the micro:bit need from us
        read repeatedly from (3) to find out the events that the micro:bit is interested in receiving.
        e.g. if a kids app registers to receive events <10,3><15,2> then the first read will give you <10,3> the second <15,2>,
        the third will give you a zero length value.
        You can send events to the micro:bit that haven't been asked for, but as no-one will be listening, they will be silently dropped.
        */
        BluetoothGattCharacteristic microbit_requirements = eventService.getCharacteristic(Constants.ES_MICROBIT_REQUIREMENTS);
        if(microbit_requirements == null) {
            logi("register_eventsFromMicrobit() :: ES_MICROBIT_REQUIREMENTS Not found");
            return false;
        }
        BluetoothGattDescriptor microbit_requirementsDescriptor = microbit_requirements.getDescriptor(Constants.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        if(microbit_requirementsDescriptor == null) {
            logi("register_eventsFromMicrobit() :: CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR Not found");
            return false;
        }


        BluetoothGattCharacteristic characteristic = readCharacteristic(microbit_requirements);
        while (characteristic !=null && characteristic.getValue() != null && characteristic.getValue().length != 0 ){
            String service = BluetoothUtils.parse(characteristic) ;
            logi("microbit interested in  = " + service);
            if (service.equalsIgnoreCase("4F-04-07-00")) //Incoming Call service
            {
                sendMicroBitNeedsCallNotification();
            }
            if (service.equalsIgnoreCase("4F-04-08-00")) //Incoming SMS service
            {
                sendMicroBitNeedsSmsNotification();
            }
            characteristic = readCharacteristic(microbit_requirements);
        }

        registerForSignalStrength(enable);
        registerForDeviceInfo(enable);

        logi("registerMicrobitRequirements() :: found Constants.ES_MICROBIT_REQUIREMENTS ");
        enableCharacteristicNotification(microbit_requirements, microbit_requirementsDescriptor, enable);
        return true;
    }

    public void register_AppRequirement(BluetoothGattService eventService, boolean enable){
        /*
        write repeatedly to (4) to register for the events your app wants to see from the micro:bit.
        e.g. write <1,1> to register for a 'DOWN' event on ButtonA.
        Any events matching this will then start to be delivered via the MicroBit Event characteristic.
        */
        if (!enable)
            return;
        BluetoothGattCharacteristic app_requirements = eventService.getCharacteristic(Constants.ES_CLIENT_REQUIREMENTS);
        if(app_requirements != null)
        {
            logi("register_AppRequirement() :: found Constants.ES_CLIENT_REQUIREMENTS ");
            /*
            Registering for everything at the moment
            <1,0> which means give me all the events from ButtonA.
            <2,0> which means give me all the events from ButtonB.
            <0,0> which means give me all the events from everything.
            writeCharacteristic(Constants.EVENT_SERVICE.toString(), Constants.ES_CLIENT_REQUIREMENTS.toString(), 0, BluetoothGattCharacteristic.FORMAT_UINT32);
            */
           writeCharacteristic(Constants.EVENT_SERVICE.toString(), Constants.ES_CLIENT_REQUIREMENTS.toString(), Constants.SAMSUNG_REMOTE_CONTROL_ID, BluetoothGattCharacteristic.FORMAT_UINT32);
           writeCharacteristic(Constants.EVENT_SERVICE.toString(), Constants.ES_CLIENT_REQUIREMENTS.toString(), Constants.SAMSUNG_CAMERA_ID, BluetoothGattCharacteristic.FORMAT_UINT32);
           writeCharacteristic(Constants.EVENT_SERVICE.toString(), Constants.ES_CLIENT_REQUIREMENTS.toString(), Constants.SAMSUNG_ALERTS_ID, BluetoothGattCharacteristic.FORMAT_UINT32);
           writeCharacteristic(Constants.EVENT_SERVICE.toString(), Constants.ES_CLIENT_REQUIREMENTS.toString(), Constants.SAMSUNG_SIGNAL_STRENGTH_ID, BluetoothGattCharacteristic.FORMAT_UINT32);
           writeCharacteristic(Constants.EVENT_SERVICE.toString(), Constants.ES_CLIENT_REQUIREMENTS.toString(), Constants.SAMSUNG_DEVICE_INFO_ID, BluetoothGattCharacteristic.FORMAT_UINT32);
           //writeCharacteristic(Constants.EVENT_SERVICE.toString(), Constants.ES_CLIENT_REQUIREMENTS.toString(), Constants.SAMSUNG_TELEPHONY_ID, BluetoothGattCharacteristic.FORMAT_UINT32);


        }
    }

    public boolean registerMicroBitEvents(BluetoothGattService eventService, boolean enable){
        // Read (or register for notify) on (1) to receive events generated by the micro:bit.
        BluetoothGattCharacteristic microbit_requirements = eventService.getCharacteristic(Constants.ES_MICROBIT_EVENT);
        if(microbit_requirements == null) {
            logi("register_eventsFromMicrobit() :: ES_MICROBIT_EVENT Not found");
            return false;
        }
        BluetoothGattDescriptor microbit_requirementsDescriptor = microbit_requirements.getDescriptor(Constants.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        if(microbit_requirementsDescriptor == null) {
            logi("register_eventsFromMicrobit() :: CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR Not found");
            return false;
        }

        enableCharacteristicNotification(microbit_requirements, microbit_requirementsDescriptor, enable);
        return true;
    }

    public void disconnectAll()
    {
        logi("disconnectAll()");
        registerNotifications(false);
    }
	public boolean registerNotifications(boolean enable) {

		logi("registerNotifications() : " + enable  );

        //Read microbit firmware version
        BluetoothGattService deviceInfoService = getService(Constants.DEVICE_INFORMATION_SERVICE_UUID);
        if (deviceInfoService != null) {
            BluetoothGattCharacteristic firmwareCharacteristic = deviceInfoService.getCharacteristic(Constants.FIRMWARE_REVISION_UUID);
            if (firmwareCharacteristic != null)
            {
                String firmware  = "";
                BluetoothGattCharacteristic characteristic = readCharacteristic(firmwareCharacteristic);
                if (characteristic !=null && characteristic.getValue() != null && characteristic.getValue().length != 0)
                {
                     firmware = firmwareCharacteristic.getStringValue(0) ;
                }
                sendMicrobitFirmware(firmware);
                logi("Micro:bit firmware version String = " + firmware);
            }
        }
		BluetoothGattService eventService = getService(Constants.EVENT_SERVICE);
		if (eventService == null) {
			logi("registerNotifications() :: not found service : Constants.EVENT_SERVICE");
			return false;
		}
        if (BuildConfig.DEBUG) {
            logi("Constants.EVENT_SERVICE   = " + Constants.EVENT_SERVICE.toString());
            logi("Constants.ES_MICROBIT_REQUIREMENTS   = " + Constants.ES_MICROBIT_REQUIREMENTS.toString());
            logi("Constants.ES_CLIENT_EVENT   = " + Constants.ES_CLIENT_EVENT.toString());
            logi("Constants.ES_MICROBIT_EVENT   = " + Constants.ES_MICROBIT_EVENT.toString());
            logi("Constants.ES_CLIENT_REQUIREMENTS   = " + Constants.ES_CLIENT_REQUIREMENTS.toString());
        }
        if (!registerMicrobitRequirements(eventService, enable)){
            if (BuildConfig.DEBUG) {
                logi("***************** Cannot Register Microbit Requirements.. Will continue ************** ");
            }
        }

        register_AppRequirement(eventService, enable);

        if (!registerMicroBitEvents(eventService, enable)){
            logi("Failed to registerMicroBitEvents");
            return false;
        }
		logi("registerNotifications() : done");
		return true;
	}

	@Override
	protected void handleCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        String UUID = characteristic.getUuid().toString();
        int value = 0 ;
        Integer integerValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);

        if (integerValue == null) {
            return;
        }
        value = integerValue.intValue();
		int eventSrc = value & 0x0ffff;
		if (eventSrc < 1001) {
			return;
		}
        logi("Characteristic UUID = " + UUID);
        logi("Characteristic Value = " + value);
        logi("eventSrc = " + eventSrc) ;

		int event = (value >> 16) & 0x0ffff;
        logi("event = " + event) ;
		sendMessage(eventSrc, event);
	}

	@Override
	protected void handleUnexpectedConnectionEvent(int event) {

		logi("handleUnexpectedConnectionEvent() :: event = " + event);
		if ((event & BLEManager.BLE_CONNECTED) != 0) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					logi("handleUnexpectedConnectionEvent() :: BLE_CONNECTED");
					discoverServices();
					registerNotifications(true);
					setNotification(true, 0);
				}
			}).start();

		} else if (event == BLEManager.BLE_DISCONNECTED) {
			logi("handleUnexpectedConnectionEvent() :: BLE_DISCONNECTED");
			setNotification(false, 0);
		}
	}

    protected void sendMicrobitFirmware(String firmware)
    {
        logi("sendMicrobitFirmware() :: firmware = " + firmware);
        NameValuePair[] args = new NameValuePair[2];
        args[0] = new NameValuePair(IPCMessageManager.BUNDLE_ERROR_CODE, 0);
        args[1] = new NameValuePair(IPCMessageManager.BUNDLE_MICROBIT_FIRMWARE, firmware);
        sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_CHARACTERISTIC_CHANGED, null, args);
    }

    protected void sendMicroBitNeedsCallNotification()
    {
        logi("sendMicroBitNeedsCallNotification()");
        NameValuePair[] args = new NameValuePair[2];
        args[0] = new NameValuePair(IPCMessageManager.BUNDLE_ERROR_CODE, 0);
        args[1] = new NameValuePair(IPCMessageManager.BUNDLE_MICROBIT_REQUESTS, IPCMessageManager.IPC_NOTIFICATION_INCOMING_CALL_REQUESTED);
        sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_CHARACTERISTIC_CHANGED, null, args);
    }

    protected void sendMicroBitNeedsSmsNotification()
    {
        logi("sendMicroBitNeedsSmsNotification()");
        NameValuePair[] args = new NameValuePair[2];
        args[0] = new NameValuePair(IPCMessageManager.BUNDLE_ERROR_CODE, 0);
        args[1] = new NameValuePair(IPCMessageManager.BUNDLE_MICROBIT_REQUESTS, IPCMessageManager.IPC_NOTIFICATION_INCOMING_SMS_REQUESTED);
        sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_CHARACTERISTIC_CHANGED, null, args);
    }
	@Override
	protected void setNotification(boolean isConnected, int errorCode) {

		logi("setNotification() :: isConnected = " + isConnected);
        logi("setNotification() :: errorCode = " + errorCode);
        logi("setNotification() :: actual_Error = " + actual_Error);


        NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		String notificationString = null;
        boolean onGoingNotification = false;

		NameValuePair[] args = new NameValuePair[3];

		args[0] = new NameValuePair(IPCMessageManager.BUNDLE_ERROR_CODE, errorCode);
		args[1] = new NameValuePair(IPCMessageManager.BUNDLE_DEVICE_ADDRESS, deviceAddress);
		args[2] = new NameValuePair(IPCMessageManager.BUNDLE_ERROR_MESSAGE, ErrorUtils.broadcastGetErrorMessage(actual_Error));

		if (!isConnected) {
			logi("setNotification() :: !isConnected");
			if (bluetoothAdapter != null) {
				if (!bluetoothAdapter.isEnabled()) {

					logi("setNotification() :: !bluetoothAdapter.isEnabled()");
					reset();
					//bleManager = null;
					bluetoothDevice = null;
				}
			}
            notificationString = getString(R.string.tray_notification_failure) ;
            onGoingNotification = false;

			sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_GATT_DISCONNECTED, null, args);
			sendtoPluginService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_GATT_DISCONNECTED, null, args);
		} else {
            notificationString = getString(R.string.tray_notification_sucsess);
            onGoingNotification = true;

			sendtoIPCService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_GATT_CONNECTED, null, args);
			sendtoPluginService(IPCMessageManager.ANDROID_MESSAGE, IPCMessageManager.IPC_NOTIFICATION_GATT_CONNECTED, null, args);
		}
    }

	// ######################################################################

	int lastEvent = Constants.SAMSUNG_REMOTE_CONTROL_EVT_PAUSE;

	void sendMessage(int eventSrc, int event) {

		logi("Sending eventSrc " + eventSrc + "  event=" + event);
		int msgService = 0;
		CmdArg cmd = null;
		switch (eventSrc) {
			case Constants.SAMSUNG_REMOTE_CONTROL_ID:
			case Constants.SAMSUNG_ALERTS_ID:
			case Constants.SAMSUNG_AUDIO_RECORDER_ID:
			case Constants.SAMSUNG_CAMERA_ID:
                //TODO Rohit - This is not actually storing the value from the msg. Rectify this.
				msgService = eventSrc;
				cmd = new CmdArg(event, "1000");
				break;

			default:
				return;
		}
		if (cmd != null) {
			sendtoPluginService(IPCMessageManager.MICROBIT_MESSAGE, msgService, cmd, null);
		}
	}

    void registerForSignalStrength(boolean register)
    {
        logi("registerForSignalStrength() -- " + register);
        CmdArg cmd = register? new CmdArg(Constants.REG_SIGNALSTRENGTH, "On") : new CmdArg(Constants.REG_SIGNALSTRENGTH, "Off");
        sendtoPluginService(IPCMessageManager.MICROBIT_MESSAGE, Constants.SAMSUNG_SIGNAL_STRENGTH_ID, cmd, null);
    }

    void registerForDeviceInfo(boolean register)
    {
        logi("registerForDeviceInfo() -- " + register);
        //Device Orientation
        CmdArg cmd = register? new CmdArg(Constants.REG_DEVICEORIENTATION, "On") : new CmdArg(Constants.REG_DEVICEORIENTATION, "Off");
        sendtoPluginService(IPCMessageManager.MICROBIT_MESSAGE, Constants.SAMSUNG_DEVICE_INFO_ID, cmd, null);

        //Device Gesture
        CmdArg cmd1 = register? new CmdArg(Constants.REG_DEVICEGESTURE, "On") : new CmdArg(Constants.REG_DEVICEGESTURE, "Off");
        sendtoPluginService(IPCMessageManager.MICROBIT_MESSAGE, Constants.SAMSUNG_DEVICE_INFO_ID, cmd1, null);


        //Device Battery Strength
        CmdArg cmd2 = register? new CmdArg(Constants.REG_BATTERYSTRENGTH, "On") : new CmdArg(Constants.REG_BATTERYSTRENGTH, "Off");
        sendtoPluginService(IPCMessageManager.MICROBIT_MESSAGE, Constants.SAMSUNG_DEVICE_INFO_ID, cmd2, null);

        //Device Temperature
        CmdArg cmd3 = register? new CmdArg(Constants.REG_TEMPERATURE, "On") : new CmdArg(Constants.REG_TEMPERATURE, "Off");
        sendtoPluginService(IPCMessageManager.MICROBIT_MESSAGE, Constants.SAMSUNG_DEVICE_INFO_ID, cmd3, null);


        //Register Telephony
        CmdArg cmd4 = register? new CmdArg(Constants.REG_TELEPHONY, "On") : new CmdArg(Constants.REG_TELEPHONY, "Off");
        sendtoPluginService(IPCMessageManager.MICROBIT_MESSAGE, Constants.SAMSUNG_TELEPHONY_ID, cmd4, null);

        //Register Messaging
        CmdArg cmd5 = register? new CmdArg(Constants.REG_MESSAGING, "On") : new CmdArg(Constants.REG_MESSAGING, "Off");
        sendtoPluginService(IPCMessageManager.MICROBIT_MESSAGE, Constants.SAMSUNG_TELEPHONY_ID, cmd5, null);

        //Register Display
        CmdArg cmd6 = register? new CmdArg(Constants.REG_DISPLAY, "On") : new CmdArg(Constants.REG_DISPLAY, "Off");
        sendtoPluginService(IPCMessageManager.MICROBIT_MESSAGE, Constants.SAMSUNG_DEVICE_INFO_ID, cmd6, null);

    }
	/*
	 * IPC Messenger handling
	 */
	@Override
	public IBinder onBind(Intent intent) {

		return IPCMessageManager.getInstance().getClientMessenger().getBinder();
	}

	public void startIPCListener() {

        logi("startIPCListener()");
		if (IPCMessageManager.getInstance() == null) {


			logi("startIPCListener() :: IPCMessageManager.getInstance() == null");
			IPCMessageManager inst = IPCMessageManager.getInstance("BLEServiceReceiver", new Handler() {

				@Override
				public void handleMessage(Message msg) {
					logi("startIPCListener().handleMessage");
					handleIncomingMessage(msg);
				}

			});
		}
	}

	public void sendtoPluginService(int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {

		logi("sendtoPluginService()");
		Class destService = PluginService.class;
		sendIPCMessge(destService, mbsService, functionCode, cmd, args);
	}

	public void sendtoIPCService(int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {

		logi("sendtoIPCService()");
		Class destService = IPCService.class;
        sendIPCMessge(destService, mbsService, functionCode, cmd, args);
    }

    public void sendIPCMessge(Class destService, int mbsService, int functionCode, CmdArg cmd, NameValuePair[] args) {

		IPCMessageManager inst = IPCMessageManager.getInstance();
		if (!inst.isConnected(destService)) {
			inst.configureServerConnection(destService, this);
		}

		if (mbsService != IPCMessageManager.ANDROID_MESSAGE && mbsService != IPCMessageManager.MICROBIT_MESSAGE) {
			return;
		}

		Message msg = Message.obtain(null, mbsService);
        msg.arg1 = functionCode;
		Bundle bundle = new Bundle();
		if (cmd != null) {
			bundle.putInt(IPCMessageManager.BUNDLE_DATA, cmd.getCMD());
			bundle.putString(IPCMessageManager.BUNDLE_VALUE, cmd.getValue());
		}

		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				bundle.putSerializable(args[i].getName(), args[i].getValue());
            }
        }

		msg.setData(bundle);
        try {
            inst.sendMessage(destService, msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

    public void writeCharacteristicByte(String serviceGuid, String characteristic, byte[] value) {

        if (!isConnected()) {
            logi("writeCharacteristic() :: Not connected. Returning");
            return;
        }

        BluetoothGattService s = getService(UUID.fromString(serviceGuid));
        if (s ==null)
        {
            logi("writeCharacteristic() :: Service not found");
            return;
        }

        BluetoothGattCharacteristic c = s.getCharacteristic(UUID.fromString(characteristic));
        if (c ==null)
        {
            logi("writeCharacteristic() :: characteristic not found");
            return;
        }
        c.setValue(value);
        writeCharacteristic(c);
    }

	public void writeCharacteristic(String serviceGuid, String characteristic, int value, int type) {

		if (!isConnected()) {
            logi("writeCharacteristic() :: Not connected. Returning");
			return;
		}

		BluetoothGattService s = getService(UUID.fromString(serviceGuid));
        if (s ==null)
        {
            logi("writeCharacteristic() :: Service not found");
            return;
        }

        BluetoothGattCharacteristic c = s.getCharacteristic(UUID.fromString(characteristic));
        if (c ==null)
        {
            logi("writeCharacteristic() :: characteristic not found");
            return;
        }
		c.setValue(value, type, 0);
        int ret = writeCharacteristic(c);
        logi("writeCharacteristic() :: returns - " + ret);
	}

	private void handleIncomingMessage(Message msg) {
		logi("handleIncomingMessage() :: Start BLEService");
		Bundle bundle = msg.getData();
		if (msg.what == IPCMessageManager.ANDROID_MESSAGE) {
			logi("handleIncomingMessage() :: IPCMessageManager.ANDROID_MESSAGE msg.arg1 = " + msg.arg1);

			switch (msg.arg1) {
				case IPCMessageManager.IPC_FUNCTION_CONNECT:
					logi("handleIncomingMessage() :: IPCMessageManager.IPC_FUNCTION_CONNECT bleManager = " + bleManager);
					setupBLE();
					break;

				case IPCMessageManager.IPC_FUNCTION_DISCONNECT:
					logi("handleIncomingMessage() :: IPCMessageManager.IPC_FUNCTION_DISCONNECT = " + bleManager);
					if (reset()) {
						setNotification(false, 0);
					}

					break;

				case IPCMessageManager.IPC_FUNCTION_RECONNECT:
					logi("handleIncomingMessage() :: IPCMessageManager.IPC_FUNCTION_RECONNECT = " + bleManager);
					if (reset()) {
						setupBLE();
					}

					break;

				default:
			}
		} else if (msg.what == IPCMessageManager.MICROBIT_MESSAGE) {
			logi("handleIncomingMessage() :: IPCMessageManager.MICROBIT_MESSAGE msg.arg1 = " + msg.arg1);
			switch (msg.arg1) {
				case IPCMessageManager.IPC_FUNCTION_WRITE_CHARACTERISTIC:
					logi("handleIncomingMessage() :: IPCMessageManager.IPC_FUNCTION_WRITE_CHARACTERISTIC = " + bleManager);
					String service = (String) bundle.getSerializable(IPCMessageManager.BUNDLE_SERVICE_GUID);
					String characteristic = (String) bundle.getSerializable(IPCMessageManager.BUNDLE_CHARACTERISTIC_GUID);
					int value = (int) bundle.getSerializable(IPCMessageManager.BUNDLE_CHARACTERISTIC_VALUE);
					int type = (int) bundle.getSerializable(IPCMessageManager.BUNDLE_CHARACTERISTIC_TYPE);
					writeCharacteristic(service, characteristic, value, type);
					break;

				default:
			}
		}
	}
}
