package com.sharethrough.sdk;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import java.util.Date;
import java.util.Timer;

class AdImageView extends ImageView
{
    Timer visibleBeaconTimer;
    AdViewTimerTask visibleBeaconTask;
    Sharethrough sharethrough;
    Creative creative;
    IAdView adView;
    int feedPosition;
    BeaconService beaconService;
    Context context;

    AdImageView(Context context, Sharethrough sharethrough, Creative creative, IAdView adview, int feedPosition, BeaconService beaconService){
        super(context);
        this.sharethrough = sharethrough;
        this.creative = creative;
        this.adView = adview;
        this.feedPosition = feedPosition;
        this.beaconService = beaconService;
        this.context = context;
    }

    protected Timer getTimer()
    {
        return new Timer();
    }

    protected DateProvider getDateProvider() {
        return new DateProvider();
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        //cancel previously created timers before recreating them
        if (visibleBeaconTask != null && visibleBeaconTimer != null) {
            visibleBeaconTask.cancel();
            visibleBeaconTimer.cancel();
            visibleBeaconTimer.purge();
        }

        //call sharethrough.putcreativeintoadview if ad timed out, this would never get called for listadapter ads because the creative is always new
        if ((getDateProvider().get().getTime() - creative.renderedTime) >= sharethrough.getAdCacheTimeInMilliseconds() && creative.wasVisible) {
            Log.d("jermaine", "putting in new ad because time expired");
            sharethrough.putCreativeIntoAdView(adView, feedPosition);
        } else {

            visibleBeaconTimer = getTimer();
            visibleBeaconTask = new AdViewTimerTask(adView, feedPosition, creative, beaconService, new DateProvider(), sharethrough);
            visibleBeaconTimer.schedule(visibleBeaconTask, 0, 100);
        }
        //   Log.d("jermaine", creative + " window attached");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (visibleBeaconTask != null && visibleBeaconTimer != null) {
            visibleBeaconTask.cancel();
            visibleBeaconTimer.cancel();
            visibleBeaconTimer.purge();

        }
        // Log.d("jermaine", creative + " window detached");


    }
}

