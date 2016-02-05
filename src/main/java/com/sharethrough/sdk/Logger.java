package com.sharethrough.sdk;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;



/**
 There are two ways to enable logging:
 1. set enabled "true" in this class
 2. create a file named sharethroughlog in /storage/sdcard0/Android/data/com.{APPID}/files/Download

 To disable:
 1. set enabled "false"
 */
public class Logger {

    public static boolean enabled = false;
    static private Context mContext;

    public static boolean isExternalStorageReadable(){
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static boolean isLoggingEnabled() {
        if (enabled) return true;

        if (mContext != null && isExternalStorageReadable()) {
            try {
                StringBuilder path = new StringBuilder(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString());
                path.append("/sharethroughlog");

                File file = new File(path.toString());
                if (file.exists()) {
                    enabled = true;
                }
            }
            catch (Exception e){
            }
        }

        return enabled;
    }

    public static void setContext(Context context) {
        if (context == null) {
            Log.w(createTag(), "Cannot initialize Logger with null context");
        }
        mContext = context.getApplicationContext();
    }

    private static String createTag() {
        final int stackLevel = 3;
        Throwable stack = new Throwable().fillInStackTrace();
        StackTraceElement[] trace = stack.getStackTrace();
        StringBuilder logStatement = new StringBuilder("");
        logStatement.append("STRLOG/");
        logStatement.append(trace[stackLevel].getClassName());
        logStatement.append(".");
        logStatement.append(trace[stackLevel].getMethodName());
        logStatement.append(".");
        logStatement.append(trace[stackLevel].getLineNumber());

        return logStatement.toString();
    }

    public static void d(String statement, Object... params) {
        if(false == isLoggingEnabled()) {
            return;
        }

        if(params.length != 0) {
            statement = String.format(statement, params);
        }
        Log.d(createTag(), statement);
    }

    public static void i(String statement, Object... params) {
        if(false == isLoggingEnabled()) {
            return;
        }

        if(params.length != 0) {
            statement = String.format(statement, params);
        }
        Log.i(createTag(), statement);
    }

    public static void w(String statement, Object... params) {
        if(false == isLoggingEnabled()) {
            return;
        }

        if(params.length != 0) {
            statement = String.format(statement, params);
        }
        Log.w(createTag(), statement);
    }

    public static void e(String statement, Exception e,  Object... params) {
        if(false == isLoggingEnabled()) {
            return;
        }
        if(params.length != 0) {
            statement = String.format(statement, params);
        }
        Log.e(createTag(), statement, e);
    }
}
