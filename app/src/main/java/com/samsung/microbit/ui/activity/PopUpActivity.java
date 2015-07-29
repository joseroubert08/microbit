package com.samsung.microbit.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.samsung.microbit.R;

/**
 * Created by fma on 28/07/15.
 */
public class PopUpActivity extends Activity implements View.OnClickListener{

    static public final String INTENT_ACTION_OK_PRESSED = "PopUpActivity.OK_PRESSED";
    static public final String INTENT_ACTION_CANCEL_PRESSED = "PopUpActivity.CANCEL_PRESSED";

    private ImageButton okButton;
    private ImageButton cancelButton;
    private EditText inputText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_popup);

        okButton = (ImageButton) findViewById(R.id.imageButtonOk);
        cancelButton = (ImageButton) findViewById(R.id.imageButtonCancel);
        inputText = (EditText) findViewById(R.id.inputText);

        TextView titleTextView = (TextView)findViewById(R.id.titleTxt);
        titleTextView.setText(getIntent().getStringExtra("title"));
        TextView messageTextView = (TextView)findViewById(R.id.messageTxt);
        messageTextView.setText(getIntent().getStringExtra("message"));

        inputText.setText(getIntent().getStringExtra("inputText"));
        inputText.setSelection(inputText.getText().length());
    }

    @Override
    public void onClick(View v) {

        finish();
        Intent intent = new Intent();
        intent.putExtra("inputText", inputText.getText().toString());

        if (v == okButton) {
            intent.setAction(INTENT_ACTION_OK_PRESSED);
        }
        else if (v == cancelButton) {
            intent.setAction(INTENT_ACTION_CANCEL_PRESSED);
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
