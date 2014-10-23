package com.sharethrough.sdk;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.VideoView;

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
                adView.getThumbnail().setImageBitmap(Creative.this.getThumbnailImage());

                ((View) adView).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showFullscreen(v.getContext());
                    }
                });
            }
        });
    }

    private void showFullscreen(final Context context) {
        Dialog dialog = new YoutubeDialog(context, this, Sharethrough.EXECUTOR_SERVICE, new Provider<VideoView>() {
            @Override
            public VideoView get() {
                return new VideoView(context);
            }
        });
        dialog.show();
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
        void doWithMediaUrl(ExecutorService executorService, Function<String, Void> mediaUrlHandler);
    }
}
