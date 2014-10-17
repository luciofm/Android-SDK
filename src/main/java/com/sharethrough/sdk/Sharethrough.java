package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sharethrough {
    public static final String API_URL_PREFIX = "http://btlr.sharethrough.com/v3?placement_key=";
    static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    private String key;
    private List<Response.Creative> availableCreatives = new ArrayList<Response.Creative>();
    private List<IAdView> waitingAdViews = Collections.synchronizedList(new ArrayList<IAdView>());

    public Sharethrough(String key) {
        this(EXECUTOR_SERVICE, key);
    }
    public Sharethrough(ExecutorService executorService, final String key) {
        if (key == null) throw new KeyRequiredException("placement_key is required");
        this.key = key;

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                String url = API_URL_PREFIX + key;
                ObjectMapper mapper = new ObjectMapper();
                mapper.disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
                try {
                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpGet request = new HttpGet(url);
                    request.addHeader("User-Agent", System.getProperty("http.agent"));
                    InputStream content = client.execute(request).getEntity().getContent();
                    // TODO: handle errors
                    Response response = mapper.readValue(content, Response.class);

                    availableCreatives.addAll(response.creatives);
                    for (IAdView waitingAdView : waitingAdViews) {
                        // TODO: don't go over the end
//                        if (availableCreatives.size() == 0) break;
                        // TODO: else load more creatives
                        putCreativeIntoAdView(availableCreatives.remove(0), waitingAdView);
                    }
                } catch (Exception e) {
                    // TODO: log more thoroughly
                    Log.wtf("Sharethrough", "failed to get ads for key " + key, e);
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public <V extends View & IAdView> void putCreativeIntoAdView(V adView) {
        waitingAdViews.add(adView);
    }

    private static void putCreativeIntoAdView(final Response.Creative creative, final IAdView adView) {
        // TODO: check that the AdView is attached to the window & avoid memory leaks
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
//        adView.addOnAttachStateChangeListener(null);
                adView.getTitle().setText(creative.creative.title);
                adView.getDescription().setText(creative.creative.description);
                adView.getAdvertiser().setText(creative.creative.advertiser);
            }
        });
    }
}
