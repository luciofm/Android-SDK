package com.sharethrough.sdk;

import android.content.Context;
import android.widget.ImageView;
import java.util.Timer;

class AdImageView extends ImageView {
    Timer visibleBeaconTimer;
    AdViewTimerTask visibleBeaconTask;
    Creative creative;
    IAdView adView;
    int feedPosition;
    BeaconService beaconService;
    Context context;

    AdImageView(Context context, Creative creative, IAdView adview, int feedPosition, BeaconService beaconService) {
        super(context);
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
        scheduleVisibleBeaconTask();

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        killVisibleBeaconTask();
    }

    private void scheduleVisibleBeaconTask() {
        visibleBeaconTimer = getTimer();
        visibleBeaconTask = new AdViewTimerTask(adView, feedPosition, creative, beaconService, new DateProvider());
        visibleBeaconTimer.schedule(visibleBeaconTask, 0, 100);
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

