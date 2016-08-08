package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import com.sharethrough.android.sdk.BuildConfig;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BeaconService {
    public static String TRACKING_URL = "http://b.sharethrough.com/butler";
    private final Provider<Date> dateProvider;
    private final AdvertisingIdProvider advertisingIdProvider;
    private final UUID session;
    private String placementKey;
    private final ContextInfo contextInfo;
    private RequestQueue requestQueue;

    public BeaconService(final Provider<Date> dateProvider, final UUID session, final AdvertisingIdProvider advertisingIdProvider,final ContextInfo contextInfo,final String pkey) {
        this.dateProvider = dateProvider;
        this.session = session;
        this.advertisingIdProvider = advertisingIdProvider;
        this.placementKey = pkey;
        this.contextInfo = contextInfo;
        this.requestQueue = new RequestQueue(new NoCache(), new BasicNetwork(new HurlStack()));
        this.requestQueue.start();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    Map<String, String> commonParams() {
        Map<String, String> result = new HashMap();
        result.put("umtime", String.valueOf(dateProvider.get().getTime()));
        result.put("ploc", contextInfo.getAppPackageName());
        result.put("bwidth", "" + contextInfo.getScreenSize().x);
        result.put("bheight", "" + contextInfo.getScreenSize().y);

        if (advertisingIdProvider.getAdvertisingId() != null) result.put("uid", advertisingIdProvider.getAdvertisingId());
        result.put("session", session.toString());

        result.put("ua", Build.MODEL + "; Android " + Build.VERSION.RELEASE + "; " + contextInfo.getAppPackageName() + "; STR " + BuildConfig.VERSION_NAME);
        result.put("appName", contextInfo.getAppPackageName());
        result.put("appId", contextInfo.getAppVersionName());

        return result;
    }

    Map<String, String> commonParamsWithCreative(final Creative creative) {
        Map<String, String> result = commonParams();
        result.put("pkey", placementKey);
        result.put("vkey", creative.getVariantKey());
        result.put("ckey", creative.getCreativeKey());
        result.put("campkey", creative.getCampaignKey());
        result.put("arid", creative.getAdserverRequestId());
        result.put("awid", creative.getAuctionWinId());
        if(false == creative.getDealId().isEmpty()) {
            result.put("deal_id", creative.getDealId());
        }
        result.put("mrid", creative.getMediationRequestId());
        return result;
    }

    public void fireArticleDurationForAd(final Creative creative, long timeSpentInView){
        Map<String, String> beaconParams = commonParamsWithCreative(creative);
        beaconParams.put("type","userEvent");
        beaconParams.put("userEvent","articleViewDuration");
        beaconParams.put("duration", String.valueOf(timeSpentInView));
        beaconParams.put("engagement", "true");
        fireBeacon(beaconParams);
    }

    public void adClicked(final String userEvent, final Creative creative, View view, int feedPosition) {
        Map<String, String> beaconParams = commonParamsWithCreative(creative);
        beaconParams.put("pheight", "" + view.getHeight());
        beaconParams.put("pwidth", "" + view.getWidth());
        beaconParams.put("type", "userEvent");
        beaconParams.put("userEvent", userEvent);
        beaconParams.put("engagement", "true");
        beaconParams.put("placementIndex", String.valueOf(feedPosition));


        fireThirdPartyBeacons(creative.getClickBeacons());
        fireThirdPartyBeacons(creative.getPlayBeacons());
        fireBeacon(beaconParams);
    }

    private void fireThirdPartyBeacons(List<String> thirdPartyBeacons) {
        for (String uri : thirdPartyBeacons) {
            String cachedBustedUri = replaceCacheBusterParam(uri);
            fireBeacon(new HashMap<String, String>(), "http:" + cachedBustedUri);
        }
    }

    public void adRequested(final String placementKey) {
        Map<String, String> beaconParams = commonParams();
        beaconParams.put("type", "impressionRequest");
        beaconParams.put("pkey", placementKey);

        fireBeacon(beaconParams);
    }

    public void adReceived(final Context context, final Creative creative, int feedPosition) {
        Map<String, String> beaconParams = commonParamsWithCreative(creative);
        beaconParams.put("type", "impression");
        beaconParams.put("placementIndex", String.valueOf(feedPosition));

        fireThirdPartyBeacons(creative.getImpressionBeacons());
        fireBeacon(beaconParams);
    }

    public void adVisible(final View adView, final Creative creative, int feedPosition) {
        Context context = adView.getContext();
        Map<String, String> beaconParams = commonParamsWithCreative(creative);
        beaconParams.put("pheight", "" + adView.getHeight());
        beaconParams.put("pwidth", "" + adView.getWidth());
        beaconParams.put("type", "visible");
        beaconParams.put("placementIndex", String.valueOf(feedPosition));

        fireThirdPartyBeacons(creative.getVisibleBeacons());
        fireBeacon(beaconParams);
    }


    public void adShared(final Context context, final Creative creative, final String medium, int feedPosition) {
        Map<String, String> beaconParams = commonParamsWithCreative(creative);
        beaconParams.put("type", "userEvent");
        beaconParams.put("userEvent", "share");
        beaconParams.put("engagement", "true");
        beaconParams.put("share", medium);
        beaconParams.put("placementIndex", String.valueOf(feedPosition));
        fireBeacon(beaconParams);
    }

    public void autoplayVideoEngagement(final Context context, final Creative creative, final int duration, int feedPosition) {
        Map<String, String> beaconParams = commonParamsWithCreative(creative);
        beaconParams.put("type", "userEvent");
        beaconParams.put("userEvent", "autoplayVideoEngagement");
        beaconParams.put("videoDuration", String.valueOf(duration));
        beaconParams.put("placementIndex", String.valueOf(feedPosition));
        fireBeacon(beaconParams);
    }

    public void silentAutoPlayDuration(final Creative creative, final int duration, int feedPosition) {
        Map<String, String> beaconParams = commonParamsWithCreative(creative);
        beaconParams.put("type", "silentAutoPlayDuration");
        beaconParams.put("duration", String.valueOf(duration));
        beaconParams.put("placementIndex", String.valueOf(feedPosition));
        silentThirdPartyAutoDuration(creative, duration);
        fireBeacon(beaconParams);

    }

    public void silentThirdPartyAutoDuration(final Creative creative, final int duration) {
        if( duration == 3000 ) {
            fireThirdPartyBeacons(creative.getSilentPlayBeacons());
        }
        else if( duration == 10000) {
            fireThirdPartyBeacons(creative.getTenSecondSilentPlayBeacons());
        }
        else if( duration == 15000) {
            fireThirdPartyBeacons(creative.getFifteenSecondSilentPlayBeacons());
        }
        else if( duration == 30000) {
            fireThirdPartyBeacons(creative.getThirtySecondSilentPlayBeacons());
        }
    }

    public void videoViewDuration(final Creative creative, final int duration, final boolean isSilent, int feedPosition) {
        Map<String, String> beaconParams = commonParamsWithCreative(creative);
        beaconParams.put("type", "videoViewDuration");
        beaconParams.put("duration", String.valueOf(duration));
        beaconParams.put("silent", String.valueOf(isSilent));
        beaconParams.put("placementIndex", String.valueOf(feedPosition));
        fireBeacon(beaconParams);
    }

    public void videoPlayed(final Context context, final Creative creative, final int percent, final boolean isSilent, int feedPosition) {
        if( percent >= 95 && creative.getCompletedSilentPlayBeacons()!= null ){
            fireThirdPartyBeacons(creative.getCompletedSilentPlayBeacons());
        }

        Map<String, String> beaconParams = commonParamsWithCreative(creative);
        beaconParams.put("type", "completionPercent");
        beaconParams.put("value", String.valueOf(percent));
        beaconParams.put("isSilentPlay", String.valueOf(isSilent));
        beaconParams.put("placementIndex", String.valueOf(feedPosition));
        fireBeacon(beaconParams);
    }

    private void fireBeacon(final Map<String, String> beaconParams) {
        fireBeacon(beaconParams, TRACKING_URL);
    }

    private void fireBeacon(final Map<String, String> beaconParams, final String uri) {
//        STRExecutorService.getInstance().execute(new Runnable() {
//            @Override
//            public void run() {
//                Uri.Builder uriBuilder = Uri.parse(uri).buildUpon();
//                for (Map.Entry<String, String> entry : beaconParams.entrySet()) {
//                    uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
//                }
//
//
//                String url = uriBuilder.build().toString();
//
//
//                url = url.replace("[", "%5B").replace("]", "%5D");
//                Logger.d("beacon fired type: %s", beaconParams.get("type") == null ? "third party beacon " : beaconParams.get("type"));
//                Logger.d("beacon user event: %s", beaconParams.get("userEvent"));
//                Logger.i("beacon url: %s", url);
//
//                try {
//                    AdFetcher.STRStringRequest
//
//                    request.addHeader("User-Agent", Sharethrough.USER_AGENT + "; " + contextInfo.getAppPackageName());
//
//
//                    /**
//                     *  STRStringRequest stringRequest = new STRStringRequest(Request.Method.GET, adRequestUrl,
//                     new com.android.volley.Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//                    handleResponse(response);
//                    }
//                    }, new com.android.volley.Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                    Logger.i("Ad request error: " + error.toString());
//                    adFetcherListener.onAdResponseFailed();
//                    }
//                    });
//
//                     requestQueue.add(stringRequest);
//                     */
//                    client.execute(request);
//                } catch (Exception e) {
//                    Logger.e("beacon fired failed for %s", e, url);
//                }
//            }
//        });
    }

    private String replaceCacheBusterParam(String uri) {
        return uri.replaceAll("\\[timestamp\\]", String.valueOf(dateProvider.get().getTime()));
    }
}
