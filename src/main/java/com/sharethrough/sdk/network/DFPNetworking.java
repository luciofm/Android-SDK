package com.sharethrough.sdk.network;

import android.util.Log;
import com.sharethrough.sdk.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutorService;

public class DFPNetworking {

    private static String BASE_URL = "https://native.sharethrough.com/placements/";

    public static void FetchDFPEndpoint(ExecutorService executorService, final String key, final DFPFetcherCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
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
                }
            }
        });
    }

    private static String parseDFPUrlResponse(String json) throws JSONException {
        JSONObject jsonResponse = new JSONObject(json);
        return jsonResponse.getString("dfp_path");
    }

    public interface DFPFetcherCallback {
        void receivedURL(String url);

        void DFPError(String errorMessage);
    }

}
