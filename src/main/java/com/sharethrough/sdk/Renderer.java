package com.sharethrough.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class Renderer {
    public void putCreativeIntoAdView(final IAdView adView, final Creative creative) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // TODO: check that the AdView is attached to the window & avoid memory leaks
//              adView.addOnAttachStateChangeListener(null);
                adView.getTitle().setText((creative).getTitle());
                adView.getDescription().setText(creative.getDescription());
                adView.getAdvertiser().setText(creative.getAdvertiser());

                FrameLayout thumbnail = adView.getThumbnail();
                Context context = adView.getContext();

                ImageView thumbnailImage = new ImageView(context);
                Bitmap thumbnailBitmap = creative.getThumbnailImage();
                thumbnailImage.setImageBitmap(thumbnailBitmap);
                thumbnailImage.setScaleType(ImageView.ScaleType.FIT_START);
                thumbnail.addView(thumbnailImage,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                final Creative.Media media = creative.getMedia();
                media.overlayThumbnail(adView);
                ((View) adView).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        media.fireAdClickBeacon(creative, adView);
                        media.getClickListener().onClick(v);
                    }
                });
            }
        });
    }

}
