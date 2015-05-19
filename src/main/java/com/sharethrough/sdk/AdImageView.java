package com.sharethrough.sdk;

import android.content.Context;
import android.widget.ImageView;
import java.util.Timer;

class AdImageView extends ImageView {
    Timer visibleBeaconTimer;
    AdViewTimerTask visibleBeaconTask;
    Sharethrough sharethrough;
    Creative creative;
    IAdView adView;
    int feedPosition;
    BeaconService beaconService;
    Context context;

    AdImageView(Context context, Sharethrough sharethrough, Creative creative, IAdView adview, int feedPosition, BeaconService beaconService) {
        super(context);
        this.sharethrough = sharethrough;
        this.creative = creative;
        this.adView = adview;
        this.feedPosition = feedPosition;
        this.beaconService = beaconService;
        this.context = context;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        killVisibleBeaconTask();

        if (forceAdRefreshIfRecyclerViewAndExpired()) {
            return;
        }

        scheduleVisibleBeaconTask();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        killVisibleBeaconTask();
    }

    private void scheduleVisibleBeaconTask() {
        visibleBeaconTimer = getTimer();
        visibleBeaconTask = new AdViewTimerTask(adView, feedPosition, creative, beaconService, new DateProvider(), sharethrough);
        visibleBeaconTimer.schedule(visibleBeaconTask, 0, 100);
    }

    private boolean forceAdRefreshIfRecyclerViewAndExpired() {
        long currentTime = (new DateProvider()).get().getTime();
        if (creative.wasVisible && (currentTime - creative.renderedTime) >= sharethrough.getAdCacheTimeInMilliseconds()) {
            sharethrough.putCreativeIntoAdView(adView, feedPosition);
            return true;
        }

        return false;
    }

    private void killVisibleBeaconTask() {
        if (visibleBeaconTask != null && visibleBeaconTimer != null) {
            visibleBeaconTask.cancel();
            visibleBeaconTimer.cancel();
            visibleBeaconTimer.purge();
        }
    }

    protected Timer getTimer() {
        return new Timer();
    }

}

