package com.sharethrough.sdk.media.Vine;

import android.graphics.Bitmap;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;

public abstract class ThumbnailOverlayingMedia implements Creative.Media {
    @Override
    public final void overlayThumbnail(IAdView adView) {
        FrameLayout thumbnail = adView.getThumbnail();
        ImageView youtubeIcon = new ImageView(adView.getContext());
        youtubeIcon.setImageResource(getOverlayImageResourceId());
        youtubeIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Bitmap thumbnailBitmap = getCreative().getThumbnailImage();
        int overlayDimensionMax = Math.min(thumbnailBitmap.getWidth(), thumbnailBitmap.getHeight()) / 4;
        thumbnail.addView(youtubeIcon, new FrameLayout.LayoutParams(overlayDimensionMax, overlayDimensionMax, Gravity.TOP | Gravity.LEFT));
    }

    protected abstract int getOverlayImageResourceId();

    protected abstract Creative getCreative();
}
