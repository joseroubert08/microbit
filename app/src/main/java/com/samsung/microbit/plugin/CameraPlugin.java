package com.samsung.microbit.plugin;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.app.Activity;
import android.provider.MediaStore;
import android.net.Uri;
import java.io.File;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.samsung.microbit.model.CmdArg;

/**
 * Created by t.maestri on 11/05/2015.
 */
public class CameraPlugin {

    private static Context context = null;
    private enum Service {
        LAUNCH_CAMERA, TAKE_PIC, REC_VIDEO_START, REC_VIDEO_STOP
    }

    private static Uri fileUri;

    public static void pluginEntry(Context ctx, CmdArg cmd) {

        context = ctx;
        switch (Service.values()[cmd.getCMD()]) {
            case LAUNCH_CAMERA:
                launchCamera();
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

    private static void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        fileUri = getOutputMediaFileUri(Type_file.MEDIA_TYPE_IMAGE); // create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

        // start the image capture Intent
        //startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    private static void takePic() {

    }

    private static void recVideoStart() {

    }

    private static void recVideoStop() {

    }

}
