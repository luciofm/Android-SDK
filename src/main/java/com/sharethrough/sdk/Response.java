package com.sharethrough.sdk;

import java.util.List;

public class Response {
    @JsonProperty("placement")
    public Placement placement;

    public static class Placement {
        @JsonProperty("layout")
        public String layout;
        @JsonProperty("articlesBeforeFirstAd")
        public int articlesBeforeFirstAd;
        @JsonProperty("articlesBetweenAds")
        public int articlesBetweenAds;
        @JsonProperty("status")
        public String status;
        @JsonProperty("allowInstantPlay")
        public Boolean allowInstantPlay;
    }

    @JsonProperty("creatives")
    public List<Creative> creatives;

    public static class Creative {
        @JsonProperty("adserverRequestId")
        public String adserverRequestId;
        @JsonProperty("auctionWinId")
        public String auctionWinId;
        @JsonProperty("price")
        public Integer price;
        @JsonProperty("signature")
        public String signature;

        @JsonProperty("creative")
        public CreativeInner creative;

        @JsonProperty("priceType")
        public String priceType;
        @JsonProperty("version")
        public int version;

        public static class CreativeInner {
            @JsonProperty("opt_out_text")
            public String optOutText;
            @JsonProperty("creative_key")
            public String creativeKey;
            @JsonProperty("campaign_key")
            public String campaignKey;
            @JsonProperty("description")
            public String description;
            @JsonProperty("opt_out_url")
            public String optOutUrl;
            @JsonProperty("media_url")
            public String mediaUrl;
            @JsonProperty("share_url")
            public String brandLogoUrl;
            @JsonProperty("brand_logo_url")
            public String shareUrl;
            @JsonProperty("variant_key")
            public String variantKey;
            @JsonProperty("advertiser")
            public String advertiser;
            @JsonProperty("beacons")
            public Beacon beacon;
            @JsonProperty("thumbnail_url")
            public String thumbnailUrl;
            @JsonProperty("title")
            public String title;
            @JsonProperty("action")
            public String action;
            public String customEngagementUrl;
            @JsonProperty("custom_engagement_label")
            public String customEngagementLabel;
            @JsonProperty("deal_id")
            public String dealId;
            @JsonProperty("force_click_to_play")
            public Boolean forceClickToPlay;

            public static class Beacon {
                @JsonProperty("impression")
                public List<String> impression;
                @JsonProperty("visible")
                public List<String> visible;
                @JsonProperty("click")
                public List<String> click;
                @JsonProperty("play")
                public List<String> play;
                @JsonProperty("silent_play")
                public List<String> silentPlay;
                @JsonProperty("ten_second_silent_play")
                public List<String> tenSecondSilentPlay;
                @JsonProperty("fifteen_second_silent_play")
                public List<String> fifteenSecondSilentPlay;
                @JsonProperty("thirty_second_silent_play")
                public List<String> thirtySecondSilentPlay;
                @JsonProperty("completed_silent_play")
                public List<String> completedSilentPlay;
            }
        }
    }
}
