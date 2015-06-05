package com.samsung.microbit.plugin;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.samsung.microbit.model.CmdArg;

import java.io.IOException;

/**
 * Created by frederic.ma on 13/05/2015.
 */
public class AudioPlugin
{
    private static Context context = null;

    //Audio plugin action
    public static final int STOP = 0;
    public static final int START = 1;

    private static MediaRecorder mRecorder = null;

    public static void pluginEntry(Context ctx, CmdArg cmd)
    {
        context = ctx;
        switch (cmd.getCMD())
        {
            case START:
            {
                startRecording();
                break;
            }
            case STOP: {
                stopRecording();
                break;
            }
        }
    }

    private static void startRecording()
    {
        if (mRecorder != null)
            return;

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile("/sdcard/Download/test.3gp");//TODO set right path and unique filename
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try
        {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e("AudioPlugin", "prepare() failed");
        }

        mRecorder.start();
    }

    private static void stopRecording()
    {
        if (mRecorder == null)
            return;

        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }
}
