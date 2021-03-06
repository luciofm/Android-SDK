package com.sharethrough.sdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.sharethrough.sdk.media.*;

import java.util.List;

public class Creative {
    public enum CreativeType {
        HOSTEDVIDEO,
        CLICKOUT,
        INSTAGRAM,
        PINTEREST,
        VINE,
        YOUTUBE,
        ARTICLE
    }
    protected final Response.Creative responseCreative;
    public boolean wasRendered;
    public long renderedTime = Long.MAX_VALUE;
    private boolean wasClicked = false;
    public boolean wasVisible = false;

    public Creative(Response.Creative responseCreative, String mediationRequestId) {
        this.responseCreative = responseCreative;
        // To remove for asap v2
        this.responseCreative.mediationRequestId = mediationRequestId;
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

    public String getThumbnailUrl() {
        return convertToAbsoluteUrl(responseCreative.creative.thumbnailUrl);
    }

    public String getBrandLogoUrl() {
        return convertToAbsoluteUrl(responseCreative.creative.brandLogoUrl);
    }

    public CreativeType getType() {
        switch (responseCreative.creative.action) {
            case "video":
                return CreativeType.YOUTUBE;
            case "vine":
                return CreativeType.VINE;
            case "hosted-video":
                return CreativeType.HOSTEDVIDEO;
            case "instagram":
                return CreativeType.INSTAGRAM;
            case "pinterest":
                return CreativeType.PINTEREST;
            case "article":
                return CreativeType.ARTICLE;
            case "clickout":
            default:
                return CreativeType.CLICKOUT;
        }
    }

    public String getAdserverRequestId() {
        return responseCreative.adserverRequestId;
    }

    public String getMediationRequestId() {
        return responseCreative.mediationRequestId;
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

    public String getVariantKey() {
        return responseCreative.creative.variantKey;
    }

    public String getCreativeKey() {
        return responseCreative.creative.creativeKey;
    }

    public String getCampaignKey() {
        return responseCreative.creative.campaignKey;
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

    public List<String> getSilentPlayBeacons() { return responseCreative.creative.beacon.silentPlay; }

    public List<String> getTenSecondSilentPlayBeacons() { return responseCreative.creative.beacon.tenSecondSilentPlay; }

    public List<String> getFifteenSecondSilentPlayBeacons() { return responseCreative.creative.beacon.fifteenSecondSilentPlay; }

    public List<String> getThirtySecondSilentPlayBeacons() { return responseCreative.creative.beacon.thirtySecondSilentPlay; }

    public List<String> getCompletedSilentPlayBeacons() { return responseCreative.creative.beacon.completedSilentPlay; }

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

    public String getDealId(){
        return responseCreative.creative.dealId;
    }

    public String getOptOutText() {
        return responseCreative.creative.optOutText;
    }

    public String getOptOutUrl() {
        return responseCreative.creative.optOutUrl;
    }
}
