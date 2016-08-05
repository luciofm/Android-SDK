package com.sharethrough.sdk.mediation;

import com.facebook.ads.Ad;

/**
 * Created by engineer on 8/3/16.
 */
public class FacebookAd implements ICreative {
    private Ad fbAd;

    @Override
    public String getNetworkType() {
        return MediationManager.FAN_NETWORK;
    }

    public FacebookAd(Ad ad) {
        this.fbAd = ad;
    }

    public Ad getFbAd() {
        return fbAd;
    }
}
