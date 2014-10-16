package com.sharethrough.sdk;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class BasicAd extends FrameLayout {
    public BasicAd(Context context) {
        super(context);
    }

    public BasicAd(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasicAd(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public <V extends View & IAdView> void showAd(V adView) {
        addView(adView);
//        getAd(new AdGetter() {
//            @Override
//            public void gotAd(Ad ad) {
//
//            }
//        });
//
//        adView.getTitle().setText(ad.getTitle());
    }

}
