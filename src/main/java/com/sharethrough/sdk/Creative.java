package com.sharethrough.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import com.sharethrough.sdk.media.Clickout;
import com.sharethrough.sdk.media.Youtube;

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

    public Creative.Media getMedia() {
        switch (responseCreative.creative.action) {
            case "video":
                return new Youtube(this, beaconService);
            case "instagram":
            case "pinterest":
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


    public interface Media {
        void overlayThumbnail(IAdView adView);
        View.OnClickListener getClickListener();
        void fireAdClickBeacon(Creative creative, IAdView adView);
    }
}
