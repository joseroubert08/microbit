package com.samsung.microbit.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.samsung.microbit.BuildConfig;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.data.constants.PermissionCodes;
import com.samsung.microbit.ui.PopUp;

public class CameraActivityPermissionChecker extends AppCompatActivity {


    private Intent mIntent = null ;
    private boolean mOPenForPic = false ;

    protected boolean debug = BuildConfig.DEBUG;


    private enum  REQUEST_STATE{
        LAUNCH_CAMERA_FOR_PIC,
        LAUNCH_CAMERA_FOR_VIDEO,
    };


    private REQUEST_STATE mRequestedState;

    protected String TAG = "CameraActivityPermissionChecker";
    protected void logi(String message) {
        if (debug) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_activity_permission_checker);
        Intent intent = getIntent(); //Store this for later use


        if (intent.getAction().contains("OPEN_FOR_PIC")) {
            mRequestedState = REQUEST_STATE.LAUNCH_CAMERA_FOR_PIC;
        } else if (intent.getAction().contains("OPEN_FOR_VIDEO")) {
            mRequestedState = REQUEST_STATE.LAUNCH_CAMERA_FOR_VIDEO;
        }
        checkPermissionsForCamera();
    }


    private void startCameraActivity()
    {
        //Do not launch camera if in Do not Disturb Mode
        //Check more details on #122
        if (BluetoothUtils.inZenMode(this))
        {
            PopUp.show(MBApp.getApp(),
                    getString(R.string.dnd_error_msg),
                    getString(R.string.dnd_error_title),
                    R.drawable.message_face, R.drawable.blue_btn, PopUp.GIFF_ANIMATION_NONE,
                    PopUp.TYPE_ALERT,
                    null,
                    null);
        } else {
            Intent intent = new Intent(this, CameraActivity_OldAPI.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            switch (mRequestedState) {
                case LAUNCH_CAMERA_FOR_PIC:
                    intent.setAction("com.samsung.microbit.activity.CameraActivity.action.OPEN_FOR_PIC");
                    break;
                case LAUNCH_CAMERA_FOR_VIDEO:
                    intent.setAction("com.samsung.microbit.activity.CameraActivity.action.OPEN_FOR_VIDEO");
                    break;
            }

            startActivity(intent);
        }
        //Finish current activity
        finish();
    }
    private void checkPermissionsForCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA )!= PermissionChecker.PERMISSION_GRANTED)
        {
            PopUp.show(MBApp.getApp(),
                    getString(R.string.camera_permission),
                    getString(R.string.permissions_needed_title),
                    R.drawable.message_face, R.drawable.blue_btn, PopUp.GIFF_ANIMATION_NONE,
                    PopUp.TYPE_CHOICE,
                    cameraPermissionOKHandler,
                    cameraPermissionCancelHandler);
        } else {
            startCameraActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode)
        {
            case PermissionCodes.CAMERA_PERMISSIONS_REQUESTED: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCameraActivity();
                } else {
                    PopUp.show(MBApp.getApp(),
                            getString(R.string.camera_permission_error),
                            "",
                            R.drawable.error_face, R.drawable.red_btn,
                            PopUp.GIFF_ANIMATION_ERROR,
                            PopUp.TYPE_ALERT,
                            null, null);
                    finish();
                }
            }
            break;

        }
    }

    private void requetPermission(String[] permissions, final int requestCode) {
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    View.OnClickListener cameraPermissionOKHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("cameraPermissionOKHandler");
            PopUp.hide();
            String[] permissionsNeeded = {Manifest.permission.CAMERA};
            requetPermission(permissionsNeeded, PermissionCodes.CAMERA_PERMISSIONS_REQUESTED);
        }
    };

    View.OnClickListener cameraPermissionCancelHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            logi("cameraPermissionCancelHandler");
            PopUp.hide();
            PopUp.show(MBApp.getApp(),
                    getString(R.string.camera_permission_error),
                    "",
                    R.drawable.error_face, R.drawable.red_btn,
                    PopUp.GIFF_ANIMATION_ERROR,
                    PopUp.TYPE_ALERT,
                    null, null);
            finish();
        }
    };
}
