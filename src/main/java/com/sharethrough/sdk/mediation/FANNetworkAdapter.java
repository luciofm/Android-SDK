package com.sharethrough.sdk.mediation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.facebook.ads.*;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.Logger;
import com.sharethrough.sdk.mediation.FANCreative;
import com.sharethrough.sdk.mediation.ICreative;
import com.sharethrough.sdk.mediation.MediationManager;
import com.sharethrough.sdk.mediation.STRMediationAdapter;
import com.sharethrough.sdk.network.ASAPManager;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class FANNetworkAdapter implements STRMediationAdapter {
    private NativeAd nativeAd = null;

    @Override
    public void loadAd(final Context context, final MediationManager.MediationListener mediationListener, final ASAPManager.AdResponse adResponse, final ASAPManager.AdResponse.Network network) {
        nativeAd = new NativeAd(context, "548597075312947_565374090301912");
        AdSettings.addTestDevice("2b96b2c92445c088a7d6b2f12aef1f93");
        nativeAd.setAdListener(new AdListener() {

            @Override
            public void onError(Ad ad, AdError error) {
                int i = 4;
            }

            @Override
            public void onAdLoaded(Ad ad) {
                if (nativeAd.equals(ad)) {
                    Logger.d("Facebook return 1 creative");
                    List<ICreative> creatives = new ArrayList<>();
                    FANCreative convertedFbAd = new FANCreative(nativeAd);
                    convertedFbAd.setNetworkType(network.name);
                    creatives.add(convertedFbAd);
                    mediationListener.onAdLoaded(creatives);
                    return;
                }
            }

            @Override
            public void onAdClicked(Ad ad) {

            }
        });

        nativeAd.loadAd();
    }

    @Override
    public void render(final IAdView adview, final ICreative creative, int feedPosition) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final ViewGroup container = adview.getAdView();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if( nativeAd.isAdLoaded()){
                    FANCreative fanCreative = (FANCreative) creative;
                    adview.getTitle().setText(nativeAd.getAdTitle());
                    adview.getDescription().setText(nativeAd.getAdSubtitle());
                    adview.adReady();
                }
            }
        });


    }
}
