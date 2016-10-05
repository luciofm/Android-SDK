package com.sharethrough.sdk;

import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Interface that defines methods used to construct a Sharethrough ad unit.
 */
public interface IAdView {
    /**
     * This method is called when the ad is ready to be displayed.
     */
    void adReady();

    /**
     * @return The <code>TextView</code> into which the slug text should be rendered. May not be null.
     */
    TextView getSlug();

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

    /**
     * @return The visibility listener that listens for onScreen and offScreen events
     */
    ScreenVisibilityListener getScreenVisibilityListener();

    /**
     * Sets the visibility listener that listens for onScreen and offScreen events
     * @param screenListener
     */
    void setScreenVisibilityListener(ScreenVisibilityListener screenListener);

    //when ad is visible this is called via the timertask
    void onScreen();

    //when ad is not visible this is called via the timertask
    void offScreen();

    interface ScreenVisibilityListener{
        public void onScreen();
        public void offScreen();
    }
}
