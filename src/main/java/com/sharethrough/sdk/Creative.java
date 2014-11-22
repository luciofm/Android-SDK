package com.sharethrough.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.sharethrough.sdk.media.*;

import java.util.List;

public class Creative {
    private final Response.Creative responseCreative;
    private final byte[] imageBytes;
    private final String placementKey;
    public boolean wasRendered;
    public long renderedTime = Long.MAX_VALUE;

    public Creative(Response.Creative responseCreative, byte[] imageBytes, String placementKey) {
        this.responseCreative = responseCreative;
        this.imageBytes = imageBytes;
        this.placementKey = placementKey;
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

    public Bitmap makeThumbnailImage() {
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public Media getMedia() {
        switch (responseCreative.creative.action) {
            case "video":
                return new Youtube(this);
            case "vine":
                return new Vine(this);
            case "hosted-video":
                return new HostedVideo(this);
            case "instagram":
                return new Instagram(this);
            case "pinterest":
                return new Pinterest(this);
            case "clickout":
            default:
                return new Clickout(this);
        }
    }

    public String getShareUrl() {
        return responseCreative.creative.shareUrl;
    }

    public String getMediaUrl() {
        return responseCreative.creative.mediaUrl;
    }

    public String getPlacementKey() {
        return placementKey;
    }

    public String getVariantKey() {
        return responseCreative.creative.variantKey;
    }

    public String getCreativeKey() {
        return responseCreative.creative.key;
    }

    public String getSignature() {
        return responseCreative.signature;
    }

    public String getAuctionType() {
        return responseCreative.priceType;
    }

    public String getAuctionPrice() {
        return String.valueOf(responseCreative.price);
    }

    public List<String> getClickBeacons() {
        return responseCreative.creative.beacon.click;
    }

    public List<String> getPlayBeacons() {
        return responseCreative.creative.beacon.play;
    }

    public List<String> getVisibleBeacons() {
        return responseCreative.creative.beacon.visible;
    }
}
