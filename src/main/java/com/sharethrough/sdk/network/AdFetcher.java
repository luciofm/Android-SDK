package com.sharethrough.sdk.network;

import android.content.Context;
import com.android.volley.*;
import com.android.volley.toolbox.*;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    public void fetchAds(String adRequestUrl) {
        STRStringRequest stringRequest = new STRStringRequest(Request.Method.GET, adRequestUrl,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        handleResponse(response);
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

    protected void handleResponse(String response) {
        try {
            adFetcherListener.onAdResponseLoaded(getResponse(response));
        } catch (JSONException e) {
            e.printStackTrace();
            adFetcherListener.onAdResponseFailed();
        }
    }

    protected Response getResponse(String json) throws JSONException {
        JSONObject jsonResponse = new JSONObject(json);
        Response response = new Response();

        JSONObject jsonPlacement = jsonResponse.getJSONObject("placement");
        Response.Placement placement = new Response.Placement();
        placement.layout = jsonPlacement.getString("layout");
        placement.articlesBeforeFirstAd = jsonPlacement.optInt("articlesBeforeFirstAd", Integer.MAX_VALUE);
        placement.articlesBetweenAds = jsonPlacement.optInt("articlesBetweenAds", Integer.MAX_VALUE);
        placement.status = jsonPlacement.getString("status");
        placement.allowInstantPlay = jsonPlacement.optBoolean("allowInstantPlay", false);
        response.placement = placement;

        JSONArray creatives = jsonResponse.getJSONArray("creatives");
        response.creatives = new ArrayList<>(creatives.length());
        String adserverRequestId = jsonResponse.getString("adserverRequestId");
        for (int i = 0; i < creatives.length(); i++) {
            JSONObject jsonCreative = creatives.getJSONObject(i);
            Response.Creative creative = new Response.Creative();

            creative.adserverRequestId = adserverRequestId;


            creative.auctionWinId = jsonCreative.getString("auctionWinId");

            JSONObject jsonCreativeInner = jsonCreative.getJSONObject("creative");

            creative.creative = new Response.Creative.CreativeInner();
            creative.creative.optOutText = jsonCreativeInner.optString("opt_out_text");
            creative.creative.optOutUrl = jsonCreativeInner.optString("opt_out_url");
            creative.creative.action = jsonCreativeInner.getString("action");
            creative.creative.mediaUrl = jsonCreativeInner.getString("media_url");
            creative.creative.shareUrl = jsonCreativeInner.getString("share_url");
            creative.creative.brandLogoUrl = jsonCreativeInner.optString("brand_logo_url");
            creative.creative.title = jsonCreativeInner.getString("title");
            creative.creative.description = jsonCreativeInner.getString("description");
            creative.creative.advertiser = jsonCreativeInner.getString("advertiser");
            creative.creative.thumbnailUrl = jsonCreativeInner.getString("thumbnail_url");

            creative.creative.customEngagementUrl = jsonCreativeInner.optString("custom_engagement_url");
            creative.creative.customEngagementLabel = jsonCreativeInner.optString("custom_engagement_label");
            creative.creative.dealId = jsonCreativeInner.optString("deal_id");
            creative.creative.forceClickToPlay = jsonCreativeInner.optBoolean("force_click_to_play", false);

            creative.creative.creativeKey = jsonCreativeInner.getString("creative_key");
            creative.creative.campaignKey = jsonCreativeInner.getString("campaign_key");
            creative.creative.variantKey = jsonCreativeInner.getString("variant_key");

            JSONObject beacons = jsonCreativeInner.getJSONObject("beacons");
            creative.creative.beacon = new Response.Creative.CreativeInner.Beacon();
            parseBeacons(placement, creative, beacons);

            response.creatives.add(creative);
        }
        return response;
    }

    private void parseBeacons(Response.Placement placement, Response.Creative creative, JSONObject beacons) throws JSONException {
        creative.creative.beacon.impression = new ArrayList<>();
        creative.creative.beacon.visible = new ArrayList<>();
        creative.creative.beacon.play = new ArrayList<>();
        creative.creative.beacon.click = new ArrayList<>();
        creative.creative.beacon.silentPlay = new ArrayList<>();
        creative.creative.beacon.tenSecondSilentPlay = new ArrayList<>();
        creative.creative.beacon.fifteenSecondSilentPlay = new ArrayList<>();
        creative.creative.beacon.thirtySecondSilentPlay = new ArrayList<>();
        creative.creative.beacon.completedSilentPlay = new ArrayList<>();

        //we don't parse third party beacons when placement is pre live ( won't fire third party beacons)
        if (placement.status.equals("pre-live") ){
            return;
        }

        JSONArray impressionBeacons = beacons.getJSONArray("impression");
        for (int k = 0; k < impressionBeacons.length(); k++) {
            String s = impressionBeacons.getString(k);
            creative.creative.beacon.impression.add(s);
        }
        JSONArray visibleBeacons = beacons.getJSONArray("visible");
        for (int k = 0; k < visibleBeacons.length(); k++) {
            String s = visibleBeacons.getString(k);
            creative.creative.beacon.visible.add(s);
        }
        JSONArray clickBeacons = beacons.getJSONArray("click");
        for (int k = 0; k < clickBeacons.length(); k++) {
            String s = clickBeacons.getString(k);
            creative.creative.beacon.click.add(s);
        }
        JSONArray playBeacons = beacons.getJSONArray("play");
        for (int k = 0; k < playBeacons.length(); k++) {
            String s = playBeacons.getString(k);
            creative.creative.beacon.play.add(s);
        }
        JSONArray silentPlayBeacons = beacons.optJSONArray("silent_play");
        for (int k = 0; silentPlayBeacons != null && k < silentPlayBeacons.length() ; k++) {
            String s = silentPlayBeacons.getString(k);
            creative.creative.beacon.silentPlay.add(s);
        }
        JSONArray tenSecondSilentPlayBeacons = beacons.optJSONArray("ten_second_silent_play");
        for (int k = 0; tenSecondSilentPlayBeacons != null && k < tenSecondSilentPlayBeacons.length(); k++) {
            String s = tenSecondSilentPlayBeacons.getString(k);
            creative.creative.beacon.tenSecondSilentPlay.add(s);
        }
        JSONArray fifteenSecondSilentPlayBeacons = beacons.optJSONArray("fifteen_second_silent_play");
        for (int k = 0; fifteenSecondSilentPlayBeacons != null && k < fifteenSecondSilentPlayBeacons.length(); k++) {
            String s = fifteenSecondSilentPlayBeacons.getString(k);
            creative.creative.beacon.fifteenSecondSilentPlay.add(s);
        }
        JSONArray thirtySecondSilentPlayBeacons = beacons.optJSONArray("thirty_second_silent_play");
        for (int k = 0; thirtySecondSilentPlayBeacons != null && k < thirtySecondSilentPlayBeacons.length(); k++) {
            String s = thirtySecondSilentPlayBeacons.getString(k);
            creative.creative.beacon.thirtySecondSilentPlay.add(s);
        }
        JSONArray completedSilentPlayBeacons = beacons.optJSONArray("completed_silent_play");
        for (int k = 0; completedSilentPlayBeacons != null && k < completedSilentPlayBeacons.length(); k++) {
            String s = completedSilentPlayBeacons.getString(k);
            creative.creative.beacon.completedSilentPlay.add(s);
        }


    }

}
