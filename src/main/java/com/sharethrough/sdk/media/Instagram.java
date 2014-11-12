package com.sharethrough.sdk.media;

import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.Creative;

public class Instagram extends Clickout {
    public Instagram(Creative creative) {
        super(creative);
    }

    @Override
    public int getOverlayImageResourceId() {
        return R.drawable.instagram;
    }
}
