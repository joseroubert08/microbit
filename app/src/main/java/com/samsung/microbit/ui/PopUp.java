package com.samsung.microbit.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import com.samsung.microbit.ui.activity.PopUpActivity;
import com.samsung.microbit.ui.activity.PopUpHolderActivity;

/*
How to use:

To show a popup from an application/activity context, call the below.
To show a popup from a Plugin class or service context, use showFromService function.

PopUp.show(context,
        "Accept Audio Recording?\nClick Yes to allow", //message
        "Privacy", //title
        R.drawable.recording, //image icon res id (pass 0 to use default icon)
		0, //image icon background res id (pass 0 if there is no background)
        PopUp.TYPE_CHOICE, //type of popup.
        new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
                //Write your own code
            }
        },//override click listener for ok button
        null);//pass null to use default listener
 */
public class PopUp {

    static public final int TYPE_CHOICE = 0;//2 buttons type
    static public final int TYPE_ALERT = 1;//1 button type
    static public final int TYPE_PROGRESS = 2;//0 button progress bar type
	static public final int TYPE_NOBUTTON = 3;//No button type
    static public final int TYPE_SPINNER = 4;//0 button type spinner
    static public final int TYPE_INPUTTEXT = 5;//2 buttons type + edittext
    static public final int TYPE_PROGRESS_NOT_CANCELABLE = 6;//0 button progress bar type not cancelable (backpress disabled)
    static public final int TYPE_SPINNER_NOT_CANCELABLE = 7;//0 button type spinner not cancelable (backpress disabled)
    static public final int TYPE_MAX = 8;

    static private int current_type = TYPE_MAX;

    static private Context ctx = null;

    static private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PopUpActivity.INTENT_ACTION_OK_PRESSED)) {
                current_type = TYPE_MAX;
                inputText = intent.getStringExtra(PopUpActivity.INTENT_EXTRA_INPUTTEXT);
                if (okPressListener != null)
                    okPressListener.onClick(null);
            } else if (intent.getAction().equals(PopUpActivity.INTENT_ACTION_CANCEL_PRESSED)) {
                current_type = TYPE_MAX;
                inputText = intent.getStringExtra(PopUpActivity.INTENT_EXTRA_INPUTTEXT);
                if (cancelPressListener != null)
                    cancelPressListener.onClick(null);
            }
        }
    };

    static private boolean registered = false;
    static private View.OnClickListener okPressListener = null;
    static private View.OnClickListener cancelPressListener = null;
    static private String inputText = "";

    public static boolean isBlockingPopUpDisplayed()
    {
        return current_type == TYPE_CHOICE || current_type == TYPE_PROGRESS || current_type == TYPE_INPUTTEXT;
    }

    public static void hide()
    {
        if (current_type == TYPE_MAX) {
            Log.e("Popup","Nothing to hide");
            return;
        }

        LocalBroadcastManager.getInstance(ctx).sendBroadcastSync(new Intent(PopUpActivity.INTENT_ACTION_CLOSE));
        current_type = TYPE_MAX;// reset current type to none
    }

    public static void updateProgressBar(int val) {
        Intent intent = new Intent(PopUpActivity.INTENT_ACTION_UPDATE_PROGRESS);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_PROGRESS, val);
        LocalBroadcastManager.getInstance(ctx).sendBroadcastSync(intent);
    }

    public static String getInputText() {
        return inputText;
    }

    public static void setInputText(String text) {
        inputText = text;
    }

    //Interface function for showing a popup inside a service plugin class
    //only supports TYPE_ALERT popup for now.
    public static void showFromService(Context context, String message, String title,
                                       int imageResId, int imageBackgroundResId, int type) {
        Intent intent = new Intent(context, PopUpHolderActivity.class);
        intent.putExtra(PopUpHolderActivity.INTENT_EXTRA_POPUP_TYPE, type);
        intent.putExtra(PopUpHolderActivity.INTENT_EXTRA_POPUP_TITLE, title);
        intent.putExtra(PopUpHolderActivity.INTENT_EXTRA_POPUP_MESSAGE, message);
        intent.putExtra(PopUpHolderActivity.INTENT_EXTRA_POPUP_ICON, imageResId);
        intent.putExtra(PopUpHolderActivity.INTENT_EXTRA_POPUP_ICONBG, imageBackgroundResId);
        // flag required in background mode
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    //Interface function for showing popup inside an application activity
    //pass 0 to imageResId to use default icon
    //pass 0 to imageBackgroundResId if no background needed for icon
    public static boolean show(Context context, String message, String title,
                            int imageResId, int imageBackgroundResId, int type,
                            View.OnClickListener okListener, View.OnClickListener cancelListener)
    {
        if (!(context instanceof Activity)) {
            Log.e("PopUp","Cannot show popup because context is not an activity. PopUp.show must be called from an activity");
            return false;
        }
        ctx = context;
        //if blocking popup is already displayed, do not show another popup
        if (isBlockingPopUpDisplayed()) {
            Log.e("PopUp","Cannot show popup because blocking pop-up is displayed");
            return false;
        }

        PopUp.hide();

        current_type = type;

        okPressListener = okListener;
        cancelPressListener = cancelListener;

        if (registered == false) {

            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, new IntentFilter(PopUpActivity.INTENT_ACTION_OK_PRESSED));
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, new IntentFilter(PopUpActivity.INTENT_ACTION_CANCEL_PRESSED));

            registered = true;
        }

        Intent intent = new Intent(context, PopUpActivity.class);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_TYPE, type);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_TITLE, title);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_MESSAGE, message);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_INPUTTEXT, inputText);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_ICON, imageResId);
        intent.putExtra(PopUpActivity.INTENT_EXTRA_ICONBG, imageBackgroundResId);
        switch (type)
        {
            case TYPE_PROGRESS_NOT_CANCELABLE:
            case TYPE_SPINNER_NOT_CANCELABLE:
                intent.putExtra(PopUpActivity.INTENT_EXTRA_CANCELABLE, false);
                break;
            default:
                intent.putExtra(PopUpActivity.INTENT_EXTRA_CANCELABLE, true);
        }

        // keep same instance of activity
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        return true;
    }
}
