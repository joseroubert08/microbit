package com.samsung.microbit.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {

    private static final int BUFFER_SIZE = 4096;

    private IOUtils() {
    }

    public static long copy(InputStream src, OutputStream dest) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        long countBytes = 0;

        int count;

        while ((count = src.read(buffer)) > 0) {
            dest.write(buffer, 0, count);
            countBytes += count;
        }

        return countBytes;
    }
}
