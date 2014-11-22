package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
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

                Log.d("MEMORY", adView.hashCode() + "/" + creative + " 000");
                final View.OnAttachStateChangeListener onAttachStateChangeListener1 = new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        Log.d("MEMORY", adView.hashCode() + "/" + creative + " child attached");
                        timer.schedule(task, 0, 100);
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        Log.d("MEMORY", adView.hashCode() + "/" + creative + " child detached");
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
                thumbnailContainer.removeAllViews();
                Context context = container.getContext();

                final ImageView thumbnailImage = new ImageView(context);
                Bitmap thumbnailBitmap = creative.makeThumbnailImage();
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
            }
        });
    }
}
