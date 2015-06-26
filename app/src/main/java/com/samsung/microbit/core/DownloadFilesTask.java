package com.samsung.microbit.core;

import android.os.AsyncTask;
import android.widget.Toast;

import com.samsung.microbit.MBApp;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by kkulendiran on 26/06/15.
 */
public class DownloadFilesTask extends AsyncTask<String, Integer, Long> {
    protected Long doInBackground(String... urls) {
        int count = urls.length;
        long totalSize = 0;
        for (int i = 0; i < count; i++) {

            URI uri = null;
            try {
                uri = new URI(urls[i]);

                String path = uri.getPath();
                String fileName = path.substring(path.lastIndexOf('/') + 1);

                DownloadManager downloadMgr = new DownloadManager();
                totalSize += downloadMgr.download(urls[i], fileName);
                publishProgress((int) ((i / (float) count) * 100));

            } catch (URISyntaxException e) {

            }
            // Escape early if cancel() is called
            if (isCancelled()) break;
        }
        return totalSize;
    }

    protected void onProgressUpdate(Integer... progress) {

    }

    protected void onPostExecute(Long result) {
        Toast.makeText(MBApp.getContext(), "Downloaded", Toast.LENGTH_SHORT).show();
    }
}