package com.samsung.microbit.core;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class DownloadManager {

    volatile boolean cancelled = false;

    private static final String TAG = DownloadManager.class.getSimpleName();

    public DownloadManager() {
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public long download(String sourceUrl, String destinationFile) {

        long objectSize = 0L;
        try {
            URL url = new URL(sourceUrl);
            URLConnection urlConnection = url.openConnection();
            InputStream is = urlConnection.getInputStream();
            OutputStream os = new FileOutputStream(new File(destinationFile));
            objectSize = copyStream(is, os);
            os.close();
            is.close();
        } catch (IOException ex) {
            Log.e(TAG, ex.toString());
        }

        return objectSize;
    }

    long copyStream(InputStream src, OutputStream dest) throws IOException {
        long bytesProcessed = 0L;
        byte[] buffer = new byte[1024];
        int i;

        while (!cancelled) {
            i = src.read(buffer);
            if (i == -1) {
                break;
            }

            dest.write(buffer, 0, i);
            bytesProcessed += i;
        }

        return bytesProcessed;
    }
}
