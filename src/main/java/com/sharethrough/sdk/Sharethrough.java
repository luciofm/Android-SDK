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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sharethrough {
    private String apiUrlPrefix = "http://btlr.sharethrough.com/v3?placement_key=";
    static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(4); // TODO: pick a reasonable number
    public static final String USER_AGENT = System.getProperty("http.agent");
    private String key;
    private List<Creative> availableCreatives = Collections.synchronizedList(new ArrayList<Creative>());
    private List<IAdView> waitingAdViews = Collections.synchronizedList(new ArrayList<IAdView>());

    public Sharethrough(Context context, String key) {
        this(context, EXECUTOR_SERVICE, key);
    }

    public Sharethrough(Context context, final ExecutorService executorService, final String key) {
        if (key == null) throw new KeyRequiredException("placement_key is required");
        this.key = key;

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
                                        Creative creative = new Creative(responseCreative, imageBytes);
                                        synchronized (waitingAdViews) {
                                            if (waitingAdViews.size() > 0) {
                                                IAdView adView = waitingAdViews.remove(0);
                                                creative.putIntoAdView(adView);
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
            creative.creative.mediaUrl = jsonCreativeInner.getString("media_url");
            creative.creative.shareUrl = jsonCreativeInner.getString("share_url");
            creative.creative.title = jsonCreativeInner.getString("title");
            creative.creative.description = jsonCreativeInner.getString("description");
            creative.creative.advertiser = jsonCreativeInner.getString("advertiser");
            creative.creative.thumbnailUrl = jsonCreativeInner.getString("thumbnail_url");

            response.creatives.add(creative);
        }
        return response;
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
    public <V extends View & IAdView> void putCreativeIntoAdView(V adView) {
        synchronized (availableCreatives) {
            if (availableCreatives.size() > 0) {
                availableCreatives.remove(0).putIntoAdView(adView);
            } else {
                waitingAdViews.add(adView);
            }
        }
    }
}
