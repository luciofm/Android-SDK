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
    private final DFPNetworking dfpNetworking;
    private String placementKey;
    private String apiUrlPrefix = "http://btlr.sharethrough.com/v3?placement_key=";
    private String dfpApiUrlPrefix = "&creative_key=";
    static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(4); // TODO: pick a reasonable number
    private final CreativesQueue availableCreatives;
    private final Map<IAdView, Integer> waitingAdViews = Collections.synchronizedMap(new LinkedHashMap<IAdView, Integer>());
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

    //TODO make the constructors cleaner
    public Sharethrough(Context context, String placementKey) {
        this(context, placementKey, DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS);
    }

    public Sharethrough(Context context, String placementKey, int adCacheTimeInMilliseconds) {
        this(context, placementKey, adCacheTimeInMilliseconds, new AdvertisingIdProvider(context, EXECUTOR_SERVICE, UUID.randomUUID().toString()));
    }

    Sharethrough(Context context, String placementKey, int adCacheTimeInMilliseconds, AdvertisingIdProvider advertisingIdProvider) {
        this(context, placementKey, adCacheTimeInMilliseconds, new Renderer(), new CreativesQueue(),
                new BeaconService(new DateProvider(), UUID.randomUUID(), EXECUTOR_SERVICE, advertisingIdProvider),
                new AdFetcher(context, placementKey, EXECUTOR_SERVICE, new BeaconService(new DateProvider(), UUID.randomUUID(),
                        EXECUTOR_SERVICE, advertisingIdProvider)), new ImageFetcher(EXECUTOR_SERVICE, placementKey),
                new PlacementFetcher(placementKey, EXECUTOR_SERVICE), null);
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
                    if (waitingAdViews.size() > 0) {
                        Map.Entry<IAdView, Integer> waiting = waitingAdViews.entrySet().iterator().next();
                        IAdView adView = waiting.getKey();
                        waitingAdViews.remove(adView);
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
                Log.d("DFP", "received Error message: " + errorMessage);
            }
        };

        final DFPNetworking.DFPPathFetcherCallback pathFetcherCallback = new DFPNetworking.DFPPathFetcherCallback() {
            @Override
            public void receivedURL(final String receivedDFPPath) {
                Log.d("DFP", "received URL " + receivedDFPPath);
                dfpPath = receivedDFPPath;
                dfpNetworking.fetchCreativeKey(context, dfpPath, creativeKeyCallback);
            }

            @Override
            public void DFPError(String errorMessage) {
                Log.d("DFP", "Error fetching DFP path: " + errorMessage);
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
            waitingAdViews.put(adView, feedPosition);
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
    public IAdView getAdView(Context context, int feedPosition, int adLayoutResourceId, int title, int description,
                             int advertiser, int thumbnail, IAdView convertView) {
        IAdView view = convertView;
        if (view == null) {
            BasicAdView v = new BasicAdView(context);
            v.prepareWithResourceIds(adLayoutResourceId, title, description, advertiser, thumbnail);
            view = v;
        }
        putCreativeIntoAdView(view, feedPosition);
        return view;
    }

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
