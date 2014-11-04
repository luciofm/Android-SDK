package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class BeaconService {
    private final Provider<Date> dateProvider;
    private final StrSession session;
    private final ExecutorService executorService;

    public BeaconService(Provider<Date> dateProvider, StrSession session, ExecutorService executorService) {
        this.dateProvider = dateProvider;
        this.session = session;
        this.executorService = executorService;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    Map<String, String> commonParams(Context context) {
        Map<String, String> result = new HashMap();
        result.put("umtime", String.valueOf(dateProvider.get().getTime()));
        result.put("ploc", context.getPackageName());

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        result.put("bwidth", "" + size.x);
        result.put("bheight", "" + size.y);

        result.put("uid", "TODO");
        result.put("session", session.toString());

        result.put("ua", Sharethrough.USER_AGENT);

        return result;
    }

    Map<String, String> commonParamsWithCreative(Context context, Creative creative) {
        Map<String, String> result = commonParams(context);
        result.put("pkey", creative.getPlacementKey());
        result.put("vkey", creative.getVariantKey());
        result.put("ckey", creative.getCreativeKey());
        result.put("as", creative.getSignature());
        result.put("at", creative.getAuctionType());
        result.put("ap", creative.getAuctionPrice());
        return result;
    }

    public void adClicked(final Context context, final String userEvent, final Creative creative, final IAdView adView) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Uri.Builder uriBuilder = Uri.parse("http://b.sharethrough.com/butler").buildUpon();
                Map<String, String> commonParams = commonParamsWithCreative(context, creative);
                for (Map.Entry<String, String> entry : commonParams.entrySet()) {
                    uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
                }

                uriBuilder.appendQueryParameter("pheight", "" + adView.getHeight());
                uriBuilder.appendQueryParameter("pwidth", "" + adView.getWidth());

                uriBuilder.appendQueryParameter("type", "userEvent");
                uriBuilder.appendQueryParameter("userEvent", userEvent);
                uriBuilder.appendQueryParameter("engagement", "true");

                DefaultHttpClient client = new DefaultHttpClient();
                String url = uriBuilder.build().toString();
                Log.i("Sharethrough", "beacon:\t" + url);
                HttpGet request = new HttpGet(url);
                request.addHeader("User-Agent", Sharethrough.USER_AGENT);
                try {
                    client.execute(request);
                } catch (IOException e) {
                    Log.e("Sharethrough", "beacon fire failed for " + url, e);
                }
            }
        });
    }
}
