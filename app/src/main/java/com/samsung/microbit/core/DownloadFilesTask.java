package com.samsung.microbit.core;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.ui.PopUp;
import com.samsung.microbit.ui.activity.ProjectActivity;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by kkulendiran on 26/06/15.
 */
public class DownloadFilesTask extends AsyncTask<String, Integer, String> {
    protected String doInBackground(String... urls) {
        int count = urls.length;
        long totalSize = 0;
        String newresult=null;
        for (int i = 0; i < count; i++) {

            URI uri = null;
            try {
                uri = new URI(urls[i]);

                String path = uri.getPath();
                String fileName = path.substring(path.lastIndexOf('/') + 1);

                DownloadManager downloadMgr = new DownloadManager();
                totalSize += downloadMgr.download(urls[i], fileName);
                publishProgress((int) ((i / (float) count) * 100));
                newresult = new String(fileName);

            } catch (URISyntaxException e) {

            }
            // Escape early if cancel() is called
            if (isCancelled()) break;
        }
        return newresult;
    }

    protected void onProgressUpdate(Integer... progress) {

    }

    protected void onPostExecute(final String result) {
        PopUp.show(MBApp.getContext(),
                MBApp.getContext().getString(R.string.download_complete),
                "",
                0, 0,
                PopUp.TYPE_CHOICE,
                new View.OnClickListener(){
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