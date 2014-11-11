package com.sharethrough.sdk.webview;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;

public class YoutubeJavascriptInterface {
    private final Context context;
    private final Creative creative;
    private final BeaconService beaconService;
    private boolean quarterFired;

    public YoutubeJavascriptInterface(Context context, Creative creative, BeaconService beaconService) {
        this.context = context;
        this.creative = creative;
        this.beaconService = beaconService;
    }

    @JavascriptInterface
    public void timeUpdate(double time, double duration) {
        Log.v("Sharethrough", creative.getCreativeKey() + " video has played " + time + "s of " + duration + "s");
        if (!quarterFired && time * 4 > duration) {
            quarterFired = true;
            beaconService.videoPlayed(context, creative, 25);
        }
    }
}
