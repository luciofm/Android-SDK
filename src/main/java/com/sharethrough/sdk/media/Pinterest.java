package com.sharethrough.sdk.media;

import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.dialogs.PinterestDialog;

public class Pinterest extends Clickout {
    public Pinterest(Creative creative) {
        super(creative);
    }

    @Override
    public int getOverlayImageResourceId() {
        return R.drawable.pinterest;
    }

    @Override
    public void wasClicked(View v, BeaconService beaconService) {
        new PinterestDialog(v.getContext(), getCreative(), beaconService).show();
    }
}
