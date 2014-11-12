package com.sharethrough.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.sharethrough.sdk.media.*;

import java.util.ArrayList;

public class Creative {
    private final Response.Creative responseCreative;
    private final String placementKey;
    private final Bitmap thumbnailImage;
    public boolean wasRendered;

    public Creative(Response.Creative responseCreative, byte[] imageBytes, String placementKey) {
        this.responseCreative = responseCreative;
        this.placementKey = placementKey;
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

    public ArrayList<String> getClickBeacons() {
        return (ArrayList<String>) responseCreative.creative.beacon.click;
    }

    public ArrayList<String> getPlayBeacons() {
        return (ArrayList<String>) responseCreative.creative.beacon.play;
    }

    public ArrayList<String> getVisibleBeacons() {
        return (ArrayList<String>) responseCreative.creative.beacon.visible;
    }
}
