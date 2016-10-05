package com.sharethrough.sdk.network;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Pair;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.mediation.ICreative;
import com.sharethrough.sdk.mediation.MediationManager;
import com.sharethrough.sdk.mediation.STRMediationAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class STXNetworkAdapter implements STRMediationAdapter {

    private Context applicationContext;
    private BeaconService beaconService;
    private MediationManager.MediationListener mediationListener;
    protected AdFetcher adFetcher;
    private Renderer renderer;
    private String mediationRequestId = ""; // To remove for asap v2
    private ASAPManager.AdResponse.Network network = new ASAPManager.AdResponse.Network();

    private static final String sharethroughEndPoint = "http://btlr.sharethrough.com/v4";
    public static final String KEY_TYPE = "keyType";
    public static final String KEY_VALUE = "keyValue";

    @Override
    public void loadAd(Context context, MediationManager.MediationListener mediationListener, ASAPManager.AdResponse asapResponse, ASAPManager.AdResponse.Network network) {
        this.mediationListener = mediationListener;
        this.network = network;

        //todo: make android id accessible through singleton
        fetchAds(sharethroughEndPoint, generateQueryStringParams(asapResponse, network), AdvertisingIdProvider.getAdvertisingId(), asapResponse.mrid);
    }

    @Override
    public void render(IAdView adview, ICreative creative, int feedPosition) {
        renderer.putCreativeIntoAdView(adview, ((Creative) creative), beaconService, feedPosition, new Timer("AdView timer for " + creative));
    }

    public STXNetworkAdapter(Context context, BeaconService beaconService) {
        this.applicationContext = context;
        this.beaconService = beaconService;

        this.adFetcher = new AdFetcher(context);
        setAdFetcherListener();
        this.renderer = new Renderer();
    }

    public void setMediationListener(MediationManager.MediationListener mediationListener) {
        this.mediationListener = mediationListener;
    }

    protected void setAdFetcherListener() {
        adFetcher.setAdFetcherListener(new AdFetcher.AdFetcherListener() {
            @Override
            public void onAdResponseLoaded(Response response) {
                handleAdResponseLoaded(response);
            }

            @Override
            public void onAdResponseFailed() {
                handleAdResponseFailed();
            }
        });
    }

    public void handleAdResponseLoaded(Response response) {
        List<ICreative> creatives = convertToCreatives(response);
        Logger.d("Sharethrough STX returned %d creatives ", creatives.size());
        if(creatives.isEmpty()){
            mediationListener.onAdFailedToLoad();
        }else {
            mediationListener.onAdLoaded(creatives);
        }
        // To remove for asap v2
        mediationRequestId = "";
    }

    public void handleAdResponseFailed() {
        mediationListener.onAdFailedToLoad();
        // To remove for asap v2
        mediationRequestId = "";
    }

    protected List<ICreative> convertToCreatives(Response response) {
        ArrayList<ICreative> creatives = new ArrayList<>();
        for (final Response.Creative responseCreative : response.creatives) {
            Creative creative;
            if (responseCreative.creative.action.equals("hosted-video")) {
                if (!responseCreative.creative.forceClickToPlay && response.placement.allowInstantPlay) {
                    creative = new InstantPlayCreative(responseCreative, mediationRequestId);
                } else {
                    creative = new Creative(responseCreative, mediationRequestId);
                }
            } else {
                creative = new Creative(responseCreative, mediationRequestId);
            }
            creative.setNetworkType(network.name);
            creative.setClassName(network.androidClassName);
            creatives.add(creative);
        }
        return creatives;
    }

    public synchronized void fetchAds(String url, List<Pair<String,String>> queryStringParams, String advertisingId, String mediationRequestId){
        // To remove for asap v2
        this.mediationRequestId = mediationRequestId;

        String adRequestUrl = generateRequestUrl(url, queryStringParams, advertisingId);

        Logger.d("ad request sent pkey: %s", queryStringParams.get(0));
        Logger.d("ad request url: %s", adRequestUrl);
        adFetcher.fetchAds(adRequestUrl);
    }

    protected List<Pair<String, String>> generateQueryStringParams(ASAPManager.AdResponse asapResponse, ASAPManager.AdResponse.Network network) {
        ArrayList<Pair<String,String>> queryStringParams = new ArrayList<>();
        queryStringParams.add(new Pair<>(ASAPManager.PLACEMENT_KEY, asapResponse.pkey));

        if (network.parameters.get(KEY_TYPE) != null && network.parameters.get(KEY_VALUE) != null) {
            String keyType = network.parameters.get(KEY_TYPE).getAsString();
            String keyValue = network.parameters.get(KEY_VALUE).getAsString();
            if (!keyType.equals(ASAPManager.PROGRAMMATIC)
                    && !keyType.equals(ASAPManager.ASAP_UNDEFINED)
                    && !keyValue.equals(ASAPManager.ASAP_UNDEFINED)) {
                queryStringParams.add(new Pair<>(keyType, keyValue));
            }
        }

        return queryStringParams;
    }

    private String generateRequestUrl(String url, List<Pair<String, String>> queryStringParams, String advertisingId) {
        if (advertisingId != null) {
            queryStringParams.add(new Pair<>("uid", advertisingId));
        }

        String appPackageName = applicationContext.getPackageName();
        String versionName = null;
        try {
            versionName = applicationContext.getPackageManager().getPackageInfo(appPackageName, PackageManager.GET_META_DATA).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if(versionName != null){
            queryStringParams.add(new Pair<>("appId", versionName));
        }
        queryStringParams.add(new Pair<>("appName", appPackageName));

        Uri.Builder builder = new Uri.Builder();
        for (Pair<String,String> queryParam : queryStringParams) {
            builder.appendQueryParameter(queryParam.first, queryParam.second);
        }

        return url + builder.toString();
    }

    public void setMediationRequestId(String mediationRequestId) {
        this.mediationRequestId = mediationRequestId;
    }

    public String getMediationRequestId() {
        return this.mediationRequestId;
    }
}
