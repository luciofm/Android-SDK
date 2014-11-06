package com.sharethrough.sdk.media.Vine;

import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;

public class Vine extends ThumbnailOverlayingMedia {
    private final Creative creative;
    private final BeaconService beaconService;

    public Vine(Creative creative, BeaconService beaconService) {
        this.creative = creative;
        this.beaconService = beaconService;
    }

    @Override
    public View.OnClickListener getClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
            }
        };
    }

    @Override
    public void fireAdClickBeacon(Creative creative, IAdView adView) {
        // TODO
    }

    @Override
    protected int getOverlayImageResourceId() {
        return R.drawable.vine_squared;
    }

    @Override
    protected Creative getCreative() {
        return creative;
    }
}
