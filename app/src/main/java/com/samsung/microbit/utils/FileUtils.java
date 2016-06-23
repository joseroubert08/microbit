package com.samsung.microbit.utils;

import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.FileConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {

    public enum RenameResult {
        //0
        SUCCESS,
        //1
        NEW_PATH_ALREADY_EXIST,
        //2
        OLD_PATH_NOT_CORRECT,
        //3
        RENAME_ERROR;
    }

    private FileUtils() {
    }

    public static boolean installSamples() {
        try {
            MBApp app = MBApp.getApp();

            Resources resources = app.getResources();
            final int internalResource = resources.getIdentifier(FileConstants.ZIP_INTERNAL_NAME, "raw", app.getPackageName());
            final String outputDir = Environment.getExternalStoragePublicDirectory(Environment
                    .DIRECTORY_DOWNLOADS).getAbsolutePath();
            Log.d("MicroBit", "Resource id: " + internalResource);
            //Unzip the file now
            ZipInputStream zin = new ZipInputStream(resources.openRawResource(internalResource));
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                Log.v("MicroBit", "Unzipping " + ze.getName());

                if (ze.isDirectory()) {
                    dirChecker(ze.getName());
                } else {
                    FileOutputStream fout = new FileOutputStream(outputDir + File.separator + ze.getName());
                    for (int c = zin.read(); c != -1; c = zin.read()) {
                        fout.write(c);
                    }
                    zin.closeEntry();
                    fout.close();
                }
            }
            zin.close();
        } catch (Resources.NotFoundException e) {
            Log.e("MicroBit", "No internal zipfile present", e);
            return false;
        } catch (Exception e) {
            Log.e("MicroBit", "unzip", e);
            return false;
        }
        return true;
    }

    private static void dirChecker(String dir) {
        File f = new File(Environment.DIRECTORY_DOWNLOADS + dir);

        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }

    public static RenameResult renameFile(String filePath, String newName) {
        File oldPathname = new File(filePath);
        newName = newName.replace(' ', '_');
        if (!newName.toLowerCase().endsWith(".hex")) {
            newName = newName + ".hex";
        }

        File newPathname = new File(oldPathname.getParentFile().getAbsolutePath(), newName);
        if (newPathname.exists()) {
            return RenameResult.NEW_PATH_ALREADY_EXIST;
        }

        if (!oldPathname.exists() || !oldPathname.isFile()) {
            return RenameResult.OLD_PATH_NOT_CORRECT;
        }

        if (oldPathname.renameTo(newPathname)) {
            return RenameResult.SUCCESS;
        } else {
            return RenameResult.RENAME_ERROR;
        }
    }

    public static boolean deleteFile(String filePath) {
        File fileForDelete = new File(filePath);
        if (fileForDelete.exists()) {
            if (fileForDelete.delete()) {
                Log.d("MicroBit", "file Deleted :" + filePath);
                return true;
            } else {
                Log.d("MicroBit", "file not Deleted :" + filePath);
            }
        }

        return false;
    }

    public static String getFileSize(String filePath) {
        String size = "0";
        File file = new File(filePath);
        if (file.exists()) {
            size = Long.toString(file.length());
        }
        return size;
    }
}
