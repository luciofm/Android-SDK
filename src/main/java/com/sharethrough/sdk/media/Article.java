package com.sharethrough.sdk.media;

import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.Placement;

public class Article extends Clickout {
    public Article(Creative creative) {
        super(creative);
    }

    public void fireAdClickBeacon(Creative creative, IAdView adView, BeaconService beaconService, int feedPosition, Placement placement) {
        beaconService.adClicked("articleView", creative, adView.getAdView(), feedPosition, placement);
    }
}
