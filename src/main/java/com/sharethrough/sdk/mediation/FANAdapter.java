package com.sharethrough.sdk.mediation;

import java.util.Map;

import android.content.Context;
import com.facebook.ads.*;


/**
 * Created by danicashei on 8/1/16.
 */
public class FANAdapter implements STRMediationAdapter {
    public static String FAN_PLACEMENT_ID = "FAN_PLACEMENT_ID";

    @Override
    public void loadAd(Context context, final MediationManager.MediationListener mediationListener, Map<String, String> extras) {
        final NativeAd fbAd = new NativeAd(context, extras.get(FAN_PLACEMENT_ID));


        ImpressionListener fbImpressionListener = new ImpressionListener() {
            @Override
            public void onLoggingImpression(Ad ad) {

            }
        };

        AdListener fbAdListener = new AdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                // This identity check is from Facebook's Native API sample code:
                // https://developers.facebook.com/docs/audience-network/android/native-api
                if (!fbAd.equals(ad)) {
                    mediationListener.onAdFailedToLoad();
                    return;
                }

            }

            @Override
            public void onAdLoaded(Ad ad) {
                mediationListener.onAdLoaded();
            }

            @Override
            public void onAdClicked(Ad ad) {

            }
        };

        fbAd.setAdListener(fbAdListener);
        fbAd.setImpressionListener(fbImpressionListener);
        fbAd.loadAd();
    }






}
