package com.sharethrough.sdk;

import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class AdViewTimerTask extends TimerTask {
    public static final long VISIBILITY_TIME_THRESHOLD = TimeUnit.SECONDS.toMillis(1);
    private final View adView;
    private final Creative creative;
    private final BeaconService beaconService;
    private final Provider<Date> dateProvider;
    private final Sharethrough sharethrough;
    private boolean isCancelled;
    private boolean isVisible;
    private Date visibleStartTime;
    private final int adCacheTimeInMilliseconds;

    public AdViewTimerTask(View adView, Creative creative, BeaconService beaconService, Provider<Date> dateProvider,
                           Sharethrough sharethrough) {
        this.adView = adView;
        this.creative = creative;
        this.beaconService = beaconService;
        this.dateProvider = dateProvider;
        this.sharethrough = sharethrough;
        this.adCacheTimeInMilliseconds = sharethrough.getAdCacheTimeInMilliseconds();
    }

    @Override
    public void run() {
        Log.d("Sharethrough", "AdViewTimer on " + adView + " with " + creative);
        if (isCancelled) return;

        Rect rect = new Rect();
        if (!isVisible) {
            if (adView.isShown() && adView.getGlobalVisibleRect(rect)) {
                int visibleArea = rect.width() * rect.height();
                int viewArea = adView.getHeight() * adView.getWidth();

                if (visibleArea * 2 >= viewArea) {
                    if (visibleStartTime != null) {
                        if (dateProvider.get().getTime() - visibleStartTime.getTime() >= VISIBILITY_TIME_THRESHOLD) {
                            beaconService.adVisible(adView, creative);
                            isVisible = true;
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
                sharethrough.putCreativeIntoAdView(adView);
                cancel();
            }
        }
    }

    @Override
    public boolean cancel() {
        isCancelled = true;
        Log.d("Sharethrough", "canceling AdViewTimer for " + creative);
        return super.cancel();
    }

    public View getAdView() {
        return adView;
    }

    public boolean isCancelled() {
        return isCancelled;
    }
}
