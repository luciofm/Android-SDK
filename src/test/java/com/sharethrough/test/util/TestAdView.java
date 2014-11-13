package com.sharethrough.test.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.sharethrough.sdk.IAdView;

public abstract class TestAdView extends FrameLayout implements IAdView {
    public TestAdView(Context context) {
        super(context);
    }

    public TestAdView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TestAdView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public ViewGroup getAdView() {
        return this;
    }
}
