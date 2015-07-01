package com.sharethrough.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.sharethrough.sdk.media.*;

import java.util.List;

public class Creative {
    private final Response.Creative responseCreative;
    private final byte[] imageBytes;
    private final String placementKey;
    private final byte[] logoBytes;
    public boolean wasRendered;
    public long renderedTime = Long.MAX_VALUE;
    private boolean wasClicked = false;
    public boolean wasVisible = false;

    public Creative(Response.Creative responseCreative, byte[] imageBytes, byte[] logoBytes, String placementKey) {
        this.responseCreative = responseCreative;
        this.imageBytes = imageBytes;
        this.logoBytes = logoBytes;
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

    public Bitmap makeThumbnailImage(int height, int width) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.outHeight = height;
        opts.outWidth = width;
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, opts);
    }

    public Bitmap makeBrandLogo() {
        if (logoBytes != null && logoBytes.length > 0) {
            return BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.length);
        }
        return null;
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
            case "article":
                return new Article(this);
            case "clickout":
            default:
                return new Clickout(this);
        }
    }

    public boolean hasExpired(int adCacheTimeInMilliseconds) {
        return (new DateProvider().get().getTime()) - renderedTime >= adCacheTimeInMilliseconds;
    }

    public String getAdserverRequestId() {
        return responseCreative.adserverRequestId;
    }

    public String getAuctionWinId() {
        return responseCreative.auctionWinId;
    }

    public String getShareUrl() {
        return convertToAbsoluteUrl(responseCreative.creative.shareUrl);
    }

    public String getMediaUrl() {
        return convertToAbsoluteUrl(responseCreative.creative.mediaUrl);
    }

    public String getPlacementKey() {
        return placementKey;
    }

    public String getVariantKey() {
        return responseCreative.creative.variantKey;
    }

    public String getCreativeKey() {
        return responseCreative.creative.creativeKey;
    }

    public String getCampaignKey() {
        return responseCreative.creative.campaignKey;
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

    public String getCustomEngagementUrl() {
        return convertToAbsoluteUrl(responseCreative.creative.customEngagementUrl);
    }

    public String getCustomEngagementLabel() {
        return responseCreative.creative.customEngagementLabel;
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

    public List<String> getImpressionBeacons() { return responseCreative.creative.beacon.impression; }

    public boolean wasClicked(){
        return wasClicked;
    }

    public void setClicked() {
        wasClicked = true;
    }

    public static String convertToAbsoluteUrl(String url) {
        String pattern = "(^(\\s|/)+)|((\\s|/)+$)";
        String canonicalUrl = url.replaceAll(pattern, "");
        if (Uri.parse(canonicalUrl).isAbsolute()) {
            return canonicalUrl;
        } else {
            return "http://" + canonicalUrl;
        }
    }
}
