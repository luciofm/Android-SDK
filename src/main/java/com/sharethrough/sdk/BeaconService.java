package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class BeaconService {
    public static String TRACKING_URL = "http://b.sharethrough.com/butler";
    private final Provider<Date> dateProvider;
    private final AdvertisingIdProvider advertisingIdProvider;
    private final UUID session;
    private final String appPackageName;
    private final Context context;
    private String appVersionName;
    private String placementKey;

    public BeaconService(final Provider<Date> dateProvider, final UUID session, final AdvertisingIdProvider advertisingIdProvider, Context context, String pkey) {
        this.dateProvider = dateProvider;
        this.session = session;
        this.advertisingIdProvider = advertisingIdProvider;
        this.context = context;
        this.placementKey = pkey;
        appPackageName = context.getPackageName();
        try {
            appVersionName = context.getPackageManager().getPackageInfo(appPackageName, PackageManager.GET_META_DATA).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            appVersionName = "unknown";
            e.printStackTrace();
        }
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

        if (advertisingIdProvider.getAdvertisingId() != null) result.put("uid", advertisingIdProvider.getAdvertisingId());
        result.put("session", session.toString());

        result.put("ua", "" + Sharethrough.USER_AGENT + "; " + appPackageName);
        result.put("appName", appPackageName);
        result.put("appId", appVersionName);

        return result;
    }

    Map<String, String> commonParamsWithCreative(final Context context, final Creative creative) {
        Map<String, String> result = commonParams(context);
        result.put("pkey", placementKey);
        result.put("vkey", creative.getVariantKey());
        result.put("ckey", creative.getCreativeKey());
        result.put("campkey", creative.getCampaignKey());
        result.put("as", creative.getSignature());
        result.put("at", creative.getAuctionType());
        result.put("ap", creative.getAuctionPrice());
        result.put("arid", creative.getAdserverRequestId());
        result.put("awid", creative.getAuctionWinId());

        return result;
    }

    public void fireArticleDurationForAd(final Context context, final Creative creative, long timeSpentInView){
        Map<String, String> beaconParams = commonParamsWithCreative(context, creative);
        beaconParams.put("type","userEvent");
        beaconParams.put("userEvent","articleViewDuration");
        beaconParams.put("duration",String.valueOf(timeSpentInView));
        beaconParams.put("engagement", "true");
        fireBeacon(beaconParams);
    }

    public void adClicked(final String userEvent, final Creative creative, View view, int feedPosition, Placement placement) {
        Map<String, String> beaconParams = commonParamsWithCreative(view.getContext(), creative);
        beaconParams.put("pheight", "" + view.getHeight());
        beaconParams.put("pwidth", "" + view.getWidth());
        beaconParams.put("type", "userEvent");
        beaconParams.put("userEvent", userEvent);
        beaconParams.put("engagement", "true");
        beaconParams.put("placementIndex", String.valueOf(feedPosition));


        fireThirdPartyBeacons(creative.getClickBeacons(), placement.getStatus());
        fireThirdPartyBeacons(creative.getPlayBeacons(), placement.getStatus());
        fireBeacon(beaconParams);
    }

    private void fireThirdPartyBeacons(List<String> thirdPartyBeacons, String status) {
        if (!status.equals("pre-live")) {
            for (String uri : thirdPartyBeacons) {
                String cachedBustedUri = replaceCacheBusterParam(uri);
                fireBeacon(new HashMap<String, String>(), "http:" + cachedBustedUri);
            }
        }
    }

    public void adRequested(final String placementKey) {
        Map<String, String> beaconParams = commonParams(context);
        beaconParams.put("type", "impressionRequest");
        beaconParams.put("pkey", placementKey);

        fireBeacon(beaconParams);
    }

    public void adReceived(final Context context, final Creative creative, int feedPosition, final Placement placement) {
        Map<String, String> beaconParams = commonParamsWithCreative(context, creative);
        beaconParams.put("type", "impression");
        beaconParams.put("placementIndex", String.valueOf(feedPosition));

        fireThirdPartyBeacons(creative.getImpressionBeacons(), placement.getStatus());
        fireBeacon(beaconParams);
    }

    public void adVisible(final View adView, final Creative creative, int feedPosition, Placement placement) {
        Context context = adView.getContext();
        Map<String, String> beaconParams = commonParamsWithCreative(context, creative);
        beaconParams.put("pheight", "" + adView.getHeight());
        beaconParams.put("pwidth", "" + adView.getWidth());
        beaconParams.put("type", "visible");
        beaconParams.put("placementIndex", String.valueOf(feedPosition));

        fireThirdPartyBeacons(creative.getVisibleBeacons(), placement.getStatus());
        fireBeacon(beaconParams);
    }


    public void adShared(final Context context, final Creative creative, final String medium, int feedPosition) {
        Map<String, String> beaconParams = commonParamsWithCreative(context, creative);
        beaconParams.put("type", "userEvent");
        beaconParams.put("userEvent", "share");
        beaconParams.put("engagement", "true");
        beaconParams.put("share", medium);
        beaconParams.put("placementIndex", String.valueOf(feedPosition));
        fireBeacon(beaconParams);
    }

    public void videoPlayed(final Context context, final Creative creative, final int percent, int feedPosition) {
        Map<String, String> beaconParams = commonParamsWithCreative(context, creative);
        beaconParams.put("type", "completionPercent");
        beaconParams.put("value", String.valueOf(percent));
        beaconParams.put("placementIndex", String.valueOf(feedPosition));
        fireBeacon(beaconParams);
    }

    private void fireBeacon(final Map<String, String> beaconParams) {
        fireBeacon(beaconParams, TRACKING_URL);
    }

    private void fireBeacon(final Map<String, String> beaconParams, final String uri) {
        STRExecutorService.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                Uri.Builder uriBuilder = Uri.parse(uri).buildUpon();
                for (Map.Entry<String, String> entry : beaconParams.entrySet()) {
                    uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
                }


                DefaultHttpClient client = new DefaultHttpClient();
                String url = uriBuilder.build().toString();


                url = url.replace("[", "%5B").replace("]", "%5D");
                Logger.d("beacon fired type: %s", beaconParams.get("type") == null ? "third party beacon " : beaconParams.get("type"));
                Logger.d("beacon user event: %s", beaconParams.get("userEvent"));
                Logger.i("beacon url: %s", url);
                try {
                    HttpGet request = new HttpGet(url);
                    request.addHeader("User-Agent", Sharethrough.USER_AGENT + "; " + appPackageName);
                    client.execute(request);
                } catch (Exception e) {
                    Logger.e("beacon fired failed for %s", e, url);
                }
            }
        });
    }

    private String replaceCacheBusterParam(String uri) {
        return uri.replaceAll("\\[timestamp\\]", String.valueOf(dateProvider.get().getTime()));
    }

    public String getAppPackageName() {
        return appPackageName;
    }

    public String getAppVersionName() {
        return appVersionName;
    }
}
