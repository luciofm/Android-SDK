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
    private static final String PROGRAMMATIC = "stx_monetize";
    private static final String ASAP_ENDPOINT = "https://asap-staging.sharethrough.com/v1";
    private static final String PLACEMENT_KEY = "placement_key";
    private static final String ASAP_UNDEFINED = "undefined";
    private static final String ASAP_OK = "OK";
    private String placementKey;
    private RequestQueue requestQueue;
    private boolean isRunning = false;

    public ASAPManager(String placementKey, RequestQueue requestQueue) {
        this.placementKey = placementKey;
        this.requestQueue = requestQueue;
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
            if (adResponse.pkey == null || adResponse.adServer == null
                    || adResponse.keyType == null || adResponse.keyValue == null || adResponse.status == null) {
                asapManagerListener.onError("ASAP response does not have correct key values");
            } else if (!adResponse.status.equals(ASAP_OK)) {
                asapManagerListener.onError(adResponse.status);
            } else {
                ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
                queryStringParams.add(new BasicNameValuePair(PLACEMENT_KEY, placementKey));
                if (!adResponse.keyType.equals(PROGRAMMATIC) && !adResponse.keyType.equals(ASAP_UNDEFINED) && !adResponse.keyValue.equals(ASAP_UNDEFINED)) {
                    queryStringParams.add(new BasicNameValuePair(adResponse.keyType, adResponse.keyValue));
                }
                asapManagerListener.onSuccess(queryStringParams);
            }
        } catch (JsonParseException e) {
            asapManagerListener.onError(e.toString());
        }
    }

    public void callASAP(final ASAPManagerListener asapManagerListener) {
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