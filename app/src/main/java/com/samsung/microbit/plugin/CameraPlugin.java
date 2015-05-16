package com.samsung.microbit.plugin;

import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.net.Uri;
import java.io.File;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.service.PluginService;

/**
 * Created by t.maestri on 11/05/2015.
 */
public class CameraPlugin {

    private static Context context = null;
    private enum Service {
        LAUNCH_EXTERNAL_CAMERA_FOR_PIC, LAUNCH_EXTERNAL_CAMERA_FOR_VIDEO, LAUNCH_CAMERA, TAKE_PIC, REC_VIDEO_START, REC_VIDEO_STOP
    }

    private static Uri fileUri;

    //private static CameraManager mCameraManager;

    public static void pluginEntry(Context ctx, CmdArg cmd) {

        context = ctx;
        switch (Service.values()[cmd.getCMD()]) {
            case LAUNCH_EXTERNAL_CAMERA_FOR_PIC:
                launchCameraForPic();
                break;
            case LAUNCH_EXTERNAL_CAMERA_FOR_VIDEO:
                launchCameraForPic();
                break;
            case LAUNCH_CAMERA:
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
            default:
                break;
        }
    }

    private enum Type_file {
        MEDIA_TYPE_IMAGE, MEDIA_TYPE_VIDEO
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

    private static Uri getOutputMediaFileUri(Type_file type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private static File getOutputMediaFile(Type_file type){

        //TODO: Not sure this is the only valid status
        if(Environment.getExternalStorageState()!=Environment.MEDIA_MOUNTED)
            return null;

        //TODO: The files are stored in the directory "Microbit", the value should be defined in strings
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Microbit");

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                //TODO Log: failed to create directory
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == Type_file.MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "MicroBit_"+ timeStamp + ".jpg");
        } else if(type == Type_file.MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "MicroBit_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    static CameraAppActivity mCameraAppActivity;

    static
    {
        mCameraAppActivity = null;
    }

    //This function should trigger an Activity that would be responsible of starting the camera app to take a picture.
    //The same activity should also store the result of the camera app, if valid
    private static void launchCameraForPic() {
//        if(mCameraAppActivity == null)
//            mCameraAppActivity = new CameraAppActivity();
    }

    //This activity can launch the camera app and manage the result
    //TODO Not clear where this activity should live...
    public class CameraAppActivity extends Activity
    {
        private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
        private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;

        public void LaunchCameraForPic() {
            // create Intent to take a picture and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            fileUri = getOutputMediaFileUri(Type_file.MEDIA_TYPE_IMAGE); // create a file to save the image
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

            // start the image capture Intent
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }

        public void LaunchCameraForVideo() {
            // create Intent to take a picture and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

            fileUri = getOutputMediaFileUri(Type_file.MEDIA_TYPE_VIDEO); // create a file to save the video
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the video file name
            //TODO The video quality is set to high, shall we provide way to change it?
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video quality to high

            // start the image capture Intent
            startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    // Image captured and saved to fileUri specified in the Intent
                    // Send the acknowledge to BLE
                    CmdArg cmd = new CmdArg(0,"Image captured ok");
                    CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
                } else if (resultCode == RESULT_CANCELED) {
                    // User cancelled the image capture
                    CmdArg cmd = new CmdArg(0,"Image captured cancelled");
                    CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
                } else {
                    // Image capture failed, advise user
                    CmdArg cmd = new CmdArg(0,"Image captured failed");
                    CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
                }
            }

            if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    // Video captured and saved to fileUri specified in the Intent
                    CmdArg cmd = new CmdArg(0,"Video captured ok");
                    CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
                } else if (resultCode == RESULT_CANCELED) {
                    // User cancelled the video capture
                    CmdArg cmd = new CmdArg(0,"Image captured cancelled");
                    CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
                } else {
                    // Video capture failed, advise user
                    CmdArg cmd = new CmdArg(0,"Image captured failed");
                    CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
                }
            }
        }
    }

    private static void takePic() {

    }

    private static void recVideoStart() {

    }

    private static void recVideoStop() {

    }

}
