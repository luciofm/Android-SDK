package com.sharethrough.sdk.network;

import android.content.Context;
import com.android.volley.*;
import com.android.volley.toolbox.*;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.Response;
import org.json.JSONException;

public class AdFetcher {
    protected AdFetcherListener adFetcherListener;
    protected RequestQueue requestQueue;

    public interface AdFetcherListener{
        void onAdResponseLoaded(Response response);
        void onAdResponseFailed();
    }

    public AdFetcher(Context context) {
        requestQueue = new RequestQueue(new NoCache(), new BasicNetwork(new HurlStack()));
        requestQueue.start();
    }

    public void setAdFetcherListener(AdFetcherListener adFetcherListener) {
        this.adFetcherListener = adFetcherListener;
    }

    public void fetchAds(String adRequestUrl, final boolean isDirectSell) {
        STRStringRequest stringRequest = new STRStringRequest(Request.Method.GET, adRequestUrl,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        handleResponse(response, isDirectSell);
                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Logger.i("Ad request error: " + error.toString());
                adFetcherListener.onAdResponseFailed();
            }
        });

        requestQueue.add(stringRequest);
    }

    protected void handleResponse(String response, boolean isDirectSell) {
        try {
            adFetcherListener.onAdResponseLoaded(getResponse(response, isDirectSell));
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            adFetcherListener.onAdResponseFailed();
        }
    }

    protected Response getResponse(String stxResponse, boolean isDirectSell) throws JsonSyntaxException {
        Gson gson = new Gson();
        Response response = gson.fromJson(stxResponse, Response.class);
        setAdRequestIdForEachCreative(response);
        setPromotedByTextForEachCreative(isDirectSell, response);
        //todo don't fire third party beacons if pre-live
        return response;
    }

    protected void setAdRequestIdForEachCreative(Response response) {
        String adserverRequestId = response.adserverRequestId;
        for (Response.Creative creative : response.creatives) {
            creative.adserverRequestId = adserverRequestId;
        }
    }

    protected void setPromotedByTextForEachCreative(boolean isDirectSell, Response response) {
        for (Response.Creative creative : response.creatives) {
            String directSoldSlugCampaignOverride = creative.creative.promoted_by_text;
            String directSoldSlug = response.placement.placementAttributes.directSellPromotedByText;
            String programmaticSlug = response.placement.placementAttributes.promotedByText;

            if (isDirectSell) {
                if (directSoldSlugCampaignOverride != null && !directSoldSlugCampaignOverride.isEmpty()) {
                    creative.creative.custom_set_promoted_by_text = directSoldSlugCampaignOverride;
                } else if (directSoldSlug != null && !directSoldSlug.isEmpty()) {
                    creative.creative.custom_set_promoted_by_text = directSoldSlug;
                } else {
                    creative.creative.custom_set_promoted_by_text = programmaticSlug;
                }
            } else {
                if (programmaticSlug != null && !programmaticSlug.isEmpty()) {
                    creative.creative.custom_set_promoted_by_text = programmaticSlug;
                } else {
                    creative.creative.custom_set_promoted_by_text = "Ad By";
                }
            }
        }
    }
}
