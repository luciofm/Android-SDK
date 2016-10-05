package com.sharethrough.sdk;

import android.graphics.Rect;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class AdViewTimerTask extends TimerTask {
    public static final long VISIBILITY_TIME_THRESHOLD = TimeUnit.SECONDS.toMillis(1);
    private final WeakReference<IAdView> adViewRef;
    private final Creative creative;
    private final BeaconService beaconService;
    private final Provider<Date> dateProvider;
    private final int feedPosition;
    private boolean isCancelled;
    private Date onScreenStartTimeForVisibleBeacon;
    private Date onScreenStartTimeForAutoPlayStart;
    protected boolean adViewHasBeenVisible = false;

    public AdViewTimerTask(IAdView adView, int feedPosition, Creative creative, BeaconService beaconService, Provider<Date> dateProvider) {
        this.adViewRef = new WeakReference<>(adView);
        this.feedPosition = feedPosition;
        this.creative = creative;
        this.beaconService = beaconService;
        this.dateProvider = dateProvider;
    }

    private void setOnScreenStartTimeForAutoplay() {
        if (onScreenStartTimeForAutoPlayStart == null) {
            onScreenStartTimeForAutoPlayStart = dateProvider.get();
        }
    }
    private void setOnScreenStartTimeForVisibleBeacon()
    {
        if( onScreenStartTimeForVisibleBeacon == null ){
            onScreenStartTimeForVisibleBeacon = dateProvider.get();
        }
    }

    private void fireBeacon(IAdView adView) {
        creative.renderedTime = onScreenStartTimeForVisibleBeacon.getTime();
        creative.wasVisible = true;
        beaconService.adVisible(adView.getAdView(), creative, feedPosition);
    }

    private boolean hasAdBeenOnScreenForTimeThreshold() {
        return dateProvider.get().getTime() - onScreenStartTimeForVisibleBeacon.getTime() >= VISIBILITY_TIME_THRESHOLD;
    }

    private boolean hasAdBeenOnScreenForTimeThresholdForAutoplay() {
        return dateProvider.get().getTime() - onScreenStartTimeForAutoPlayStart.getTime() >= VISIBILITY_TIME_THRESHOLD;
    }

    private void fireVisibleBeaconIfThresholdReached( IAdView adView ) {
        if (creative.wasVisible) {
            return;
        }

        if(is50PercentOfAdIsOnScreen(adView)) {
            setOnScreenStartTimeForVisibleBeacon();
            if (hasAdBeenOnScreenForTimeThreshold()) {
                fireBeacon(adView);
            }
        } else {
            onScreenStartTimeForVisibleBeacon = null;
        }
    }

    private void fireVisibilityEventsWhenAppropriate( IAdView adView ) {
        checkIfOnScreen(adView);
        checkIfOffScreen(adView);
    }

    private void checkIfOnScreen(IAdView adView) {
        if( adView == null ) return;

        if(is50PercentOfAdIsOnScreen(adView)) {
            setOnScreenStartTimeForAutoplay();
            if (hasAdBeenOnScreenForTimeThresholdForAutoplay()) {
                adView.onScreen();
                adViewHasBeenVisible = true;
            }
        } else {
            onScreenStartTimeForAutoPlayStart = null;
        }
    }

    private void checkIfOffScreen(IAdView adView) {
        if( adView == null ) return;

        if (is80PercentOfAdIsOffScreen(adView) && adViewHasBeenVisible) {
            adView.offScreen();
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

    private boolean is80PercentOfAdIsOffScreen(IAdView adView) {
        Rect rect = new Rect();
        if( adView.getAdView().isShown() && adView.getAdView().getGlobalVisibleRect(rect) ) {
            int visibleArea = rect.width() * rect.height();
            int viewArea = adView.getAdView().getHeight() * adView.getAdView().getWidth();

            if (viewArea * .20 > visibleArea) {
                return true;
            }
        }

        return false;
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
