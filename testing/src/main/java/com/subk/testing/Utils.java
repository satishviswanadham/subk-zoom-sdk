package com.subk.testing;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class Utils {
    public static String loadJSONFromAsset(Context context, String filename) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.e("loadJSONFromAsset", "Error reading JSON file: " + filename);
        }
        return json;
    }
    public static String getJsonFromAssets(Context context, int fileName) {
        String jsonString;
        try {
            InputStream is = context.getResources().openRawResource(fileName);

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            jsonString = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return jsonString;
    }
}