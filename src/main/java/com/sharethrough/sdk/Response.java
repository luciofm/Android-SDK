package com.sharethrough.sdk;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

public class Response {
    @JsonProperty("creatives")
    public List<Creative> creatives;

    public static class Creative {
        @JsonProperty("price")
        public Integer price;
        @JsonProperty("signature")
        public String signature;

        @JsonProperty("creative")
        public CreativeInner creative;

        @JsonProperty("priceType")
        public String priceType; // TODO: enum?
        @JsonProperty("version")
        public int version;


        public static class CreativeInner {
            @JsonProperty("creative_key")
            public String key;
            @JsonProperty("description")
            public String description;
            @JsonProperty("media_url")
            public String mediaUrl;
            @JsonProperty("share_url")
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

            public static class Beacon {
                @JsonProperty("visible")
                public List<String> visible;
                @JsonProperty("click")
                public List<String> click;
                @JsonProperty("play")
                public List<String> play;
            }
        }
    }

}
