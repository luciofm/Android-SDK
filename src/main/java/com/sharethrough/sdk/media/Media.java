package com.sharethrough.sdk.media;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.Placement;
import android.util.Log;
import java.lang.String;

public abstract class Media {
    public final static String THUMBNAIL = "ThumbnailImageView";

    public void fireAdClickBeaconOnFirstClick(Creative creative, IAdView adView, BeaconService beaconService, int feedPosition, Placement placement) {
        if (!getCreative().wasClicked()) {
            fireAdClickBeacon(creative, adView, beaconService, feedPosition, placement);
            getCreative().setClicked();
        }
    }

    public abstract void wasClicked(View view, BeaconService beaconService, int feedPosition);

    public abstract void fireAdClickBeacon(Creative creative, IAdView adView, BeaconService beaconService, int feedPosition, Placement placement);

    /*
    Puts the Media icon over where the thumbnail image will be (ie. red youtube icon)
     */
    public void swapMedia(IAdView adView, ImageView thumbnailImage) {
        int overlayImageResourceId = getOverlayImageResourceId();
        if (overlayImageResourceId < 0) return;
        FrameLayout thumbnail = adView.getThumbnail();
        OverlayImage overlayIcon = new OverlayImage(adView.getAdView().getContext());
        overlayIcon.setImageResource(overlayImageResourceId);

        int overlayMax = Math.min(thumbnailImage.getWidth(), thumbnailImage.getHeight()) / 4;

        overlayIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int gravity = isThumbnailOverlayCentered() ? Gravity.CENTER : Gravity.TOP | Gravity.LEFT;
        thumbnail.addView(overlayIcon, new FrameLayout.LayoutParams(overlayMax, overlayMax, gravity));

        thumbnailImage.setTag(THUMBNAIL);


    }

    public boolean isThumbnailOverlayCentered() {
        return false;
    }

    public abstract Creative getCreative();

    public abstract int getOverlayImageResourceId();

}