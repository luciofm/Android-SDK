package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import com.sharethrough.sdk.network.AdFetcher;
import com.sharethrough.sdk.network.DFPNetworking;
import com.sharethrough.sdk.network.ImageFetcher;
import com.sharethrough.sdk.network.PlacementFetcher;

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
    private final PlacementFetcher placementFetcher;
    private final int adCacheTimeInMilliseconds;
    private final String placementKey;
    private String apiUrl;
    private String apiUrlPrefix = "http://btlr.sharethrough.com/v3?placement_key=";
    static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(4); // TODO: pick a reasonable number
    private final CreativesQueue availableCreatives;
    private final Map<IAdView, Runnable> waitingAdViews = Collections.synchronizedMap(new LinkedHashMap<IAdView, Runnable>());
    private final Function<Creative, Void> creativeHandler;
    private AdFetcher adFetcher;
    private ImageFetcher imageFetcher;
    private AdFetcher.Callback adFetcherCallback;
    private OnStatusChangeListener onStatusChangeListener = new OnStatusChangeListener() {
        @Override
        public void newAdsToShow() {

        }

        @Override
        public void noAdsToShow() {

        }
    };
    private Handler handler = new Handler(Looper.getMainLooper());
    private LruCache<Integer, BasicAdView> adViewsByAdSlot;

    public Sharethrough(Context context, String placementKey) {
        this(context, placementKey, DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS);
    }

    public Sharethrough(Context context, String placementKey, int adCacheTimeInMilliseconds) {
        this(context, placementKey, adCacheTimeInMilliseconds, new AdvertisingIdProvider(context, EXECUTOR_SERVICE, UUID.randomUUID().toString()));
    }

    Sharethrough(Context context, String placementKey, int adCacheTimeInMilliseconds, AdvertisingIdProvider advertisingIdProvider) {
        this(context, placementKey, adCacheTimeInMilliseconds, new Renderer(new Timer("Sharethrough visibility watcher")),
                new BeaconService(new DateProvider(), UUID.randomUUID(), EXECUTOR_SERVICE, advertisingIdProvider),
                new AdFetcher(context, placementKey, EXECUTOR_SERVICE, new BeaconService(new DateProvider(), UUID.randomUUID(),
                        EXECUTOR_SERVICE, advertisingIdProvider)), new ImageFetcher(EXECUTOR_SERVICE, placementKey),
                new CreativesQueue(),
                new PlacementFetcher(placementKey, EXECUTOR_SERVICE), false);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    Sharethrough(final Context context, final String placementKey, int adCacheTimeInMilliseconds, final Renderer renderer, final BeaconService beaconService, AdFetcher adFetcher, ImageFetcher imageFetcher, final CreativesQueue availableCreatives, PlacementFetcher placementFetcher, boolean dfpMode) {
        this.renderer = renderer;
        this.beaconService = beaconService;
        this.placementFetcher = placementFetcher;
        this.adCacheTimeInMilliseconds = Math.max(adCacheTimeInMilliseconds, MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS);
        this.availableCreatives = availableCreatives;
        this.placementKey = placementKey;

        adViewsByAdSlot = new LruCache<>(20);

        if (placementKey == null) throw new KeyRequiredException("placement_key is required");

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
                    if (waitingAdViews.size() > 0) {
                        Map.Entry<IAdView, Runnable> waiting = waitingAdViews.entrySet().iterator().next();
                        IAdView adView = waiting.getKey();
                        waitingAdViews.remove(adView);
                        renderer.putCreativeIntoAdView(adView, creative, beaconService, Sharethrough.this, waiting.getValue());
                    } else {
                        Sharethrough.this.availableCreatives.add(creative);
                        fireNewAdsToShow();
                    }
                }

                return null;
            }
        };
        adFetcherCallback = new AdFetcher.Callback() {
            @Override
            public void finishedLoading() {
                if (availableCreatives.readyForMore()) {
                    fetchAds();
                }
            }

            @Override
            public void finishedLoadingWithNoAds() {
                fireNoAdsToShow();
            }
        };
        this.adFetcher = adFetcher;
        this.imageFetcher = imageFetcher;

        if (dfpMode) {
            fetchDFPUrl(placementKey);
        } else {
            apiUrl = apiUrlPrefix + placementKey;
            fetchAds();
        }
    }

    //No tests around this
    private void fetchDFPUrl(String key) {
        DFPNetworking.FetchDFPEndpoint(EXECUTOR_SERVICE, key, new DFPNetworking.DFPFetcherCallback() {
            @Override
            public void receivedURL(String url) {
                Log.d("DFP", "received DFP url: " + url);
                apiUrl = url;
                fetchAds();
            }

            @Override
            public void DFPError(String errorMessage) {
                Log.e("DFP", "error getting DFP url: " + errorMessage);
            }
        });
    }

    private void fireNoAdsToShow() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                onStatusChangeListener.noAdsToShow();
            }
        });
    }

    private void fireNewAdsToShow() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                onStatusChangeListener.newAdsToShow();
            }
        });
    }

    private void fetchAds() {
        this.adFetcher.fetchAds(this.imageFetcher, apiUrl, creativeHandler, adFetcherCallback);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void putCreativeIntoAdView(IAdView adView, Runnable adReadyCallback) {
        synchronized (availableCreatives) {
            Creative nextCreative = availableCreatives.getNext();
            if (nextCreative != null) {
                renderer.putCreativeIntoAdView(adView, nextCreative, beaconService, this, adReadyCallback);
            } else {
                waitingAdViews.put(adView, adReadyCallback);
            }
            if (availableCreatives.readyForMore()) {
                fetchAds();
            }
        }
    }

    public int getAdCacheTimeInMilliseconds() {
        return adCacheTimeInMilliseconds;
    }

    public void getPlacement(Callback<Placement> placementCallback) {
        placementFetcher.fetch(placementCallback);
    }

    public void setOnStatusChangeListener(OnStatusChangeListener onStatusChangeListener) {
        this.onStatusChangeListener = onStatusChangeListener;
    }

    public OnStatusChangeListener getOnStatusChangeListener() {
        return onStatusChangeListener;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public BasicAdView getAdView(Context context, int position, int adLayoutResourceId, int title, int description, int advertiser, int thumbnail) {
        BasicAdView cachedView = adViewsByAdSlot.get(position);
        if (cachedView != null) {
            return cachedView;
        } else {
            BasicAdView basicAdView = new BasicAdView(context);
            adViewsByAdSlot.put(position, basicAdView);
            return basicAdView.showAd(this, adLayoutResourceId, title, description, advertiser, thumbnail);
        }
    }

    public interface OnStatusChangeListener {
        void newAdsToShow();

        void noAdsToShow();
    }
}
