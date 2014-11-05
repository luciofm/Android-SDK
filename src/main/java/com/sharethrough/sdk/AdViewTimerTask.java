package com.sharethrough.sdk;

import android.graphics.Rect;
import android.view.View;

import java.util.TimerTask;

public class AdViewTimerTask extends TimerTask {
    private final View adView;
    private final Creative creative;
    private final BeaconService beaconService;
    private boolean isCancelled;
    private boolean isVisible;

    public AdViewTimerTask(View adView, Creative creative, BeaconService beaconService) {
        this.adView = adView;
        this.creative = creative;
        this.beaconService = beaconService;
    }

    @Override
    public void run() {
        Rect rect = new Rect();
        if (!isVisible && adView.isShown() && adView.getGlobalVisibleRect(rect)) {
            int visibleArea = rect.width() * rect.height();
            int viewArea = adView.getHeight() * adView.getWidth();

            if (visibleArea * 2 >= viewArea) {
                beaconService.adVisible(adView, creative);
                isVisible = true;
            }
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
