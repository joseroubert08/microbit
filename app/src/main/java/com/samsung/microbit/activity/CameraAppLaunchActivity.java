package com.samsung.microbit.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.service.PluginService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DateFormat;

import com.samsung.microbit.plugin.CameraPlugin;

/**
 * Created by t.maestri on 19/05/2015.
 */
public class CameraAppLaunchActivity extends Activity {

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;

    private enum Type_file {
        MEDIA_TYPE_IMAGE, MEDIA_TYPE_VIDEO
    }

    private static Uri fileUri;

    private static Uri getOutputMediaFileUri(Type_file type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private static File getOutputMediaFile(Type_file type){

        //TODO: Not sure this is the only valid status
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
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

        //TODO Using DateFormat.getDateInstance() to have the correct Date Format based on the country
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == Type_file.MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "MicroBit_"+ timeStamp + ".jpg");
        } else if(type == Type_file.MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "MicroBit_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    public void LaunchCameraForPic() {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        fileUri = getOutputMediaFileUri(Type_file.MEDIA_TYPE_IMAGE); // create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

        // start the image capture Intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }

    public void LaunchCameraForVideo() {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        fileUri = getOutputMediaFileUri(Type_file.MEDIA_TYPE_VIDEO); // create a file to save the video
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the video file name
        //TODO The video quality is set to high, shall we provide way to change it?
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video quality to high

        // start the image capture Intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
        }
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
        finish();
    }
}
