package com.sharethrough.sdk;

import android.content.Context;
import android.util.LruCache;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.sharethrough.sdk.mediation.MediationManager;
import com.sharethrough.sdk.network.ASAPManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class STRSdkConfig {
    private String placementKey;
    private String serializedSharethrough;
    private BeaconService beaconService;
    private CreativesQueue creativeQueue;
    private LruCache<Integer, Creative> creativesBySlot;
    private Set<Integer> creativeIndices;
    private AdvertisingIdProvider advertisingIdProvider;
    private RequestQueue requestQueue;
    private ASAPManager asapManager;
    private MediationManager mediationManager;

    public Context context;

    public STRSdkConfig(Context context, String placementKey) {
        this.context = context;
        Logger.setContext(context);
        Logger.enabled = true;

        this.placementKey = placementKey;
        this.advertisingIdProvider = new AdvertisingIdProvider(context);
        this.beaconService = new BeaconService(new DateProvider(), UUID.randomUUID(), advertisingIdProvider, context, placementKey);
        this.creativeQueue = new CreativesQueue();
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        this.asapManager = new ASAPManager(placementKey, requestQueue);
        this.creativesBySlot = new LruCache<>(10);
        this.creativeIndices = new HashSet<>(); //contains history of all indices for creatives, whereas creativesBySlot only caches the last 10
        this.mediationManager = new MediationManager(context, beaconService);
    }

    public void setSerializedSharethrough(String serializedSharethrough) {
        this.serializedSharethrough = serializedSharethrough;
        this.creativeQueue = SharethroughSerializer.getCreativesQueue(serializedSharethrough);
        this.creativesBySlot = SharethroughSerializer.getSlot(serializedSharethrough);
    }

    public String getSerializedSharethrough() {
        return serializedSharethrough;
    }

    public BeaconService getBeaconService() {
        return beaconService;
    }

    public CreativesQueue getCreativeQueue() {
        return creativeQueue;
    }

    public LruCache<Integer, Creative> getCreativesBySlot() {
        return creativesBySlot;
    }

    public Set<Integer> getCreativeIndices() {
        return creativeIndices;
    }

    public AdvertisingIdProvider getAdvertisingIdProvider() {
        return advertisingIdProvider;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public ASAPManager getAsapManager() {
        return asapManager;
    }

    public MediationManager getMediationManager() {
        return mediationManager;
    }
}
