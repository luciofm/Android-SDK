package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.sharethrough.sdk.media.Media;

import java.util.Timer;
import java.util.TimerTask;

public class Renderer<V extends View & IAdView> {

    private final Timer timer;

    public Renderer(Timer timer) {
        this.timer = timer;
    }

    public void putCreativeIntoAdView(final V adView, final Creative creative, final BeaconService beaconService, final Sharethrough sharethrough) {
        if (!creative.wasRendered) {
            beaconService.adReceived(adView.getContext(), creative);
            creative.wasRendered = true;
        }
        final Handler handler = new Handler(Looper.getMainLooper());

        final TimerTask task = new AdViewTimerTask(adView, creative, beaconService, new DateProvider(), sharethrough);
        timer.schedule(task, 0, 100);

        handler.post(new Runnable() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
            @Override
            public void run() {
                adView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

                    @Override
                    public void onViewAttachedToWindow(View v) {
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        task.cancel();
                        timer.purge(); // TODO: test
                        adView.removeOnAttachStateChangeListener(this);
                    }
                });

                adView.getTitle().setText((creative).getTitle());
                TextView description = adView.getDescription();
                if (description != null) {
                    description.setText(creative.getDescription());
                }
                adView.getAdvertiser().setText(creative.getAdvertiser());

                FrameLayout thumbnail = adView.getThumbnail();
                Context context = adView.getContext();

                ImageView thumbnailImage = new ImageView(context);
                Bitmap thumbnailBitmap = creative.getThumbnailImage();
                thumbnailImage.setImageBitmap(thumbnailBitmap);
                thumbnailImage.setScaleType(ImageView.ScaleType.FIT_START);
                thumbnail.addView(thumbnailImage,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                final Media media = creative.getMedia();
                media.overlayThumbnail(adView);
                adView.setOnClickListener(new View.OnClickListener() {
                                              @Override
                                              public void onClick(View v) {
                                                  media.fireAdClickBeacon(creative, adView);
                                                  media.getClickListener().onClick(v);
                                              }
                                          }
                );
            }
        });
    }
}
