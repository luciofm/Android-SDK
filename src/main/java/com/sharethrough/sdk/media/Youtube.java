package com.sharethrough.sdk.media;

import android.net.Uri;
import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.dialogs.YoutubeDialog;
import com.sharethrough.sdk.media.Vine.ThumbnailOverlayingMedia;

public class Youtube extends ThumbnailOverlayingMedia {
    private static final String EMBED_PREFIX = "/embed/";
    private final BeaconService beaconService;
    private Creative creative;

    public Youtube(Creative creative, BeaconService beaconService) {
        this.creative = creative;
        this.beaconService = beaconService;
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
    protected int getOverlayImageResourceId() {
        return R.drawable.youtube_squared;
    }

    @Override
    protected Creative getCreative() {
        return creative;
    }

    @Override
    public View.OnClickListener getClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new YoutubeDialog(v.getContext(), creative, beaconService).show();
            }
        };
    }

    @Override
    public <V extends View & IAdView> void fireAdClickBeacon(Creative creative, V adView) {
        beaconService.adClicked(adView.getContext(), "youtubePlay", creative, adView);
    }
}
