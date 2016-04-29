package com.sharethrough.sdk.network;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Created by engineers on 4/26/16.
 */
public class ASAPManager {
    private static final String ASAP_ENDPOINT = "https://asap-staging.sharethrough.com/v1";
    private String placementKey;
    private RequestQueue requestQueue;

    public ASAPManager(String placementKey, RequestQueue requestQueue) {
        this.placementKey = placementKey;
        this.requestQueue = requestQueue;
    }

    public void callASAP(final ASAPManagerListener asapManagerListener) {
        String url = ASAP_ENDPOINT + "?pkey=" + placementKey;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new Gson();
                        AdResponse adResponse = gson.fromJson(response, AdResponse.class);

                        if (Arrays.asList(adResponse.errorRange).contains(adResponse.responseCode)) {
                            asapManagerListener.onError(adResponse.responseError);
                        } else {
                            ArrayList<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
                            queryStringParams.add(new BasicNameValuePair("placement_key", placementKey));
                            if (adResponse.requestTypeName != null && adResponse.requestTypeValue != null) {
                                queryStringParams.add(new BasicNameValuePair(adResponse.requestTypeName, adResponse.requestTypeValue));
                            }
                            asapManagerListener.onSuccess(queryStringParams);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                asapManagerListener.onError(error.toString());
            }
        });

        requestQueue.add(stringRequest);
    }

    public interface ASAPManagerListener {
        void onSuccess(ArrayList<NameValuePair> queryStringParams);
        void onError(String error);
    }

    public class AdResponse {
        String placementKey;
        String requestTypeName;
        String requestTypeValue;
        int responseCode;
        String responseError;
        int[] errorRange;
    }
}