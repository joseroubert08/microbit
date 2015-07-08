package com.samsung.microbit.core;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.model.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by kkulendiran on 26/06/15.
 */
public class DownloadManager {

    public DownloadManager() {
    }

    public long download(String sourceUrl, String filename) {

        String destinationFile = Constants.HEX_FILE_DIR.getAbsolutePath() + "/" +  filename;

        long objectSize = 0L;
        try {
            URL url = new URL(sourceUrl);
            URLConnection urlConnection = url.openConnection();
            InputStream is = urlConnection.getInputStream();
            OutputStream os = new FileOutputStream(new File(destinationFile));
            objectSize = copyStream(is, os);
            os.close();
            is.close();

        } catch (MalformedURLException ex) {
        } catch (IOException ex) {
        }

        return objectSize;
    }

    long copyStream(InputStream src, OutputStream dest) throws IOException {
        long bytesProcessed = 0L;
        byte[] buffer = new byte[1024];
        int i;

        while (true) {
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
