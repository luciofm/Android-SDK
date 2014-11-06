package com.sharethrough.sdk;

import android.graphics.Rect;
import android.view.View;

import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class AdViewTimerTask extends TimerTask {
    private final View adView;
    private final Creative creative;
    private final BeaconService beaconService;
    private final Provider<Date> dateProvider;
    private boolean isCancelled;
    private boolean isVisible;
    private Date startTime;

    public AdViewTimerTask(View adView, Creative creative, BeaconService beaconService, Provider<Date> dateProvider) {
        this.adView = adView;
        this.creative = creative;
        this.beaconService = beaconService;
        this.dateProvider = dateProvider;
    }

    @Override
    public void run() {
        Rect rect = new Rect();
        if (!isVisible && adView.isShown() && adView.getGlobalVisibleRect(rect)) {
            int visibleArea = rect.width() * rect.height();
            int viewArea = adView.getHeight() * adView.getWidth();

            if (visibleArea * 2 >= viewArea) {
                if (startTime != null){
                    if (dateProvider.get().getTime() - startTime.getTime() >= TimeUnit.SECONDS.toMillis(1)) {
                        beaconService.adVisible(adView, creative);
                        isVisible = true;
                    }
                } else {
                    startTime = dateProvider.get();
                }
            } else {
                startTime = null;
            }
        } else {
            startTime = null;
        }
    }

    @Override
    public boolean cancel() {
        isCancelled = true;
        return super.cancel();
    }

    public View getAdView() {
        return adView;
    }

    public boolean isCancelled() {
        return isCancelled;
    }
}
