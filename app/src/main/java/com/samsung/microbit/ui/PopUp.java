package com.samsung.microbit.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.samsung.microbit.R;

/**
 * Created by frederic.ma on 23/06/2015.
 */
/*
How to use:

PopUp.show(context,
        "Accept Audio Recording?\nClick Yes to allow", //message
        "Privacy", //title
        R.drawable.microphone_on, //image icon res id
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
    static public final int TYPE_CHOICE = 0;//2 buttons type
    static public final int TYPE_ALERT = 1;//1 button type

    static private View.OnClickListener defaultListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dialog.dismiss();
        }
    };

    public static void hide()
    {
        if (dialog == null)
            return;

        dialog.dismiss();
    }

    //TODO: manage stack of popup?
    public static void show(Context context, String message, String title, int imageResId, int type,
                            View.OnClickListener okListener, View.OnClickListener cancelListener)
    {
        dialog = new Dialog(context, R.style.PopUpDialog);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popUpView = inflater.inflate(R.layout.dialog_popup, null);
        ImageView imageView = (ImageView)popUpView.findViewById(R.id.imageIcon);
        imageView.setImageResource(imageResId);
        TextView titleTextView = (TextView)popUpView.findViewById(R.id.titleTxt);
        titleTextView.setText(title);
        TextView messageTextView = (TextView)popUpView.findViewById(R.id.messageTxt);
        messageTextView.setText(message);

        ViewSwitcher switcher = (ViewSwitcher)popUpView.findViewById(R.id.switcher);

        switch (type) {
            case TYPE_CHOICE: {
                switcher.setDisplayedChild(0);
                ImageButton imageButtonOk = (ImageButton) popUpView.findViewById(R.id.imageButtonOk);
                if (okListener != null) {
                    imageButtonOk.setOnClickListener(okListener);
                } else {
                    imageButtonOk.setOnClickListener(defaultListener);
                }

                ImageButton imageButtonCancel = (ImageButton) popUpView.findViewById(R.id.imageButtonCancel);
                if (cancelListener != null) {
                    imageButtonCancel.setOnClickListener(cancelListener);
                } else {
                    imageButtonCancel.setOnClickListener(defaultListener);
                }
                break;
            }
            case TYPE_ALERT:
            {
                switcher.setDisplayedChild(1);
                ImageButton imageButton = (ImageButton) popUpView.findViewById(R.id.imageButton);
                if (okListener != null) {
                    imageButton.setOnClickListener(okListener);
                } else {
                    imageButton.setOnClickListener(defaultListener);
                }
                break;
            }
        }

        dialog.setContentView(popUpView);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);//needed by Service class
        dialog.getWindow().setLayout(WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.FILL_PARENT);
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(150, 0, 0, 0)));
    }
}
