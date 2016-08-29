package com.sharethrough.sdk.mediation;

import android.content.Context;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.network.ASAPManager;

/**
 * Created by danicashei on 8/29/16.
 */
public class DummyMediationAdapter implements STRMediationAdapter {
    @Override
    public void loadAd(Context context, MediationManager.MediationListener mediationListener, ASAPManager.AdResponse adResponse, ASAPManager.AdResponse.Network network) {

    }

    @Override
    public void render(IAdView adview, ICreative creative, int feedPosition) {

    }
}
