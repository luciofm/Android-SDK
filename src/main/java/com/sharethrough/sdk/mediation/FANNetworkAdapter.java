package com.sharethrough.sdk.mediation;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.ads.*;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.Logger;
import com.sharethrough.sdk.Placement;
import com.sharethrough.sdk.network.ASAPManager;

import java.util.ArrayList;
import java.util.List;

public class FANNetworkAdapter implements STRMediationAdapter {
    private NativeAd nativeAd = null;
    private static String FAN_PLACEMENT_ID = "fanPlacementId";

    @Override
    public void loadAd(final Context context, final MediationManager.MediationListener mediationListener, final ASAPManager.AdResponse adResponse, final ASAPManager.AdResponse.Network network) {
        if (!networkHasValidParams(network)) {
            mediationListener.onAdFailedToLoad();
        };

        String facebookPlacementId = network.parameters.get(FAN_PLACEMENT_ID).getAsString();
        nativeAd = new NativeAd(context, facebookPlacementId);
//        AdSettings.addTestDevice("ebe0abdb73271a5598f9a0b4f6308ff1");
        nativeAd.setAdListener(new AdListener() {

            @Override
            public void onError(Ad ad, AdError error) {
                Logger.d("Facebook returned 0 creatives");
                mediationListener.onAdFailedToLoad();
            }

            @Override
            public void onAdLoaded(Ad ad) {
                if (nativeAd.equals(ad)) {
                    Logger.d("Facebook returned 1 creative");
                    List<ICreative> creatives = new ArrayList<>();
                    FANCreative convertedFbAd = new FANCreative(nativeAd);
                    convertedFbAd.setNetworkType(network.name);
                    convertedFbAd.setClassName(network.androidClassName);
                    creatives.add(convertedFbAd);
                    mediationListener.onAdLoaded(creatives, null);
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

                    //setting ui element
                    final FrameLayout thumbnailContainer = adview.getThumbnail();
                    MediaView mediaView = new MediaView(thumbnailContainer.getContext());
                    mediaView.setNativeAd(nativeAd);
                    ImageView nativeAdIcon = adview.getBrandLogo();
                    TextView nativeAdTitle = adview.getTitle();
                    TextView nativeAdBody = adview.getDescription();
                    ImageView optoutIcon = adview.getOptout();
                    TextView slug = adview.getSlug();
                    slug.setText("Promoted");


                    //filling in data
                    thumbnailContainer.addView(mediaView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
                    nativeAdTitle.setText(nativeAd.getAdTitle());
                    nativeAdBody.setText(nativeAd.getAdBody());

                    //set Ad icon
                    NativeAd.Image adIcon = nativeAd.getAdIcon();
                    NativeAd.downloadAndDisplayImage(adIcon, nativeAdIcon);

                    NativeAd.Image adChoices = nativeAd.getAdChoicesIcon();
                    NativeAd.downloadAndDisplayImage(adChoices, optoutIcon);

                    nativeAd.registerViewForInteraction(adview.getAdView());

                    optoutIcon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (nativeAd.getAdChoicesLinkUrl() != null && false == nativeAd.getAdChoicesLinkUrl().isEmpty()) {
                                Intent adChoicesIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(nativeAd.getAdChoicesLinkUrl()));
                                v.getContext().startActivity(adChoicesIntent);
                            }
                        }
                    });

                    adview.adReady();
                }
            }
        });


    }

    private boolean networkHasValidParams(ASAPManager.AdResponse.Network network) {
        if (network == null || network.parameters == null || network.parameters.get(FAN_PLACEMENT_ID) == null) {
            return false;
        }
        return true;
    }
}
