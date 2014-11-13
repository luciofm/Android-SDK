package com.sharethrough.sdk.network;

import android.util.Log;
import com.sharethrough.sdk.Callback;
import com.sharethrough.sdk.Misc;
import com.sharethrough.sdk.Placement;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class PlacementTemplateFetcher {
    private final static String API_FORMAT_STRING = "http://native.sharethrough.com/placements/PLACEMENT_KEY/template.json";
    private final String placementKey;
    private final ExecutorService executorService;

    public PlacementTemplateFetcher(String placementKey, ExecutorService executorService) {
        this.placementKey = placementKey;
        this.executorService = executorService;
    }

    public void fetch(final Callback<Placement> callback) {
        final String apiUrl = API_FORMAT_STRING.replace("PLACEMENT_KEY", placementKey);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                HttpClient client = new DefaultHttpClient();
                try {
                    HttpResponse response = client.execute(new HttpGet(apiUrl));
                    JSONObject jsonObject = new JSONObject(Misc.convertStreamToString(response.getEntity().getContent()));
                    Placement placement = new Placement(jsonObject.getInt("articlesBeforeFirstAd"), jsonObject.getInt("articlesBetweenAds"));
                    callback.call(placement);
                } catch (JSONException | IOException e) {
                    Log.e("Sharethrough", "failed to get placement template for key: " + placementKey, e);
                }
            }
        });
    }
}
