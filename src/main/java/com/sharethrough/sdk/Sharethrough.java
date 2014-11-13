package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import com.sharethrough.sdk.network.AdFetcher;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Sharethrough<V extends View & IAdView> {
    public static final int DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(20);
    private static final int MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(20);
    public static String TRACKING_URL = "http://b.sharethrough.com/butler";
    private final Renderer renderer;
    private final BeaconService beaconService;
    private final int adCacheTimeInMilliseconds;
    private String apiUrlPrefix = "http://btlr.sharethrough.com/v3?placement_key=";
    static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(4); // TODO: pick a reasonable number
    private List<Creative> availableCreatives = Collections.synchronizedList(new ArrayList<Creative>());
    private Map<V, Runnable> waitingAdViews = Collections.synchronizedMap(new LinkedHashMap<V, Runnable>());

    public Sharethrough(Context context, String key) {
        this(context, key, DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS);
    }

    public Sharethrough(Context context, String key, int adCacheTimeInMilliseconds) {
        this(context, EXECUTOR_SERVICE, key, new Renderer(new Timer("Sharethrough visibility watcher")), adCacheTimeInMilliseconds,
                new BeaconService(new DateProvider(), UUID.randomUUID(), EXECUTOR_SERVICE, new AdvertisingIdProvider(context, EXECUTOR_SERVICE, UUID.randomUUID().toString())));
    }

    Sharethrough(final Context context, final ExecutorService executorService, final String key, final Renderer renderer, int adCacheTimeInMilliseconds, final BeaconService beaconService) {
        this.beaconService = beaconService;
        this.adCacheTimeInMilliseconds = Math.max(adCacheTimeInMilliseconds, MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS);
        if (key == null) throw new KeyRequiredException("placement_key is required");
        this.renderer = renderer;

        try {
            Bundle bundle = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            if (bundle != null) {
                String adserverApi = bundle.getString("STR_ADSERVER_API");
                if (adserverApi != null) apiUrlPrefix = adserverApi;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        new AdFetcher(context, key, apiUrlPrefix, executorService, new Function<Creative, Void>() {
            @Override
            public Void apply(Creative creative) {
                synchronized (waitingAdViews) {
                    if (waitingAdViews.size() > 0) {
                        Map.Entry<V, Runnable> waiting = waitingAdViews.entrySet().iterator().next();
                        V adView = waiting.getKey();
                        waitingAdViews.remove(adView);
                        renderer.putCreativeIntoAdView(adView, creative, beaconService, Sharethrough.this, waiting.getValue());
                    } else {
                        availableCreatives.add(creative);
                    }
                }

                return null;
            }
        }, beaconService).fetch();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void putCreativeIntoAdView(V adView, Runnable adReadyCallback) {
        synchronized (availableCreatives) {
            if (availableCreatives.size() > 0) {
                renderer.putCreativeIntoAdView(adView, availableCreatives.remove(0), beaconService, this, adReadyCallback);
            } else {
                waitingAdViews.put(adView, adReadyCallback);
            }
        }
    }

    public int getAdCacheTimeInMilliseconds() {
        return adCacheTimeInMilliseconds;
    }
}
