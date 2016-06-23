package com.samsung.microbit.ui.activity;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.samsung.microbit.BuildConfig;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.constants.Constants;
import com.samsung.microbit.data.constants.FileConstants;
import com.samsung.microbit.data.constants.RawConstants;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.presentation.PlayAudioPresenter;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.ui.view.CameraPreview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraActivity_OldAPI extends Activity {

    private static boolean mInstanceActive = false;

    private CameraPreview mPreview;
    private ImageButton mButtonClick, mButtonBack_portrait, mButtonBack_landscape;
    private Camera mCamera;
    private int mCameraIdx;
    private static boolean mfrontCamera = true;
    private BroadcastReceiver mMessageReceiver;
    private boolean mVideo = false;
    private boolean mIsRecording = false;
    private MediaRecorder mMediaRecorder;
    private File mVideoFile = null;
    private OrientationEventListener myOrientationEventListener;
    private int mCurrentRotation = -1;
    private int mStoredRotation = -1;
    private int mOrientationOffset = 0;
    private int mCurrentIconIndex = 0;
    private ArrayList<Drawable> mTakePhoto, mStartRecord, mStopRecord, mCurrentIconList;
    private Camera.Parameters mParameters = null;
    private Boolean bActivityInBackground = false;
    private Boolean bTakePicOnResume = false;
    private Boolean bRecordVideoOnResume = false;

    private PlayAudioPresenter playAudioPresenter;

    private static final String TAG = "CameraActivity_OldAPI";
    private boolean debug = BuildConfig.DEBUG;


    private MediaRecorder.OnInfoListener m_MediaInfoListner = new MediaRecorder.OnInfoListener() {

        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
            switch (what) {
                case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                    playAudioPresenter.setNotificationForPlay(RawConstants.MAX_VIDEO_RECORDED);
                    playAudioPresenter.start();
                    stopRecording();
                    break;
                case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                    logi("Error in media recorder - What = " + what + " extra = " + extra);
                    stopRecording();
                    break;
            }
        }
    };

    void logi(String message) {
        if (debug) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    private int getCurrentCamera() {
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (mfrontCamera && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                logi("returning front camera");
                return camIdx;
            } else if (!mfrontCamera && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                logi("returning back camera");
                return camIdx;
            }

        }
        return -1;
    }

    private Drawable rotateIcon(Drawable icon, int rotation) {
        Bitmap existingBitmap = ((BitmapDrawable) icon).getBitmap();
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation - mOrientationOffset);
        Bitmap rotated = Bitmap.createBitmap(existingBitmap, 0, 0, existingBitmap.getWidth(), existingBitmap.getHeight(), matrix, true);
        return new BitmapDrawable(rotated);
    }

    private void createRotatedIcons() {
        Drawable icon = getResources().getDrawable(R.drawable.take_photo);
        mTakePhoto = new ArrayList<Drawable>();
        mStartRecord = new ArrayList<Drawable>();
        mStopRecord = new ArrayList<Drawable>();
        mTakePhoto.add(rotateIcon(icon, 0));
        mTakePhoto.add(rotateIcon(icon, -90));
        mTakePhoto.add(rotateIcon(icon, 180));
        mTakePhoto.add(rotateIcon(icon, -270));
        icon = getResources().getDrawable(R.drawable.start_record_icon);
        mStartRecord.add(rotateIcon(icon, 0));
        mStartRecord.add(rotateIcon(icon, -90));
        mStartRecord.add(rotateIcon(icon, 180));
        mStartRecord.add(rotateIcon(icon, -270));
        icon = getResources().getDrawable(R.drawable.stop_record_icon);
        mStopRecord.add(rotateIcon(icon, 0));
        mStopRecord.add(rotateIcon(icon, -90));
        mStopRecord.add(rotateIcon(icon, 180));
        mStopRecord.add(rotateIcon(icon, -270));
    }

    private void setButtonForBackAction() {
        mButtonBack_portrait.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                goBackAction();
            }
        });

        mButtonBack_landscape.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                goBackAction();
            }
        });
    }

    private void goBackAction() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateButtonClickIcon() {
        mButtonClick.setBackground(mCurrentIconList.get(mCurrentIconIndex));
        mButtonClick.invalidate();
    }

    private void updateCameraRotation() {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setRotation(getRotationCameraCorrection(mCurrentRotation)); //set rotation to save the picture
            mCamera.setParameters(parameters);
        }
    }

    private void updateButtonOrientation(int rotation) {
        rotation = (rotation + mOrientationOffset) % 360;
        int quant_rotation = 0;
        boolean buttonPortraitVisible = true;
        if (rotation == OrientationEventListener.ORIENTATION_UNKNOWN)
            return;
        if (rotation < 45 || rotation >= 315) {
            buttonPortraitVisible = true;
            quant_rotation = 0;
            mCurrentIconIndex = 0;
        } else if ((rotation >= 135 && rotation < 225)) {
            buttonPortraitVisible = true;
            quant_rotation = 180;
            //mCurrentIconIndex = 2;
            mCurrentIconIndex = 0; //This way only 2 configurations are allowed
        } else if ((rotation >= 45 && rotation < 135)) {
            buttonPortraitVisible = false;
            quant_rotation = 270;
            //mCurrentIconIndex = 1;
            mCurrentIconIndex = 3; //This way only 2 configurations are allowed
        } else if ((rotation >= 225 && rotation < 315)) {
            buttonPortraitVisible = false;
            quant_rotation = 90;
            mCurrentIconIndex = 3;
        }
        if (quant_rotation != mCurrentRotation) {
            mCurrentRotation = quant_rotation;

            if (buttonPortraitVisible) {
                mButtonBack_landscape.setVisibility(View.INVISIBLE);
                mButtonBack_portrait.setVisibility(View.VISIBLE);
                mButtonBack_portrait.bringToFront();
            } else {
                mButtonBack_landscape.setVisibility(View.VISIBLE);
                mButtonBack_landscape.bringToFront();
                mButtonBack_portrait.setVisibility(View.INVISIBLE);
            }
            mButtonBack_portrait.invalidate();
            mButtonBack_landscape.invalidate();

            updateButtonClickIcon();
            updateCameraRotation();
        }
    }

    private void setButtonForPicture() {

        mCurrentIconList = mTakePhoto;
        updateButtonClickIcon();
        mButtonClick.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
                } catch (final Exception ex) {
                    sendCameraError();
                    Log.e(TAG, "Error during take picture", ex);
                }
            }
        });

        mButtonClick.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                mCamera.autoFocus(new AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean arg0, Camera arg1) {
                        //TODO Is there anything we have to do after autofocus?
                    }
                });
                return true;
            }
        });

    }

    private void setPreviewForPicture() {
        mPreview.setSoundEffectsEnabled(false);
        mPreview.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mCamera.autoFocus(new AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean arg0, Camera arg1) {
                        //TODO Is there anything we have to do after autofocus?
                    }
                });
            }
        });
    }

    private void stopRecording() {
        logi("Stop recording");
        mMediaRecorder.stop(); // stop the recording
        refreshGallery(mVideoFile);
        releaseMediaRecorder(); // release the MediaRecorder object
        mIsRecording = false;
        mCurrentIconList = mStartRecord;
        updateButtonClickIcon();
        releaseMediaRecorder();
        resetCam();
    }

    private void setButtonForVideo() {

        mCurrentIconList = mStartRecord;
        updateButtonClickIcon();
        mButtonClick.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mIsRecording) {
                    // stop recording and release mCamera
                    stopRecording();
                } else {
                    if (!prepareMediaRecorder()) {
                        sendCameraError();
                        Log.e(TAG, "Error preparing mediaRecorder");
                        finish();
                    }

                    mCurrentIconList = mStopRecord;
                    updateButtonClickIcon();
                    //TODO Video recodring crashing. Check #112 for details. Temporary fix for the BETT
                    //indicateVideoRecording();
                    //TODO Check that is true
                    // work on UiThread for better performance
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                mMediaRecorder.start();
                            } catch (final Exception ex) {
                                sendCameraError();
                                Log.e(TAG, "Error during video recording", ex);
                                finish();
                            }
                        }
                    });
                    mIsRecording = true;
                }
            }
        });
    }

    private void indicateVideoRecording() {
        logi("indicateVideoRecording");
        if (mParameters == null && mCamera != null) {
            setParameters();

        }
        new CountDownTimer(Constants.MAX_VIDEO_RECORDING_TIME_MILLIS, Constants.VIDEO_FLASH_PICK_INTERVAL) {
            boolean flashON = false;

            public void onTick(long millisUntilFinished) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (mIsRecording && mCamera != null) {
                            if (flashON) {
                                logi("turning flash ON");
                                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

                            } else {
                                logi("turning flash OFF");
                                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            }
                            mCamera.setParameters(mParameters);
                            flashON = !flashON;
                        }
                    }
                });
            }

            public void onFinish() {
                //Do nothing
            }
        }.start();
    }

    private void setParameters() {
        mParameters = mCamera.getParameters();

        List<String> flashModes = mParameters.getSupportedFlashModes();
        for (String flashmode : flashModes) {
            logi("Supported flash mode : " + flashmode);
        }
        mCamera.setParameters(mParameters);
        mCamera.enableShutterSound(true);
    }

    private void setPreviewForVideo() {

    }

    private int getRotationCameraCorrection(int current_rotation) {
        int degree = (current_rotation + 270) % 360;

        int result;
        String model =  Build.MODEL;

        if(model.contains("Nexus 5X")) {
            //Workaround for Nexus 5X camera issue
            //TODO: Use Camera API 2 to fix this correctly
            result = (mOrientationOffset + degree) % 360;

            if (!mfrontCamera) {
                if (result == 0)
                    result += 180;
                else if (result == 180)
                    result = 0;
            }

        } else {
            if (mfrontCamera) {
                result = (mOrientationOffset + degree) % 360;
            } else { // back-facing
                result = (mOrientationOffset - degree + 360) % 360;
            }
        }

        return result;
    }

    private void sendCameraError() {
        CmdArg cmd = new CmdArg(0, "Camera Error");
        CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
    }

    private void setOrientationOffset() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        Point screenSize = new Point(0, 0);
        getWindowManager().getDefaultDisplay().getSize(screenSize);

        logi("Display size " + screenSize.x + "x" + screenSize.y);

        //Checking if it's a tablet
        if (rotation == 0 || rotation == 2) {
            if (screenSize.x > screenSize.y) {
                //Tablet
                mOrientationOffset = 270;
                logi("Tablet");
            } else {
                //Phone
                mOrientationOffset = 0;
                logi("Phone");
            }
        } else {
            if (screenSize.x > screenSize.y) {
                //Phone
                mOrientationOffset = 0;
                logi("Phone");
            } else {
                //Tablet
                mOrientationOffset = 270;
                logi("Tablet");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        logi("onCreate() :: Start");
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        playAudioPresenter = new PlayAudioPresenter();

        createRotatedIcons();

        setOrientationOffset();

        myOrientationEventListener
                = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int arg0) {
                updateButtonOrientation(arg0);
            }
        };

        mCamera = null;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera_old_api);
        Intent intent = getIntent();
        if (intent.getAction().contains("OPEN_FOR_PIC")) {
            mVideo = false;
        } else if (intent.getAction().contains("OPEN_FOR_VIDEO")) {
            mVideo = true;
        }

        SurfaceView mSurfaceView = new SurfaceView(this);
        mPreview = new CameraPreview(this, mSurfaceView);
        mPreview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ((FrameLayout) findViewById(R.id.camera_preview_container)).addView(mPreview);
        mPreview.setKeepScreenOn(true);
        mPreview.setParentActivity(this);

        mButtonClick = (ImageButton) findViewById(R.id.picture);
        mButtonBack_portrait = (ImageButton) findViewById(R.id.back_portrait);
        mButtonBack_landscape = (ImageButton) findViewById(R.id.back_landscape);

        setButtonForBackAction();

        if (mVideo) {
            //Setup specific to OPEN_FOR_VIDEO
            setButtonForVideo();
            setPreviewForVideo();
        } else {
            //Setup specific to OPEN_FOR_PIC
            setButtonForPicture();
            setPreviewForPicture();
        }

        updateButtonOrientation(0);

        mMessageReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("CLOSE")) {
                    finish();
                } else if (!mVideo && intent.getAction().equals("TAKE_PIC")) {
                    mfrontCamera = true;
                    takePic();
                } else if (intent.getAction().equals("TOGGLE_CAMERA")) {
                    toggleCamera();
                } else if (mVideo && !mIsRecording && intent.getAction().equals("START_VIDEO")) {
                    mfrontCamera = true;
                    if(bActivityInBackground) {
                        bringActivityToFront();
                        bRecordVideoOnResume = true;
                    } else {
                        recordVideo();
                    }
                } else if (mVideo && mIsRecording && intent.getAction().equals("STOP_VIDEO")) {
                    mButtonClick.callOnClick();
                } else {
                    //Wrong sequence of commands
                    CmdArg cmd = new CmdArg(0, "Wrong Camera Command Sequence");
                    CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
                    Log.e(TAG, "Wrong command sequence");
                }
            }
        };

        this.registerReceiver(mMessageReceiver, new IntentFilter("CLOSE"));

        if (mVideo) {
            this.registerReceiver(mMessageReceiver, new IntentFilter("START_VIDEO"));
            this.registerReceiver(mMessageReceiver, new IntentFilter("STOP_VIDEO"));
            this.registerReceiver(mMessageReceiver, new IntentFilter("TOGGLE_CAMERA"));
        } else {
            this.registerReceiver(mMessageReceiver, new IntentFilter("TAKE_PIC"));
            this.registerReceiver(mMessageReceiver, new IntentFilter("TOGGLE_CAMERA"));
        }

        logi("onCreate() :: Done");
    }

    private void bringActivityToFront() {
        Intent intent = new Intent(getApplicationContext(), CameraActivity_OldAPI.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void toggleCamera() {
        mfrontCamera = !mfrontCamera;
        if(bActivityInBackground) {
            bringActivityToFront();
        } else {
            recreate();
        }
    }

    private void takePic() {
        if(bActivityInBackground) {
            bringActivityToFront();
            bTakePicOnResume = true;
        } else {
            startTakePicCounter();
        }
    }

    private void startTakePicCounter () {

        playAudioPresenter.setNotificationForPlay(RawConstants.TAKING_PHOTO_AUDIO);
        playAudioPresenter.start();
        final Toast toast = Toast.makeText(MBApp.getApp().getApplicationContext(),"bbb", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);

        //Toast.LENGTH_SHORT will keep the toast for 2s, our interval is 1s and calling toast.show()
        //after 1s will cause some count to be missed. Only call toast.show() just before 2s interval.
        //Also add delay to show the "Ready" toast.
        new CountDownTimer(Constants.PIC_COUNTER_DURATION_MILLIS, Constants.PIC_COUNTER_INTERVAL_MILLIS) {

            public void onTick(long millisUntilFinished) {
                int count = (int) millisUntilFinished / Constants.PIC_COUNTER_INTERVAL_MILLIS;
                toast.setText("Ready in... " + count);
                if(count%2 != 0)
                    toast.show();
            }

            public void onFinish() {
                toast.setText("Ready");
                toast.show();
                mButtonClick.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mButtonClick.callOnClick();
                    }

                }, 200);
            }
        }.start();
    }

    @Override
    protected void onResume() {
        logi("onCreate() :: onResume");

        super.onResume();

        bActivityInBackground = false;
        //This intent filter has to be set even if no camera is found otherwise the unregisterReceiver()
        //fails during the onPause()
        if (myOrientationEventListener.canDetectOrientation()) {
            logi("DetectOrientation Enabled");
            myOrientationEventListener.enable();
        } else {
            logi("DetectOrientation Disabled");
        }

        mCameraIdx = getCurrentCamera();
        logi("mCameraIdx = " + mCameraIdx);
        try {
            mCamera = Camera.open(mCameraIdx);
            if (mCamera == null) {
                logi("Couldn't open the camera");
            }
            logi("Step 2");
            mPreview.setCamera(mCamera, mCameraIdx);
            logi("Step 3");

            logi("Step 3");
            mPreview.restartCameraPreview();
            logi("Step 4");
            updateCameraRotation();
            logi("onCreate() :: onResume # ");

            if(bTakePicOnResume) {
                bTakePicOnResume = false;
                startTakePicCounter();
            } else if(bRecordVideoOnResume) {
                bRecordVideoOnResume = false;
                recordVideo();
            }

        } catch (RuntimeException ex) {
            logi(ex.toString());
            Toast.makeText(this, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
            sendCameraError();
            finish();
        }
    }

    private void recordVideo() {
        playAudioPresenter.setNotificationForPlay(RawConstants.RECORDING_VIDEO_AUDIO);
        playAudioPresenter.start();
        mButtonClick.postDelayed(new Runnable() {
            @Override
            public void run() {
                mButtonClick.callOnClick();
            }

        }, 200);
    }

    @Override
    protected void onPause() {

        logi("onCreate() :: onPause");

        if (mIsRecording) {
            stopRecording();
        }

        if (myOrientationEventListener.canDetectOrientation()) {
            logi("DetectOrientation Disabled");
            myOrientationEventListener.disable();
        }

        if (mCamera != null) {
            mCamera.stopPreview();
            mPreview.setCamera(null, -1);
            mCamera.release();
            mCamera = null;
        }
        super.onPause();

        bActivityInBackground = true;
    }

    private void resetCam() {
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            logi("Set Flash mode ON");
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters);
            mCamera.startPreview();
            mCamera.autoFocus(new AutoFocusCallback() {
                public void onAutoFocus(boolean success, Camera camera) {
                }
            });
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(parameters);
        } catch (RuntimeException e) {
            logi(e.getMessage());
        }
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    //TODO Add Sound here
    //Currently if the device is on silent mode no sound is going to be heard
    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
            ImageView blinkRect = (ImageView) findViewById(R.id.blink_rectangle);
            blinkRect.setVisibility(View.VISIBLE);
            blinkRect.bringToFront();
            blinkRect.invalidate();
        }
    };

    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            // Display toast here and play audio
            Toast toast = Toast.makeText(MBApp.getApp().getApplicationContext(), "Photo taken", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

            playAudioPresenter.setNotificationForPlay(RawConstants.PICTURE_TAKEN_AUDIO);
            playAudioPresenter.start();

        }
    };
    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SaveImageTask().execute(data);
            DrawBlink();
            resetCam();
        }
    };

    void DrawBlink() {
        SystemClock.sleep(500);
        ImageView blinkRect = (ImageView) findViewById(R.id.blink_rectangle);
        blinkRect.setVisibility(View.INVISIBLE);
        blinkRect.invalidate();
    }

    @Override
    protected void onStart() {
        logi("onCreate() :: onStart");
        //Informing microbit that the mCamera is active now
        CmdArg cmd = new CmdArg(0, "Camera on");
        CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        logi("onCreate() :: onDestroy");
        //Informing microbit that the mCamera is active now
        CmdArg cmd = new CmdArg(0, "Camera off");
        CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);

        this.unregisterReceiver(mMessageReceiver);

        playAudioPresenter.destroy();

        super.onDestroy();
    }

    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;

            // Write to SD Card
            try {
                File dir = FileConstants.MEDIA_OUTPUT_FOLDER;

                if (!dir.exists()) {
                    dir.mkdirs();
                }

                //TODO defining the file name
                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);

                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);

                outStream.flush();
                outStream.close();

                refreshGallery(outFile);

                CmdArg cmd = new CmdArg(0, "Camera picture saved");
                CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }

    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset(); // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mVideoFile = null;
            mCamera.lock(); // lock camera for later use
        }
    }


    private boolean prepareMediaRecorder() {

        if (mCameraIdx < 0 || mCamera == null)
            return false;

        mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setOnInfoListener(m_MediaInfoListner);

        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        //Rohit - Removed the audio source as BBC doesn't want sound in the recording
        //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        //TODO Check because depending on the quality on some devices the MediaRecorder doesn't work
        if (CamcorderProfile.hasProfile(mCameraIdx, CamcorderProfile.QUALITY_TIME_LAPSE_HIGH))
            mMediaRecorder.setProfile(CamcorderProfile.get(mCameraIdx, CamcorderProfile.QUALITY_TIME_LAPSE_HIGH));
        else if (CamcorderProfile.hasProfile(mCameraIdx, CamcorderProfile.QUALITY_TIME_LAPSE_LOW))
            mMediaRecorder.setProfile(CamcorderProfile.get(mCameraIdx, CamcorderProfile.QUALITY_TIME_LAPSE_LOW));
        else {
            releaseMediaRecorder();
            Log.e(TAG, "Error preparing media Recorder: no CamcorderProfile available");
            return false;
        }

        //Setting output file
        File dir = FileConstants.MEDIA_OUTPUT_FOLDER;
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = String.format("%d.mp4", System.currentTimeMillis());
        mVideoFile = new File(dir, fileName);
        mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());

        //Setting limits
        mMediaRecorder.setMaxDuration(Constants.MAX_VIDEO_RECORDING_TIME_MILLIS);
        mMediaRecorder.setMaxFileSize(Constants.MAX_VIDEO_FILE_SIZE_BYTES);

        int rotation = getRotationCameraCorrection(mCurrentRotation);
        mMediaRecorder.setOrientationHint(rotation);

        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            Log.e(TAG, "Error preparing media Recorder: " + e.getLocalizedMessage());
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            Log.e(TAG, "Error preparing media Recorder IOException: " + e.getLocalizedMessage());
            return false;
        }
        return true;

    }
}
