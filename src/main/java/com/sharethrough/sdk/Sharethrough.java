package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.sharethrough.sdk.network.AdFetcher;
import com.sharethrough.sdk.network.ImageFetcher;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Sharethrough {
    public static final int DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(20);
    private static final int MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(20);
    public static final String USER_AGENT = System.getProperty("http.agent");
    private final Renderer renderer;
    private final BeaconService beaconService;
    private final int adCacheTimeInMilliseconds;
    private String apiUrlPrefix = "http://btlr.sharethrough.com/v3?placement_key=";
    static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(4); // TODO: pick a reasonable number
    private final CreativesQueue availableCreatives;
    private final Map<IAdView, Runnable> waitingAdViews = Collections.synchronizedMap(new LinkedHashMap<IAdView, Runnable>());
    private final Function<Creative, Void> creativeHandler;
    private AdFetcher adFetcher;
    private ImageFetcher imageFetcher;

    public Sharethrough(Context context, String key) {
        this(context, key, DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS);
    }

    public Sharethrough(Context context, String key, int adCacheTimeInMilliseconds) {
        this(context, key, adCacheTimeInMilliseconds, new AdvertisingIdProvider(context, EXECUTOR_SERVICE, UUID.randomUUID().toString()));
    }

    Sharethrough(Context context, String key, int adCacheTimeInMilliseconds, AdvertisingIdProvider advertisingIdProvider) {
        this(context, key, adCacheTimeInMilliseconds, new Renderer(new Timer("Sharethrough visibility watcher")),
                new BeaconService(new DateProvider(), UUID.randomUUID(), EXECUTOR_SERVICE, advertisingIdProvider),
                new AdFetcher(context, key, EXECUTOR_SERVICE, new BeaconService(new DateProvider(), UUID.randomUUID(),
                        EXECUTOR_SERVICE, advertisingIdProvider)), new ImageFetcher(EXECUTOR_SERVICE, key),
                new CreativesQueue()
        );
    }

    Sharethrough(final Context context, final String key, int adCacheTimeInMilliseconds, final Renderer renderer, final BeaconService beaconService, AdFetcher adFetcher, ImageFetcher imageFetcher, CreativesQueue availableCreatives) {
        this.renderer = renderer;
        this.beaconService = beaconService;
        this.adCacheTimeInMilliseconds = Math.max(adCacheTimeInMilliseconds, MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS);
        this.availableCreatives = availableCreatives;
        if (key == null) throw new KeyRequiredException("placement_key is required");

        try {
            Bundle bundle = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            if (bundle != null) {
                String adServerApi = bundle.getString("STR_ADSERVER_API");
                if (adServerApi != null) apiUrlPrefix = adServerApi;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        creativeHandler = new Function<Creative, Void>() {
            @Override
            public Void apply(Creative creative) {
                synchronized (waitingAdViews) {
                    Log.i("DEBUG", "Got creative (waitingAdViews = " + waitingAdViews.size() + ")");
                    if (waitingAdViews.size() > 0) {
                        Map.Entry<IAdView, Runnable> waiting = waitingAdViews.entrySet().iterator().next();
                        IAdView adView = waiting.getKey();
                        waitingAdViews.remove(adView);
                        renderer.putCreativeIntoAdView(adView, creative, beaconService, Sharethrough.this, waiting.getValue());
                    } else {
                        Sharethrough.this.availableCreatives.add(creative);
                    }
                }

                return null;
            }
        };
        this.adFetcher = adFetcher;
        this.imageFetcher = imageFetcher;
        this.adFetcher.fetchAds(this.imageFetcher, apiUrlPrefix, creativeHandler);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void putCreativeIntoAdView(IAdView adView, Runnable adReadyCallback) {
        synchronized (availableCreatives) {
            Creative next = availableCreatives.getNext();
            if (next != null) {
                renderer.putCreativeIntoAdView(adView, next, beaconService, this, adReadyCallback);
            } else {
                waitingAdViews.put(adView, adReadyCallback);
                adFetcher.fetchAds(imageFetcher, apiUrlPrefix, creativeHandler);
            }
        }
    }

    public int getAdCacheTimeInMilliseconds() {
        return adCacheTimeInMilliseconds;
    }
}
