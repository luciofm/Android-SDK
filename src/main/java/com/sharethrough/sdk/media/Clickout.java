package com.sharethrough.sdk.media;

import android.view.View;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.dialogs.WebViewDialog;

public class Clickout extends Media {
    protected final Creative creative;

    public Clickout(Creative creative) {
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
    public void wasClicked(View v, BeaconService beaconService) {
        new WebViewDialog(v.getContext(), creative, beaconService).show();
    }

    public <V extends View & IAdView> void fireAdClickBeacon(Creative creative, V adView, BeaconService beaconService) {
        beaconService.adClicked("clickout", creative, adView);
    }
}
