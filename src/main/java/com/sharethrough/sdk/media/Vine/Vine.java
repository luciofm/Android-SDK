package com.sharethrough.sdk.media.Vine;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.dialogs.WebViewDialog;

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
                final Context context = v.getContext();
                new WebViewDialog(context, creative, beaconService) {
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    protected void loadPage() {
                        String baseUrl = "http://www.sharethrough.com/";
                        String html = context.getString(R.string.video_html)
                                .replace("videoURL", creative.getMediaUrl())
                                .replace("thumbnailURL", creative.getThumbnailUrl());
                        Log.d("Sharethrough", "Vine HTML:\n" + html);
                        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", baseUrl);
                    }
                }.show();
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
