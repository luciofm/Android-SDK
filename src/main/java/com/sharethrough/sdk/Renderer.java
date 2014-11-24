package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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

import java.util.Timer;
import java.util.TimerTask;

public class Renderer {
    public void putCreativeIntoAdView(final IAdView adView, final Creative creative, final BeaconService beaconService,
                                      final Sharethrough sharethrough, final Timer timer) {
        final ViewGroup container = adView.getAdView();
        if (!creative.wasRendered) {
            beaconService.adReceived(container.getContext(), creative);
            creative.wasRendered = true;
        }
        final Handler handler = new Handler(Looper.getMainLooper());

        final TimerTask task = new AdViewTimerTask(adView, creative, beaconService, new DateProvider(), sharethrough);

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
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
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
                adView.getAdvertiser().setText(creative.getAdvertiser());

                FrameLayout thumbnailContainer = adView.getThumbnail();

                int height = thumbnailContainer.getHeight();
                int width = thumbnailContainer.getWidth();

                Bitmap thumbnailBitmap;
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
                        media.fireAdClickBeacon(creative, adView, beaconService);
                        media.wasClicked(v, beaconService);
                    }
                  }
                );

                placeOptoutIcon(container);
            }
        });
    }

    private void placeOptoutIcon(final ViewGroup container) {
        View oldIcon = container.findViewWithTag("SHARETHROUGH PRIVACY INFORMATION");
        if (oldIcon != null) {
            container.removeView(oldIcon);
        }

        final ImageView optout = new ImageView(container.getContext());
        optout.setImageResource(R.drawable.optout);
        optout.setTag("SHARETHROUGH PRIVACY INFORMATION");
        optout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent privacyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.sharethrough.com/privacy-policy/"));
                v.getContext().startActivity(privacyIntent);
            }
        });
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                int size = Math.min(container.getHeight(), container.getWidth()) / 6;
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(size, size, Gravity.BOTTOM | Gravity.RIGHT);
                optout.setPadding(0, 0, size / 3, size / 3);
                container.addView(optout, layoutParams);
            }
        });
    }
}
