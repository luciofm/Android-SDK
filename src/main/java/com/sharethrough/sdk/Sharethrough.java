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

import com.sharethrough.android.sdk.BuildConfig;
import com.sharethrough.sdk.network.AdManager;


import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Methods to handle configuration for Sharethrough's Android SDK.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class Sharethrough {
    public static final int DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(300);
    private static final int MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(20);
    public static final String SDK_VERSION_NUMBER = BuildConfig.VERSION_NAME;
    public static String USER_AGENT = System.getProperty("http.agent") + "; STR " + SDK_VERSION_NUMBER;
    public static final String PRIVACY_POLICY_ENDPOINT = "http://platform-cdn.sharethrough.com/privacy-policy.html?opt_out_url={OPT_OUT_URL}&opt_out_text={OPT_OUT_TEXT}";
    private final Renderer renderer;
    private final BeaconService beaconService;
    private final int adCacheTimeInMilliseconds;
    private String placementKey;
    private String apiUrlPrefix = "http://btlr.sharethrough.com/v3";
    private final CreativesQueue availableCreatives;
    private AdvertisingIdProvider advertisingIdProvider;
    private AdManager adManager;

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
    public Set<Integer> creativeIndices = new HashSet<>(); //contains history of all indices for creatives, whereas creativesBySlot only caches the last 10

    private final Context context; //TODO decide whether this is needed
    public boolean firedNewAdsToShow;
    Placement placement;
    public boolean placementSet;
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
        this(context, placementKey, adCacheTimeInMilliseconds, new AdvertisingIdProvider(context), false);
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
        this(context, placementKey, adCacheTimeInMilliseconds, new AdvertisingIdProvider(context), dfpEnabled);
    }

    Sharethrough(Context context, String placementKey, int adCacheTimeInMilliseconds, AdvertisingIdProvider advertisingIdProvider, boolean dfpEnabled) {
        this(context, placementKey, adCacheTimeInMilliseconds, new Renderer(), new CreativesQueue(),
                new BeaconService(new DateProvider(), UUID.randomUUID(), advertisingIdProvider, context, placementKey), null, new AdManager(context));
    }

    protected AdvertisingIdProvider getAdvertisingIdProvider(Context context){
        return new AdvertisingIdProvider(context.getApplicationContext());
    }

    /**
     *
     * @param context Android context
     * @param placementKey Sharethrough placementkey key that you will receive from Sharethrough
     * @param serializedSharethrough serialized Sharethrough object used to initialize Sharethough to previous state
     */
    public Sharethrough(Context context, String placementKey, String serializedSharethrough) {
        this(context, placementKey, false, serializedSharethrough);
    }

    /**
     *
     * @param context Android context
     * @param placementKey Sharethrough placementkey key that you will receive from Sharethrough
     * @param dfpEnabled boolean indicating use of Sharethrough's Direct Sell functionality via DFP
     * @param serializedSharethrough serialized Sharethrough object used to initialize Sharethough to previous state
     */
    public Sharethrough(Context context, String placementKey, boolean dfpEnabled, String serializedSharethrough) {
        this(context, placementKey,
                DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS,
                new Renderer(),
                SharethroughSerializer.getCreativesQueue(serializedSharethrough),
                new BeaconService(new DateProvider(), UUID.randomUUID(), new AdvertisingIdProvider(context), context, placementKey),
                null,
                new AdManager(context));

        creativesBySlot = SharethroughSerializer.getSlot(serializedSharethrough);

        Logger.d("deserializing Sharethrough: queue count - " + availableCreatives.size() + ", slot snapshot: " + creativesBySlot.snapshot());
        Response.Placement responsePlacement = new Response.Placement();
        responsePlacement.articlesBetweenAds = SharethroughSerializer.getArticlesBetween(serializedSharethrough);
        responsePlacement.articlesBeforeFirstAd = SharethroughSerializer.getArticlesBefore(serializedSharethrough);
        placement = new Placement(responsePlacement);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    Sharethrough(final Context context, final String placementKey, int adCacheTimeInMilliseconds, final Renderer renderer, final CreativesQueue availableCreatives, final BeaconService beaconService, Object dfpNetworking, AdManager adManager) {
        Logger.setContext(context); //initialize logger with context
        Logger.enabled = true;
        this.context = context;
        this.placementKey = placementKey;
        this.renderer = renderer;
        this.beaconService = beaconService;
        this.adCacheTimeInMilliseconds = Math.max(adCacheTimeInMilliseconds, MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS);
        this.availableCreatives = availableCreatives;
        this.advertisingIdProvider = getAdvertisingIdProvider(context);
        this.waitingAdViews = new SynchronizedWeakOrderedSet<AdViewFeedPositionPair>();
        this.adManager = adManager;
        adManager.setAdManagerListener(adManagerListener);

        Response.Placement responsePlacement = new Response.Placement();
        responsePlacement.articlesBetweenAds = Integer.MAX_VALUE;
        responsePlacement.articlesBeforeFirstAd = Integer.MAX_VALUE;
        responsePlacement.status = "";
        placement = new Placement(responsePlacement);

        // TODO build Picasso instance with STRExecutorService

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

        fetchAds();
    }

    protected AdManager.AdManagerListener adManagerListener = new AdManager.AdManagerListener() {
        @Override
        public void onAdsReady(List<Creative> listOfCreativesReadyForShow, Placement placement) {
            if (!placementSet) {
                Sharethrough.this.placement = placement;
                placementSet = true;
                placementCallback.call(placement);
            }

            for(Creative creative : listOfCreativesReadyForShow) {
                if (waitingAdViews.size() == 0) {
                    availableCreatives.add(creative);
                    if (creative != null) {
                        Logger.d("insert creative ckey: %s, creative cache size %d", creative.getCreativeKey(), availableCreatives.size());
                    }
                    fireNewAdsToShow();
                } else {
                    AdViewFeedPositionPair adViewFeedPositionPair = waitingAdViews.popNext();
                    if (adViewFeedPositionPair != null) {
                        IAdView adView = (IAdView) adViewFeedPositionPair.adView;
                        int feedPosition = (int) adViewFeedPositionPair.feedPosition;
                        insertCreativeIntoSlot(feedPosition, creative);
                        renderer.putCreativeIntoAdView(adView, creative, beaconService, Sharethrough.this, feedPosition, new Timer("AdView timer for " + creative));
                        fireNewAdsToShow();
                    }
                }
            }
        }

        @Override
        public void onNoAdsToShow(){
            fireNoAdsToShow();
        }

        @Override
        public void onAdsFailedToLoad() {

        }
    };

    private void insertCreativeIntoSlot(int feedPosition, Creative creative) {
        if( creative != null ) {
            creativesBySlot.put(feedPosition, creative);
            creativeIndices.add(feedPosition);
        }
    }

    private void fireNoAdsToShow() {
        firedNewAdsToShow = false;
        if( availableCreatives.size() != 0 ) return;
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
        //TODO:replace with new asap manager code
    }

    private void invokeAdFetcher(String url, ArrayList<NameValuePair> queryStringParams) {
        adManager.fetchAds(url, queryStringParams, advertisingIdProvider.getAdvertisingId());
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
                insertCreativeIntoSlot(feedPosition, creative);
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
            Logger.d("there are no ads to show at position: " + feedPosition);
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
        return creativeIndices.size();
    }

    /**
     *
     * @param position the position in your feed
     * @return the number of ads shown before a certain position
     */
    public int getNumberOfAdsBeforePosition(int position){
        int numberOfAdsBeforePosition = 0;
        Set<Integer> filledPositions = creativeIndices;
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

    public String serialize() {
        Logger.d("serializing Sharethrough: queue count - " + availableCreatives.size() + ", slot snapshot: " + creativesBySlot.snapshot());
        return SharethroughSerializer.serialize(availableCreatives, creativesBySlot, getArticlesBeforeFirstAd(), getArticlesBetweenAds());
    }
}
