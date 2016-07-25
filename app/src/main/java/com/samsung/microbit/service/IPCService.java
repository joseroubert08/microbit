package com.samsung.microbit.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.constants.RequestCodes;
import com.samsung.microbit.data.constants.ServiceIds;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.model.ConnectedDevice;
import com.samsung.microbit.utils.ServiceUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public class IPCService extends Service {

    private static final String TAG = IPCService.class.getSimpleName();

    private ServiceConnector serviceConnector;

    private int justPaired;

    private static final class IPCHandler extends Handler {
        private WeakReference<IPCService> ipcServiceWeakReference;

        private IPCHandler(IPCService bleService) {
            super();
            ipcServiceWeakReference = new WeakReference<>(bleService);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(ipcServiceWeakReference.get() != null) {
                ipcServiceWeakReference.get().handleMessage(msg);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        serviceConnector = new ServiceConnector(this);
        serviceConnector.bindServices();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        IPCHandler ipcHandler = new IPCHandler(this);
        serviceConnector.setClientHandler(ipcHandler);

        return serviceConnector.mClientMessenger.getBinder();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Intent intent = new Intent(this, IPCService.class);
        intent.putExtra(IPCConstants.INTENT_TYPE, EventCategories.IPC_START_PROCESS);

        PendingIntent pi = PendingIntent.getService(getApplicationContext(), RequestCodes.REQUEST_RESTART_SERVICES,
                intent, PendingIntent.FLAG_ONE_SHOT);

        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + TimeUnit.SECONDS
                .toMillis(2), pi);

        /*
        //TODO check why service is not recreating by system itself on real devices. Why do we need to use AlarmManager?
        if(serviceConnector != null) {
            Log.i(TAG, "Unbind services");
            serviceConnector.unbindServices();
        }*/
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int category = intent.getIntExtra(IPCConstants.INTENT_TYPE, EventCategories.CATEGORY_UNKNOWN);

        if(category == EventCategories.CATEGORY_UNKNOWN) {
            Log.e(TAG, "Unknown category");
            return START_REDELIVER_INTENT;
        }

        switch(category) {
            case EventCategories.IPC_START_PROCESS: {
                Log.i(TAG, "Process started");
                break;
            }
            case EventCategories.IPC_BLE_CONNECT: {
                Message message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID, EventCategories
                        .IPC_BLE_CONNECT, ServiceIds.SERVICE_BLE, null, null);

                if(message != null) {
                    justPaired = intent.getIntExtra(IPCConstants.INTENT_CONNECTION_TYPE, 0);
                    handleMessage(message);
                }
                break;
            }
            case EventCategories.IPC_BLE_DISCONNECT: {
                Message message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID, EventCategories
                        .IPC_BLE_DISCONNECT, ServiceIds.SERVICE_BLE, null, null);

                if(message != null) {
                    handleMessage(message);
                }
                break;
            }
            case EventCategories.CATEGORY_REPLY: {
                sendReplyCommand(intent.getIntExtra(IPCConstants.INTENT_MBS_SERVICE, 0), intent.getIntExtra(IPCConstants.INTENT_REPLY_TO, ServiceIds.SERVICE_NONE), (CmdArg) intent.getParcelableExtra(IPCConstants.INTENT_CMD_ARG));
                break;
            }
            case EventCategories.IPC_PLUGIN_STOP_PLAYING: {
                Message message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID,
                        EventCategories.IPC_PLUGIN_STOP_PLAYING, ServiceIds.SERVICE_PLUGIN, null, null);
                handleMessage(message);
                break;
            }
            case EventCategories.IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED: {
                Message message = ServiceUtils.composeBLECharacteristicMessage(intent.getIntExtra(IPCConstants.INTENT_CHARACTERISTIC_MESSAGE, 0));
                message.arg2 = ServiceIds.SERVICE_PLUGIN;
                handleMessage(message);
                break;
            }
            default:
                Log.e(TAG, "Unknown category");
        }

        return START_REDELIVER_INTENT;
    }

    private void handleMessage(Message message) {
        String replyToServiceName = null;
        switch(message.arg2) {
            case ServiceIds.SERVICE_PLUGIN:
                replyToServiceName = PluginService.class.getName();
                break;
            case ServiceIds.SERVICE_BLE:
                replyToServiceName = BLEService.class.getName();
                break;
        }

        if(replyToServiceName != null) {
            ServiceUtils.IMessengerFinder messengerFinder = serviceConnector.getConnection();

            if(messengerFinder != null) {
                Messenger messenger = messengerFinder.getMessengerForService(replyToServiceName);
                if(messenger != null) {
                    Message newMessage = ServiceUtils.copyMessageFromOld(message, ServiceIds.SERVICE_NONE);
                    newMessage.replyTo = serviceConnector.mClientMessenger;
                    if(justPaired != 0) {
                        newMessage.arg2 = justPaired;
                        justPaired = 0;
                    }
                    try {
                        messenger.send(newMessage);
                    } catch(RemoteException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
        } else {
            if(message.arg1 == EventCategories.IPC_BLE_NOTIFICATION_GATT_CONNECTED ||
                    message.arg1 == EventCategories.IPC_BLE_NOTIFICATION_GATT_DISCONNECTED) {
                Context appContext = getApplicationContext();

                ConnectedDevice cd = BluetoothUtils.getPairedMicrobit(appContext);
                cd.mStatus = (message.arg1 == EventCategories.IPC_BLE_NOTIFICATION_GATT_CONNECTED);
                BluetoothUtils.setPairedMicroBit(appContext, cd);

                Bundle messageData = message.getData();

                int errorCode = (int) messageData.getSerializable(IPCConstants.BUNDLE_ERROR_CODE);

                String error_message = (String) messageData.getSerializable(IPCConstants.BUNDLE_ERROR_MESSAGE);

                String firmware = (String) messageData.getSerializable(IPCConstants.BUNDLE_MICROBIT_FIRMWARE);

                int microbitRequest = -1;
                if(messageData.getSerializable(IPCConstants.BUNDLE_MICROBIT_REQUESTS) != null) {
                    microbitRequest = (int) messageData.getSerializable(IPCConstants.BUNDLE_MICROBIT_REQUESTS);
                }

                Intent intent = new Intent(IPCConstants.INTENT_BLE_NOTIFICATION);
                intent.putExtra(IPCConstants.NOTIFICATION_CAUSE, message.arg1);
                intent.putExtra(IPCConstants.BUNDLE_ERROR_CODE, errorCode);
                intent.putExtra(IPCConstants.BUNDLE_ERROR_MESSAGE, error_message);
                intent.putExtra(IPCConstants.BUNDLE_MICROBIT_FIRMWARE, firmware);
                intent.putExtra(IPCConstants.BUNDLE_MICROBIT_REQUESTS, microbitRequest);

                LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
            }
        }
    }

    /**
     * Send some reply message to the ipc.
     *
     * @param mbsService MbsService of reply.
     * @param cmd        Command should be sent, as reply.
     */
    private void sendReplyCommand(int mbsService, int replyTo, CmdArg cmd) {
        Message msg = Message.obtain(null, mbsService);
        Bundle bundle = new Bundle();
        bundle.putInt("cmd", cmd.getCMD());
        bundle.putString("value", cmd.getValue());
        msg.setData(bundle);
        msg.arg2 = replyTo;
        handleMessage(msg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serviceConnector.unbindServices();
    }
}
