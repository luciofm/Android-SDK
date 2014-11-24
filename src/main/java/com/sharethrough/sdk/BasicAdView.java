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
    private ProgressBar spinner;

    public BasicAdView(Context context) {
        this(context, null, 0);
    }

    public BasicAdView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BasicAdView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     *
     * @param layoutResourceId The custom layout for Sharethrough's native ad unit.
     * @param titleViewId The view which will display the ad's title.
     * @param advertiserViewId The view which will display the ad's advertiser.
     * @param thumbnailViewId The view which will display the ad's thumbnail image.
     */
    public void prepareWithResourceIds(final int layoutResourceId, final int titleViewId, final int advertiserViewId, final int thumbnailViewId) {
        prepareWithResourceIds(layoutResourceId, titleViewId, -1, advertiserViewId, thumbnailViewId);
    }

    /**
     *
     * @param layoutResourceId The custom layout for Sharethrough's native ad unit.
     * @param titleViewId The view which will display the ad's title.
     * @param descriptionViewId The view which will display the ad's description.
     * @param advertiserViewId The view which will display the ad's advertiser.
     * @param thumbnailViewId The view which will display the ad's thumbnail image.
     */
    public void prepareWithResourceIds(final int layoutResourceId, final int titleViewId, final int descriptionViewId, final int advertiserViewId, final int thumbnailViewId) {
        this.titleViewId = titleViewId;
        this.descriptionViewId = descriptionViewId;
        this.advertiserViewId = advertiserViewId;
        this.thumbnailViewId = thumbnailViewId;
        createChildren(layoutResourceId);
    }

    private void createChildren(int layoutResourceId) {
        spinner = new ProgressBar(getContext());
        addView(spinner, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        view = LayoutInflater.from(getContext()).inflate(layoutResourceId, this, false);
        addView(view);
        view.setVisibility(GONE);
    }

    @Override
    public void adReady() {
        spinner.setVisibility(GONE);
        view.setVisibility(VISIBLE);
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

    @Override
    public ViewGroup getAdView() {
        return this;
    }
}
