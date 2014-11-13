package com.sharethrough.sdk.network;

import android.content.Context;
import android.util.Log;
import com.sharethrough.sdk.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class AdFetcher {
    public static final String USER_AGENT = System.getProperty("http.agent");
    private final String apiUrlPrefix;
    private final ExecutorService executorService;
    private final Function<Creative, Void> creativeHandler;
    private final BeaconService beaconService;
    private final Context context;
    private final String key;

    public AdFetcher(Context context, String key, String apiUrlPrefix, ExecutorService executorService, Function<Creative, Void> creativeHandler, BeaconService beaconService) {
        this.context = context;
        this.key = key;
        this.apiUrlPrefix = apiUrlPrefix;
        this.executorService = executorService;
        this.creativeHandler = creativeHandler;
        this.beaconService = beaconService;
    }

    public void fetch() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                beaconService.adRequested(context, key);
                try {
                    String urlString = apiUrlPrefix + key;
                    final URI uri = URI.create(urlString);

                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpGet request = new HttpGet(uri);
                    request.addHeader("User-Agent", USER_AGENT);
                    // TODO: handle errors
                    InputStream content = client.execute(request).getEntity().getContent();
                    Response response = getResponse(content);
//                    ObjectMapper mapper = new ObjectMapper();
//                    mapper.disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
//                    Response response = mapper.readValue(content, Response.class);

                    for (final Response.Creative responseCreative : response.creatives) {
                        new ImageFetcher(executorService, key).fetchImage(responseCreative, creativeHandler, uri);
                    }
                } catch (Exception e) {
                    // TODO: log more thoroughly
                    Log.wtf("Sharethrough", "failed to get ads for key " + key, e);
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

    private Response getResponse(InputStream content) throws JSONException {
        JSONObject jsonResponse = new JSONObject(Misc.convertStreamToString(content));
        Response response = new Response();
        JSONArray creatives = jsonResponse.getJSONArray("creatives");
        response.creatives = new ArrayList<Response.Creative>(creatives.length());
        for (int i = 0; i < creatives.length(); i++) {
            JSONObject jsonCreative = creatives.getJSONObject(i);
            Response.Creative creative = new Response.Creative();
            creative.price = jsonCreative.getInt("price");

            JSONObject jsonCreativeInner = jsonCreative.getJSONObject("creative");

            creative.creative = new Response.Creative.CreativeInner();
            creative.creative.action = jsonCreativeInner.getString("action");
            creative.creative.mediaUrl = jsonCreativeInner.getString("media_url");
            creative.creative.shareUrl = jsonCreativeInner.getString("share_url");
            creative.creative.title = jsonCreativeInner.getString("title");
            creative.creative.description = jsonCreativeInner.getString("description");
            creative.creative.advertiser = jsonCreativeInner.getString("advertiser");
            creative.creative.thumbnailUrl = jsonCreativeInner.getString("thumbnail_url");

            creative.creative.key = jsonCreativeInner.getString("creative_key");
            creative.creative.variantKey = jsonCreativeInner.getString("variant_key");
            creative.price = jsonCreative.getInt("price");
            creative.priceType = jsonCreative.getString("priceType");
            creative.signature = jsonCreative.getString("signature");

            // TODO: more fields
            JSONObject beacons = jsonCreativeInner.getJSONObject("beacons");
            creative.creative.beacon = new Response.Creative.CreativeInner.Beacon();
            parseBeacons(creative, beacons);

            response.creatives.add(creative);
        }
        return response;
    }
}
