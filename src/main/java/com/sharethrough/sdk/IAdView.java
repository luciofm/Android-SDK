package com.sharethrough.sdk;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.TextView;

public interface IAdView {
    TextView getTitle();
    TextView getDescription();
    TextView getAdvertiser();
    FrameLayout getThumbnail();

    Context getContext();
    int getHeight();
    int getWidth();
}
