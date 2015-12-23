package com.example.arty.bluetalkie.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by sergey on 22/12/15.
 */
public class ImageUtils {
    public static boolean saveImage(Context context, String packageName, String filename, Bitmap bitmap) {
        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + packageName;
        try {
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            OutputStream fOut = null;
            File file = new File(fullPath, filename);
            file.createNewFile();
            fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
            return true;
        } catch (Exception e) {
            Log.e("saveToExternalStorage()", e.getMessage());
            return false;
        }
    }

    public static Bitmap loadImage(String packageName, String filename) {
        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + packageName + "/" + filename;
        return BitmapFactory.decodeFile(fullPath);
    }
}
