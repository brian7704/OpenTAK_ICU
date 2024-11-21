package io.opentakserver.opentakicu;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SystemProperties {

    private static final String GETPROP_EXECUTABLE_PATH = "/system/bin/getprop";
    private static final String TAG = "SystemProperties";

    public static String getString(String propName, String defaultValue) {
        Process process = null;
        BufferedReader bufferedReader = null;

        try {
            process = new ProcessBuilder().command(GETPROP_EXECUTABLE_PATH, propName).redirectErrorStream(true).start();
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = bufferedReader.readLine();
            if (line == null){
                line = defaultValue; //prop not set
            }
            Log.i(TAG,"read System Property: " + propName + "=" + line);
            return line;
        } catch (Exception e) {
            Log.e(TAG,"Failed to read System Property " + propName,e);
            return defaultValue;
        } finally{
            if (bufferedReader != null){
                try {
                    bufferedReader.close();
                } catch (IOException e) {}
            }
            if (process != null){
                process.destroy();
            }
        }
    }

    public static int getInt(String propName, int defaultValue) {
        String value = getString(propName, defaultValue + "");

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}