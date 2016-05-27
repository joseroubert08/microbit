package com.samsung.microbit.ui.control;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

public class ExtendedEditText extends EditText {

	protected String TAG = "ExtendedEditText";
	protected boolean debug = true;

	protected void logi(String message) {
		Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
	}

	public ExtendedEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

	}

	public ExtendedEditText(Context context, AttributeSet attrs) {
		super(context, attrs);

	}

	public ExtendedEditText(Context context) {
		super(context);

	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {

		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
			onEditorAction(-1);
			dispatchKeyEvent(event);

			return false;
		}

		return super.onKeyPreIme(keyCode, event);
	}

}