package com.sharethrough.sdk;

import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public interface IAdView {
    void adReady();

    /**
     * @return The <code>TextView</code> into which the title text should be rendered. May not be null.
     */
    TextView getTitle();

    /**
     * @return The <codeText>View</code> into which the description text should be rendered. May be null.
     */
    TextView getDescription();

    /**
     * @return The <code>TextView</code> into which the advertiser/sponsor text should be rendered. May not be null.
     */
    TextView getAdvertiser();

    /**
     * @return The <code>FrameLayout</code> into which the image should be rendered. May not be null.
     */
    FrameLayout getThumbnail();

    /**
     * @return The <code>ImageView</code> into which the brand logo should be rendered. May be null.
     */
    ImageView getBrandLogo();

    /**
     * @return The <code>ImageView</code> into which the optout icon should be rendered. May not be null.
     */
    ImageView getOptout();

    /**
     * @return The <code>View</code> containing the contents of the ad.  Normally your <code>IAdView</code>
     * implementation will be a subclass of <code>ViewGroup</code> and you should return <code>this</code>.
     */
    ViewGroup getAdView();
}
