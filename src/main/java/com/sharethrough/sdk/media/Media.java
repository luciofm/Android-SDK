package com.sharethrough.sdk.media;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;

public abstract class Media {
    public final static String THUMBNAIL = "ThumbnailImageView";

    public abstract void wasClicked(View view, BeaconService beaconService);

    public abstract void fireAdClickBeacon(Creative creative, IAdView adView, BeaconService beaconService);

    public final void overlayThumbnail(IAdView adView, ImageView thumbnailImage) {
        int overlayImageResourceId = getOverlayImageResourceId();
        if (overlayImageResourceId < 0) return;
        FrameLayout thumbnail = adView.getThumbnail();
        OverlayImage overlayIcon = new OverlayImage(adView.getAdView().getContext());
        overlayIcon.setImageResource(overlayImageResourceId);
        overlayIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int gravity = isThumbnailOverlayCentered() ? Gravity.CENTER : Gravity.TOP | Gravity.LEFT;
        thumbnail.addView(overlayIcon, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity));

        thumbnailImage.setTag(THUMBNAIL);
    }

    public boolean isThumbnailOverlayCentered() {
        return false;
    }

    public abstract Creative getCreative();

    public abstract int getOverlayImageResourceId();

}
