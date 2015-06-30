package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.IPCMessageManager;
import com.samsung.microbit.core.Utils;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;

import java.util.List;
import java.util.logging.Handler;

public class HomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        MBApp.setContext(this);

        setContentView(R.layout.activity_home);

        RelativeLayout connectBarView = (RelativeLayout) findViewById(R.id.connectBarView);
        connectBarView.getBackground().setAlpha(128);

        updateConnectBarView();

        RelativeLayout projectBarView = (RelativeLayout) findViewById(R.id.projectBarView);
        projectBarView.getBackground().setAlpha(128);

        updateProjectBarView();

        // Start the other services - local service to handle IPC in the main process
        Intent ipcIntent = new Intent(this, IPCService.class);
        startService(ipcIntent);

        Intent bleIntent = new Intent(this, BLEService.class);
        startService(bleIntent);

        Intent intent = new Intent(this, PluginService.class);
        startService(intent);
    }

    public void onBtnClicked(View v) {
        if (v.getId() == R.id.addDevice) {
            Intent intent = new Intent(this, ConnectActivity.class);
            startActivity(intent);
        } else if (v.getId() == R.id.startNewProject) {
            Intent intent = new Intent(this, WebViewActivity.class);
            intent.putExtra(Constants.URL, getString(R.string.touchDevURLNew));
            startActivity(intent);
        } else if (v.getId() == R.id.numOfProjects) {
            Intent intent = new Intent(this, ProjectActivity.class);
            startActivity(intent);
        } else if (v.getId() == R.id.connectBtn) {
            updateConnectBarView();
            ConnectedDevice connectedDevice = Utils.getPairedMicrobit(this);
            if (connectedDevice.mPattern != null) {
                if (connectedDevice.mStatus) {
                    IPCService.getInstance().bleDisconnect();
                } else {
                    IPCService.getInstance().bleConnect();
                }
            }
        }
    }

    private final void updateConnectBarView() {
        Button addDeviceButton = (Button) findViewById(R.id.addDevice);
        ConnectedDevice connectedDevice = Utils.getPairedMicrobit(this);

        if (connectedDevice.mPattern != null) {
            String styledText = "<big><font color='blue'>"
                    + (connectedDevice.mName != null ? connectedDevice.mName : "")
                    + "</font>"
                    + "<font color='blue'> (" + connectedDevice.mPattern + ")</font></big>";
            addDeviceButton.setText(Html.fromHtml(styledText));
        } else {
            addDeviceButton.setText("Connect to your Micro:Bit");
        }
        addDeviceButton.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);

        ImageButton connectButton = (ImageButton) findViewById(R.id.connectBtn);
        if (connectedDevice.mPattern != null && connectedDevice.mStatus) {
            connectButton.setImageResource(R.drawable.connected);
            connectButton.setBackgroundColor(0XFF00FF00);
        } else {
            connectButton.setImageResource(R.drawable.disconnected);
            connectButton.setBackgroundColor(0XFFFF0000);
        }
    }

    private final void updateProjectBarView() {
        Button numOfProjects = (Button) findViewById(R.id.numOfProjects);
        String styledText = "<big>"
                + Integer.toString(Utils.findProgramsAndPopulate(null, null))
                + " saved projects"
                + "</big>";
        numOfProjects.setText(Html.fromHtml(styledText));
        numOfProjects.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    }

    public void onResume() {
        super.onResume();

        updateConnectBarView();
        updateProjectBarView();
    }

    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    protected String TAG = "BLEBaseService";
    protected boolean debug = true;

    protected void logi(String message) {
        if (debug) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }
}
