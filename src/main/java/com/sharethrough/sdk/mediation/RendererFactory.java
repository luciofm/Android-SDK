package com.sharethrough.sdk.mediation;

import com.sharethrough.sdk.*;

import java.util.Timer;

/**
 * Created by danicashei on 8/3/16.
 */
public class RendererFactory {
    public void render(MediationManager manager, IAdView adView, ICreative creative, BeaconService beaconService, Sharethrough sharethrough, int feedPosition) {
        String type = creative.getNetworkType();
        IRenderer renderer = manager.getRenderer(type);

        if (type.equals(ICreative.FacebookAd)) {
            ((Renderer )renderer).putCreativeIntoAdView(adView, ((Creative)creative), beaconService, sharethrough, feedPosition, new Timer("AdView timer for " + creative));
        } else if (type.equals(ICreative.STRAd)) {

        }

    }

}
