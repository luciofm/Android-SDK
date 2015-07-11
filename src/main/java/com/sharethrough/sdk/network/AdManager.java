package com.sharethrough.sdk.network;

import android.content.Context;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.Placement;
import com.sharethrough.sdk.Response;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class AdManager {
    // Interface to notify Sharethrough ads are ready to show
    public interface AdManagerListener{
        void onAdsReady(List<Creative> listOfCreativesReadyForShow, Placement placement);
        void onAdsFailedToLoad();
    }

    public void registerListener(AdManagerListener listener) {
        adManagerListeners.add(listener);
    }

    public void unregisterListener(AdManagerListener listener){
        adManagerListeners.remove(listener);
    }

    // ImageFetcherListener

    //private AdManager admanager = new AdManager();
    private Context mApplicationContext;
    private Set<AdManagerListener> adManagerListeners = new HashSet<>();
    private AdFetcher2 adFetcher = new AdFetcher2();
    //private final static ImageFetcher imageFetcher;

    private boolean isRunning = false;

    public AdManager() {
        //set variables
        registerListeners();
    }

    /*public static AdManager getInstance( Context applicationContext ){
        mApplicationContext = applicationContext;
        return admanager;
    }*/

    private void registerListeners() {
        // AdFetcherListener
        adFetcher.registerAdFetcherListener(new AdFetcher2.AdFetcherListener() {
            @Override
            public void onAdResponseLoaded(Response response) {
                List<Creative> creatives = convertToCreatives(response);
                for (AdManagerListener adManagerListener : adManagerListeners) {
                    adManagerListener.onAdsReady(creatives, new Placement(response.placement));
                }

                isRunning = false;

            }

            @Override
            public void onAdResponseFailed() {

            }
        });
        //imageFetcher.registerImageFetcherListener(imageFetcherListener);
    }

    private List<Creative> convertToCreatives(Response response) {
        ArrayList<Creative> creatives = new ArrayList<Creative>();
        for (final Response.Creative responseCreative : response.creatives) {
            Creative creative = new Creative(responseCreative, null, null, "");
            creatives.add(creative);
        }
        return creatives;
    }

    public void fetchAds(String url, ArrayList<NameValuePair> queryStringParams){

        if(isRunning) {
            return;
        }
        isRunning = true;



        //spawn a thread to fetchJson
        adFetcher.fetchAds(mApplicationContext, url, queryStringParams);

    }








}
