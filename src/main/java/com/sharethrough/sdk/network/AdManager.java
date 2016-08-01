package com.sharethrough.sdk.network;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Pair;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.mediation.MediationManager;
import com.sharethrough.sdk.mediation.STRMediationAdapter;
//import org.apache.http.NameValuePair;
//import org.apache.http.client.utils.URLEncodedUtils;
//import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AdManager implements STRMediationAdapter {

    private Context applicationContext;
    private AdManagerListener adManagerListener;
    private MediationManager.MediationListener mediationListener;
    protected AdFetcher adFetcher;

    private boolean isRunning = false;
    private String mediationRequestId = ""; // To remove for asap v2

    @Override
    public void loadAd(MediationManager.MediationListener mediationListener, Map<String, String> extras) {
        fetchAds("", null, "", "");
    }

    // Interface to notify Sharethrough ads are ready to show
    public interface AdManagerListener{
        void onAdsReady(List<Creative> listOfCreativesReadyForShow, Placement placement);
        void onNoAdsToShow();
        void onAdsFailedToLoad();
    }

    public AdManager(Context context) {
        this.applicationContext = context;
        adFetcher = new AdFetcher(context);
        setAdFetcherListener();
    }

    public void setAdManagerListener(AdManagerListener adManagerListener) {
        this.adManagerListener = adManagerListener;
    }

    public void setMediationListener(MediationManager.MediationListener mediationListener) {
        this.mediationListener = mediationListener;
    }

    protected void setAdFetcherListener() {
        adFetcher.setAdFetcherListener(new AdFetcher.AdFetcherListener() {
            @Override
            public void onAdResponseLoaded(Response response) {
                handleAdResponseLoaded(response);
            }

            @Override
            public void onAdResponseFailed() {
                handleAdResponseFailed();
            }
        });
    }

    public void handleAdResponseLoaded(Response response) {
        List<Creative> creatives = convertToCreatives(response);
        Logger.d("ad request returned %d creatives ", creatives.size());
        if(creatives.isEmpty()){
            adManagerListener.onNoAdsToShow();
        }else {
            adManagerListener.onAdsReady(creatives, new Placement(response.placement));
            mediationListener.onAdLoaded();
        }
        isRunning = false;
        // To remove for asap v2
        mediationRequestId = "";
    }

    public void handleAdResponseFailed() {
        adManagerListener.onAdsFailedToLoad();
        isRunning = false;
        // To remove for asap v2
        mediationRequestId = "";
    }

    protected List<Creative> convertToCreatives(Response response) {
        ArrayList<Creative> creatives = new ArrayList<Creative>();
        for (final Response.Creative responseCreative : response.creatives) {
            Creative creative;
            if (responseCreative.creative.action.equals("hosted-video")) {
                if (!responseCreative.creative.forceClickToPlay && response.placement.allowInstantPlay) {
                    creative = new InstantPlayCreative(responseCreative, mediationRequestId);
                } else {
                    creative = new Creative(responseCreative, mediationRequestId);
                }
            } else {
                creative = new Creative(responseCreative, mediationRequestId);
            }
            creatives.add(creative);
        }
        return creatives;
    }

    public synchronized void fetchAds(String url, ArrayList<Pair<String,String>> queryStringParams, String advertisingId, String mediationRequestId){
        if(isRunning) {
            return;
        }
        isRunning = true;
        // To remove for asap v2
        this.mediationRequestId = mediationRequestId;

        String adRequestUrl = generateRequestUrl(url, queryStringParams, advertisingId);

        Logger.d("ad request sent pkey: %s", queryStringParams.get(0));
        Logger.d("ad request url: %s", adRequestUrl);
        adFetcher.fetchAds(adRequestUrl);
    }

    private String generateRequestUrl(String url, ArrayList<Pair<String, String>> queryStringParams, String advertisingId) {
        if (advertisingId != null) {
            queryStringParams.add(new Pair<>("uid", advertisingId));
        }

        String appPackageName = applicationContext.getPackageName();
        String versionName = null;
        try {
            versionName = applicationContext.getPackageManager().getPackageInfo(appPackageName, PackageManager.GET_META_DATA).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if(versionName != null){
            queryStringParams.add(new Pair<>("appId", versionName));
        }
        queryStringParams.add(new Pair<>("appName", appPackageName));
//        String formattedQueryStringParams = URLEncodedUtils.format(queryStringParams, "utf-8");

        String formattedQueryStringParams = "";
        String result = url + "?" + formattedQueryStringParams;
        return result;
    }

    public void setMediationRequestId(String mediationRequestId) {
        this.mediationRequestId = mediationRequestId;
    }

    public String getMediationRequestId() {
        return this.mediationRequestId;
    }
}
