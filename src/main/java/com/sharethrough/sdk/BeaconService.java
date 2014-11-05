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
    private final AdvertisingIdProvider advertisingIdProvider;

    public BeaconService(Provider<Date> dateProvider, StrSession session, ExecutorService executorService, AdvertisingIdProvider advertisingIdProvider) {
        this.dateProvider = dateProvider;
        this.session = session;
        this.executorService = executorService;
        this.advertisingIdProvider = advertisingIdProvider;
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

        result.put("uid", advertisingIdProvider.getAdvertisingId());
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
                Map<String, String> beaconParams = commonParamsWithCreative(context, creative);
                beaconParams.put("pheight", "" + adView.getHeight());
                beaconParams.put("pwidth", "" + adView.getWidth());
                beaconParams.put("type", "userEvent");
                beaconParams.put("userEvent", userEvent);
                beaconParams.put("engagement", "true");

                fireBeacon(context, beaconParams);
            }
        });
    }

    public void adRequested(final Context context, final String placementKey) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Map<String, String> beaconParams = commonParams(context);
                beaconParams.put("type", "userEvent");
                beaconParams.put("userEvent", "impressionRequest");
                beaconParams.put("pkey", placementKey);

                fireBeacon(context, beaconParams);
            }
        });
    }

    public void adReceived(final Context context, final Creative creative) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Map<String, String> beaconParams = commonParamsWithCreative(context, creative);
                beaconParams.put("type", "impression");
                fireBeacon(context, beaconParams);
            }
        });
    }

    public void fireBeacon(Context context, Map<String,String> beaconParams) {
        Uri.Builder uriBuilder = Uri.parse("http://b.sharethrough.com/butler").buildUpon();
        for (Map.Entry<String, String> entry : beaconParams.entrySet()) {
            uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

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
}
