package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import com.android.volley.RequestQueue;
import com.sharethrough.android.sdk.BuildConfig;
import com.sharethrough.sdk.network.ASAPManager;
import com.sharethrough.sdk.network.AdManager;
import org.apache.http.NameValuePair;
import java.util.*;

/**
 * Methods to handle configuration for Sharethrough's Android SDK.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class Sharethrough {
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
    private Handler handler = new Handler(Looper.getMainLooper());
    private LruCache<Integer, Creative> creativesBySlot = new LruCache<>(10);
    public Set<Integer> creativeIndices = new HashSet<>(); //contains history of all indices for creatives, whereas creativesBySlot only caches the last 10
    public boolean firedNewAdsToShow;
    Placement placement;
    public boolean placementSet;
    public RequestQueue requestQueue;

    private Callback<Placement> placementCallback = new Callback<Placement>() {
        @Override
        public void call(Placement result) {
        }
    };
    private OnStatusChangeListener onStatusChangeListener = new OnStatusChangeListener() {
        @Override
        public void newAdsToShow() {

        }

        @Override
        public void noAdsToShow() {

        }
    };

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public Sharethrough(STRSdkConfig config) {
        this.placementKey = config.placementKey;
        this.renderer = config.renderer;
        this.beaconService = config.beaconService;
        this.adCacheTimeInMilliseconds = 5000;
        this.availableCreatives = config.creativeQueue;
        this.advertisingIdProvider = config.advertisingIdProvider;
        this.adManager = config.adManager;
        this.adManager.setAdManagerListener(adManagerListener);
        this.requestQueue = config.requestQueue;

        Response.Placement responsePlacement = new Response.Placement();
        responsePlacement.articlesBetweenAds = Integer.MAX_VALUE;
        responsePlacement.articlesBeforeFirstAd = Integer.MAX_VALUE;
        responsePlacement.status = "";
        this.placement = new Placement(responsePlacement);

        if (config.serializedSharethrough != null && !config.serializedSharethrough.isEmpty()) {
            this.creativesBySlot = SharethroughSerializer.getSlot(config.serializedSharethrough);
            Logger.d("deserializing Sharethrough: queue count - " + availableCreatives.size() + ", slot snapshot: " + creativesBySlot.snapshot());
            responsePlacement = new Response.Placement();
            responsePlacement.articlesBetweenAds = SharethroughSerializer.getArticlesBetween(config.serializedSharethrough);
            responsePlacement.articlesBeforeFirstAd = SharethroughSerializer.getArticlesBefore(config.serializedSharethrough);
            this.placement = new Placement(responsePlacement);
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
                availableCreatives.add(creative);
                if (creative != null) {
                    Logger.d("insert creative ckey: %s, creative cache size %d", creative.getCreativeKey(), availableCreatives.size());
                }
                fireNewAdsToShow();
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
        ASAPManager asapManager = new ASAPManager(placementKey, requestQueue);
        asapManager.callASAP(new ASAPManager.ASAPManagerListener() {
            @Override
            public void onSuccess(ArrayList<NameValuePair> queryStringParams) {
                invokeAdFetcher(apiUrlPrefix, queryStringParams);
            }

            @Override
            public void onError(String error) {
                Logger.d("ASAP error: " + error);
            }
        });
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

        //prevent pubs from calling this before we fire new ads to show
        boolean[] isAdRenewed = new boolean[1];
        isAdRenewed[0] = false;

        Creative creative = getCreativeToShow(feedPosition, isAdRenewed);
        if (creative != null) {
            renderer.putCreativeIntoAdView(adView, creative, beaconService, this, feedPosition, new Timer("AdView timer for " + creative));
            if( isAdRenewed[0] ){
                fireNewAdsToShow();
            }
        }

        //grab more ads if appropriate
        synchronized (availableCreatives) {
            if (availableCreatives.readyForMore()) {
                fetchAds();
            }
        }
    }

    public void setOrCallPlacementCallback(Callback<Placement> placementCallback) {
        if (placementSet){
            placementCallback.call(placement);
        } else {
            this.placementCallback = placementCallback;
        }
    }

     /** @return how many articles should be between ads. This is set on the server side.
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

    public long getAdCacheTimeInMilliseconds() {
        return adCacheTimeInMilliseconds;
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
