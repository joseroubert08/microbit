package com.samsung.microbit.data.constants;

import android.os.Environment;

import java.io.File;

public class FileConstants {
    private FileConstants() {
    }

    public static final File MEDIA_OUTPUT_FOLDER = Environment.getExternalStoragePublicDirectory(Environment
            .DIRECTORY_DCIM + "/bbc-microbit");

    public static final String ZIP_INTERNAL_NAME = "raw/samples";
}
