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
import com.sharethrough.sdk.network.ASAPManager;

import java.util.ArrayList;
import java.util.List;

public class FANNetworkAdapter implements STRMediationAdapter {
    private NativeAd nativeAd = null;

    @Override
    public void loadAd(final Context context, final MediationManager.MediationListener mediationListener, final ASAPManager.AdResponse adResponse, final ASAPManager.AdResponse.Network network) {
        nativeAd = new NativeAd(context, "548597075312947_565374090301912");
        AdSettings.addTestDevice("3e41895b1482f5e3fd3d8e7ef60e7970");
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

                    //setting ui element
                    final FrameLayout thumbnailContainer = adview.getThumbnail();
                    MediaView mediaView = new MediaView(thumbnailContainer.getContext());
                    mediaView.setNativeAd(nativeAd);
                    ImageView nativeAdIcon = adview.getBrandLogo();
                    TextView nativeAdTitle = adview.getTitle();
                    TextView nativeAdBody = adview.getDescription();
                    ImageView optoutIcon = adview.getOptout();


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
}
