package com.sharethrough.sdk;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import com.google.ads.AdSize;
import com.google.ads.mediation.MediationAdRequest;
import com.google.ads.mediation.customevent.CustomEventBanner;
import com.google.ads.mediation.customevent.CustomEventBannerListener;

import java.util.Set;


public class STRDFPMediator implements CustomEventBanner, com.google.android.gms.ads.mediation.customevent.CustomEventBanner {

    //TOP methods are for 5.+
    @Override
    public void requestBannerAd(CustomEventBannerListener customEventBannerListener,
                                Activity activity,
                                String tag,
                                String serverParameter,
                                AdSize adSize,
                                MediationAdRequest mediationAdRequest,
                                Object o) {
        Set<String> keywords = mediationAdRequest.getKeywords();
        Sharethrough.addCreativeKey(keywords.iterator().next(), serverParameter);
        customEventBannerListener.onReceivedAd(new View(activity));
    }

    @Override
    public void destroy() {

    }

    //BOTTOM methods are for 6.+
    @Override
    public void requestBannerAd(Context context,
                                com.google.android.gms.ads.mediation.customevent.CustomEventBannerListener customEventBannerListener,
                                String serverParameter,
                                com.google.android.gms.ads.AdSize adSize,
                                com.google.android.gms.ads.mediation.MediationAdRequest mediationAdRequest,
                                Bundle bundle) {
        Set<String> keywords = mediationAdRequest.getKeywords();
        Sharethrough.addCreativeKey(keywords.iterator().next(), serverParameter);
        customEventBannerListener.onAdLoaded(new View(context));
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }
}
