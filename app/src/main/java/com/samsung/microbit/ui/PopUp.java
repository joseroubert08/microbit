package com.samsung.microbit.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.samsung.microbit.R;
import com.samsung.microbit.ui.activity.PopUpActivity;

/**
 * Created by frederic.ma on 23/06/2015.
 */
/*
How to use:

Note that the "context" has to be activity or application based.
Passing service "context" will not work anymore because TYPE_SYSTEM_ALERT has to be disabled
in order to fix the overlap bug with the system power dialog.
To show the popup from service, you must call the popup from an activity

PopUp.show(context,
        "Accept Audio Recording?\nClick Yes to allow", //message
        "Privacy", //title
        R.drawable.microphone_on, //image icon res id (pass 0 to use default icon)
		0, //image icon background res id (pass 0 if there is no background)
        PopUp.TYPE_CHOICE, //type of popup.
        new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                PopUp.hide();
                Toast.makeText(HomeActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
                //Write your own code
            }
        },//override click listener for ok button
        null);//pass null to use default listener
 */
public class PopUp {

    static private Dialog dialog = null;
    static private ProgressBar progressBar = null;
    static public final int TYPE_CHOICE = 0;//2 buttons type
    static public final int TYPE_ALERT = 1;//1 button type
    static public final int TYPE_PROGRESS = 2;//1 button progress bar type
	static public final int TYPE_NOBUTTON = 3;//No button type
    static public final int TYPE_SPINNER = 4;//button type spinner
    static public final int TYPE_INPUTTEXT = 5;//2 buttons type + edittext
    static public final int TYPE_MAX = 6;

    static public int current_type = TYPE_MAX;

    static private BroadcastReceiver broadcastReceiver = null;
    static private View.OnClickListener okPressListener = null;
    static private View.OnClickListener cancelPressListener = null;
    static private String inputText = "";

