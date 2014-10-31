package com.sharethrough.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;

public class Creative {
    private final Response.Creative responseCreative;
    private final Bitmap thumbnailImage;

    public Creative(Response.Creative responseCreative, byte[] imageBytes) {
        this.responseCreative = responseCreative;
        thumbnailImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public String getTitle() {
        return responseCreative.creative.title;
    }

    public String getAdvertiser() {
        return responseCreative.creative.advertiser;
    }

    public String getDescription() {
        return responseCreative.creative.description;
    }

    public Bitmap getThumbnailImage() {
        return thumbnailImage;
    }

    public Creative.Media getMedia() {
        return new Youtube(this);
    }

    public String getShareUrl() {
        return responseCreative.creative.shareUrl;
    }

    public String getMediaUrl() {
        return responseCreative.creative.mediaUrl;
    }

    public interface Media {
        void overlayThumbnail(IAdView adView);
        View.OnClickListener getClickListener();
    }
}
