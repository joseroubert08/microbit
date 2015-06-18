package com.samsung.microbit.ui.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.samsung.microbit.R;
import com.samsung.microbit.plugin.AudioPlugin;

/**
 * Created by frederic.ma on 14/06/2015.
 */
public class AudioRecorderActivity extends Activity {


    private TextView filenameTxt;
    private Chronometer chronometer;
    private ImageView imageMic;
    private Drawable drawable_mic_off;//TODO: make sure they are destroyed after use
    private Drawable drawable_mic_on;//TODO: make sure they are destroyed after use

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_audio_recorder);

        filenameTxt = (TextView)findViewById(R.id.filenameTxt);
        chronometer = (Chronometer)findViewById(R.id.chronometer);
        imageMic = (ImageView)findViewById(R.id.imageMic);
        //preallocate to avoid memory leak
        drawable_mic_off = getResources().getDrawable(R.drawable.microphone_off);
        drawable_mic_on = getResources().getDrawable(R.drawable.microphone_on);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

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
    }
}
