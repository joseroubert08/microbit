package com.samsung.microbit.ui.activity;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.samsung.microbit.R;
import com.samsung.microbit.plugin.AudioPlugin;
import com.samsung.microbit.ui.PopUp;

/**
 * Created by frederic.ma on 14/06/2015.
 */
public class AudioRecorderActivity extends Activity {

    static final int NOTIFICATION_ID = 1;

    private TextView filenameTxt;
    private Chronometer chronometer;
    private ImageView imageMic;
    private Drawable drawable_mic_off;//TODO: make sure they are destroyed after use
    private Drawable drawable_mic_on;//TODO: make sure they are destroyed after use

    private boolean backPressed;
    private Bitmap notificationLargeIconBitmap;

    private void create()
    {
        setContentView(R.layout.activity_audio_recorder);

        filenameTxt = (TextView)findViewById(R.id.filenameTxt);
        chronometer = (Chronometer)findViewById(R.id.chronometer);
        imageMic = (ImageView)findViewById(R.id.imageMic);
        //preallocate to avoid memory leak
        drawable_mic_off = getResources().getDrawable(R.drawable.microphone_off);
        drawable_mic_on = getResources().getDrawable(R.drawable.microphone_on);

        backPressed = false;

        notificationLargeIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.microphone_on);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PopUp.show(this,
                "Accept Audio Recording?\nClick Yes to allow", //message
                "Privacy", //title
                R.drawable.microphone_on, //image icon res id (pass 0 to use default icon)
                0, //image icon background res id (pass 0 if there is no background)
                PopUp.TYPE_CHOICE, //type of popup.
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        PopUp.hide();
                        AudioRecorderActivity.this.create();
                    }
                },//override click listener for ok button
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        PopUp.hide();
                        AudioRecorderActivity.this.finish();
                    }
                });//pass null to use default listener
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //do not create notification if back pressed (or it is not recording?)
        if (!backPressed) {
            Intent resultIntent = new Intent(this, AudioRecorderActivity.class);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//bring existing activity to foreground

            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT //update existing notification instead of creating new one
                    );


            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setLargeIcon(notificationLargeIconBitmap)
                            .setTicker("Micro:bit Audio Recorder")//TODO: use string id
                            .setContentTitle("Micro:bit Audio Recorder");//TODO: use string id

            mBuilder.setContentIntent(resultPendingIntent);
            Notification notification = mBuilder.build();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;//the notification should disappear when it is clicked by the user.
            notification.flags |= Notification.FLAG_NO_CLEAR;//the notification should not be removed when the user clicks the Clear all button.

            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getAction() == null)
            return;

        if (intent.getAction().toString().equals(AudioPlugin.INTENT_ACTION_START_RECORD))
        {
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();

            String filename = intent.getStringExtra("filename");
            if (filename != null)
                filenameTxt.setText(filename);

            imageMic.setImageDrawable(drawable_mic_on);
        }
        else if (intent.getAction().toString().equals(AudioPlugin.INTENT_ACTION_STOP_RECORD))
        {   //TODO: delay this call if activity was in background?
            //otherwise the time is not updated
            chronometer.stop();

            imageMic.setImageDrawable(drawable_mic_off);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //make sure we remove existing notification if any when activity is destroyed
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(NOTIFICATION_ID);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        backPressed = true;
        //TODO: if recording is active, then stop recording. Also do not call super.onBackPressed();
        //do like existing samsung voice recorder app
    }
}
