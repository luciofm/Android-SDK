package com.sharethrough.sdk.network;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by engineers on 4/26/16.
 */
public class ASAPManager {

    private static final String ASAP_ENDPOINT = "https://asap-staging.sharethrough.com/v1";
    private static final String DFP_CREATIVE_KEY = "creative_key";
    private static final String DFP_CAMPAIGN_KEY = "campaign_key";
    private String placementKey;
    private RequestQueue requestQueue;

    public ASAPManager(String placementKey, RequestQueue requestQueue) {
        this.placementKey = placementKey;
        this.requestQueue = requestQueue;
    }

    public void callASAP(final ASAPManagerListener asapManagerListener) {
        // Instantiate the RequestQueue.
        String url = ASAP_ENDPOINT + "?pkey=" + placementKey;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        asapManagerListener.onSuccess(parseAsapResponse(response));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                asapManagerListener.onError(error.toString());
            }
        });

        // Add the request to the RequestQueue.
        requestQueue.add(stringRequest);
    }

    public interface ASAPManagerListener {
        void onSuccess(ArrayList<NameValuePair> queryStringParams);
        void onError(String error);
    }

    public ArrayList<NameValuePair> parseAsapResponse(String asapResponseString) {
        HashMap<String, String> asapKeys= new HashMap<String, String>();
        if (asapResponseString.contains(DFP_CREATIVE_KEY)) {
            String[] tokens = asapResponseString.split("=");
            asapKeys.put(DFP_CREATIVE_KEY, tokens[1]);
        } else if (asapResponseString.contains(DFP_CAMPAIGN_KEY)) {
            String[] tokens = asapResponseString.split("=");
            asapKeys.put(DFP_CAMPAIGN_KEY, tokens[1]);
        } else {
            asapKeys.put(DFP_CREATIVE_KEY, asapResponseString);
        }

        ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>(2);
        queryStringParams.add(new BasicNameValuePair("placement_key", placementKey));
        if (asapKeys.containsKey(DFP_CREATIVE_KEY)) {
            String creativeKey = asapKeys.get(DFP_CREATIVE_KEY);
            if (!creativeKey.equals("STX_MONETIZE")){
                queryStringParams.add(new BasicNameValuePair("creative_key", creativeKey));
            }
        } else if (asapKeys.containsKey(DFP_CAMPAIGN_KEY)) {
            String campaignKey = asapKeys.get(DFP_CAMPAIGN_KEY);
            if (!campaignKey.equals("STX_MONETIZE")) {
                queryStringParams.add(new BasicNameValuePair("campaign_key", campaignKey));
            }
        }

        return queryStringParams;
    }
}