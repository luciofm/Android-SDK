package com.sharethrough.sdk.network;

import android.content.Context;
import android.content.pm.PackageManager;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.Logger;
import com.sharethrough.sdk.Placement;
import com.sharethrough.sdk.Response;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;


public class AdManager {

    private static AdManager instance;
    private static Context applicationContext;
    private AdManagerListener adManagerListener;
    protected AdFetcher adFetcher = new AdFetcher();

    private boolean isRunning = false;

    // Interface to notify Sharethrough ads are ready to show
    public interface AdManagerListener{
        void onAdsReady(List<Creative> listOfCreativesReadyForShow, Placement placement);
        void onNoAdsToShow();
        void onAdsFailedToLoad();
    }

    public static AdManager getInstance(Context context){
        if (instance == null && context != null) {
            applicationContext = context;
            instance = new AdManager();
        }
        return instance;
    }

    private AdManager() {
        setAdFetcherListener();
    }

    public void setAdManagerListener(AdManagerListener adManagerListener) {
        this.adManagerListener = adManagerListener;
    }

    private void setAdFetcherListener() {
        adFetcher.setAdFetcherListener(new AdFetcher.AdFetcherListener() {
            @Override
            public void onAdResponseLoaded(Response response) {
                List<Creative> creatives = convertToCreatives(response);
                Logger.d("ad request returned %d creatives ", creatives.size());
                if(creatives.isEmpty()){
                    adManagerListener.onNoAdsToShow();
                }else {
                    adManagerListener.onAdsReady(creatives, new Placement(response.placement));
                }
                isRunning = false;
            }

            @Override
            public void onAdResponseFailed() {
                adManagerListener.onAdsFailedToLoad();
                isRunning = false;
            }
        });
    }

    private List<Creative> convertToCreatives(Response response) {
        ArrayList<Creative> creatives = new ArrayList<Creative>();
        for (final Response.Creative responseCreative : response.creatives) {
            Creative creative = new Creative(responseCreative);
            creatives.add(creative);
        }
        return creatives;
    }

    public synchronized void fetchAds(String url, ArrayList<NameValuePair> queryStringParams, String advertisingId){
        if(isRunning) {
            return;
        }
        isRunning = true;

        String adRequestUrl = generateRequestUrl(url, queryStringParams, advertisingId);

        Logger.d("ad request sent pkey: %s", queryStringParams.get(0));
        adFetcher.fetchAds(adRequestUrl);
    }

    private String generateRequestUrl(String url, ArrayList<NameValuePair> queryStringParams, String advertisingId) {
        if (advertisingId != null) {
            queryStringParams.add(new BasicNameValuePair("uid", advertisingId));
        }

        String appPackageName = applicationContext.getPackageName();
        String versionName = null;
        try {
            versionName = applicationContext.getPackageManager().getPackageInfo(appPackageName, PackageManager.GET_META_DATA).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if(versionName != null){
            queryStringParams.add(new BasicNameValuePair("appId", versionName));
        }
        queryStringParams.add(new BasicNameValuePair("appName", appPackageName));
        String formattedQueryStringParams = URLEncodedUtils.format(queryStringParams, "utf-8");

        String result = url + "?" + formattedQueryStringParams;
        return result;
    }








}
