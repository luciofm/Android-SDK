package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import com.sharethrough.android.sdk.BuildConfig;
import com.sharethrough.sdk.mediation.MediationManager;
import com.sharethrough.sdk.network.ASAPManager;
import com.sharethrough.sdk.network.AdManager;
import java.util.*;

/**
 * Methods to handle configuration for Sharethrough's Android SDK.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class Sharethrough {
    public static final String SDK_VERSION_NUMBER = BuildConfig.VERSION_NAME;
    public static final String PRIVACY_POLICY_ENDPOINT = "http://platform-cdn.sharethrough.com/privacy-policy.html?opt_out_url={OPT_OUT_URL}&opt_out_text={OPT_OUT_TEXT}";
    public static String USER_AGENT = System.getProperty("http.agent") + "; STR " + SDK_VERSION_NUMBER;
    protected boolean firedNewAdsToShow;

    private String apiUrlPrefix = "http://btlr.sharethrough.com/v4";
    private Handler handler = new Handler(Looper.getMainLooper());

    protected Placement placement;
    protected boolean placementSet;
    protected STRSdkConfig strSdkConfig;
    protected final int adCacheTimeInMilliseconds = 300000;

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
        this.strSdkConfig = config;
        this.placement = createPlacement(Integer.MAX_VALUE, Integer.MAX_VALUE);

        if (strSdkConfig.getSerializedSharethrough() != null && !strSdkConfig.getSerializedSharethrough().isEmpty()) {
            Logger.d("deserializing Sharethrough: queue count - " + strSdkConfig.getCreativeQueue().size() + ", slot snapshot: " + strSdkConfig.getCreativesBySlot().snapshot());
            int articlesBetweenAds = SharethroughSerializer.getArticlesBetween(strSdkConfig.getSerializedSharethrough());
            int articlesBeforeFirstAd = SharethroughSerializer.getArticlesBefore(strSdkConfig.getSerializedSharethrough());
            this.placement = createPlacement(articlesBetweenAds, articlesBeforeFirstAd);
        }

        strSdkConfig.getAdManager().setAdManagerListener(adManagerListener);
        strSdkConfig.getAdManager().setMediationListener(mediationListener);
    }

    protected Placement createPlacement(int articlesBetweenAds, int articlesBeforeFirstAd) {
        Response.Placement responsePlacement = new Response.Placement();
        responsePlacement.articlesBetweenAds = articlesBetweenAds;
        responsePlacement.articlesBeforeFirstAd = articlesBeforeFirstAd;
        responsePlacement.status = "";
        return new Placement(responsePlacement);
    }

    public void fetchAds(Map<String, String> ... customKeyValues) {
        if (customKeyValues.length > 0) {
            strSdkConfig.getAsapManager().updateAsapEndpoint(customKeyValues[0]);
        }

        strSdkConfig.getAsapManager().callASAP(new ASAPManager.ASAPManagerListener() {
            @Override
            public void onSuccess(ArrayList<Pair<String,String>> queryStringParams, String mediationRequestId) {
                String asapResponse = "fake_asap_response";
                Map<String, String> extras = new HashMap<String, String>();
                strSdkConfig.getMediationManager().initiateWaterfallAndLoadAd(asapResponse, mediationListener, extras);
                //invokeAdFetcher(apiUrlPrefix, queryStringParams, mediationRequestId);
            }

            @Override
            public void onError(String error) {
                Logger.e("ASAP error: " + error, null);
            }
        });
    }

    protected MediationManager.MediationListener mediationListener = new MediationManager.MediationListener() {
        @Override
        public void onAdLoaded() {
            fireNewAdsToShow();
        }

        @Override
        public void onAdFailedToLoad() {
            strSdkConfig.getMediationManager().loadNextAd();
        }
    };

    protected AdManager.AdManagerListener adManagerListener = new AdManager.AdManagerListener() {
        @Override
        public void onAdsReady(List<Creative> listOfCreativesReadyForShow, Placement placement) {
            if (!placementSet) {
                Sharethrough.this.placement = placement;
                placementSet = true;
                placementCallback.call(placement);
            }

            for(Creative creative : listOfCreativesReadyForShow) {
                strSdkConfig.getCreativeQueue().add(creative);
                if (creative != null) {
                    Logger.d("insert creative ckey: %s, creative cache size %d", creative.getCreativeKey(), strSdkConfig.getCreativeQueue().size());
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
            strSdkConfig.getCreativesBySlot().put(feedPosition, creative);
            strSdkConfig.getCreativeIndices().add(feedPosition);
        }
    }

    protected void fireNoAdsToShow() {
        firedNewAdsToShow = false;
        if( strSdkConfig.getCreativeQueue().size() != 0 ) return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                onStatusChangeListener.noAdsToShow();
            }
        });
    }

    protected void fireNewAdsToShow() {
        if (firedNewAdsToShow) return;
        firedNewAdsToShow = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                onStatusChangeListener.newAdsToShow();
            }
        });
    }

//    private void invokeAdFetcher(String url, ArrayList<NameValuePair> queryStringParams, String mediationRequestId) {
//        strSdkConfig.getAdManager().fetchAds(url, queryStringParams, strSdkConfig.getAdvertisingIdProvider().getAdvertisingId(), mediationRequestId);
//    }

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
        Creative creative = strSdkConfig.getCreativesBySlot().get(feedPosition);
        if (creative == null || creative.hasExpired(adCacheTimeInMilliseconds)) {

            if (strSdkConfig.getCreativeQueue().size() != 0) {
                synchronized (strSdkConfig.getCreativeQueue()) {
                    creative = strSdkConfig.getCreativeQueue().getNext();
                }
                insertCreativeIntoSlot(feedPosition, creative);
                (isAdRenewed[0]) = true;
            }
        }

        if (creative != null) {
            if (isAdRenewed[0]) {
                Logger.d("pop creative ckey: %s at position %d , creative cache size: %d ", creative.getCreativeKey() ,feedPosition , strSdkConfig.getCreativeQueue().size());
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
            strSdkConfig.getRenderer().putCreativeIntoAdView(adView, creative, strSdkConfig.getBeaconService(), this, feedPosition, new Timer("AdView timer for " + creative));
            if( isAdRenewed[0] ){
                fireNewAdsToShow();
            }
        }

        //grab more ads if appropriate
        synchronized (strSdkConfig.getCreativeQueue()) {
            if (strSdkConfig.getCreativeQueue().readyForMore()) {
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
        if (strSdkConfig.getCreativeQueue().readyForMore()) {
            fetchAds();
        }
    }

    /**
     * @param position The position to check for an ad
     * @return whether there is an ad to be displayed at the current position.
     */
    public boolean isAdAtPosition(int position) {
        return strSdkConfig.getCreativesBySlot().get(position) != null;
    }

    /**
     * @return the number of prefetched ads available to show.
     */
    public int getNumberOfAdsReadyToShow() {
        return strSdkConfig.getCreativeQueue().size();
    }

    /**
     * @return a set containing all of the positions currently filled by ads
     */
    public Set<Integer> getPositionsFilledByAds(){
        return strSdkConfig.getCreativesBySlot().snapshot().keySet();
    }

    /**
     * Note: The max number of stored ads is set to 10. New ads stored will eject the least recently seen ad.
     * @return the number of ads currently placed.
     */
    public int getNumberOfPlacedAds() {
        return strSdkConfig.getCreativeIndices().size();
    }

    /**
     *
     * @param position the position in your feed
     * @return the number of ads shown before a certain position
     */
    public int getNumberOfAdsBeforePosition(int position){
        int numberOfAdsBeforePosition = 0;
        Set<Integer> filledPositions = strSdkConfig.getCreativeIndices();
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
        Logger.d("serializing Sharethrough: queue count - " + strSdkConfig.getCreativeQueue().size() + ", slot snapshot: " + strSdkConfig.getCreativesBySlot().snapshot());
        return SharethroughSerializer.serialize(strSdkConfig.getCreativeQueue(), strSdkConfig.getCreativesBySlot(), getArticlesBeforeFirstAd(), getArticlesBetweenAds());
    }
}
