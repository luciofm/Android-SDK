package com.sharethrough.sdk;

import android.util.Log;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sharethrough {
    public static final String API_URL_PREFIX = "http://btlr.sharethrough.com/v3?placement_key=";
    static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    private String key;
    private List<Response.Creative> availableCreatives = new ArrayList<Response.Creative>();

    public Sharethrough(String key) {
        this(EXECUTOR_SERVICE, key);
    }
    Sharethrough(ExecutorService executorService, final String key) {
        this.key = key;

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                String url = API_URL_PREFIX + key;
                ObjectMapper mapper = new ObjectMapper();
                mapper.disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
                try {
                    // TODO: do this on a background thread
                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpGet request = new HttpGet(url);
                    request.addHeader("User-Agent", System.getProperty("http.agent"));
                    InputStream content = client.execute(request).getEntity().getContent();
                    // TODO: handle errors
                    Response response = mapper.readValue(content, Response.class);
                    availableCreatives.addAll(response.creatives);
                } catch (Exception e) {
                    // TODO: log more thoroughly
                    Log.wtf("Sharethrough", "failed to get ads for key " + key, e);
                }
            }
        });
    }

    public Response.Creative getCreative() {
        if (key == null) throw new KeyRequiredException("placement_key is required");
        if (availableCreatives.size() > 0) return availableCreatives.remove(0);
        // TODO: else load more creatives
        return null;
    }
}
