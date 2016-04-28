package com.sharethrough.sdk;

import android.content.Context;
import com.sharethrough.sdk.network.AdManager;

import java.util.UUID;

public class STRSdkConfig {
    protected String placementKey;
    protected String serializedSharethrough;
    protected BeaconService beaconService;
    protected AdManager adManager;
    protected Renderer renderer;
    protected CreativesQueue creativeQueue;
    protected AdvertisingIdProvider advertisingIdProvider;

    public STRSdkConfig(Context context, String placementKey) throws Exception {

        if (placementKey == null || placementKey.isEmpty()) {
            throw new Exception("Placement key is required");
        }

        if (context == null){
            throw new Exception("Context is required");
        }

        Logger.setContext(context);
        Logger.enabled = true;

        this.placementKey = placementKey;
        this.advertisingIdProvider = new AdvertisingIdProvider(context);
        this.beaconService = new BeaconService(new DateProvider(), UUID.randomUUID(), advertisingIdProvider, context, placementKey);
        this.adManager = new AdManager(context);
        this.renderer = new Renderer();
        this.creativeQueue = new CreativesQueue();
    }

    public void setSerializedSharethrough( String serializedSharethrough ){
        this.serializedSharethrough = serializedSharethrough;
    }
}
