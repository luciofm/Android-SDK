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
import java.util.concurrent.TimeUnit;

public class Sharethrough {
    public static final int DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(20);
    private static final int MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(20);
    public static final String USER_AGENT = System.getProperty("http.agent");
    private final Renderer renderer;
    private final BeaconService beaconService;
    private final PlacementFetcher placementFetcher;
    private final int adCacheTimeInMilliseconds;
    private final DFPNetworking dfpNetworking;
    private String placementKey;
    private String apiUrlPrefix = "http://btlr.sharethrough.com/v3?placement_key=";
    private String dfpApiUrlPrefix = "&creative_key=";
    static final ExecutorService EXECUTOR_SERVICE = DefaultSharethroughExecutorServiceProivder.create();
    private final CreativesQueue availableCreatives;
    private final SynchronizedWeakOrderedSet<IAdView> waitingAdViews = new SynchronizedWeakOrderedSet<>();
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
    private LruCache<Integer, Creative> creativesBySlot = new LruCache<>(10);

    private static final Map<String, String> dfpCreativeIds = new HashMap<>();
    private final Context context; //TODO decide whether this is needed
    private String dfpPath;
    private boolean firedNewAdsToShow;

    //TODO make the constructors cleaner

    /**
     * Returns an instance of the Sharethrough object which is used to fetch and display native ads within your mobile app.
     * <p/>
     * A default ad cache time of 20 seconds will be used.
     *
     * @param context      The Android context.
     * @param placementKey Your Sharethrough placement key that you will receive from Sharethrough.
     */
    public Sharethrough(Context context, String placementKey) {
        this(context, placementKey, DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS);
    }

    /**
     * @param context                   The Android context.
     * @param placementKey              Your Sharethrough placement key that you will receive from Sharethrough.
     * @param adCacheTimeInMilliseconds The ad cache time in milliseconds. e.g. a value of 100,000 will result in ads being refreshed every 100 seconds.
     */
    public Sharethrough(Context context, String placementKey, int adCacheTimeInMilliseconds) {
        this(context, placementKey, adCacheTimeInMilliseconds, new AdvertisingIdProvider(context, EXECUTOR_SERVICE, UUID.randomUUID().toString()), false);
    }

    public Sharethrough(Context context, String placementKey, boolean dfpEnabled) {
        this(context, placementKey, DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS, dfpEnabled);
    }

    public Sharethrough(Context context, String placementKey, int adCacheTimeInMilliseconds, boolean dfpEnabled) {
        this(context, placementKey, adCacheTimeInMilliseconds, new AdvertisingIdProvider(context, EXECUTOR_SERVICE, UUID.randomUUID().toString()), dfpEnabled);
    }

