package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.media.Media;
import com.squareup.picasso.Picasso;

import java.util.Timer;

public class Renderer {

    public void putCreativeIntoAdView(final IAdView adView, final Creative creative, final BeaconService beaconService,
    final Sharethrough sharethrough, final Timer timer) {
        putCreativeIntoAdView(adView, creative, beaconService, sharethrough, 0, timer);
    }

    public void putCreativeIntoAdView(final IAdView adView, final Creative creative, final BeaconService beaconService,
                                      final Sharethrough sharethrough, final int feedPosition, final Timer timer) {
        final ViewGroup container = adView.getAdView();
        if (!creative.wasRendered) {
            beaconService.adReceived(container.getContext(), creative, feedPosition, sharethrough.placement);
            creative.wasRendered = true;
        }
        final Handler handler = new Handler(Looper.getMainLooper());

        container.setTag(creative);

        handler.post(new Runnable() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
            @Override
            public void run() {
                if (container.getTag() != creative) return; // container has been recycled
                adView.adReady();

                adView.getTitle().setText(creative.getTitle());
                TextView description = adView.getDescription();
                if (description != null) {
                    description.setText(creative.getDescription());
                }

                adView.getAdvertiser().setText(creative.getAdvertiser());

                ImageView brandLogoView = adView.getBrandLogo();
                if (brandLogoView != null ){
                    if(creative.getBrandLogoUrl() != null && false == creative.getBrandLogoUrl().isEmpty()) {
                        Picasso.with(container.getContext()).load(creative.getBrandLogoUrl()).fit().centerCrop().tag("STRBrandLogo").into(brandLogoView);
                        brandLogoView.setVisibility(View.VISIBLE);
                    }else{
                        brandLogoView.setVisibility(View.GONE);
                    }
                }

                FrameLayout thumbnailContainer = adView.getThumbnail();
                thumbnailContainer.removeAllViews();
                final AdImageView thumbnailImage = new AdImageView(container.getContext(), sharethrough, creative, adView, feedPosition, beaconService);
                Picasso.with(container.getContext()).load(creative.getThumbnailUrl()).fit().centerCrop().tag("STRAdImage").into(thumbnailImage);
                sharethrough.fetchAdsIfReadyForMore();
                thumbnailContainer.addView(thumbnailImage,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));

                final Media media = creative.getMedia();
                handler.post(new Runnable() { // give thumbnailImage a chance to render so we can use its size
                    @Override
                    public void run() {
                        media.overlayThumbnail(adView, thumbnailImage);
                    }
                });
                container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        media.fireAdClickBeaconOnFirstClick(creative, adView, beaconService, feedPosition, sharethrough.placement);
                        media.wasClicked(v, beaconService, feedPosition);
                    }
                  }
                );

                placeOptoutIcon(adView);
            }
        });
    }

    private void placeOptoutIcon(final IAdView adView) {
        final ImageView optout = adView.getOptout();
        optout.setImageResource(R.drawable.optout);
        optout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent privacyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Sharethrough.PRIVACY_POLICY_ENDPOINT));
                v.getContext().startActivity(privacyIntent);
            }
        });

        optout.setMinimumHeight(20);
        optout.setMinimumWidth(20);
        optout.setVisibility(View.VISIBLE);
        optout.bringToFront();
    }
}
