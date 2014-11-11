package com.sharethrough.sdk.media;

import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;

public abstract class Media {
    public abstract View.OnClickListener getClickListener();
    public abstract < V extends View & IAdView> void fireAdClickBeacon(Creative creative, V adView);

    public final <V extends View & IAdView> void overlayThumbnail(V adView) {
        int overlayImageResourceId = getOverlayImageResourceId();
        if (overlayImageResourceId < 0) return;
        FrameLayout thumbnail = adView.getThumbnail();
        ImageView overlayIcon = new ImageView(adView.getContext());
        overlayIcon.setImageResource(overlayImageResourceId);
        overlayIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Bitmap thumbnailBitmap = getCreative().getThumbnailImage();
        int overlayDimensionMax = Math.min(thumbnailBitmap.getWidth(), thumbnailBitmap.getHeight()) / 4;
        thumbnail.addView(overlayIcon, new FrameLayout.LayoutParams(overlayDimensionMax, overlayDimensionMax, Gravity.TOP | Gravity.LEFT));
    }

    public abstract Creative getCreative();

    public abstract int getOverlayImageResourceId();
}
