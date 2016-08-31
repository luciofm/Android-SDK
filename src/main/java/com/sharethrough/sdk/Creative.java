package com.sharethrough.sdk;

import android.net.Uri;
import com.google.gson.Gson;
import com.sharethrough.sdk.mediation.ICreative;

import java.util.List;

public class Creative implements ICreative {
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
    private String networkType;
    private String className;

    public Creative(Response.Creative responseCreative, String mediationRequestId) {
        this.responseCreative = responseCreative;
        // To remove for asap v2
        this.responseCreative.mediationRequestId = mediationRequestId;
    }

    @Override
    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    @Override
    public String getNetworkType() {
        return networkType;
    }

    @Override
    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getClassName() {
        return className;
    }

    public static String serialize(ICreative creative) {
        Gson gson = new Gson();
        return gson.toJson(creative);
    }

    public static ICreative deserialize(String serializedCreative) {
        Gson gson = new Gson();
        return ((Creative)gson.fromJson(serializedCreative, Creative.class));
    }

    public String getTitle() {
        return returnEmptyIfNull(responseCreative.creative.title);
    }

    public String getAdvertiser() {
        return returnEmptyIfNull(responseCreative.creative.advertiser);
    }

    public String getDescription() {
        return returnEmptyIfNull(responseCreative.creative.description);
    }

    public String getThumbnailUrl() {
        return returnEmptyIfNull(convertToAbsoluteUrl(responseCreative.creative.thumbnail_url));
    }

    public String getBrandLogoUrl() {
        return returnEmptyIfNull(convertToAbsoluteUrl(responseCreative.creative.brand_logo_url));
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
        return returnEmptyIfNull(responseCreative.adserverRequestId);
    }

    public String getMediationRequestId() {
        return returnEmptyIfNull(responseCreative.mediationRequestId);
    }

    public String getAuctionWinId() {
        return returnEmptyIfNull(responseCreative.auctionWinId);
    }

    public String getShareUrl() {
        return returnEmptyIfNull(convertToAbsoluteUrl(responseCreative.creative.share_url));
    }

    public String getMediaUrl() {
        return returnEmptyIfNull(convertToAbsoluteUrl(responseCreative.creative.media_url));
    }

    public String getVariantKey() {
        return responseCreative.creative.variant_key;
    }

    public String getCreativeKey() {
        return returnEmptyIfNull(responseCreative.creative.creative_key);
    }

    public String getCampaignKey() {
        return returnEmptyIfNull(responseCreative.creative.campaign_key);
    }

    public String getCustomEngagementUrl() {
        return returnEmptyIfNull(convertToAbsoluteUrl(responseCreative.creative.custom_engagement_url));
    }

    public String getCustomEngagementLabel() {
        return responseCreative.creative.custom_engagement_label;
    }

    public List<String> getClickBeacons() {
        return responseCreative.creative.beacons.click;
    }

    public List<String> getPlayBeacons() {
        return responseCreative.creative.beacons.play;
    }

    public List<String> getVisibleBeacons() {
        return responseCreative.creative.beacons.visible;
    }

    public List<String> getImpressionBeacons() { return responseCreative.creative.beacons.impression; }

    public List<String> getSilentPlayBeacons() { return responseCreative.creative.beacons.silent_play; }

    public List<String> getTenSecondSilentPlayBeacons() { return responseCreative.creative.beacons.ten_second_silent_play; }

    public List<String> getFifteenSecondSilentPlayBeacons() { return responseCreative.creative.beacons.fifteen_second_silent_play; }

    public List<String> getThirtySecondSilentPlayBeacons() { return responseCreative.creative.beacons.thirty_second_silent_play; }

    public List<String> getCompletedSilentPlayBeacons() { return responseCreative.creative.beacons.completed_silent_play; }

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

    public String getOptOutText() {
        return "";
    }

    public String getOptOutUrl() {
        return "";
    }

    public String getDealId(){
        return returnEmptyIfNull(responseCreative.creative.deal_id);
    }

    public String getSlug() {
        return returnEmptyIfNull(responseCreative.creative.custom_set_promoted_by_text);
    }

    private String returnEmptyIfNull(String value) {
        if (value == null) {
            return "";
        } else {
            return value;
        }
    }
}
