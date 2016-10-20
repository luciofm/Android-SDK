package com.sharethrough.sdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.sharethrough.android.sdk.BuildConfig;
import com.sharethrough.sdk.mediation.ICreative;
import com.sharethrough.sdk.mediation.MediationManager;
import com.sharethrough.sdk.network.ASAPManager;

import java.util.*;

/**
 * Methods to handle configuration for Sharethrough's Android SDK.
 */
public class Sharethrough {
    public static final String SDK_VERSION_NUMBER = BuildConfig.VERSION_NAME;
    public static final String PRIVACY_POLICY_ENDPOINT = "http://platform-cdn.sharethrough.com/privacy-policy.html?opt_out_url={OPT_OUT_URL}&opt_out_text={OPT_OUT_TEXT}";
    public static String USER_AGENT = System.getProperty("http.agent") + "; STR " + SDK_VERSION_NUMBER;
    protected boolean firedNewAdsToShow;

    private Handler handler = new Handler(Looper.getMainLooper());

    protected Placement placement;
    protected boolean placementSet;
    protected STRSdkConfig strSdkConfig;

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

    public Sharethrough(STRSdkConfig config) {
        this.strSdkConfig = config;
        this.placement = createPlacement(Integer.MAX_VALUE, Integer.MAX_VALUE);

        if (strSdkConfig.getSerializedSharethrough() != null && !strSdkConfig.getSerializedSharethrough().isEmpty()) {
            Logger.d("deserializing Sharethrough: queue count - " + strSdkConfig.getCreativeQueue().size() + ", slot snapshot: " + strSdkConfig.getCreativesBySlot().snapshot());
            int articlesBetweenAds = SharethroughSerializer.getArticlesBetween(strSdkConfig.getSerializedSharethrough());
            int articlesBeforeFirstAd = SharethroughSerializer.getArticlesBefore(strSdkConfig.getSerializedSharethrough());
            this.placement = createPlacement(articlesBetweenAds, articlesBeforeFirstAd);
        }
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
            public void onSuccess(ASAPManager.AdResponse asapResponse) {
                strSdkConfig.getMediationManager().initiateWaterfallAndLoadAd(asapResponse, mediationListener, new MediationManager.MediationWaterfall(asapResponse.mediationNetworks));
            }

            @Override
            public void onError(String error) {
                strSdkConfig.getAsapManager().setWaterfallComplete();
                Logger.e("ASAP error: " + error, null);
            }
        });
    }

    protected MediationManager.MediationListener mediationListener = new MediationManager.MediationListener() {
        @Override
        public void onAdLoaded(List<ICreative> creatives) {
            strSdkConfig.getAsapManager().setWaterfallComplete();

            for(ICreative creative : creatives) {
                creative.setPlacementIndex(strSdkConfig.getMediationManager().getPlacementIndex());
                strSdkConfig.getMediationManager().incrementPlacementIndex();

                strSdkConfig.getCreativeQueue().add(creative);
                Logger.d("insert creative, creative cache size %d", strSdkConfig.getCreativeQueue().size());
                fireNewAdsToShow();
            }
        }

        @Override
        public void onAdFailedToLoad() {
            strSdkConfig.getMediationManager().loadNextAd();
        }

        @Override
        public void onAllAdsFailedToLoad() {
            strSdkConfig.getMediationManager().incrementPlacementIndex();
            strSdkConfig.getAsapManager().setWaterfallComplete();
            fireNoAdsToShow();
        }
    };

    private void insertCreativeIntoSlot(int feedPosition, ICreative creative) {
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

    private ICreative getCreativeToShow(int feedPosition) {
        ICreative creative = strSdkConfig.getCreativesBySlot().get(feedPosition);
        if (creative == null) {
            if (strSdkConfig.getCreativeQueue().size() != 0) {
                synchronized (strSdkConfig.getCreativeQueue()) {
                    creative = strSdkConfig.getCreativeQueue().getNext();
                }
                insertCreativeIntoSlot(feedPosition, creative);
                Logger.d("pop creative at position %d , creative cache size: %d ", feedPosition , strSdkConfig.getCreativeQueue().size());
            }
        }

        if (creative != null) {
            Logger.d("get creative from creative slot at position %d", feedPosition);
        }

        return creative;
    }

    /**
     *
     * @param adView    Any class that implements IAdView.
     * @param feedPosition The starting position in your feed where you would like your first ad.
     */
    public void putCreativeIntoAdView(final IAdView adView, final int feedPosition) {
        ICreative creative = getCreativeToShow(feedPosition);
        if (creative != null) {
            strSdkConfig.getMediationManager().render(adView, creative, feedPosition);
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

    public IAdView getAdView(Context context, int feedPosition, int adLayoutResourceId, int title, int description,
                             int advertiser, int thumbnail, int optoutId, int brandLogoId, IAdView convertView, int slug) {
        IAdView view = convertView;
        if (view == null) {
            BasicAdView v = new BasicAdView(context);
            v.prepareWithResourceIds(adLayoutResourceId, title, description, advertiser, thumbnail, optoutId, brandLogoId, slug);
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
        Logger.d("serializing Sharethrough: queue count - " + strSdkConfig.getCreativeQueue().size() + ", slot snapshot: " + strSdkConfig.getCreativesBySlot().snapshot());
        return SharethroughSerializer.serialize(strSdkConfig.getCreativeQueue(), strSdkConfig.getCreativesBySlot(), getArticlesBeforeFirstAd(), getArticlesBetweenAds());
    }
}
