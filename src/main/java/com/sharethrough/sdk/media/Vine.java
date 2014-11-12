package com.sharethrough.sdk.media;

import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.dialogs.VideoDialog;

public class Vine extends Media {
    private final Creative creative;
    private final BeaconService beaconService;

    public Vine(Creative creative, BeaconService beaconService) {
        this.creative = creative;
        this.beaconService = beaconService;
    }

    @Override
    public void wasClicked(View v) {
        new VideoDialog(v.getContext(), creative, beaconService, true).show();
    }

    @Override
    public <V extends View & IAdView> void fireAdClickBeacon(Creative creative, V adView) {
        beaconService.adClicked("vinePlay", creative, adView);
    }

    @Override
    public int getOverlayImageResourceId() {
        return R.drawable.vine;
    }

    @Override
    public Creative getCreative() {
        return creative;
    }
}
