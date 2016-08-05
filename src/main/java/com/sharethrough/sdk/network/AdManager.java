package com.sharethrough.sdk.network;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Pair;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.mediation.ICreative;
import com.sharethrough.sdk.mediation.MediationManager;
import com.sharethrough.sdk.mediation.STRMediationAdapter;
//import org.apache.http.NameValuePair;
//import org.apache.http.client.utils.URLEncodedUtils;
//import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class AdManager implements STRMediationAdapter {

    private Context applicationContext;
    private MediationManager.MediationListener mediationListener;
    protected AdFetcher adFetcher;
    private boolean isRunning = false;
    private String mediationRequestId = ""; // To remove for asap v2

    @Override
    public void loadAd(Context context, MediationManager.MediationListener mediationListener, ASAPManager.AdResponse asapResponse, ASAPManager.AdResponse.Network network) {
        ArrayList<Pair<String,String>> queryStringParams = new ArrayList<>();
        queryStringParams.add(new Pair<>(ASAPManager.PLACEMENT_KEY, asapResponse.pkey));
        if (!asapResponse.keyType.equals(ASAPManager.PROGRAMMATIC)
                && !asapResponse.keyType.equals(ASAPManager.ASAP_UNDEFINED)
                && !asapResponse.keyValue.equals(ASAPManager.ASAP_UNDEFINED)) {
            queryStringParams.add(new Pair<>(asapResponse.keyType, asapResponse.keyValue));
        }

        fetchAds("", queryStringParams, "", asapResponse.mrid);
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
        List<ICreative> creatives = convertToCreatives(response);
        Logger.d("ad request returned %d creatives ", creatives.size());
        if(creatives.isEmpty()){
            mediationListener.onAdFailedToLoad();
        }else {
            mediationListener.onAdLoaded(creatives, new Placement(response.placement));
        }
        isRunning = false;
        // To remove for asap v2
        mediationRequestId = "";
    }

    public void handleAdResponseFailed() {
        mediationListener.onAdFailedToLoad();
        isRunning = false;
        // To remove for asap v2
        mediationRequestId = "";
    }

    protected List<ICreative> convertToCreatives(Response response) {
        ArrayList<ICreative> creatives = new ArrayList<>();
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

        //http://www.test.com?param1=2&param2=test
        if(versionName != null){
            queryStringParams.add(new Pair<>("appId", versionName));
        }
        queryStringParams.add(new Pair<>("appName", appPackageName));
//        String formattedQueryStringParams = URLEncodedUtils.format(queryStringParams, "utf-8");

        Uri.Builder builder = new Uri.Builder();
        for (Pair<String,String> queryParam : queryStringParams) {
            builder.appendQueryParameter(queryParam.first, queryParam.second);
        }

        String result = url + "?" + builder.toString();
        return result;
    }

    public void setMediationRequestId(String mediationRequestId) {
        this.mediationRequestId = mediationRequestId;
    }

    public String getMediationRequestId() {
        return this.mediationRequestId;
    }
}
