package com.sharethrough.sdk.media;

import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.beacons.VideoCompletionBeaconService;
import com.sharethrough.sdk.dialogs.VideoDialog;

import java.util.Timer;

public class HostedVideo extends Media {
    private final Creative creative;

    public HostedVideo(Creative creative) {
        this.creative = creative;
    }

    @Override
    public void wasClicked(View view, BeaconService beaconService) {
        new VideoDialog(view.getContext(), creative, beaconService, false, new Timer(), new VideoCompletionBeaconService(view.getContext(), creative, beaconService)).show();
    }

    @Override
    public void fireAdClickBeacon(Creative creative, IAdView adView, BeaconService beaconService) {
        beaconService.adClicked("videoPlay", creative, adView.getAdView());
    }

    @Override
    public Creative getCreative() {
        return creative;
    }

    @Override
    public int getOverlayImageResourceId() {
        return R.drawable.hosted_video;
    }

    @Override
    public boolean isThumbnailOverlayCentered() {
        return true;
    }
}
