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
    private boolean hasBeenShown;
    private Date visibleStartTime;
    private final int adCacheTimeInMilliseconds;
    private final int adViewHashCode;

    public AdViewTimerTask(IAdView adView, int feedPosition, Creative creative, BeaconService beaconService, Provider<Date> dateProvider,
                           Sharethrough sharethrough) {
        this.adViewRef = new WeakReference<>(adView);
        this.adViewHashCode = adView.hashCode();
        this.feedPosition = feedPosition;
        this.creative = creative;
        this.beaconService = beaconService;
        this.dateProvider = dateProvider;
        this.sharethrough = sharethrough;
        this.adCacheTimeInMilliseconds = sharethrough.getAdCacheTimeInMilliseconds();
    }

    private void fireVisibleBeaconIfThresholdReached( IAdView adView ) {
        Rect rect = new Rect();
        if (isCurrentlyVisible(adView, rect)) {
            int visibleArea = rect.width() * rect.height();
            int viewArea = adView.getAdView().getHeight() * adView.getAdView().getWidth();

            if (visibleArea * 2 >= viewArea) {
                if (visibleStartTime != null) {
                    if (dateProvider.get().getTime() - visibleStartTime.getTime() >= VISIBILITY_TIME_THRESHOLD) {
                        beaconService.adVisible(adView.getAdView(), creative, feedPosition, sharethrough.placement);
                        creative.renderedTime = visibleStartTime.getTime();
                        creative.wasVisible = true;
                        //hasBeenShown = true;
                        Log.d("jermaine", "fires beacon");
                    }
                } else {
                    visibleStartTime = dateProvider.get();
                }
            } else {
                visibleStartTime = null;
            }
        } else {
            visibleStartTime = null;
        }
    }

    private void replaceAdOnTimeout(IAdView adView) {
        Rect rect = new Rect();
        if (visibleStartTime == null) {
            visibleStartTime = dateProvider.get();
        }
        if ((dateProvider.get().getTime() - (visibleStartTime.getTime())) >= adCacheTimeInMilliseconds) {
            if (!isCurrentlyVisible(adView, rect)) {
                Log.d("jermaine", " " +
                        "renews ad");
                sharethrough.putCreativeIntoAdView(adView, feedPosition);
                cancel();
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

        if (!creative.wasVisible) {
            fireVisibleBeaconIfThresholdReached(adView);
        } else {
            // if visible beacon fired and creative has timed out, replace creative with new one
            replaceAdOnTimeout(adView);
        }

    }

    private boolean isCurrentlyVisible(IAdView adView, Rect rect) {
        return adView.getAdView().isShown() && adView.getAdView().getGlobalVisibleRect(rect);
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
