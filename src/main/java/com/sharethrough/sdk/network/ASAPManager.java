package com.sharethrough.sdk.network;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sharethrough.sdk.Logger;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


/**
 * Created by engineers on 4/26/16.
 */
public class ASAPManager {
    private static final String DFP_CREATIVE_KEY = "creative_key";
    private static final String DFP_CAMPAIGN_KEY = "campaign_key";
    private static final String ASAP_ENDPOINT = "https://asap-staging.sharethrough.com/v1";
    private String placementKey;
    private RequestQueue requestQueue;
    private boolean isRunning = false;

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
    public interface ASAPManagerListener {
        void onSuccess(ArrayList<NameValuePair> queryStringParams);
        void onError(String error);
    }

    public class AdResponse {
        String pkey;
        String adServer;
        String keyType;
        String keyValue;
        String status;
    }

    protected void handleResponse(String response, ASAPManagerListener asapManagerListener) {
        Gson gson = new Gson();
        try {
            AdResponse adResponse = gson.fromJson(response, AdResponse.class);
            if (adResponse.pkey == "null" || adResponse.adServer == "null"
                    || adResponse.keyType == null || adResponse.keyValue == null || adResponse.status == null) {
                asapManagerListener.onError("ASAP response does not have correct key values");
            } else if (!adResponse.status.equals("OK")) {
                asapManagerListener.onError(adResponse.status);
            } else {
                ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
                queryStringParams.add(new BasicNameValuePair("placement_key", placementKey));
                if (!adResponse.keyType.equals("STX_MONETIZE") && !adResponse.keyType.equals("undefined") && !adResponse.keyValue.equals("undefined")) {
                    queryStringParams.add(new BasicNameValuePair(adResponse.keyType, adResponse.keyValue));
                }
                asapManagerListener.onSuccess(queryStringParams);
            }
        } catch (JsonParseException e) {
            asapManagerListener.onError(e.toString());
        }
    }

    public void callASAP2(final ASAPManagerListener asapManagerListener) {
        if (isRunning) {
            return;
        }
        isRunning = true;
        String url = ASAP_ENDPOINT + "?pkey=" + placementKey;
        Logger.d("Making ASAP Request: " + url);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        isRunning = false;
                        handleResponse(response, asapManagerListener);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isRunning = false;
                asapManagerListener.onError(error.toString());
            }
        });

        requestQueue.add(stringRequest);
    }
}