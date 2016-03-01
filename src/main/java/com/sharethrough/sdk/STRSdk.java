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
import com.sharethrough.sdk.network.DFPNetworking;


import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Methods to handle configuration for Sharethrough's Android SDK.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class STRSdk {
    public static final int DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(300);
    private static final int MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(20);
    public static final String SDK_VERSION_NUMBER = BuildConfig.VERSION_NAME;
    public static String USER_AGENT = System.getProperty("http.agent") + "; STR " + SDK_VERSION_NUMBER;
    public static final String PRIVACY_POLICY_ENDPOINT = "http://platform-cdn.sharethrough.com/privacy-policy.html";
    private static final String DFP_CREATIVE_KEY = "creative_key";
    private static final String DFP_CAMPAIGN_KEY = "campaign_key";
    public final BeaconService beaconService;
    private final int adCacheTimeInMilliseconds;
    private final DFPNetworking dfpNetworking;
    private String placementKey;
    private String apiUrlPrefix = "http://btlr.sharethrough.com/v3";
    private final CreativesQueue availableCreatives;
    private AdvertisingIdProvider advertisingIdProvider;

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


    private static final Map<String, Map<String, String>> dfpAdGroupIds = new HashMap<>();
    private final Context context;
    private String dfpPath;
    public boolean firedNewAdsToShow;
    Placement placement;
    public boolean placementSet;
    private Callback<Placement> placementCallback = new Callback<Placement>() {
        @Override
        public void call(Placement result) {
        }
    };

    protected AdvertisingIdProvider getAdvertisingIdProvider(Context context){
        return new AdvertisingIdProvider(context.getApplicationContext());
    }

    public STRSdk(Context context, STRSdkConfig config){
        Logger.setContext(context); //initialize logger with context
        Logger.enabled = true;
        this.context = context;
        this.placementKey = config.placementKey;
        this.advertisingIdProvider = getAdvertisingIdProvider(context);
        this.beaconService = new BeaconService(new DateProvider(), UUID.randomUUID(), advertisingIdProvider, context, placementKey);
        this.adCacheTimeInMilliseconds = Math.max(config.adCacheTimer, MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS);
        this.availableCreatives = SharethroughSerializer.getCreativesQueue(config.serializedSharethrough);
        this.dfpNetworking = config.dfpNetworking? new DFPNetworking(): null;
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

        AdManager.getInstance(context).setAdManagerListener(adManagerListener);

        fetchAds();
    }

    public Creative getNextCreative(){
        if(!placementSet){
            Logger.w("Please initialized sharethrough before attemping to get creatives.");
        }

        fetchAdsIfReadyForMore();

        if( availableCreatives.size() > 0 ){
            Creative c = availableCreatives.getNext();
            return c;
        }
        return null;
    }

    protected AdManager.AdManagerListener adManagerListener = new AdManager.AdManagerListener() {
        @Override
        public void onAdsReady(List<Creative> listOfCreativesReadyForShow, Placement placement) {
            if (!placementSet) {
                STRSdk.this.placement = placement;
                placementSet = true;
                placementCallback.call(placement);
            }

            for(Creative creative : listOfCreativesReadyForShow) {
                availableCreatives.add(creative);
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
        if (dfpNetworking != null) {
            fetchDfpAds();
        } else {
            ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>(1);
            queryStringParams.add(new BasicNameValuePair("placement_key", placementKey));
            invokeAdFetcher(apiUrlPrefix, queryStringParams);
        }
    }

    private void invokeAdFetcher(String url, ArrayList<NameValuePair> queryStringParams) {
        AdManager.getInstance(context).fetchAds(url, queryStringParams, advertisingIdProvider.getAdvertisingId());
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

        dfpNetworking.fetchDFPPath(placementKey, pathFetcherCallback);
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
     * @return the number of prefetched ads available to show.
     */
    public int getNumberOfAdsReadyToShow() {
        return availableCreatives.size();
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

    public String serialize() {
        Logger.d("serializing Sharethrough: queue count - " + availableCreatives.size() + ", slot snapshot: " + creativesBySlot.snapshot());
        return SharethroughSerializer.serialize(availableCreatives, creativesBySlot, -1,-1);
    }
}
