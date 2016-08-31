package com.sharethrough.sdk;

import java.util.ArrayList;

public class Response {
    public Placement placement;
    public ArrayList<Creative> creatives;
    public String adserverRequestId;

    public static class Placement {
        public int articlesBeforeFirstAd;
        public int articlesBetweenAds;
        public String status;
        public Boolean allowInstantPlay;
        public PlacementAttributes placementAttributes;

        public static class PlacementAttributes {
            public String promotedByText;
            public String directSellPromotedByText;
        }
    }

    public static class Creative {
        public String adserverRequestId; /** not in actual stxMediation response */
        public String mediationRequestId; /** not in actual stxMediation response */

        public String auctionWinId;
        public int version;
        public CreativeInner creative;

        public static class CreativeInner {
            public String creative_key;
            public String campaign_key;
            public String description;
            public String media_url;
            public String brand_logo_url;
            public String share_url;
            public String variant_key;
            public String advertiser;
            public String thumbnail_url;
            public String title;
            public String action;
            public String custom_engagement_url;
            public String custom_engagement_label;
            public String deal_id;
            public Boolean force_click_to_play;
            public String promoted_by_text;
            public String custom_set_promoted_by_text; /** not in actual stxMediation response */

            public Beacon beacons;
            public static class Beacon {
                public ArrayList<String> impression;
                public ArrayList<String> visible;
                public ArrayList<String> click;
                public ArrayList<String> play;
                public ArrayList<String> silent_play;
                public ArrayList<String> ten_second_silent_play;
                public ArrayList<String> fifteen_second_silent_play;
                public ArrayList<String> thirty_second_silent_play;
                public ArrayList<String> completed_silent_play;
            }
        }
    }
}
