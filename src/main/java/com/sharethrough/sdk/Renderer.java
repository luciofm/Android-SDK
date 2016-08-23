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
import com.sharethrough.sdk.beacons.VideoCompletionBeaconService;
import com.sharethrough.sdk.media.*;
import com.sharethrough.sdk.mediation.IRenderer;
import com.squareup.picasso.Picasso;


import java.util.Timer;

public class Renderer implements IRenderer {

    public void putCreativeIntoAdView(final IAdView adView, final Creative creative, final BeaconService beaconService, final Timer timer) {
        putCreativeIntoAdView(adView, creative, beaconService, 0, timer);
    }

    public void putCreativeIntoAdView(final IAdView adView, final Creative creative, final BeaconService beaconService,
                                      final int feedPosition, final Timer timer) {
        final ViewGroup container = adView.getAdView();
        if (!creative.wasRendered) {
            beaconService.adReceived(container.getContext(), creative, feedPosition);
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
                    if(creative.getBrandLogoUrl() != null && !creative.getBrandLogoUrl().isEmpty()) {
                        Picasso.with(container.getContext()).load(creative.getBrandLogoUrl()).fit().centerCrop().tag("STRBrandLogo").into(brandLogoView);
                        brandLogoView.setVisibility(View.VISIBLE);
                    }else{
                        brandLogoView.setVisibility(View.GONE);
                    }
                }

                FrameLayout thumbnailContainer = adView.getThumbnail();

                thumbnailContainer.removeAllViews();
                final AdImageView thumbnailImage = new AdImageView(container.getContext(), creative, adView, feedPosition, beaconService);
                if (creative.getThumbnailUrl() != null && !creative.getThumbnailUrl().isEmpty())
                    Picasso.with(container.getContext()).load(creative.getThumbnailUrl()).fit().centerCrop().tag("STRAdImage").into(thumbnailImage);
                thumbnailContainer.addView(thumbnailImage,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
                final Media media = createMedia(adView, creative, beaconService, feedPosition);
                handler.post(new Runnable() { // give thumbnailImage a chance to render so we can use its size
                    @Override
                    public void run() {
                        media.wasRendered(adView, thumbnailImage);
                    }
                });
                container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        media.fireAdClickBeaconOnFirstClick(creative, adView, beaconService, feedPosition);
                        media.wasClicked(v, beaconService, feedPosition);
                    }
                  }
                );

                placeOptoutIcon(adView, creative.getOptOutUrl(), creative.getOptOutText());
            }
        });
    }

    protected Media createMedia(IAdView adview, Creative creative, BeaconService beaconService, int feedPosition) {
        if (creative.getType().equals(Creative.CreativeType.YOUTUBE)) {
            return new Youtube(creative);
        } else if (creative.getType().equals(Creative.CreativeType.VINE)) {
            return new Vine(creative);
        } else if (creative.getType().equals(Creative.CreativeType.HOSTEDVIDEO)) {
            if (creative instanceof InstantPlayCreative) {
                VideoCompletionBeaconService videoCompletionBeaconService = new VideoCompletionBeaconService(adview.getAdView().getContext(), creative, beaconService, feedPosition);
                return new InstantPlayVideo(creative, beaconService, videoCompletionBeaconService, feedPosition);
            }
            return new HostedVideo(creative);
        } else if (creative.getType().equals(Creative.CreativeType.INSTAGRAM)) {
            return new Instagram(creative);
        } else if (creative.getType().equals(Creative.CreativeType.PINTEREST)) {
            return new Pinterest(creative);
        } else if (creative.getType().equals(Creative.CreativeType.ARTICLE)) {
            return new Article(creative);
        } else {
            return new Clickout(creative);
        }
    }

    private void placeOptoutIcon(final IAdView adView, final String optOutUrl, final String optOutText) {
        final ImageView optout = adView.getOptout();
        optout.setImageResource(R.drawable.optout);
        optout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String privacyPolicyUrl = Sharethrough.PRIVACY_POLICY_ENDPOINT;
                if(optOutText != null && !optOutText.isEmpty() && optOutUrl != null && !optOutUrl.isEmpty()) {
                    privacyPolicyUrl = privacyPolicyUrl.replace("{OPT_OUT_URL}", Uri.encode(optOutUrl));
                    privacyPolicyUrl = privacyPolicyUrl.replace("{OPT_OUT_TEXT}",Uri.encode(optOutText));
                }else{
                    privacyPolicyUrl = privacyPolicyUrl.replace("?opt_out_url={OPT_OUT_URL}&opt_out_text={OPT_OUT_TEXT}", "");
                }
                Intent privacyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
                v.getContext().startActivity(privacyIntent);
            }
        });

        optout.setMinimumHeight(20);
        optout.setMinimumWidth(20);
        optout.setVisibility(View.VISIBLE);
        optout.bringToFront();
    }
}
