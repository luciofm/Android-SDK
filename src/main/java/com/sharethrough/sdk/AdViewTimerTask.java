package com.sharethrough.sdk;

import android.graphics.Rect;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import android.util.Log;

public class AdViewTimerTask extends TimerTask {
    public static final long VISIBILITY_TIME_THRESHOLD = TimeUnit.SECONDS.toMillis(1);
    private final WeakReference<IAdView> adViewRef;
    private final Creative creative;
    private final BeaconService beaconService;
    private final Provider<Date> dateProvider;
    private final Sharethrough sharethrough;
    private final int feedPosition;
    private boolean isCancelled;
    private Date visibleStartTime;
    protected boolean adViewHasBeenVisible = false;

    public AdViewTimerTask(IAdView adView, int feedPosition, Creative creative, BeaconService beaconService, Provider<Date> dateProvider,
                           Sharethrough sharethrough) {
        this.adViewRef = new WeakReference<>(adView);
        this.feedPosition = feedPosition;
        this.creative = creative;
        this.beaconService = beaconService;
        this.dateProvider = dateProvider;
        this.sharethrough = sharethrough;
    }

    private void setVisibleStartTime()
    {
        if( visibleStartTime == null ){
            visibleStartTime = dateProvider.get();
        }
    }

    private void fireBeacon(IAdView adView) {
        creative.renderedTime = visibleStartTime.getTime();
        creative.wasVisible = true;
        beaconService.adVisible(adView.getAdView(), creative, feedPosition, sharethrough.placement);
    }

    private boolean hasAdBeenOnScreenForTimeThreshold() {
        return dateProvider.get().getTime() - visibleStartTime.getTime() >= VISIBILITY_TIME_THRESHOLD;
    }

    private void fireVisibleBeaconIfThresholdReached( IAdView adView ) {
        if (creative.wasVisible) {
            return;
        }

        if(is50PercentOfAdIsOnScreen(adView)) {
            setVisibleStartTime();
            if (hasAdBeenOnScreenForTimeThreshold()) {
                fireBeacon(adView);
            }
        } else {
            visibleStartTime = null;
        }
    }

    private void fireVisibilityEventsWhenAppropriate( IAdView adView ) {
        if( adView == null ) return;

        if (is50PercentOfAdIsOnScreen(adView)) {
            if (!adViewHasBeenVisible) {
                adViewHasBeenVisible = true;
            }

        } else {
            if (adViewHasBeenVisible) {
                adView.offScreen();
                adViewHasBeenVisible = false;
            }
        }

    }

    @Override
    public void run() {
        IAdView adView = adViewRef.get();
        if (null == adView) {
            cancel();
            return;
        }
        if (isCancelled) return;

        fireVisibleBeaconIfThresholdReached(adView);
        fireVisibilityEventsWhenAppropriate(adView);
    }

    private boolean is50PercentOfAdIsOnScreen(IAdView adView) {
        Rect rect = new Rect();
        if( adView.getAdView().isShown() && adView.getAdView().getGlobalVisibleRect(rect) ) {
            int visibleArea = rect.width() * rect.height();
            int viewArea = adView.getAdView().getHeight() * adView.getAdView().getWidth();

            if (visibleArea * 2 >= viewArea) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean cancel() {
        isCancelled = true;
        return super.cancel();
    }

    public IAdView getAdView() {
        return adViewRef.get();
    }

    public boolean isCancelled() {
        return isCancelled;
    }
}
