package com.sharethrough.sdk;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

public class ContextInfo {
    static private String appPackageName;
    static private String appVersionName;
    static private Point screenSize = new Point();
    static public boolean isChild = false;
    public ContextInfo(final Context context) {
        appPackageName = context.getPackageName();
        try {
            appVersionName = context.getPackageManager().getPackageInfo(appPackageName, PackageManager.GET_META_DATA).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            appVersionName = "unknown";
            e.printStackTrace();
        }
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getSize(screenSize);
    }

    public static String getAppPackageName() {
        return appPackageName;
    }

    public static String getAppVersionName() {
        return appVersionName;
    }

    public static Point getScreenSize() {
        return screenSize;
    }
}
