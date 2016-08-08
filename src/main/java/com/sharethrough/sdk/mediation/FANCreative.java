package com.sharethrough.sdk.mediation;

import com.facebook.ads.Ad;

/**
 * Created by engineer on 8/3/16.
 */
public class FANCreative implements ICreative {
    private Ad fbAd;
    private String networkType;

    @Override
    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    @Override
    public String getNetworkType() {
        return networkType;
    }

    public FANCreative(Ad ad) {
        this.fbAd = ad;
    }

    public Ad getFbAd() {
        return fbAd;
    }
}
