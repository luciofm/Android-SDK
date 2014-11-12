package com.sharethrough.sdk;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class BasicAdView extends FrameLayout implements IAdView {
    private int titleViewId;
    private int descriptionViewId;
    private int advertiserViewId;
    private int thumbnailViewId;
    private View view;

    public BasicAdView(Context context) {
        super(context);
    }

    public BasicAdView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasicAdView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public BasicAdView showAd(Sharethrough sharethrough, final Context context, final int layoutResourceId, final int titleViewId, final int advertiserViewId, final int thumbnailViewId) {
        return showAd(sharethrough, context, layoutResourceId, titleViewId, -1, advertiserViewId, thumbnailViewId);
    }

    public BasicAdView showAd(Sharethrough sharethrough, final Context context, final int layoutResourceId, final int titleViewId, final int descriptionViewId, final int advertiserViewId, final int thumbnailViewId) {
        this.titleViewId = titleViewId;
        this.descriptionViewId = descriptionViewId;
        this.advertiserViewId = advertiserViewId;
        this.thumbnailViewId = thumbnailViewId;
        view = LayoutInflater.from(context).inflate(layoutResourceId, this, false);
        addView(new ProgressBar(getContext()), new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        sharethrough.putCreativeIntoAdView(this, new Runnable() {
            @Override
            public void run() {
                addView(view);
            }
        });
        return this;
    }

    @Override
    public TextView getTitle() {
        return (TextView) this.findViewById(titleViewId);
    }

    @Override
    public TextView getDescription() {
        return (TextView) this.findViewById(descriptionViewId);
    }

    @Override
    public TextView getAdvertiser() {
        return (TextView) this.findViewById(advertiserViewId);
    }

    @Override
    public FrameLayout getThumbnail() {
        return (FrameLayout) this.findViewById(thumbnailViewId);
    }
}
