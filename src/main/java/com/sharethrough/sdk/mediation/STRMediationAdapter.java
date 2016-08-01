package com.sharethrough.sdk.mediation;

import java.util.Map;

/**
 * Created by danicashei on 7/28/16.
 */
public interface STRMediationAdapter {
    void loadAd(MediationManager.MediationListener mediationListener, Map<String,String> extras);
}
