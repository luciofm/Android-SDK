package com.sharethrough.sdk.mediation;

import java.util.Map;

import android.content.Context;
import com.facebook.ads.*;
import com.sharethrough.sdk.Logger;


/**
 * Created by danicashei on 8/1/16.
 */
public class FANAdapter implements STRMediationAdapter {
    public static String FAN_PLACEMENT_ID = "FAN_PLACEMENT_ID";

    @Override
    public void loadAd(Context context, final MediationManager.MediationListener mediationListener, Map<String, String> extras) {
        final NativeAd fbAd = new NativeAd(context, "548597075312947_565374090301912");


        ImpressionListener fbImpressionListener = new ImpressionListener() {
            @Override
            public void onLoggingImpression(Ad ad) {

            }
        };

        AdListener fbAdListener = new AdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                mediationListener.onAdFailedToLoad();
                Logger.d("Facebook ads failed to load");

            }

            @Override
            public void onAdLoaded(Ad ad) {
                // This identity check is from Facebook's Native API sample code:
                // https://developers.facebook.com/docs/audience-network/android/native-api
                if (fbAd.equals(ad)) {
                    Logger.d("Facebook ad loaded");
                    mediationListener.onAdLoaded(fbAd);
                    return;
                }
            }

            @Override
            public void onAdClicked(Ad ad) {
                System.out.println("ad clicked");
            }
        };

        fbAd.setAdListener(fbAdListener);
        fbAd.setImpressionListener(fbImpressionListener);
        AdSettings.addTestDevice("76de221f26624cfaa2d4c382cf7e6f8a");

        Logger.d("Mediating Facebook");
        fbAd.loadAd();
    }






}
