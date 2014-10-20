package com.sharethrough.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Creative {
    private final Response.Creative responseCreative;
    private final byte[] imageBytes;


    public Creative(Response.Creative responseCreative, byte[] imageBytes) {
        this.responseCreative = responseCreative;
        this.imageBytes = imageBytes;
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
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
}
