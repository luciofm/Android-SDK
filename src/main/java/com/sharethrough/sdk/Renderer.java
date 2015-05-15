package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.media.Media;

import java.util.Timer;
import java.util.TimerTask;

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

        final TimerTask task = new AdViewTimerTask(adView, feedPosition, creative, beaconService, new DateProvider(), sharethrough);

        container.setTag(creative);

        handler.post(new Runnable() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
            @Override
            public void run() {
                if (container.getTag() != creative) return; // container has been recycled
                adView.adReady();

                final View.OnAttachStateChangeListener onAttachStateChangeListener1 = new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        timer.schedule(task, 0, 100);
                        Log.d("jermaine Renderer", "attaching to window");
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        Log.d("jermaine Renderer", "DE-ttaching to window");
                        task.cancel();
                        timer.cancel();
                        timer.purge();
                        v.removeOnAttachStateChangeListener(this);
                    }
                };

                adView.getTitle().setText(creative.getTitle());
                TextView description = adView.getDescription();
                if (description != null) {
                    description.setText(creative.getDescription());
                }

                ImageView brandLogoView = adView.getBrandLogo();
                if (brandLogoView != null) {
                    Bitmap logoBitmap = creative.makeBrandLogo();
                    if (logoBitmap != null) {
                        brandLogoView.setImageBitmap(logoBitmap);
                        brandLogoView.setVisibility(View.VISIBLE);
                    } else {
                        brandLogoView.setVisibility(View.GONE);
                    }
                }

                adView.getAdvertiser().setText(creative.getAdvertiser());

                FrameLayout thumbnailContainer = adView.getThumbnail();

                int height = thumbnailContainer.getHeight();
                int width = thumbnailContainer.getWidth();

                final Bitmap thumbnailBitmap;
                if (height > 0 && width > 0) {
                    thumbnailBitmap = creative.makeThumbnailImage(height, width);
                } else {
                    thumbnailBitmap = creative.makeThumbnailImage();
                }

                thumbnailContainer.removeAllViews();
                Context context = container.getContext();

                final ImageView thumbnailImage = new ImageView(context);
                thumbnailImage.setImageBitmap(thumbnailBitmap);
                thumbnailImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                thumbnailImage.addOnAttachStateChangeListener(onAttachStateChangeListener1);
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
