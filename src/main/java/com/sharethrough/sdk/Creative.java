package com.sharethrough.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.sharethrough.sdk.media.*;

import java.util.ArrayList;

public class Creative {
    private final Response.Creative responseCreative;
    private final String placementKey;
    private final BeaconService beaconService;
    private final Bitmap thumbnailImage;
    private final StrSession strSession = new StrSession();
    public boolean wasRendered;

    public Creative(Response.Creative responseCreative, byte[] imageBytes, String placementKey, BeaconService beaconService) {
        this.responseCreative = responseCreative;
        this.placementKey = placementKey;
        this.beaconService = beaconService;
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
                return new Youtube(this, beaconService);
            case "vine":
                return new Vine(this, beaconService);
            case "instagram":
                return new Instagram(this, beaconService);
            case "pinterest":
                return new Pinterest(this, beaconService);
            case "clickout":
            default:
                return new Clickout(this, beaconService);
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

    public String getThumbnailUrl() {
        return responseCreative.creative.thumbnailUrl;
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
