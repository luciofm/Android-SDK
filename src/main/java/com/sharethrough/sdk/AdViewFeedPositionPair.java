package com.sharethrough.sdk;

public class AdViewFeedPositionPair<X, Y> {
    public final X adView;
    public final Y feedPosition;
    public AdViewFeedPositionPair(X adView, Y feedPosition) {
        this.adView = adView;
        this.feedPosition = feedPosition;
    }
}
