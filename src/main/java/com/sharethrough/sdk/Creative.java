package com.sharethrough.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.sharethrough.android.sdk.R;

public class Creative {
    private final Response.Creative responseCreative;
    private final byte[] imageBytes;

    public Creative(Response.Creative responseCreative, byte[] imageBytes) {
        this.responseCreative = responseCreative;
        this.imageBytes = imageBytes;
    }

    // TODO: move this somewhere else
    public void putIntoAdView(final IAdView adView) {
        // TODO: check that the AdView is attached to the window & avoid memory leaks
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
//        adView.addOnAttachStateChangeListener(null);
                adView.getTitle().setText((Creative.this).getTitle());
                adView.getDescription().setText(Creative.this.getDescription());
                adView.getAdvertiser().setText(Creative.this.getAdvertiser());

                FrameLayout thumbnail = adView.getThumbnail();
                Context context = thumbnail.getContext();

                ImageView thumbnailImage = new ImageView(context);
                Bitmap thumbnailBitmap = Creative.this.getThumbnailImage();
                thumbnailImage.setImageBitmap(thumbnailBitmap);
                thumbnailImage.setScaleType(ImageView.ScaleType.FIT_START);
                thumbnail.addView(thumbnailImage,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                ImageView youtubeIcon = new ImageView(context);
                youtubeIcon.setImageResource(R.drawable.youtube_squared);
                youtubeIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                int overlayDimensionMax = Math.min(thumbnailBitmap.getWidth(), thumbnailBitmap.getHeight()) / 4;
                thumbnail.addView(youtubeIcon,
                        new FrameLayout.LayoutParams(overlayDimensionMax, overlayDimensionMax, Gravity.TOP | Gravity.LEFT));

                ((View) adView).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new YoutubeDialog(v.getContext(), Creative.this).show();
                    }
                });
            }
        });
    }

    public String getTitle() {
        return responseCreative.creative.title;
    }

    public String getAdvertiser() {
        return responseCreative.creative.advertiser;
    }

    public String getDescription() {
        return responseCreative.creative.description;
    }

    public Bitmap getThumbnailImage() {
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public Creative.Media getMedia() {
        return new Youtube(responseCreative.creative.mediaUrl);
    }

    public String getShareUrl() {
        return responseCreative.creative.shareUrl;
    }

    public interface Media {
    }
}
