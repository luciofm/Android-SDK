package com.sharethrough.sdk.media;

import android.view.View;

import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.Placement;
import com.sharethrough.sdk.beacons.VideoCompletionBeaconService;
import com.sharethrough.sdk.dialogs.VideoDialog;

import java.util.Timer;

public class HostedVideo extends Media {
    private final Creative creative;

    public HostedVideo(Creative creative) {
        this.creative = creative;
    }

    @Override
    public void wasClicked(View view, BeaconService beaconService, int feedPosition) {
        new VideoDialog(view.getContext(), creative, beaconService, false, new Timer(), new VideoCompletionBeaconService(view.getContext(), creative, beaconService, feedPosition), feedPosition, 0).show();
    }

    @Override
    public void fireAdClickBeacon(Creative creative, IAdView adView, BeaconService beaconService, int feedPosition, Placement placement) {
        beaconService.adClicked("videoPlay", creative, adView.getAdView(), feedPosition, placement);

    }

    @Override
    public Creative getCreative() {
        return creative;
    }

    @Override
    public int getOverlayImageResourceId() {
        return R.drawable.non_yt_play;
    }
}
