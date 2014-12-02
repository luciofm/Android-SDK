package com.sharethrough.sdk;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * This class implements IAdView and extends FrameLayout. This view contains everything needed to display a Sharethrough ad.
 */
public class BasicAdView extends FrameLayout implements IAdView {
    private int titleViewId;
    private int descriptionViewId;
    private int advertiserViewId;
    private int thumbnailViewId;
    private int optoutViewId;
    private int brandLogoId;
    private View view;
    private ProgressBar spinner;

    /**
     * Constructor for creating a BasicAdView.
     *
     * @param context The Android context.
     */
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
     * This method passes in layouts and resource ids to be used during inflation and playing the BasicAdView into the view hierarchy.
     *
     * @param layoutResourceId The custom layout for Sharethrough's native ad unit.
     * @param titleViewId The view which will display the ad's title.
     * @param descriptionViewId The view which will display the ad's description. This field can be -1
     * @param brandLogoId The view which will display the brand logo (if available). This field can be -1
     * @param advertiserViewId The view which will display the ad's advertiser.
     * @param thumbnailViewId The view which will display the ad's thumbnail image.
     */
    public void prepareWithResourceIds(final int layoutResourceId, final int titleViewId, final int descriptionViewId, final int advertiserViewId, final int thumbnailViewId, final int optoutViewId, final int brandLogoId) {
        this.titleViewId = titleViewId;
        this.descriptionViewId = descriptionViewId;
        this.advertiserViewId = advertiserViewId;
        this.thumbnailViewId = thumbnailViewId;
        this.optoutViewId = optoutViewId;
        this.brandLogoId = brandLogoId;
        checkResources();
        createChildren(layoutResourceId);
    }

    private void checkResources() {
        assert (titleViewId > 0);
        assert (optoutViewId > 0);
        assert (advertiserViewId > 0);
        assert (thumbnailViewId > 0);
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

    @Override
    public ImageView getOptout() {
        return (ImageView) this.findViewById(optoutViewId);
    }

    @Override
    public ImageView getBrandLogo() {
        return (ImageView) this.findViewById(brandLogoId);
    }
}
