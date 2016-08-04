package com.sharethrough.sdk.mediation;

import android.content.Context;
import com.sharethrough.sdk.network.ASAPManager;

import java.util.Map;

/**
 * Created by danicashei on 7/28/16.
 */
public interface STRMediationAdapter {
    void loadAd(Context context, MediationManager.MediationListener mediationListener, ASAPManager.AdResponse adResponse, ASAPManager.AdResponse.Network network);
}
