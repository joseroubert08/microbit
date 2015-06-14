package com.samsung.microbit.not_used;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.microbit.R;
import com.samsung.microbit.model.CmdArg;
import com.samsung.microbit.plugin.CameraPlugin;
import com.samsung.microbit.service.PluginService;

/**
 * Created by t.maestri on 19/05/2015.
 */
public class CameraActivity extends Activity{

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_parent);
        Intent intent = getIntent();
        Log.d("Microbit_camera","CameraActivity onCreate()");
        if(intent.getAction().contains("OPEN_FOR_PIC")){
            //Creating the view for taking the picture
            if (null == savedInstanceState) {
                Log.d("Microbit_camera","CameraActivity CameraPictureActivityFragment.newInstance()");
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, CameraPictureActivityFragment.newInstance())
                        .commit();
            }
        }
        else if(intent.getAction().contains("OPEN_FOR_VIDEO")) {
            //Creating the view for recording a video
            if (null == savedInstanceState) {
                Log.d("Microbit_camera","CameraActivity CameraVideoActivityFragment.newInstance()");
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, CameraVideoActivityFragment.newInstance())
                        .commit();
            }
        }
        else {
            //TODO: remove this once the code is stable
            Log.d("Microbit_camera","CameraActivity onCreate() finish()");
            finish();
        }
    }

    @Override
    protected void onStart() {
        //Informing microbit that the camera is active now
        CmdArg cmd = new CmdArg(0,"Camera on");
        CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
        Log.d("Microbit_camera", "Sent message CameraOn to microbit");
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        //Informing microbit that the camera is active now
        CmdArg cmd = new CmdArg(0,"Camera off");
        CameraPlugin.sendReplyCommand(PluginService.CAMERA, cmd);
        super.onDestroy();
    }
}
