package com.sharethrough.test.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.sharethrough.sdk.IAdView;

public abstract class AdView extends View implements IAdView {
    public AdView(Context context) {
        super(context);
    }

    public AdView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
