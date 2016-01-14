package com.sharethrough.sdk.network;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import com.sharethrough.sdk.*;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class AdFetcher {
    protected AdFetcherListener adFetcherListener;

    public interface AdFetcherListener{
        void onAdResponseLoaded(Response response);
        void onAdResponseFailed();
    }

    public void setAdFetcherListener(AdFetcherListener adFetcherListener) {
        this.adFetcherListener = adFetcherListener;
    }

    public void fetchAds(String adRequestUrl){
        SendHttpRequestTask sendHttpRequestTask = new SendHttpRequestTask();
        sendHttpRequestTask.execute(adRequestUrl);
    }

    protected class SendHttpRequestTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            if( params[0] == null || params[0].isEmpty()){
                return null;
            }

            String json;
            try {

                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(params[0]);
                request.addHeader("User-Agent", Sharethrough.USER_AGENT);
                HttpResponse httpResponse = client.execute(request);
                int code = httpResponse.getStatusLine().getStatusCode();

                if (code == 200) {
                    InputStream content = httpResponse.getEntity().getContent();
                    json = Misc.convertStreamToString(content);
                    Response response = getResponse(json);

                    // notify adFetcherListener
                    adFetcherListener.onAdResponseLoaded(response);
                } else {
                    throw new HttpException("Status code is not 200");
                }

            } catch (Exception e) {
                e.printStackTrace();
                adFetcherListener.onAdResponseFailed();
            }
            return null;
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
        for (int i = 0; i < creatives.length(); i++) {
            JSONObject jsonCreative = creatives.getJSONObject(i);
            Response.Creative creative = new Response.Creative();
            creative.price = jsonCreative.getInt("price");
            creative.adserverRequestId = jsonCreative.getString("adserverRequestId");
            creative.auctionWinId = jsonCreative.getString("auctionWinId");

            JSONObject jsonCreativeInner = jsonCreative.getJSONObject("creative");

            creative.creative = new Response.Creative.CreativeInner();
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
            creative.price = jsonCreative.getInt("price");
            creative.priceType = jsonCreative.getString("priceType");
            creative.signature = jsonCreative.getString("signature");

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
