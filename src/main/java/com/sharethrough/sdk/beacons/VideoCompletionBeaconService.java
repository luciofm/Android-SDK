package com.sharethrough.sdk.beacons;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.Logger;

/**
 * Javascript interface that notifies video completion rate
 */
public class VideoCompletionBeaconService {
    private final Context context;
    private final Creative creative;
    private final BeaconService beaconService;
    private final int feedPosition;
    private boolean quarterFired;
    private boolean halfFired;
    private boolean threeQuartersFired;
    private boolean finishedFired;

    public VideoCompletionBeaconService(Context context, Creative creative, BeaconService beaconService, int feedPosition) {
        this.context = context;
        this.creative = creative;
        this.beaconService = beaconService;
        this.feedPosition = feedPosition;
    }

    @JavascriptInterface
    // NOTE: params must match function signature of SharethroughYoutube.timeUpdate in youtube_html string
    // DEBUGGING INSTRUCTIONS: https://developer.chrome.com/devtools/docs/remote-debugging#debugging-webviews
    public void timeUpdate(double time, double duration) {
        timeUpdate(time, duration, false);
    }

    public void timeUpdate(double time, double duration, boolean isSilent) {
        Logger.i("%s video has played %f s of %f s",creative.getCreativeKey(), time, duration);

        double percent = time / duration;
        if (!quarterFired && percent >= 0.25) {
            quarterFired = true;
            beaconService.videoPlayed(context, creative, 25, isSilent, feedPosition);
        }
        if (!halfFired && percent >= 0.5) {
            halfFired = true;
            beaconService.videoPlayed(context, creative, 50, isSilent, feedPosition);
        }
        if (!threeQuartersFired && percent >= 0.75) {
            threeQuartersFired = true;
            beaconService.videoPlayed(context, creative, 75, isSilent, feedPosition);
        }
        if (!finishedFired && percent >= 0.95) {
            finishedFired = true;
            beaconService.videoPlayed(context, creative, 95, isSilent, feedPosition);
        }
    }
}
