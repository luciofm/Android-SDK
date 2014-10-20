package com.sharethrough.sdk;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Creative {
    private final Response.Creative responseCreative;
    private final byte[] imageBytes;


    public Creative(Response.Creative responseCreative, byte[] imageBytes) {
        this.responseCreative = responseCreative;
        this.imageBytes = imageBytes;
    }

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

    private void showFullscreen(Context context) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        dialog.setContentView(linearLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(context);
        title.setText(this.getTitle());
        linearLayout.addView(title);

        TextView description = new TextView(context);
        description.setText(this.getDescription());
        linearLayout.addView(description);

        TextView advertiser = new TextView(context);
        advertiser.setText(this.getAdvertiser());
        linearLayout.addView(advertiser);

        ImageView thumbnail = new ImageView(context);
        thumbnail.setImageBitmap(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
        linearLayout.addView(thumbnail);

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
}
