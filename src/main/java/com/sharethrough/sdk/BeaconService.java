package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class BeaconService {
    private final Provider<Date> dateProvider;
    private final ExecutorService executorService;
    private final AdvertisingIdProvider advertisingIdProvider;
    private final UUID session;

    public BeaconService(final Provider<Date> dateProvider, final UUID session, final ExecutorService executorService, final AdvertisingIdProvider advertisingIdProvider) {
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

        result.put("ua", "" + Sharethrough.USER_AGENT);

        return result;
    }

    Map<String, String> commonParamsWithCreative(final Context context, final Creative creative) {
        Map<String, String> result = commonParams(context);
        result.put("pkey", creative.getPlacementKey());
        result.put("vkey", creative.getVariantKey());
        result.put("ckey", creative.getCreativeKey());
        result.put("as", creative.getSignature());
        result.put("at", creative.getAuctionType());
        result.put("ap", creative.getAuctionPrice());
        return result;
    }

    public <V extends View & IAdView> void adClicked(final String userEvent, final Creative creative, final V adView) {
        Map<String, String> beaconParams = commonParamsWithCreative(adView.getContext(), creative);
        beaconParams.put("pheight", "" + adView.getHeight());
        beaconParams.put("pwidth", "" + adView.getWidth());
        beaconParams.put("type", "userEvent");
        beaconParams.put("userEvent", userEvent);
        beaconParams.put("engagement", "true");

        for (String uri : creative.getClickBeacons()) {
            fireBeacon(new HashMap<String, String>(), "http:" + uri);
        }

        for (String uri : creative.getPlayBeacons()) {
            fireBeacon(new HashMap<String, String>(), "http:" + uri);
        }

        fireBeacon(beaconParams);
    }

    public void adRequested(final Context context, final String placementKey) {
        Map<String, String> beaconParams = commonParams(context);
        beaconParams.put("type", "impressionRequest");
        beaconParams.put("pkey", placementKey);

        fireBeacon(beaconParams);
    }

    public void adReceived(final Context context, final Creative creative) {
        Map<String, String> beaconParams = commonParamsWithCreative(context, creative);
        beaconParams.put("type", "impression");

        fireBeacon(beaconParams);
    }


    public void adVisible(final View adView, final Creative creative) {
        Context context = adView.getContext();
        Map<String, String> beaconParams = commonParamsWithCreative(context, creative);
        beaconParams.put("pheight", "" + adView.getHeight());
        beaconParams.put("pwidth", "" + adView.getWidth());
        beaconParams.put("type", "visible");

        for (String uri : creative.getVisibleBeacons()) {
            fireBeacon(new HashMap<String, String>(), "http:" + uri);
        }

        fireBeacon(beaconParams);
    }

    public void adShared(final Context context, final Creative creative, final String medium) {
        Map<String, String> beaconParams = commonParamsWithCreative(context, creative);
        beaconParams.put("type", "userEvent");
        beaconParams.put("userEvent", "share");
        beaconParams.put("engagement", "true");
        beaconParams.put("share", medium);
        fireBeacon(beaconParams);
    }


    public void videoPlayed(final Context context, final Creative creative, final int percent) {
        Map<String, String> beaconParams = commonParamsWithCreative(context, creative);
        beaconParams.put("type", "completionPercent");
        beaconParams.put("value", String.valueOf(percent));
        fireBeacon(beaconParams);
    }

    private void fireBeacon(final Map<String, String> beaconParams) {
        fireBeacon(beaconParams, Sharethrough.TRACKING_URL);
    }

    private void fireBeacon(final Map<String, String> beaconParams, final String uri) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Uri.Builder uriBuilder = Uri.parse(uri).buildUpon();
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
        });
    }
}
