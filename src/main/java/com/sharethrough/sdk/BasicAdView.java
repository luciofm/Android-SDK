package com.sharethrough.sdk;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class BasicAdView extends FrameLayout implements IAdView {
    public BasicAdView(Context context) {
        super(context);
    }

    public BasicAdView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasicAdView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public static BasicAdView showAd(Sharethrough sharethrough, final Context context, final int layoutResourceId, final int titleViewId, final int descriptionViewId, final int advertiserViewId) {
        BasicAdView basicAdView = new BasicAdView(context) {
            View view = LayoutInflater.from(context).inflate(layoutResourceId, this, true);

            @Override
            public TextView getTitle() {
                return (TextView) view.findViewById(titleViewId);
            }

            @Override
            public TextView getDescription() {
                return (TextView) view.findViewById(descriptionViewId);
            }

            @Override
            public TextView getAdvertiser() {
                return (TextView) view.findViewById(advertiserViewId);
            }

            @Override
            public ImageView getThumbnail() {
                return null;//b(ImageView) view.findViewById(thumbnailViewId);
            }
        };
        sharethrough.putCreativeIntoAdView(basicAdView);
        return basicAdView;
    }
}
