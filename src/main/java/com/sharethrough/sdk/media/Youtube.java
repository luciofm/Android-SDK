package com.sharethrough.sdk.media;

import android.net.Uri;
import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.Placement;
import com.sharethrough.sdk.dialogs.YoutubeDialog;

public class Youtube extends Media {
    private static final String EMBED_PREFIX = "/embed/";
    private Creative creative;

    public Youtube(Creative creative) {
        this.creative = creative;
    }

    public String getId() {
        Uri uri = Uri.parse(creative.getMediaUrl());
        String host = uri.getHost();

        if ("youtu.be".equals(host)) {
            return uri.getPath().substring(1);
        } else if (uri.getPath().startsWith(EMBED_PREFIX)) {
            return uri.getPath().substring(EMBED_PREFIX.length());
        } else {
            return uri.getQueryParameter("v");
        }
    }

    @Override
    public int getOverlayImageResourceId() {
        return R.drawable.youtube;
    }

    @Override
    public Creative getCreative() {
        return creative;
    }

    @Override
    public void wasClicked(View v, BeaconService beaconService, int feedPosition) {
        new YoutubeDialog(v.getContext(), creative, beaconService, feedPosition, getId()).show();
    }

    @Override
    public void fireAdClickBeacon(Creative creative, IAdView adView, BeaconService beaconService, int feedPosition) {
        beaconService.adClicked("youtubePlay", creative, adView.getAdView(), feedPosition);
    }
}
