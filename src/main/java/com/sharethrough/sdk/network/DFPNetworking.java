package com.sharethrough.sdk.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.sharethrough.sdk.Misc;
import com.sharethrough.sdk.STRExecutorService;
import com.sharethrough.sdk.Sharethrough;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutorService;

public class DFPNetworking {
    private static String BASE_URL = "https://platform-cdn.sharethrough.com/placements/";
    private boolean isRunning = false;

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }
    public void fetchDFPPath(final String key, final DFPPathFetcherCallback callback) {
        STRExecutorService.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    return;
                }
                isRunning = true;

                final URI uri = URI.create(BASE_URL + key + "/sdk.json");
                String json = null;
                try {
                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpGet request = new HttpGet(uri);
                    request.addHeader("User-Agent", Sharethrough.USER_AGENT);
                    request.addHeader("Content-Type", "application/json");
                    InputStream content = client.execute(request).getEntity().getContent();
                    json = Misc.convertStreamToString(content);

                    String dfpUrl = parseDFPUrlResponse(json);
                    callback.receivedURL(dfpUrl);

                } catch (Exception e) {
                    String msg = "failed to get dfp for key " + key + ": at uri " + uri;
                    if (json != null) {
                        msg += ": " + json;
                    }
                    Log.e("Sharethrough", msg, e);
                    callback.DFPError(msg);
                    isRunning = false;
                }
            }
        });
    }

    public void fetchCreativeKey(final Context context, final String dfpPath, final DFPCreativeKeyCallback callback) {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                PublisherAdRequest.Builder builder = new PublisherAdRequest.Builder();

                builder.addKeyword(dfpPath);

                PublisherAdRequest adRequest = builder.build();

                PublisherAdView adView = new PublisherAdView(context);
                adView.setAdUnitId(dfpPath);
                adView.setAdSizes(AdSize.BANNER);
                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        callback.DFPKeyError("creative key failed to load; error: " + errorCode);
                    }

                    @Override
                    public void onAdLoaded() {
                        callback.receivedCreativeKey();
                    }
                });

                adView.loadAd(adRequest);
            }
        });

    }

    private static String parseDFPUrlResponse(String json) throws JSONException {
        JSONObject jsonResponse = new JSONObject(json);
        return jsonResponse.getString("dfp_path");
    }

    public interface DFPPathFetcherCallback {
        void receivedURL(String url);

        void DFPError(String errorMessage);
    }

    public interface DFPCreativeKeyCallback {
        void receivedCreativeKey();

        void DFPKeyError(String errorMessage);
    }

}
