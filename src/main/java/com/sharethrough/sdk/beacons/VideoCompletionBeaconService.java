package com.sharethrough.sdk.beacons;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;

public class VideoCompletionBeaconService {
    private final Context context;
    private final Creative creative;
    private final BeaconService beaconService;
    private boolean quarterFired;
    private boolean halfFired;
    private boolean threeQuartersFired;
    private boolean finishedFired;

    public VideoCompletionBeaconService(Context context, Creative creative, BeaconService beaconService) {
        this.context = context;
        this.creative = creative;
        this.beaconService = beaconService;
    }

    @JavascriptInterface
    public void timeUpdate(double time, double duration) {
        Log.v("Sharethrough", creative.getCreativeKey() + " video has played " + time + "s of " + duration + "s");
        double percent = time / duration;
        if (!quarterFired && percent >= 0.25) {
            quarterFired = true;
            beaconService.videoPlayed(context, creative, 25);
        }
        if (!halfFired && percent >= 0.5) {
            halfFired = true;
            beaconService.videoPlayed(context, creative, 50);
        }
        if (!threeQuartersFired && percent >= 0.75) {
            threeQuartersFired = true;
            beaconService.videoPlayed(context, creative, 75);
        }
        if (!finishedFired && percent >= 0.95) {
            finishedFired = true;
            beaconService.videoPlayed(context, creative, 95);
        }
    }
}
