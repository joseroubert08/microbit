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
import com.samsung.microbit.core.BLEManager;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.NameValuePair;

import java.util.UUID;

public class BLEService extends BLEBaseService {

	protected String TAG = "BLEService";
	protected boolean debug = true;
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
		ConnectedDevice currentDevice = Utils.getPairedMicrobit(this);
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
            if (BuildConfig.DEBUG) {
                logi("Constants.SAMSUNG_REMOTE_CONTROL_IDF = " + Integer.toHexString(Constants.SAMSUNG_REMOTE_CONTROL_IDF));
                logi("Constants.SAMSUNG_CAMERA_IDF = " + Integer.toHexString(Constants.SAMSUNG_CAMERA_IDF));
                logi("Constants.SAMSUNG_ALERTS_IDF = " + Integer.toHexString(Constants.SAMSUNG_ALERTS_IDF));
            }

            writeCharacteristic(Constants.EVENT_SERVICE.toString(), Constants.ES_CLIENT_REQUIREMENTS.toString(), Constants.SAMSUNG_REMOTE_CONTROL_IDF, BluetoothGattCharacteristic.FORMAT_UINT32);
            writeCharacteristic(Constants.EVENT_SERVICE.toString(), Constants.ES_CLIENT_REQUIREMENTS.toString(), Constants.SAMSUNG_CAMERA_IDF, BluetoothGattCharacteristic.FORMAT_UINT32);
            //writeCharacteristic(Constants.EVENT_SERVICE.toString(), Constants.ES_CLIENT_REQUIREMENTS.toString(), Constants.SAMSUNG_AUDIO_RECORDER_IDF, BluetoothGattCharacteristic.FORMAT_UINT32);
            writeCharacteristic(Constants.EVENT_SERVICE.toString(), Constants.ES_CLIENT_REQUIREMENTS.toString(), Constants.SAMSUNG_ALERTS_IDF, BluetoothGattCharacteristic.FORMAT_UINT32);
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
	public boolean registerNotifications(boolean enable) {

		logi("registerNotifications()");
		BluetoothGattService eventService = getService(Constants.EVENT_SERVICE);
		if (eventService == null) {
			logi("registerNotifications() :: not found service : Constants.EVENT_SERVICE");
			return false;
		}

		logi("Constants.EVENT_SERVICE   = " + Constants.EVENT_SERVICE.toString());
		logi("Constants.ES_MICROBIT_REQUIREMENTS   = " + Constants.ES_MICROBIT_REQUIREMENTS.toString());
		logi("Constants.ES_CLIENT_EVENT   = " + Constants.ES_CLIENT_EVENT.toString());
		logi("Constants.ES_MICROBIT_EVENT   = " + Constants.ES_MICROBIT_EVENT.toString());
		logi("Constants.ES_CLIENT_REQUIREMENTS   = " + Constants.ES_CLIENT_REQUIREMENTS.toString());

        if (!registerMicrobitRequirements(eventService, enable)){
            if (BuildConfig.DEBUG) {
                logi("***************** Cannot Register Microbit Requirements.. Will continue ************** ");
                logi("***************** Rectify Later ************** ");
            }
        }

        register_AppRequirement(eventService, enable);

        if (!registerMicroBitEvents(eventService, enable)){
            logi("Failed to registerMicroBitEvents");
            return false;
        }
		logi("registerNotificationsForPairCode() : done");
		return true;
	}

	@Override
	protected void handleCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {


        String UUID = characteristic.getUuid().toString();

		int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
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
		args[2] = new NameValuePair(IPCMessageManager.BUNDLE_ERROR_MESSAGE, Utils.broadcastGetErrorMessage(actual_Error));

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

		/*
        if (!isConnected && Constants.BLE_DISCONNECTED_FOR_FLASH == errorCode){
            //Diconnected for flashing. Remove the icon
            if (notifyMgr!= null) {
                logi("Removing Notifcation as we are now flashing the device ");
                notifyMgr.cancel(Constants.NOTIFICATION_ID);
            }
        }
        else {
            //Update the tray message
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(isConnected ? R.drawable.ble_connection_on : R.drawable.ble_connection_off)
                            .setContentTitle("micro:bit companion")
                            .setOngoing(onGoingNotification)
                            .setContentText(notificationString);

            Intent intent = new Intent(this, PairingActivity.class);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            notifyMgr.notify(Constants.NOTIFICATION_ID, mBuilder.build());
        }
        */

    }

	// ######################################################################

	int lastEvent = Constants.SAMSUNG_REMOTE_CONTROL_EVT_PAUSE;

	void sendMessage(int eventSrc, int event) {

		logi("eventSrc" + eventSrc + "  event=" + event);
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
                case IPCMessageManager.IPC_FUNCTION_DISCONNECT_FOR_FLASH:
                    logi("handleIncomingMessage() :: IPCMessageManager.IPC_FUNCTION_DISCONNECT_FOR_FLASH = " + bleManager);
                    if (reset()) {
                        setNotification(false, Constants.BLE_DISCONNECTED_FOR_FLASH);
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
