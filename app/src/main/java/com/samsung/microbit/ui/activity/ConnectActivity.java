package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.model.ConnectedDevice;
import com.samsung.microbit.ui.adapter.ConnectedDeviceAdapter;
import com.samsung.microbit.ui.adapter.LEDAdapter;

import java.util.ArrayList;
import java.util.List;


public class ConnectActivity extends Activity {

    List<ConnectedDevice> connectedDeviceList = new ArrayList<ConnectedDevice>();
    ConnectedDeviceAdapter connectedDeviceAdapter;
    private ListView lvConnectedDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MBApp.setContext(this);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_connect);

        LinearLayout mainContentView = (LinearLayout) findViewById(R.id.mainContentView);
        mainContentView.getBackground().setAlpha(128);

        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new LEDAdapter(this));

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Toast.makeText(MBApp.getContext(), "LED Clicked: " + position, Toast.LENGTH_SHORT).show();
                //TODO KEEP TRACK OF ALL LED STATUS AND TOGGLE COLOR
                v.setBackground(getApplication().getResources().getDrawable(R.drawable.red_white_led_btn));
                //v.setBackground(getApplication().getResources().getDrawable(R.drawable.white_red_led_btn));
            }
        });

        lvConnectedDevice = (ListView) findViewById(R.id.connectedDeviceList);

        populateConnectedDeviceList();
        connectedDeviceAdapter = new ConnectedDeviceAdapter(connectedDeviceList);
        lvConnectedDevice.setAdapter(connectedDeviceAdapter);
    }

    private void populateConnectedDeviceList() {
        connectedDeviceList.clear();
        connectedDeviceList.add(new ConnectedDevice("Kuhee MB 0", "VUVUVU", true));
        connectedDeviceList.add(new ConnectedDevice("Kuhee MB 1", "VUVUVU", false));
        connectedDeviceList.add(new ConnectedDevice(null, null, false));
    }
}
