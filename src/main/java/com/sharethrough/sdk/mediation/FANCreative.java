package com.sharethrough.sdk.mediation;

import com.facebook.ads.Ad;

/**
 * Created by engineer on 8/3/16.
 */
public class FANCreative extends ICreative {
    private Ad fbAd;

    public FANCreative(String networkType, String className, String mrid) {
        super(networkType, className, mrid);
    }
    public void setFbAd(Ad ad) {
        this.fbAd = ad;
    }

    public Ad getFbAd() {
        return fbAd;
    }

    @Override
    public String getNetworkType() {
        return networkType;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public void setPlacementIndex(int placementIndex) {
        this.placementIndex = placementIndex;
    }

    @Override
    public int getPlacementIndex() {
        return placementIndex;
    }
}
