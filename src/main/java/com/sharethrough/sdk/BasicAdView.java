package com.sharethrough.sdk;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

public class BasicAdView extends FrameLayout implements IAdView {
    private int titleViewId;
    private int descriptionViewId;
    private int advertiserViewId;
    private int thumbnailViewId;

    public BasicAdView(Context context) {
        super(context);
    }

    public BasicAdView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasicAdView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public BasicAdView showAd(Sharethrough sharethrough, final Context context, final int layoutResourceId, final int titleViewId, final int descriptionViewId, final int advertiserViewId, final int thumbnailViewId) {
        this.titleViewId = titleViewId;
        this.descriptionViewId = descriptionViewId;
        this.advertiserViewId = advertiserViewId;
        this.thumbnailViewId = thumbnailViewId;
        addView(LayoutInflater.from(context).inflate(layoutResourceId, this, false));
        sharethrough.putCreativeIntoAdView(this);
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
