package com.samsung.microbit.plugin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.widget.Toast;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.samsung.microbit.ui.activity.CameraActivity_OldAPI;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.service.PluginService;

/**
 * Created by t.maestri on 11/05/2015.
 */
public class CameraPlugin {

    private static Context context = null;
    public static final int LAUNCH_CAMERA_FOR_PIC = 0;
    public static final int LAUNCH_CAMERA_FOR_VIDEO = 1;
    public static final int TAKE_PIC = 2;
    public static final int REC_VIDEO_START = 3;
    public static final int REC_VIDEO_STOP = 4;
    public static final int CLOSE_CAMERA = 5;

    private static final String TAG = "CameraPlugin";

    public static void pluginEntry(Context ctx, CmdArg cmd) {

        context = ctx;
        switch (cmd.getCMD()) {
            case LAUNCH_CAMERA_FOR_PIC:
                launchCameraForPic();
                break;
            case LAUNCH_CAMERA_FOR_VIDEO:
                launchCameraForVideo();
                break;
            case TAKE_PIC:
                takePic();
                break;
            case REC_VIDEO_START:
                recVideoStart();
                break;
            case REC_VIDEO_STOP:
                recVideoStop();
                break;
            case CLOSE_CAMERA:
                closeCamera();
                break;
            default:
                break;
        }
    }

    public static void sendReplyCommand(int mbsService, CmdArg cmd) {
        if(PluginService.mClientMessenger != null) {
            Message msg = Message.obtain(null, mbsService);
            Bundle bundle = new Bundle();
            bundle.putInt("cmd", cmd.getCMD());
            bundle.putString("value", cmd.getValue());
            msg.setData(bundle);

            try {
                PluginService.mClientMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    //This function should trigger an Activity that would be responsible of starting the camera app to take a picture.
    //The same activity should also store the result of the camera app, if valid
    private static void launchCameraForPic() {
        //TODO: remove Toast
        Toast.makeText(context, "Camera Activity Started", Toast.LENGTH_SHORT).show();
        Intent mIntent = new Intent(context, CameraActivity_OldAPI.class);
        mIntent.setAction("com.samsung.microbit.activity.CameraActivity.action.OPEN_FOR_PIC");
        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(mIntent);
    }

	private static void takePic() {
		//TODO: remove Toast
		Toast.makeText(context, "Take Pic Start", Toast.LENGTH_SHORT).show();
		Intent intent = new Intent("TAKE_PIC");
		context.sendBroadcast(intent);
	}

	private static void launchCameraForVideo() {
        //TODO: remove Toast
        Toast.makeText(context, "Camera Activity Started", Toast.LENGTH_SHORT).show();
        Intent mIntent = new Intent(context, CameraActivity_OldAPI.class);
        mIntent.setAction("com.samsung.microbit.activity.CameraActivity.action.OPEN_FOR_VIDEO");
        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(mIntent);
    }

    private static void recVideoStart() {
        //TODO: remove Toast
        Toast.makeText(context, "Rec Video Start", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent("START_VIDEO");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private static void recVideoStop() {
        //TODO: remove Toast
        Toast.makeText(context, "Rec Video Stop", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent("STOP_VIDEO");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private static void closeCamera() {
        //TODO: remove Toast
        Toast.makeText(context, "Close Camera", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent("CLOSE");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}
