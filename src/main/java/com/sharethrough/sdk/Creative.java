package com.sharethrough.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import com.sharethrough.android.sdk.R;

import java.util.concurrent.ExecutorService;

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
                ImageView thumbnailImage = new ImageView(adView.getThumbnail().getContext());
                thumbnailImage.setImageBitmap(Creative.this.getThumbnailImage(((View) adView).getContext()));
                adView.getThumbnail().addView(thumbnailImage);

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

    public Bitmap getThumbnailImage(Context context) {
        Bitmap result = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length).copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Bitmap youtubeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.youtube_squared);
        canvas.drawBitmap(youtubeIcon, new Matrix(), null);
        return result;
    }

    public Creative.Media getMedia() {
        return new Youtube(responseCreative.creative.mediaUrl);
    }

    public String getShareUrl() {
        return responseCreative.creative.shareUrl;
    }

    public interface Media {
        void doWithMediaUrl(ExecutorService executorService, Function<String, Void> mediaUrlHandler);
    }
}
