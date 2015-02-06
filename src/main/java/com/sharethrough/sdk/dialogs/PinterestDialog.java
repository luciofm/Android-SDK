package com.sharethrough.sdk.dialogs;

import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;


public class PinterestDialog extends WebViewDialog {
    public PinterestDialog(Context context, Creative creative, BeaconService beaconService, int feedPosition) {
        super(context, creative, beaconService, feedPosition);

        CookieSyncManager.createInstance(context);
        CookieManager.getInstance().setCookie(creative.getMediaUrl(), "stay_in_browser=1");
        CookieSyncManager.getInstance().sync();
    }
}
