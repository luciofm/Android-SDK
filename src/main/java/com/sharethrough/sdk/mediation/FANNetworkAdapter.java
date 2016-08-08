package com.sharethrough.sdk.mediation;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import com.facebook.ads.*;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.Logger;
import com.sharethrough.sdk.network.ASAPManager;


/**
 * Created by danicashei on 8/1/16.
 */
public class FANNetworkAdapter implements STRMediationAdapter {
    public static String FAN_PLACEMENT_ID = "fanPlacementId";

    @Override
    public void loadAd(Context context, final MediationManager.MediationListener mediationListener, final ASAPManager.AdResponse adResponse, final ASAPManager.AdResponse.Network network) {
        String fanPlacementId = network.parameters.get(FAN_PLACEMENT_ID).getAsString();
        final NativeAd fbAd = new NativeAd(context, "548597075312947_565374090301912");


        ImpressionListener fbImpressionListener = new ImpressionListener() {
            @Override
            public void onLoggingImpression(Ad ad) {

            }
        };

        AdListener fbAdListener = new AdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                Logger.d("Facebook ads failed to load");
                mediationListener.onAdFailedToLoad();

            }

            @Override
            public void onAdLoaded(Ad ad) {
                // This identity check is from Facebook's Native API sample code:
                // https://developers.facebook.com/docs/audience-network/android/native-api
                if (fbAd.equals(ad)) {
                    Logger.d("Facebook returned 1 creative");
                    List<ICreative> creatives = new ArrayList<ICreative>();
                    FANCreative convertedFbAd = new FANCreative(fbAd);
                    convertedFbAd.setNetworkType(network.name);
                    creatives.add(convertedFbAd);
                    mediationListener.onAdLoaded(creatives);
                    return;
                }
            }

            @Override
            public void onAdClicked(Ad ad) {
                System.out.println("Facebook ad clicked");
            }
        };

        fbAd.setAdListener(fbAdListener);
        fbAd.setImpressionListener(fbImpressionListener);
//        AdSettings.addTestDevice("76de221f26624cfaa2d4c382cf7e6f8a");
        AdSettings.addTestDevice("152459e6948206aad3bb3a37f88b7b8d");
        fbAd.loadAd();
    }

    @Override
    public void render(IAdView adview, ICreative creative, int feedPosition) {

    }


}
