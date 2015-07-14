package com.samsung.microbit.core;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;

/**
 * Created by balbirs on 14/07/15.
 */
public class CommonGUI {

	public static void commonAlertDialog(Context parent, String title, String message) {

		AlertDialog alertDialog = new AlertDialog.Builder(parent, AlertDialog.THEME_HOLO_DARK).create();
		alertDialog.setTitle(title);

		alertDialog.setMessage(Html.fromHtml(message));
		alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}
		);

		alertDialog.show();
		System.out.println("Hello");
	}

}
