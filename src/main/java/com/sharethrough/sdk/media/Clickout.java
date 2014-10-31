package com.sharethrough.sdk.media;

import android.view.View;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.dialogs.WebViewDialog;

public class Clickout implements Creative.Media {
    private final Creative creative;

    public Clickout(Creative creative) {
        this.creative = creative;
    }

    @Override
    public void overlayThumbnail(IAdView adView) {
    }

    @Override
    public View.OnClickListener getClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new WebViewDialog(v.getContext(), creative).show();
            }
        };
    }
}
