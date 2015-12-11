package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import com.samsung.microbit.R;
import com.samsung.microbit.ui.PopUp;

/*
   This class purpose is to show popup from within an activity context instead of a service context.
   This is needed because the internal state of the PopUp class needs to be maintained within
   the same process which is by reference the application process.
   The background service runs in a different process and therefore the state of the PopUp class
   cannot be shared across processes.
   This class is used internally by the PopUp class interface.
   DO NOT call this activity directly, use the PopUp.showFromService function from a service context.
 */
public class PopUpHolderActivity extends Activity{

    static public final String INTENT_EXTRA_POPUP_TYPE = "type";
    static public final String INTENT_EXTRA_POPUP_MESSAGE = "message";
    static public final String INTENT_EXTRA_POPUP_TITLE = "title";
    static public final String INTENT_EXTRA_POPUP_ICON = "icon";
    static public final String INTENT_EXTRA_POPUP_ICONBG = "iconbg";

    private View.OnClickListener defaultListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PopUpActivity.INTENT_ACTION_CLOSE)) {
                finish();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(PopUpActivity.INTENT_ACTION_CLOSE));

        if (!PopUp.show(this,
                getIntent().getStringExtra(INTENT_EXTRA_POPUP_MESSAGE),
                getIntent().getStringExtra(INTENT_EXTRA_POPUP_TITLE),
                getIntent().getIntExtra(INTENT_EXTRA_POPUP_ICON, 0),
                getIntent().getIntExtra(INTENT_EXTRA_POPUP_ICONBG, 0),
                getIntent().getIntExtra(INTENT_EXTRA_POPUP_TYPE, PopUp.TYPE_MAX),
                defaultListener, defaultListener)) {//if callback are needed later for specific operations, they will have to be defined in this class
            finish();//if popup could not be shown exit activity
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }
}
