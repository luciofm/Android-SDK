package com.sharethrough.sdk.media;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.dialogs.YoutubeDialog;

public class Youtube implements Creative.Media {
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
    public void overlayThumbnail(IAdView adView) {
        FrameLayout thumbnail = adView.getThumbnail();
        ImageView youtubeIcon = new ImageView(adView.getContext());
        youtubeIcon.setImageResource(R.drawable.youtube_squared);
        youtubeIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Bitmap thumbnailBitmap = creative.getThumbnailImage();
        int overlayDimensionMax = Math.min(thumbnailBitmap.getWidth(), thumbnailBitmap.getHeight()) / 4;
        thumbnail.addView(youtubeIcon, new FrameLayout.LayoutParams(overlayDimensionMax, overlayDimensionMax, Gravity.TOP | Gravity.LEFT));
    }

    @Override
    public View.OnClickListener getClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new YoutubeDialog(v.getContext(), creative).show();
            }
        };
    }

    @Override
    public void fireAdClickBeacon(Creative creative, IAdView adView) {
        beaconService.adClicked(adView.getContext(), "youtubePlay", creative, adView);
    }
}