    Sharethrough(Context context, String placementKey, int adCacheTimeInMilliseconds, AdvertisingIdProvider advertisingIdProvider, boolean dfpEnabled) {
        this(context, placementKey, adCacheTimeInMilliseconds, new Renderer(), new CreativesQueue(),
                new BeaconService(new DateProvider(), UUID.randomUUID(), EXECUTOR_SERVICE, advertisingIdProvider),
                new AdFetcher(context, placementKey, EXECUTOR_SERVICE, new BeaconService(new DateProvider(), UUID.randomUUID(),
                        EXECUTOR_SERVICE, advertisingIdProvider)), new ImageFetcher(EXECUTOR_SERVICE, placementKey),
                new PlacementFetcher(placementKey, EXECUTOR_SERVICE), dfpEnabled ? new DFPNetworking() : null);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    Sharethrough(final Context context, final String placementKey, int adCacheTimeInMilliseconds, final Renderer renderer, final CreativesQueue availableCreatives, final BeaconService beaconService, AdFetcher adFetcher, ImageFetcher imageFetcher, PlacementFetcher placementFetcher, DFPNetworking dfpNetworking) {
        this.placementKey = placementKey;
        this.renderer = renderer;
        this.beaconService = beaconService;
        this.placementFetcher = placementFetcher;
        this.adCacheTimeInMilliseconds = Math.max(adCacheTimeInMilliseconds, MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS);
        this.availableCreatives = availableCreatives;

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
                    IAdView adView = waitingAdViews.popNext();
                    if (adView != null) {
                        renderer.putCreativeIntoAdView(adView, creative, beaconService, Sharethrough.this, new Timer("AdView timer for " + creative));
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

        this.dfpNetworking = dfpNetworking;
        this.context = context;
        fetchAds();
    }

    private void fireNoAdsToShow() {
        firedNewAdsToShow = false;
        handler.post(new Runnable() {
            @Override
            public void run() {
                onStatusChangeListener.noAdsToShow();
            }
        });
    }

    private void fireNewAdsToShow() {
        if (firedNewAdsToShow) return;
        firedNewAdsToShow = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                onStatusChangeListener.newAdsToShow();
            }
        });
    }

    private void fetchAds() {
        if (dfpNetworking != null) {
            fetchDfpAds();
        } else {
            invokeAdFetcher(apiUrlPrefix + placementKey);
        }
    }

    private void invokeAdFetcher(String url) {
        this.adFetcher.fetchAds(this.imageFetcher, url, creativeHandler, adFetcherCallback);
    }

    private void fetchDfpAds() {
        String creativeKey = popCreativeKey(dfpPath);
        if (creativeKey == null) {
            fetchDfpPath();
        } else {
            invokeAdFetcher(apiUrlPrefix + placementKey + dfpApiUrlPrefix + creativeKey);
        }
    }

    private void fetchDfpPath() {
        final DFPNetworking.DFPCreativeKeyCallback creativeKeyCallback = new DFPNetworking.DFPCreativeKeyCallback() {
            @Override
            public void receivedCreativeKey() {
                fetchAds();
            }

            @Override
            public void DFPKeyError(String errorMessage) {
                Log.e("DFP", "received Error message: " + errorMessage);
            }
        };

        final DFPNetworking.DFPPathFetcherCallback pathFetcherCallback = new DFPNetworking.DFPPathFetcherCallback() {
            @Override
            public void receivedURL(final String receivedDFPPath) {
                dfpPath = receivedDFPPath;
                dfpNetworking.fetchCreativeKey(context, dfpPath, creativeKeyCallback);
            }

            @Override
            public void DFPError(String errorMessage) {
                Log.e("DFP", "Error fetching DFP path: " + errorMessage);
            }
        };

        dfpNetworking.fetchDFPPath(EXECUTOR_SERVICE, placementKey, pathFetcherCallback);
    }

    public void putCreativeIntoAdView(IAdView adView) {
        putCreativeIntoAdView(adView, 0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void putCreativeIntoAdView(IAdView adView, int feedPosition) {
        Creative creative = creativesBySlot.get(feedPosition);

        long currentTime = new Date().getTime();
        if (creative != null && currentTime - creative.renderedTime >= adCacheTimeInMilliseconds) { //TODO make logic better
            creativesBySlot.remove(feedPosition);
            creative = null;
        }
        if (creative == null) {
            synchronized (availableCreatives) {
                creative = availableCreatives.getNext();
                if (availableCreatives.readyForMore()) {
                    fetchAds();
                }
            }
        }
        if (creative != null) {
            creativesBySlot.put(feedPosition, creative);
            renderer.putCreativeIntoAdView(adView, creative, beaconService, this, new Timer("AdView timer for " + creative));
        } else {
            waitingAdViews.put(adView);
        }
    }

    public int getAdCacheTimeInMilliseconds() {
        return adCacheTimeInMilliseconds;
    }

    public void getPlacement(Callback<Placement> placementCallback) {
        placementFetcher.fetch(placementCallback);
    }

    /**
     * Register a callback to be invoked when the status of having or not having ads to show changes.
     *
     * @param onStatusChangeListener
     */
    public void setOnStatusChangeListener(OnStatusChangeListener onStatusChangeListener) {
        this.onStatusChangeListener = onStatusChangeListener;
    }

    /**
     * Returns status change callback registered when the status of having or not having ads to show changes.
     *
     * @return
     */
    public OnStatusChangeListener getOnStatusChangeListener() {
        return onStatusChangeListener;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public IAdView getAdView(Context context, int feedPosition, int adLayoutResourceId, int title, int description,
                             int advertiser, int thumbnail, int optoutId, IAdView convertView) {
        IAdView view = convertView;
        if (view == null) {
            BasicAdView v = new BasicAdView(context);
            v.prepareWithResourceIds(adLayoutResourceId, title, description, advertiser, thumbnail, optoutId);
            view = v;
        }
        putCreativeIntoAdView(view, feedPosition);
        return view;
    }

    /**
     * Interface definition for a callback to be invoked when the status of having or not having ads to show changes.
     */
    public interface OnStatusChangeListener {
        void newAdsToShow();

        void noAdsToShow();
    }

    public static void addCreativeKey(final String dfpPath, final String creativeKey) {
        dfpCreativeIds.put(dfpPath, creativeKey);
    }

    public static String popCreativeKey(String dfpPath) {
        return dfpCreativeIds.remove(dfpPath);
    }
}
