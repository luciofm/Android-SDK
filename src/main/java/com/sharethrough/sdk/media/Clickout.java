package com.sharethrough.sdk.media;

import android.view.View;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.dialogs.WebViewDialog;

public class Clickout extends Media {
    protected final Creative creative;
    protected final BeaconService beaconService;

    public Clickout(Creative creative, BeaconService beaconService) {
        this.beaconService = beaconService;
        this.creative = creative;
    }

    @Override
    public Creative getCreative() {
        return creative;
    }

    @Override
    public int getOverlayImageResourceId() {
        return -1;
    }

    @Override
    public void wasClicked(View v) {
        new WebViewDialog(v.getContext(), creative, beaconService).show();
    }

    public <V extends View & IAdView> void fireAdClickBeacon(Creative creative, V adView) {
        beaconService.adClicked("clickout", creative, adView);
    }
}
