package com.sharethrough.sdk.mediation;

import android.content.Context;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.network.ASAPManager;
import com.sharethrough.sdk.network.STXNetworkAdapter;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by danicashei on 7/28/16.
 */
public class MediationManager {
    private Context context;
    private BeaconService beaconService;
    private MediationListener mediationListener;
    private MediationWaterfall mediationWaterfall;
    private ASAPManager.AdResponse asapResponse;
    private Map<String, STRMediationAdapter> mediationAdapters = new HashMap<>();

    public interface MediationListener {
        /**
         * Class extending STRMediationAdapter must call this method when it successfully
         * loads an ad
         */
        void onAdLoaded(List<ICreative> creatives);

        /**
         * Classes extending STRMediationAdapter must call this method when it fails
         * to load an ad
         */
        void onAdFailedToLoad();

        /**
         *
         */
        void onAllAdsFailedToLoad();
    }
    public MediationManager(Context context, BeaconService beaconService) {
        this.context = context;
        this.beaconService = beaconService;
    }

    public void initiateWaterfallAndLoadAd(final ASAPManager.AdResponse asapResponse,
                                           final MediationListener mediationListener
    ) {
        setUpWaterfall(asapResponse, mediationListener);
        loadNextAd();
    }

    private void setUpWaterfall(final ASAPManager.AdResponse asapResponse,
                                final MediationListener mediationListener) {
        this.asapResponse = asapResponse;
        this.mediationWaterfall = new MediationWaterfall(asapResponse.mediationNetworks);
        this.mediationListener = mediationListener;
    }

    /**
     * Loads ad(s) using appropriate mediation adapter based on waterfall order
     */
    public void loadNextAd() {
        ASAPManager.AdResponse.Network network = mediationWaterfall.getNextThirdPartyNetwork();
        if (network == null) { // end of waterfall
            mediationListener.onAllAdsFailedToLoad();
        }

        try {
            STRMediationAdapter mediationAdapter  = getMediationAdapter(network);
            mediationAdapters.put(network.name, mediationAdapter);
            Logger.d("Mediating %s as network #" + "%d", network.name, mediationWaterfall.getCurrentIndex()+1);
            mediationAdapter.loadAd(context, mediationListener, asapResponse, network);
        } catch (Exception e) {
            Logger.e("Could not instantiate a STRNetworkManager based off class name: %s", e, network.androidClassName);
            mediationListener.onAdFailedToLoad();
        }
    }

    /**
     * Gets MediationAdapter if it already has been instantiated or instantiates a new one using reflection
     * @param thirdPartyNetwork
     * @return
     * @throws Exception if can not instantiate an adapter based off class name
     */
    private STRMediationAdapter getMediationAdapter(ASAPManager.AdResponse.Network thirdPartyNetwork) throws Exception {
        STRMediationAdapter mediationAdapter = mediationAdapters.get(thirdPartyNetwork.name);
        if (mediationAdapter != null) {
            return mediationAdapter;
        } else {
            mediationAdapter = createAdapter(thirdPartyNetwork.androidClassName);
        }
        return mediationAdapter;
    }

    /**
     * Creates a mediation adapter using reflection
     * @param className
     * @return
     * @throws Exception
     */
    private STRMediationAdapter createAdapter(final String className) throws Exception {
        if (className != null && !className.contains("STX")) {
            final Class<? extends STRMediationAdapter> nativeClass = Class.forName(className)
                    .asSubclass(STRMediationAdapter.class);
            //Preconditions.checkNotNull(nativeClass);

            final Constructor<?> nativeConstructor = nativeClass.getDeclaredConstructor((Class[]) null);
            nativeConstructor.setAccessible(true);
            return (STRMediationAdapter) nativeConstructor.newInstance();
        } else {
            return new STXNetworkAdapter(context, beaconService);
        }
    }

    /**
     * Renders an ad using appropriate mediation adapter
     * @param adView
     * @param creative
     * @param feedPosition
     */
    public void render(IAdView adView, ICreative creative, int feedPosition) {
        String networkName = creative.getNetworkType();
        STRMediationAdapter mediationAdapter = mediationAdapters.get(networkName);

        mediationAdapter.render(adView, creative, feedPosition);
    }

    public class MediationWaterfall {
        private List<ASAPManager.AdResponse.Network> thirdPartyNetworks = new ArrayList<>();
        private int currentIndex = -1;

        public MediationWaterfall (ArrayList<ASAPManager.AdResponse.Network> mediationNetworks) {
            if (mediationNetworks != null) {
                thirdPartyNetworks = mediationNetworks;
            } else {
                thirdPartyNetworks = new ArrayList<>();
            }
        }

        private void incrementIndex() {
            currentIndex++;
        }

        private int getCurrentIndex() {
            return currentIndex;
        }

        public ASAPManager.AdResponse.Network getNextThirdPartyNetwork() {
            incrementIndex();
            return getCurrentThirdPartyNetwork();
        }

        public ASAPManager.AdResponse.Network getCurrentThirdPartyNetwork() {
            if (getCurrentIndex() < thirdPartyNetworks.size()) {
                return thirdPartyNetworks.get(getCurrentIndex());
            } else {
                return null;
            }
        }
    }
}