    static private View.OnClickListener defaultListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PopUp.hide();
        }
    };

    static private View.OnClickListener backPressListener = null;

    static private Dialog.OnKeyListener keyListener = new Dialog.OnKeyListener() {
        @Override
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                backPressListener.onClick(null);
                return true;
            }
            return false;
        }
    };

    public static boolean isBlockingPopUpDisplayed()
    {
        return current_type == TYPE_CHOICE;
    }

    public static void hide()
    {
        if (dialog == null)
            return;

        dialog.dismiss();
        dialog = null;
        current_type = TYPE_MAX; // reset current type to none
    }

    public static void updateProgressBar(int val) {
        if(progressBar != null)
            progressBar.setProgress(val);
    }

    public static String getInputText() {
        return inputText;
    }

    public static void setInputText(String text) {
        inputText = text;
    }

    private static void showTextInputPopup(Context context, String message, String title,
                                           int imageResId, int imageBackgroundResId, int type,
                                           View.OnClickListener okListener, View.OnClickListener cancelListener) {
        okPressListener = okListener;
        cancelPressListener = cancelListener;

        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(PopUpActivity.INTENT_ACTION_OK_PRESSED)) {
                        inputText = intent.getStringExtra("inputText");
                        if (okPressListener != null)
                            okPressListener.onClick(null);
                    } else if (intent.getAction().equals(PopUpActivity.INTENT_ACTION_CANCEL_PRESSED)) {
                        inputText = intent.getStringExtra("inputText");
                        if (cancelPressListener != null)
                            cancelPressListener.onClick(null);
                    }
                }
            };
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, new IntentFilter(PopUpActivity.INTENT_ACTION_OK_PRESSED));
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, new IntentFilter(PopUpActivity.INTENT_ACTION_CANCEL_PRESSED));
        }

        Intent intent = new Intent(context, PopUpActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("inputText", inputText);
        context.startActivity(intent);
    }

    //pass 0 to imageResId to use default icon
    //pass 0 to imageBackgroundResId if no background needed for icon
    public static void show(Context context, String message, String title,
                            int imageResId, int imageBackgroundResId, int type,
                            View.OnClickListener okListener, View.OnClickListener cancelListener)
    {
        //if blocking popup is already displayed, do not show another popup
        if (current_type == TYPE_CHOICE || current_type == TYPE_PROGRESS || current_type == TYPE_INPUTTEXT)
            return;

        PopUp.hide();

        //Dialog pan/resize does not work properly when soft keyboard comes up
        //so use a separate activity for now to handle text input.
        if (type == TYPE_INPUTTEXT) {
            showTextInputPopup(context, message, title, imageResId, imageBackgroundResId, type,
                                okListener, cancelListener);
            return;
        }

        current_type = type;

        dialog = new Dialog(context, R.style.PopUpDialog);
        dialog.setOnKeyListener(keyListener);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popUpView = inflater.inflate(R.layout.dialog_popup, null);
        ImageView imageView = (ImageView)popUpView.findViewById(R.id.imageIcon);
        if (imageResId != 0) {
            imageView.setImageResource(imageResId);
        }
        if (imageBackgroundResId != 0) {
            imageView.setBackgroundResource(imageBackgroundResId);
        }
        TextView titleTextView = (TextView)popUpView.findViewById(R.id.titleTxt);
        titleTextView.setText(title);
        TextView messageTextView = (TextView)popUpView.findViewById(R.id.messageTxt);
        messageTextView.setText(message);

        if (title.isEmpty())
            titleTextView.setVisibility(View.GONE);

        ViewSwitcher switcher = (ViewSwitcher)popUpView.findViewById(R.id.switcher);

        progressBar = (ProgressBar) popUpView.findViewById(R.id.progressBar);
        ProgressBar spinnerBar = (ProgressBar) popUpView.findViewById(R.id.spinnerBar);
        spinnerBar.setVisibility(View.GONE);
        backPressListener = defaultListener;

        switch (type) {
            case TYPE_CHOICE: {
                switcher.setDisplayedChild(0);
                progressBar.setVisibility(View.GONE);
                ImageButton imageButtonOk = (ImageButton) popUpView.findViewById(R.id.imageButtonOk);
                if (okListener != null) {
                    imageButtonOk.setOnClickListener(okListener);
                } else {
                    imageButtonOk.setOnClickListener(defaultListener);
                }

                ImageButton imageButtonCancel = (ImageButton) popUpView.findViewById(R.id.imageButtonCancel);
                if (cancelListener != null) {
                    imageButtonCancel.setOnClickListener(cancelListener);
                    backPressListener = cancelListener;
                } else {
                    imageButtonCancel.setOnClickListener(defaultListener);
                }
                break;
            }
            case TYPE_ALERT:
            {
                switcher.setDisplayedChild(1);
                progressBar.setVisibility(View.GONE);
                ImageButton imageButton = (ImageButton) popUpView.findViewById(R.id.imageButton);
                if (okListener != null) {
                    imageButton.setOnClickListener(okListener);
                } else {
                    imageButton.setOnClickListener(defaultListener);
                }
                break;
            }
            case TYPE_PROGRESS:
            {
                messageTextView.setVisibility(View.GONE);
                switcher.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                break;
            }
            case TYPE_NOBUTTON:
            {
                switcher.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                break;
            }
            case TYPE_SPINNER:
            {
                messageTextView.setVisibility(View.VISIBLE);
                switcher.setVisibility(View.INVISIBLE);
                spinnerBar.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                break;
            }
        }

        dialog.setContentView(popUpView);
        //Do not use TYPE_SYSTEM_ALERT because it hides system power dialog
        //Side effect of not using TYPE_SYSTEM_ALERT is that a Popup can only be shown within an activity
        //TYPE_SYSTEM_ALERT allows a service to show a dialog without an activity.
        //dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.FILL_PARENT);
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(150, 0, 0, 0)));
    }
}
