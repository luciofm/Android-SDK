package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Sharethrough<V extends View & IAdView> {
    public static final int DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(20);
    private static final int MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS = (int) TimeUnit.SECONDS.toMillis(20);
    private final Renderer renderer;
    private final BeaconService beaconService;
    private final int adCacheTimeInMilliseconds;
    private String apiUrlPrefix = "http://btlr.sharethrough.com/v3?placement_key=";
    static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(4); // TODO: pick a reasonable number
    public static final String USER_AGENT = System.getProperty("http.agent");
    private String key;
    private List<Creative> availableCreatives = Collections.synchronizedList(new ArrayList<Creative>());
    private List<V> waitingAdViews = Collections.synchronizedList(new ArrayList<V>());

    public Sharethrough(Context context, String key) {
        this(context, key, DEFAULT_AD_CACHE_TIME_IN_MILLISECONDS);
    }

    public Sharethrough(Context context, String key, int adCacheTimeInMilliseconds) {
        this(context, EXECUTOR_SERVICE, key, new Renderer(new Timer("Sharethrough visibility watcher")), new BeaconService(new DateProvider(), new StrSession(), EXECUTOR_SERVICE, new AdvertisingIdProvider(context, EXECUTOR_SERVICE, UUID.randomUUID().toString())), adCacheTimeInMilliseconds );
    }

    Sharethrough(final Context context, final ExecutorService executorService, final String key, final Renderer renderer, final BeaconService beaconService, int adCacheTimeInMilliseconds) {
        this.beaconService = beaconService;
        this.adCacheTimeInMilliseconds = Math.max(adCacheTimeInMilliseconds, MINIMUM_AD_CACHE_TIME_IN_MILLISECONDS);
        if (key == null) throw new KeyRequiredException("placement_key is required");
        this.key = key;
        this.renderer = renderer;

        try {
            Bundle bundle = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            if (bundle != null) {
                String adserverApi = bundle.getString("STR_ADSERVER_API");
                if (adserverApi != null) apiUrlPrefix = adserverApi;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

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
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    DefaultHttpClient client = new DefaultHttpClient();

                                    URI imageURI = URIUtils.resolve(uri, responseCreative.creative.thumbnailUrl);

                                    HttpGet imageRequest = new HttpGet(imageURI);
                                    Log.d("Sharethrough", "fetching image:\t" + imageURI.toString());
                                    imageRequest.addHeader("User-Agent", USER_AGENT);
                                    HttpResponse imageResponse = client.execute(imageRequest);
                                    if (imageResponse.getStatusLine().getStatusCode() == 200) {
                                        InputStream imageContent = imageResponse.getEntity().getContent();
                                        byte[] imageBytes = convertInputStreamToByteArray(imageContent);
                                        Creative creative = new Creative(responseCreative, imageBytes, key, beaconService);
                                        synchronized (waitingAdViews) {
                                            if (waitingAdViews.size() > 0) {
                                                V adView = waitingAdViews.remove(0);
                                                renderer.putCreativeIntoAdView(adView, creative, beaconService, Sharethrough.this);
                                            } else {
                                                availableCreatives.add(creative);
                                            }
                                        }
                                    } else {
                                        Log.wtf("Sharethrough", "failed to load image from url: " + imageURI + " ; server said: " + imageResponse.getStatusLine().getStatusCode() + "\t" + imageResponse.getStatusLine().getReasonPhrase());
                                    }
                                } catch (IOException e) {
                                    Log.wtf("Sharethrough", "failed to load image from url: " + responseCreative.creative.thumbnailUrl, e);
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    // TODO: log more thoroughly
                    Log.wtf("Sharethrough", "failed to get ads for key " + key, e);
                }
            }
        });
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

    private byte[] convertInputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(inputStream.available());

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void putCreativeIntoAdView(V adView) {
        synchronized (availableCreatives) {
            if (availableCreatives.size() > 0) {
                renderer.putCreativeIntoAdView(adView, availableCreatives.remove(0), beaconService, this);
            } else {
                waitingAdViews.add(adView);
            }
        }
    }

    public int getAdCacheTimeInMilliseconds() {
        return adCacheTimeInMilliseconds;
    }
}
