package com.sharethrough.sdk.media;

import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.beacons.VideoCompletionBeaconService;
import com.sharethrough.sdk.dialogs.VideoDialog;

import java.util.Timer;

public class Vine extends Media {
    private final Creative creative;

    public Vine(Creative creative) {
        this.creative = creative;
    }

    @Override
    public void wasClicked(View v, BeaconService beaconService) {
        new VideoDialog(v.getContext(), creative, beaconService, true, new Timer(), new VideoCompletionBeaconService(v.getContext(), creative, beaconService)).show();
    }

    @Override
    public <V extends View & IAdView> void fireAdClickBeacon(Creative creative, V adView, BeaconService beaconService) {
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
