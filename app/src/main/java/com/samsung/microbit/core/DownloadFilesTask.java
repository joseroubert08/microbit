package com.samsung.microbit.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.Html;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.activity.ProjectActivity;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by kkulendiran on 26/06/15.
 */
public class DownloadFilesTask extends AsyncTask<String, Integer, String> {

	private final Object locker = new Object();

	private int state = 0;
	String currentFileName = null;

	protected String doInBackground(String... urls) {

		int count = urls.length;
		long totalSize = 0;
		String newresult = null;
		DownloadManager downloadMgr = new DownloadManager();
		String destDir = Constants.HEX_FILE_DIR.getAbsolutePath();
		for (int i = 0; i < count; i++) {

			URI uri = null;
			currentFileName = null;
			try {
				uri = new URI(urls[i]);

				String path = uri.getPath();
				currentFileName = path.substring(path.lastIndexOf('/') + 1);
				String destinationFile = null;

				do {
					destinationFile = destDir + "/" + currentFileName;
					File f = new File(destinationFile);
					state = 0;
					if (f.exists()) {
						// file with that name already exists.  need to ask user for overwrite or saveas
						((Activity) MBApp.getContext()).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								overWriteOrSaveAsDialog(MBApp.getContext());
							}
						});

						synchronized (locker) {
							try {
								if (state == 0) {
									locker.wait();
								}
							} catch (InterruptedException e) {
							}
						}

						if (state == 1) {
							// Need the saveas dialog now.
							((Activity) MBApp.getContext()).runOnUiThread(new Runnable() {
								@Override
								public void run() {
									saveAsDialog(MBApp.getContext());
								}
							});

							synchronized (locker) {
								try {
									if (state == 1) {
										locker.wait();
									}
								} catch (InterruptedException e) {
								}
							}

							destinationFile = null;
						}
					}
				} while (destinationFile == null);


				showDownloadProgress(true);
				long fileSize = downloadMgr.download(urls[i], destinationFile);
				showDownloadProgress(false);
				newresult = new String(currentFileName);
				//if (fileSize >= 0) {
				//	totalSize += fileSize;
				//	publishProgress((int) ((i / (float) count) * 100));
				//}

			} catch (URISyntaxException e) {

			}

			// Escape early if cancel() is called
			if (isCancelled()) {
				break;
			}
		}

		return newresult;
	}


	private void overWriteOrSaveAsDialog(Context parent) {

		AlertDialog alertDialog = new AlertDialog.Builder(parent, AlertDialog.THEME_HOLO_DARK).create();
		alertDialog.setTitle("File Exists");

		alertDialog.setMessage(Html.fromHtml("A file with same name already exists. What do you want to do?"));
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Overwrite existing file",
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					synchronized (locker) {
						state = 10;
						locker.notify();
					}

					dialog.dismiss();
				}
			}
		);

		alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Change name of new File",
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					synchronized (locker) {
						state = 1;
						locker.notify();
					}

					dialog.dismiss();
				}
			}
		);

		alertDialog.show();
	}

	private void saveAsDialog(Context parent) {
		final EditText input = new EditText(parent);
		input.setText(currentFileName);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		input.setTextColor(parent.getResources().getColor(R.color.white_font_color));
		input.setSelection(currentFileName.length());

		AlertDialog alertDialog = new AlertDialog.Builder(parent, AlertDialog.THEME_HOLO_DARK).create();
		alertDialog.setTitle("Rename File");

		alertDialog.setMessage(Html.fromHtml("Rename file " + currentFileName + " to ?"));
		alertDialog.setView(input);
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok",
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					synchronized (locker) {
						state = 10;
						currentFileName = input.getText().toString();
						locker.notify();
					}

					dialog.dismiss();
				}
			}
		);

		alertDialog.show();
	}

	protected void showDownloadProgress(final boolean show) {

		Activity v = (Activity) MBApp.getContext();
		v.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (show) {
					PopUp.show(MBApp.getContext(), MBApp.getContext().getString(R.string.downloading),
						MBApp.getContext().getString(R.string.downloading_file), 0, 0, PopUp.TYPE_SPINNER, null, null);
				} else {
					PopUp.hide();
				}
			}
		});
	}

	protected void onProgressUpdate(Integer... progress) {
	}

	protected void onPostExecute(final String result) {
		PopUp.show(MBApp.getContext(),
			MBApp.getContext().getString(R.string.download_complete),
			"",
			0, 0,
			PopUp.TYPE_CHOICE,
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					PopUp.hide();
					Intent intent = new Intent(MBApp.getContext(), ProjectActivity.class);
					intent.putExtra("download_file", result);
					//Toast.makeText(MBApp.getContext(), "File result "+result,Toast.LENGTH_SHORT).show();
					MBApp.getContext().startActivity(intent);
					//Write your own code
				}
			}, null);


	}
}