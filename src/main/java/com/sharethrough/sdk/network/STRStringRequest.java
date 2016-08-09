package com.sharethrough.sdk.network;

import com.android.volley.toolbox.StringRequest;
import com.sharethrough.sdk.Sharethrough;

import java.util.HashMap;
import java.util.Map;

public class STRStringRequest extends StringRequest {
    public STRStringRequest(int method, String url, com.android.volley.Response.Listener<String> listener, com.android.volley.Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders(){
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", Sharethrough.USER_AGENT);
        return headers;
    }
}