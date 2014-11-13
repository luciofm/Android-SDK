package com.sharethrough.sdk.media;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;

public abstract class Media {
    public abstract void wasClicked(View view, BeaconService beaconService);

    public abstract void fireAdClickBeacon(Creative creative, IAdView adView, BeaconService beaconService);

    public final void overlayThumbnail(IAdView adView, ImageView thumbnailImage) {
        int overlayImageResourceId = getOverlayImageResourceId();
        if (overlayImageResourceId < 0) return;
        FrameLayout thumbnail = adView.getThumbnail();
        ImageView overlayIcon = new ImageView(adView.getAdView().getContext());
        overlayIcon.setImageResource(overlayImageResourceId);
        overlayIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int overlayDimensionMax = Math.min(thumbnailImage.getWidth(), thumbnailImage.getHeight()) / 4;
        int gravity = isThumbnailOverlayCentered() ? Gravity.CENTER : Gravity.TOP | Gravity.LEFT;
        thumbnail.addView(overlayIcon, new FrameLayout.LayoutParams(overlayDimensionMax, overlayDimensionMax, gravity));
    }

    public boolean isThumbnailOverlayCentered() {
        return false;
    }

    public abstract Creative getCreative();

    public abstract int getOverlayImageResourceId();
}
