package com.samsung.microbit.utils;

import android.util.Log;

import com.samsung.microbit.model.Constants;
import com.samsung.microbit.model.Project;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class UnpackUtils {
    private UnpackUtils() {
    }

    public static int getTotalSavedPrograms() {
        File sdcardDownloads = Constants.HEX_FILE_DIR;
        int totalPrograms = 0;
        if (sdcardDownloads.exists()) {
            File files[] = sdcardDownloads.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (fileName.endsWith(".hex")) {
                        ++totalPrograms;
                    }
                }
            }
        }
        return totalPrograms;
    }

    public static int findProgramsAndPopulate(HashMap<String, String> prettyFileNameMap, List<Project> list) {
        File sdcardDownloads = Constants.HEX_FILE_DIR;
        Log.d("MicroBit", "Searching files in " + sdcardDownloads.getAbsolutePath());

        int totalPrograms = 0;
        if (sdcardDownloads.exists()) {
            File files[] = sdcardDownloads.listFiles();
            for (int i = 0; i < files.length; i++) {
                String fileName = files[i].getName();
                if (fileName.endsWith(".hex")) {

                    //Beautify the filename
                    String parsedFileName;
                    int dot = fileName.lastIndexOf(".");
                    parsedFileName = fileName.substring(0, dot);
                    parsedFileName = parsedFileName.replace('_', ' ');

                    if (prettyFileNameMap != null) {
                        prettyFileNameMap.put(parsedFileName, fileName);
                    }

                    if (list != null) {
                        list.add(new Project(parsedFileName, files[i].getAbsolutePath(), files[i].lastModified(),
                                null, false));
                    }

                    ++totalPrograms;
                }
            }
        }
        return totalPrograms;
    }
}
