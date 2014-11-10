package com.sharethrough.sdk.media;

import android.view.View;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.dialogs.WebViewDialog;

public class Clickout implements Creative.Media {
    private final Creative creative;
    private final BeaconService beaconService;

    public Clickout(Creative creative, BeaconService beaconService) {
        this.beaconService = beaconService;
        this.creative = creative;
    }

    @Override
    public  <V extends View & IAdView> void overlayThumbnail(V adView) {
    }

    @Override
    public View.OnClickListener getClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new WebViewDialog(v.getContext(), creative, beaconService).show();
            }
        };
    }

    public <V extends View & IAdView> void fireAdClickBeacon(Creative creative, V adView) {
        beaconService.adClicked(adView.getContext(), "clickout", creative, adView);
    }
}
