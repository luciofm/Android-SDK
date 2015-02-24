package com.sharethrough.sdk.network;

import android.util.Log;
import com.sharethrough.sdk.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class AdFetcher {
    private final ExecutorService executorService;
    private final BeaconService beaconService;
    private final String placementKey;
    private boolean isRunning;
    private int remainingImageRequests;
    private boolean placementSet;

    public AdFetcher(String placementKey, ExecutorService executorService, BeaconService beaconService) {
        this.placementKey = placementKey;
        this.executorService = executorService;
        this.beaconService = beaconService;
    }

    public synchronized void fetchAds(final ImageFetcher imageFetcher, final String apiUrl, final ArrayList<NameValuePair> queryStringParams, final Function<Creative, Void> creativeHandler, final Callback adFetcherCallback, final Function<Placement, Void> placementHandler) {
        if (isRunning) return;
        isRunning = true;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                beaconService.adRequested(placementKey);

                queryStringParams.add(new BasicNameValuePair("appId", beaconService.getAppVersionName()));
                queryStringParams.add(new BasicNameValuePair("appName", beaconService.getAppPackageName()));

                String formattedQueryStringParams = URLEncodedUtils.format(queryStringParams, "utf-8");
                final URI uri = URI.create(apiUrl + "?" + formattedQueryStringParams );
                String json = null;
                    try {

                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpGet request = new HttpGet(uri);
                    request.addHeader("User-Agent", Sharethrough.USER_AGENT);
                    InputStream content = client.execute(request).getEntity().getContent();
                    json = Misc.convertStreamToString(content);
                    Response response = getResponse(json);

                    if (response.placement.layout.equals("multiple") && !placementSet) {
                        if (!(response.placement.articlesBeforeFirstAd == 0 || response.placement.articlesBetweenAds == 0)) {
                            placementHandler.apply(new Placement(response.placement.articlesBeforeFirstAd, response.placement.articlesBetweenAds));
                            placementSet = true;
                        }
                    }

                    remainingImageRequests += response.creatives.size();
                    if (remainingImageRequests == 0) {
                        adFetcherCallback.finishedLoadingWithNoAds();
                        isRunning = false;
                    }
                    for (final Response.Creative responseCreative : response.creatives) {
                        imageFetcher.fetchCreativeImages(uri, responseCreative, new ImageFetcher.Callback() {
                            @Override
                            public void success(Creative value) {
                                creativeHandler.apply(value);
                                decCount();
                            }

                            @Override
                            public void failure() {
                                decCount();
                            }

                            private void decCount() {
                                synchronized (AdFetcher.this) {
                                    remainingImageRequests -= 1;
                                    if (remainingImageRequests == 0) {
                                        isRunning = false;
                                        adFetcherCallback.finishedLoading();
                                    }
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    adFetcherCallback.finishedLoadingWithNoAds();
                    String msg = "failed to get ads for key " + placementKey + ": " + uri;
                    if (json != null) {
                        msg += ": " + json;
                    }
                    isRunning = false;
                    Log.e("Sharethrough", msg, e);
                }
            }
        });
    }

    private void parseBeacons(Response.Creative creative, JSONObject beacons) throws JSONException {
        JSONArray visibleBeacons = beacons.getJSONArray("visible");
        creative.creative.beacon.visible = new ArrayList<>();
        creative.creative.beacon.play = new ArrayList<>();
        creative.creative.beacon.click = new ArrayList<>();

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

    private Response getResponse(String json) throws JSONException {
        JSONObject jsonResponse = new JSONObject(json);
        Response response = new Response();
        JSONObject jsonPlacement = jsonResponse.getJSONObject("placement");
        Response.Placement placement = new Response.Placement();
        placement.layout = jsonPlacement.getString("layout");
        placement.articlesBeforeFirstAd = jsonPlacement.optInt("articlesBeforeFirstAd", Integer.MAX_VALUE);
        placement.articlesBetweenAds = jsonPlacement.optInt("articlesBetweenAds", Integer.MAX_VALUE);
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

            creative.creative.key = jsonCreativeInner.getString("creative_key");
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

    public interface Callback {
        void finishedLoading();

        void finishedLoadingWithNoAds();
    }
}
