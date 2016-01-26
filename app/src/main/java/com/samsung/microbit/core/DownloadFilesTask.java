package com.samsung.microbit.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.model.Constants;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.activity.ProjectActivity;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class DownloadFilesTask extends AsyncTask<String, Integer, String> {

    private final Object locker = new Object();

    private int state = 0;
    String currentFileName = null;

    protected String doInBackground(String... urls) {

        int count = urls.length;
        long totalSize = 0;
        String newresult = null;
        DownloadManager downloadMgr = new DownloadManager();
        final Activity activity = (Activity) MBApp.getContext();
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
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                overWriteOrSaveAsDialog(activity);
                            }
                        });

                        synchronized (locker) {
                            try {
                                if (state == 0) {
                                    locker.wait();
                                    PopUp.hide();
                                }
                            } catch (InterruptedException e) {
                            }
                        }

                        if (state == 1) {
                            // Need the saveas dialog now.
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    saveAsDialog(activity);
                                }
                            });

                            synchronized (locker) {
                                try {
                                    if (state == 1) {
                                        locker.wait();
                                        PopUp.hide();
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
        PopUp.show(parent,
                "",
                parent.getResources().getString(R.string.q_overwrite_existing),
                R.drawable.overwrite_face, R.drawable.blue_btn,
                0, /* TODO - flashing*/
                PopUp.TYPE_CHOICE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        synchronized (locker) {
                            state = 10;
                            locker.notify();
                        }
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        synchronized (locker) {
                            state = 1;
                            locker.notify();
                        }
                    }
                });
    }

    private void saveAsDialog(Context parent) {

        PopUp.setInputText(currentFileName);
        PopUp.show(parent,
                "",
                parent.getResources().getString(R.string.rename_file),
                R.drawable.overwrite_face, R.drawable.blue_btn,
                0, /* TODO - doesn't need one */
                PopUp.TYPE_INPUTTEXT,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        synchronized (locker) {
                            state = 10;
                            currentFileName = PopUp.getInputText();
                            locker.notify();
                        }
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        synchronized (locker) {
                            locker.notify();
                        }
                    }
                });
    }

    protected void showDownloadProgress(final boolean show) {

        Activity v = (Activity) MBApp.getContext();
        v.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    PopUp.show(MBApp.getContext(), MBApp.getContext().getString(R.string.downloading),
                            MBApp.getContext().getString(R.string.downloading_file), R.drawable.flash_face, R.drawable.blue_btn,
                            1 /* TODO - flashing*/
                            , PopUp.TYPE_SPINNER, null, null);
                } else {
                    PopUp.hide();
                }
            }
        });
    }

    protected void onProgressUpdate(Integer... progress) {
    }

    protected void onPostExecute(final String result) {

        if (result == null) {
            PopUp.show(MBApp.getContext(),
                    MBApp.getContext().getString(R.string.download_failed_msg),
                    MBApp.getContext().getString(R.string.download_failed_title),
                    R.drawable.error_face, R.drawable.red_btn,
                    2, // TODO - check error
                    PopUp.TYPE_ALERT,
                    null, null);

        } else {
            PopUp.show(MBApp.getContext(),
                    MBApp.getContext().getString(R.string.download_complete),
                    "",
                    R.drawable.message_face, R.drawable.blue_btn,
                    0, // TODO - no image needed
                    PopUp.TYPE_CHOICE,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            PopUp.hide();
                            Intent intent = new Intent(MBApp.getContext(), ProjectActivity.class);
                            intent.putExtra("download_file", result);
                            MBApp.getContext().startActivity(intent);
                        }
                    }, null);
        }
    }
}