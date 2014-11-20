package com.sharethrough.sdk;

import android.graphics.Rect;
import android.util.Log;

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
    private final Sharethrough sharethrough;
    private boolean isCancelled;
    private boolean hasBeenShown;
    private Date visibleStartTime;
    private final int adCacheTimeInMilliseconds;
    private final int adViewHashCode;

    public AdViewTimerTask(IAdView adView, Creative creative, BeaconService beaconService, Provider<Date> dateProvider,
                           Sharethrough sharethrough) {
        this.adViewRef = new WeakReference<>(adView);
        this.adViewHashCode = adView.hashCode();
        this.creative = creative;
        this.beaconService = beaconService;
        this.dateProvider = dateProvider;
        this.sharethrough = sharethrough;
        this.adCacheTimeInMilliseconds = sharethrough.getAdCacheTimeInMilliseconds();
    }

    @Override
    public void run() {
        IAdView adView = adViewRef.get();
        if (null == adView) {
            cancel();
            return;
        }
        Log.v("Sharethrough", "AdViewTimer on " + adView + " with " + creative);
        if (isCancelled) return;

        Rect rect = new Rect();
        if (!hasBeenShown) {
            if (isCurrentlyVisible(adView, rect)) {
                int visibleArea = rect.width() * rect.height();
                int viewArea = adView.getAdView().getHeight() * adView.getAdView().getWidth();

                if (visibleArea * 2 >= viewArea) {
                    if (visibleStartTime != null) {
                        if (dateProvider.get().getTime() - visibleStartTime.getTime() >= VISIBILITY_TIME_THRESHOLD) {
                            beaconService.adVisible(adView.getAdView(), creative);
                            hasBeenShown = true;
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
        } else {
            if ((dateProvider.get().getTime() - (visibleStartTime.getTime())) >= adCacheTimeInMilliseconds) {
                if (!isCurrentlyVisible(adView, rect)) {
                    sharethrough.putCreativeIntoAdView(adView, NoOp.INSTANCE);
                    cancel();
                }
            }
        }
    }

    private boolean isCurrentlyVisible(IAdView adView, Rect rect) {
        return adView.getAdView().isShown() && adView.getAdView().getGlobalVisibleRect(rect);
    }

    @Override
    public boolean cancel() {
        isCancelled = true;
        Log.d("Sharethrough", "canceling AdViewTimer for " + creative);
        Log.d("MEMORY", adViewHashCode + "/" + creative + " cancelled");
        return super.cancel();
    }

    public IAdView getAdView() {
        return adViewRef.get();
    }

    public boolean isCancelled() {
        return isCancelled;
    }
}
