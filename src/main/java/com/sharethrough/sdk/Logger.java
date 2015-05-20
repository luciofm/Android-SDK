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
    public enum type {
        DEBUG,
        INFORMATION,
        WARN,
        ERROR
    }

    ;
    final private static String TAG = "SharethroughLogger";
    public static boolean enabled = false;
    static private Context mContext;

    private static boolean isLoggingEnabled() {
        if (enabled) return true;

        if (mContext != null) {
            StringBuilder path = new StringBuilder(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString());
            path.append("/sharethroughlog");

            File file = new File(path.toString());
            if (file.exists()) {
                enabled = true;
            }
        }

        return enabled;
    }

    public static void setContext(Context context) {
        if (context == null) {
            Log.w(TAG, "Cannot initialize Logger with null context");
        }
        mContext = context;
    }

    private static String createStatement(String statement) {
        final int stackLevel = 2;
        Throwable stack = new Throwable().fillInStackTrace();
        StackTraceElement[] trace = stack.getStackTrace();
        StringBuilder logStatement = new StringBuilder("");
        logStatement.append(trace[stackLevel].getClassName());
        logStatement.append(".");
        logStatement.append(trace[stackLevel].getMethodName());
        logStatement.append(".");
        logStatement.append(trace[stackLevel].getLineNumber());
        logStatement.append(" ");
        logStatement.append(statement);

        return logStatement.toString();
    }

    public static void d(String statement) {
        if (isLoggingEnabled()) {
            Log.d(TAG, createStatement(statement));
        }
    }

    public static void i(String statement) {
        if (isLoggingEnabled()) {
            Log.i(TAG, createStatement(statement));
        }
    }

    public static void w(String statement) {
        if (isLoggingEnabled()) {
            Log.w(TAG, createStatement(statement));
        }
    }

    public static void e(String statement) {
        if (isLoggingEnabled()) {
            Log.e(TAG, createStatement(statement));
        }
    }
}
