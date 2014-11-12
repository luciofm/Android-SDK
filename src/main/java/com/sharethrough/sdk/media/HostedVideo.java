package com.sharethrough.sdk.media;

import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.dialogs.VideoDialog;
import com.sharethrough.sdk.webview.VideoCompletionBeaconService;

import java.util.Timer;

public class HostedVideo extends Media {
    private final Creative creative;
    private final BeaconService beaconService;

    public HostedVideo(Creative creative, BeaconService beaconService) {
        this.creative = creative;
        this.beaconService = beaconService;
    }

    @Override
    public void wasClicked(View view) {
        new VideoDialog(view.getContext(), creative, beaconService, false, new Timer(), new VideoCompletionBeaconService(view.getContext(), creative, beaconService)).show();
    }

    @Override
    public <V extends View & IAdView> void fireAdClickBeacon(Creative creative, V adView) {

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
