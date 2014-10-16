package com.sharethrough.sdk;

import android.widget.ImageView;
import android.widget.TextView;

public interface IAdView {
    TextView getTitle();
    TextView getDescription();
    TextView getPromotedBy();
    ImageView getThumbnail();
}
