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

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Methods to handle configuration for Sharethrough's Android SDK.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class Sharethrough {
    public static final int DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(20);
    private static final int MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(20);
    public static final String SDK_VERSION_NUMBER = "1.1.6";
    public static String USER_AGENT = System.getProperty("http.agent") + "; STR " + SDK_VERSION_NUMBER;
    public static final String PRIVACY_POLICY_ENDPOINT = "http://platform-cdn.sharethrough.com/privacy-policy.html";
    private static final String DFP_CREATIVE_KEY = "creativeKey";
    private static final String DFP_CAMPAIGN_KEY = "campaignKey";
    private final Renderer renderer;
    private final BeaconService beaconService;
    private final int adCacheTimeInMilliseconds;
    private final DFPNetworking dfpNetworking;
    private String placementKey;
    private String apiUrlPrefix = "http://btlr.sharethrough.com/v3";
    static final ExecutorService EXECUTOR_SERVICE = DefaultSharethroughExecutorServiceProivder.create();
    private final CreativesQueue availableCreatives;
    private Function<Creative, Void> creativeHandler;
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
    private final LruCache<Integer, Creative> creativesBySlot = new LruCache<>(10);

    private static final Map<String, Map<String, String>> dfpAdGroupIds = new HashMap<>();
    private final Context context; //TODO decide whether this is needed
    private String dfpPath;
    public boolean firedNewAdsToShow;
    Placement placement;
    public boolean placementSet;
    private Function<Placement, Void> placementHandler;
    private Callback<Placement> placementCallback = new Callback<Placement>() {
        @Override
        public void call(Placement result) {
        }
    };
    private SynchronizedWeakOrderedSet<AdViewFeedPositionPair> waitingAdViews;

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
        this(context, placementKey, adCacheTimeInMilliseconds, new AdvertisingIdProvider(context, EXECUTOR_SERVICE), false);
    }

    /**
     * Returns an instance of the Sharethrough object which is used to fetch and display native ads within your mobile app.
     * <p/>
     * A default ad cache time of 20 seconds will be used.
     *
     * @param context   The Android context.
     * @param placementKey  Your Sharethrough placement key that you will receive from Sharethrough.
     * @param dfpEnabled    A boolean flag indicating if you would like to use Sharethrough's Direct Sell functionality via DFP.
     */
    public Sharethrough(Context context, String placementKey, boolean dfpEnabled) {
        this(context, placementKey, DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS, dfpEnabled);
    }

    /**
     *
     * @param context   The Android context.
     * @param placementKey  Your Sharethrough placement key that you will receive from Sharethrough.
     * @param adCacheTimeInMilliseconds     The ad cache time in milliseconds. e.g. a value of 100,000 will result in ads being refreshed every 100 seconds.
     * @param dfpEnabled    A boolean flag indicating if you would like to use Sharethrough's Direct Sell functionality via DFP.
     */
    public Sharethrough(Context context, String placementKey, int adCacheTimeInMilliseconds, boolean dfpEnabled) {
        this(context, placementKey, adCacheTimeInMilliseconds, new AdvertisingIdProvider(context, EXECUTOR_SERVICE), dfpEnabled);
    }

    Sharethrough(Context context, String placementKey, int adCacheTimeInMilliseconds, AdvertisingIdProvider advertisingIdProvider, boolean dfpEnabled) {
        this(context, placementKey, adCacheTimeInMilliseconds, new Renderer(), new CreativesQueue(),
                new BeaconService(new DateProvider(), UUID.randomUUID(), EXECUTOR_SERVICE, advertisingIdProvider, context),
                new AdFetcher(placementKey, EXECUTOR_SERVICE, new BeaconService(new DateProvider(), UUID.randomUUID(),
                        EXECUTOR_SERVICE, advertisingIdProvider, context), advertisingIdProvider), new ImageFetcher(EXECUTOR_SERVICE, placementKey),
                dfpEnabled ? new DFPNetworking() : null);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    Sharethrough(final Context context, final String placementKey, int adCacheTimeInMilliseconds, final Renderer renderer, final CreativesQueue availableCreatives, final BeaconService beaconService, AdFetcher adFetcher, ImageFetcher imageFetcher, DFPNetworking dfpNetworking) {
        Logger.setContext(context); //initialize logger with context
        Logger.enabled = true;
        this.placementKey = placementKey;
        this.renderer = renderer;
        this.beaconService = beaconService;
        this.adCacheTimeInMilliseconds = Math.max(adCacheTimeInMilliseconds, MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS);
        this.availableCreatives = availableCreatives;

        Response.Placement responsePlacement = new Response.Placement();
        responsePlacement.articlesBetweenAds = Integer.MAX_VALUE;
        responsePlacement.articlesBeforeFirstAd = Integer.MAX_VALUE;
        responsePlacement.status = "";
        placement = new Placement(responsePlacement);

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

        waitingAdViews = new SynchronizedWeakOrderedSet<AdViewFeedPositionPair>();
        creativeHandler = new Function<Creative,Void>() {
            @Override
            public Void apply(Creative creative) {
                if (waitingAdViews.size() == 0) {
                    availableCreatives.add(creative);
                    if (creative != null) {
                        Logger.d("insert creative ckey: %s, creative cache size %d", creative.getCreativeKey(), availableCreatives.size());
                    }
                    fireNewAdsToShow();
                } else {
                    AdViewFeedPositionPair adViewFeedPositionPair = waitingAdViews.popNext();
                    if(adViewFeedPositionPair != null){
                        IAdView adView = (IAdView) adViewFeedPositionPair.adView;
                        int feedPosition = (int )adViewFeedPositionPair.feedPosition;
                        creativesBySlot.put(feedPosition, creative);
                        renderer.putCreativeIntoAdView(adView, creative, beaconService, Sharethrough.this, feedPosition, new Timer("AdView timer for " + creative));
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

        placementHandler = new Function<Placement, Void>() {
            @Override
            public Void apply(Placement placement) {
                if (!placementSet) {
                    Sharethrough.this.placement = placement;
                    placementSet = true;
                    placementCallback.call(placement);
                }
                return null;
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
            ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>(1);
            queryStringParams.add(new BasicNameValuePair("placement_key", placementKey));
            invokeAdFetcher(apiUrlPrefix, queryStringParams);
        }
    }

    private void invokeAdFetcher(String url, ArrayList<NameValuePair> queryStringParams) {
        this.adFetcher.fetchAds(this.imageFetcher, url, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
    }

    private void fetchDfpAds() {
        Map<String, String> DFPKeys = popDFPKeys(dfpPath);
        if (DFPKeys == null) {
            fetchDfpPath();
        } else {
            ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>(2);
            queryStringParams.add(new BasicNameValuePair("placement_key", placementKey));

            if (DFPKeys.containsKey(DFP_CREATIVE_KEY)) {
                String creativeKey = DFPKeys.get(DFP_CREATIVE_KEY);
                if (!creativeKey.equals("STX_MONETIZE")){
                    queryStringParams.add(new BasicNameValuePair("creative_key", creativeKey));
                }
            } else if (DFPKeys.containsKey(DFP_CAMPAIGN_KEY)) {
                String campaignKey = DFPKeys.get(DFP_CAMPAIGN_KEY);
                if (!campaignKey.equals("STX_MONETIZE")) {
                    queryStringParams.add(new BasicNameValuePair("campaign_key", campaignKey));
                }
            }

            invokeAdFetcher(apiUrlPrefix, queryStringParams);
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

    /**
     *
     * @param adView Any class that implements IAdView.
     */
    public void putCreativeIntoAdView(IAdView adView) {
        putCreativeIntoAdView(adView, 0);
    }


    /**
     * Return a cached creative (used) or removes a new available creative from the queue (not used)
     * @param feedPosition, isAdRenewed
     * @return null if there is no creatives to show in both the slot and queue, and sets the isAdRenewed to true if
     * there is a new creative to show
     */
    private Creative getCreativeToShow(int feedPosition, boolean[] isAdRenewed) {
        Creative creative = creativesBySlot.get(feedPosition);
        if (creative == null || creative.hasExpired(adCacheTimeInMilliseconds)) {

            if (availableCreatives.size() != 0) {
                synchronized (availableCreatives) {
                    creative = availableCreatives.getNext();
                }
                creativesBySlot.put(feedPosition, creative);
                (isAdRenewed[0]) = true;
            }
        }

        if (creative != null) {
            if (isAdRenewed[0]) {
                Logger.d("pop creative ckey: %s at position %d , creative cache size: %d ", creative.getCreativeKey() ,feedPosition , availableCreatives.size());
            } else {
                Logger.d("get creative ckey: %s from creative slot at position %d",creative.getCreativeKey(), feedPosition);
            }
        }

        return creative;
    }

    /**
     *
     * @param adView    Any class that implements IAdView.
     * @param feedPosition The starting position in your feed where you would like your first ad.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void putCreativeIntoAdView(final IAdView adView, final int feedPosition) {
        boolean[] isAdRenewed = new boolean[1];
        isAdRenewed[0] = false;

        Creative creative = getCreativeToShow(feedPosition, isAdRenewed);
        if (creative != null) {
            renderer.putCreativeIntoAdView(adView, creative, beaconService, this, feedPosition, new Timer("AdView timer for " + creative));
            if( isAdRenewed[0] ){
                fireNewAdsToShow();
            }
        }
        else{
            if(Logger.enabled)Logger.d("there are no ads to show at position: " + feedPosition);
            AdViewFeedPositionPair<IAdView, Integer> adViewFeedPositionPair = new AdViewFeedPositionPair<IAdView, Integer>(adView, feedPosition);
            waitingAdViews.put(adViewFeedPositionPair);
        }

        //grab more ads if appropriate
        synchronized (availableCreatives) {
            if (availableCreatives.readyForMore()) {
                fetchAds();
            }
        }
    }

    public int getAdCacheTimeInMilliseconds() {
        return adCacheTimeInMilliseconds;
    }

    public void setOrCallPlacementCallback(Callback<Placement> placementCallback) {
        if (placementSet){
            placementCallback.call(placement);
        } else {
            this.placementCallback = placementCallback;
        }
    }

     /* @return how many articles should be between ads. This is set on the server side.
     */
    public int getArticlesBetweenAds() {
        return placement.getArticlesBetweenAds();
    }

    /**
     * @return how many articles to place before the first ad.
     */
    public int getArticlesBeforeFirstAd() {
        return placement.getArticlesBeforeFirstAd();
    }

    /**
     * Fetches more ads if running low on ads.
     */
    public void fetchAdsIfReadyForMore() {
        if (availableCreatives.readyForMore()) {
            fetchAds();
        }
    }

    /**
     * @param position The position to check for an ad
     * @return whether there is an ad to be displayed at the current position.
     */
    public boolean isAdAtPosition(int position) {
        return creativesBySlot.get(position) != null;
    }

    /**
     * @return the number of prefetched ads available to show.
     */
    public int getNumberOfAdsReadyToShow() {
        return availableCreatives.size();
    }

    /**
     * @return a set containing all of the positions currently filled by ads
     */
    public Set<Integer> getPositionsFilledByAds(){
        return creativesBySlot.snapshot().keySet();
    }

    /**
     * Note: The max number of stored ads is set to 10. New ads stored will eject the least recently seen ad.
     * @return the number of ads currently placed.
     */
    public int getNumberOfPlacedAds() {
        return creativesBySlot.size();
    }

    /**
     *
     * @param position the position in your feed
     * @return the number of ads shown before a certain position
     */
    public int getNumberOfAdsBeforePosition(int position){
        int numberOfAdsBeforePosition = 0;
        Set<Integer> filledPositions = getPositionsFilledByAds();
        for (Integer filledPosition : filledPositions) {
            if(filledPosition < position) numberOfAdsBeforePosition++;
        }
        return numberOfAdsBeforePosition;
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
                             int advertiser, int thumbnail, int optoutId, int brandLogoId, IAdView convertView) {
        IAdView view = convertView;
        if (view == null) {
            BasicAdView v = new BasicAdView(context);
            v.prepareWithResourceIds(adLayoutResourceId, title, description, advertiser, thumbnail, optoutId, brandLogoId);
            view = v;
        }
        putCreativeIntoAdView(view, feedPosition);
        return view;
    }

    /**
     * Interface definition for a callback to be invoked when the status of having or not having ads to show changes.
     */
    public interface OnStatusChangeListener {
        /**
         * The method is invoked when there are new ads to be shown.
         */
        void newAdsToShow();

        /**
         * This method is invoked when there are no ads to be shown.
         */
        void noAdsToShow();
    }

    public static void addDFPKeys(final String dfpPath, final String adGroupString) {
        HashMap<String, String> dfpKeys= new HashMap<String, String>();
        if (adGroupString.contains(DFP_CREATIVE_KEY)) {
            String[] tokens = adGroupString.split("=");
            dfpKeys.put(DFP_CREATIVE_KEY, tokens[1]);
        } else if (adGroupString.contains(DFP_CAMPAIGN_KEY)) {
            String[] tokens = adGroupString.split("=");
            dfpKeys.put(DFP_CAMPAIGN_KEY, tokens[1]);
        } else {
            dfpKeys.put(DFP_CREATIVE_KEY, adGroupString);
        }
        dfpAdGroupIds.put(dfpPath, dfpKeys);
    }

    public static Map<String, String> popDFPKeys(String dfpPath) {
        return dfpAdGroupIds.remove(dfpPath);
    }
}
