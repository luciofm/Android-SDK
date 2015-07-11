package com.sharethrough.sdk.network;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import com.sharethrough.sdk.*;
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

public class AdFetcher2 {
   AdFetcherListener adFetcherListener;
    Context applicationContext;

    public interface AdFetcherListener{
        void onAdResponseLoaded(Response response);
        void onAdResponseFailed();
    }

    public void registerAdFetcherListener(AdFetcherListener adFetcherListener) {
        this.adFetcherListener = adFetcherListener;
    }

    private void adResponseReceived(Response response) {
        adFetcherListener.onAdResponseLoaded(response);
    }

    public void fetchAds(Context applicationContext, String url, ArrayList<NameValuePair> queryStringParams){
        this.applicationContext = applicationContext;
        String adRequestUrl = generateRequestUrl(url, queryStringParams);
        SendHttpRequestTask sendHttpRequestTask = new SendHttpRequestTask();
        sendHttpRequestTask.execute(adRequestUrl);
    }

    private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            if( params[0] == null || params[0].isEmpty()){
                return null;
            }

            String json = null;
            try {

                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(params[0]);
                request.addHeader("User-Agent", Sharethrough.USER_AGENT);
                InputStream content = client.execute(request).getEntity().getContent();
                json = Misc.convertStreamToString(content);
                Response response = getResponse(json);

                // notify registered AdFetcherListeners
                adResponseReceived(response);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private String generateRequestUrl(String url, ArrayList<NameValuePair> queryStringParams) {


      /*if (advertisingIdProvider.getAdvertisingId() != null) {
            queryStringParams.add(new BasicNameValuePair("uid", advertisingIdProvider.getAdvertisingId()));
        }*/
        //queryStringParams.add(new BasicNameValuePair("appId", applicationContext.getPackageManager().getPackageInfo(appPackageName, PackageManager.GET_META_DATA).versionName));
        //queryStringParams.add(new BasicNameValuePair("appName", beaconService.getAppPackageName()));
        String formattedQueryStringParams = URLEncodedUtils.format(queryStringParams, "utf-8");
        String result = url + "?" + formattedQueryStringParams;
        return result;

    }

    private Response getResponse(String json) throws JSONException {
        JSONObject jsonResponse = new JSONObject(json);
        Response response = new Response();
        JSONObject jsonPlacement = jsonResponse.getJSONObject("placement");
        Response.Placement placement = new Response.Placement();
        placement.layout = jsonPlacement.getString("layout");
        placement.articlesBeforeFirstAd = jsonPlacement.optInt("articlesBeforeFirstAd", Integer.MAX_VALUE);
        placement.articlesBetweenAds = jsonPlacement.optInt("articlesBetweenAds", Integer.MAX_VALUE);
        placement.status = jsonPlacement.getString("status");
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

            creative.creative.creativeKey = jsonCreativeInner.getString("creative_key");
            creative.creative.campaignKey = jsonCreativeInner.getString("campaign_key");
            creative.creative.variantKey = jsonCreativeInner.getString("variant_key");
            creative.price = jsonCreative.getInt("price");
            creative.priceType = jsonCreative.getString("priceType");
            creative.signature = jsonCreative.getString("signature");

            JSONObject beacons = jsonCreativeInner.getJSONObject("beacons");
            creative.creative.beacon = new Response.Creative.CreativeInner.Beacon();
            parseBeacons(creative, beacons);

            response.creatives.add(creative);
        }
        return response;
    }

    private void parseBeacons(Response.Creative creative, JSONObject beacons) throws JSONException {
        creative.creative.beacon.impression = new ArrayList<>();
        creative.creative.beacon.visible = new ArrayList<>();
        creative.creative.beacon.play = new ArrayList<>();
        creative.creative.beacon.click = new ArrayList<>();

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
    }

}
