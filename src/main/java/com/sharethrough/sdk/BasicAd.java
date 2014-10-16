package com.sharethrough.sdk;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

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

    public void showAd(int layoutResourceId, int titleViewId, int descriptionViewId, int advertiserViewId) {
        Sharethrough sharethrough = new Sharethrough("abc");// TODO

        View view = LayoutInflater.from(getContext()).inflate(layoutResourceId, this);
        Response.Creative creative = sharethrough.getCreative();
        if (null != creative) {
            ((TextView)view.findViewById(titleViewId)).setText(creative.creative.title);
            ((TextView)view.findViewById(descriptionViewId)).setText(creative.creative.description);
            ((TextView)view.findViewById(advertiserViewId)).setText(creative.creative.advertiser);
        }
    }
}
