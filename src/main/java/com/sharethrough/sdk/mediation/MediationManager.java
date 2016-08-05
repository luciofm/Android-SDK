package com.sharethrough.sdk.mediation;

import android.content.Context;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.network.ASAPManager;
import com.sharethrough.sdk.network.AdManager;

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
    private boolean isWaterfallRunning = false;

    public final String STR_Network = "stx";
    public final String FAN_NETWORK = "fan";

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
        if (isWaterfallRunning) {
            return;
        }

        isWaterfallRunning = true;
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
     * Needs to be called by MediationListener when an ad loads successfully so the next
     * waterfall can be initiated properly. Also needs to be called when waterfall is
     * exhausted and all ads failed to load
     */
    public void setWaterfallComplete() {
        isWaterfallRunning = false;
    }

    public void loadNextAd() {
        ASAPManager.AdResponse.Network network = mediationWaterfall.getNextThirdPartyNetwork();
        STRMediationAdapter mediationAdapter = getMediationAdapter(network.name);

        if (mediationAdapter != null) {
            mediationAdapter.loadAd(context, mediationListener, asapResponse, network);
        } else {
            //end of waterfall
            mediationListener.onAllAdsFailedToLoad();
            setWaterfallComplete();
        }
    }

    private STRMediationAdapter getMediationAdapter(String thirdPartyNetwork) {
        STRMediationAdapter mediationAdapter = mediationAdapters.get(thirdPartyNetwork);
        if (mediationAdapter != null) {
            return mediationAdapter;
        } else {
            switch (thirdPartyNetwork) {
                case STR_Network:
                    mediationAdapter = new AdManager(context, beaconService);
                    mediationAdapters.put(thirdPartyNetwork, mediationAdapter);
                    Logger.d("Mediating Sharethrough as network #" + "%d", mediationWaterfall.getCurrentIndex()+1);
                case FAN_NETWORK:
                    mediationAdapter = new FANAdapter();
                    mediationAdapters.put(thirdPartyNetwork, mediationAdapter);
                    Logger.d("Mediating Facebook as network #" + "%d", mediationWaterfall.getCurrentIndex()+1);
            }
            return mediationAdapter;
        }
    }

    public void render(IAdView adView, ICreative creative, int feedPosition) {
        String networkName = creative.getNetworkType();
        STRMediationAdapter mediationAdapter = mediationAdapters.get(networkName);

        mediationAdapter.render(adView, creative, feedPosition);
    }

//    public IRenderer getRenderer(String thirdPartyNetwork) {
//        IRenderer renderer = null;
//        if (renderers.get(thirdPartyNetwork) != null) {
//            return renderers.get(thirdPartyNetwork);
//        } else {
//            switch (thirdPartyNetwork) {
//                case STR_Network:
//                    renderer = new Renderer();
//                    renderers.put(thirdPartyNetwork, renderer);
//                    return renderer;
//                case FAN_NETWORK:
//                    renderer = new Renderer();
//                    renderers.put(thirdPartyNetwork, renderer);
//                    return renderer;
//            }
//            return renderer;
//        }
//    }

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
            return thirdPartyNetworks.get(getCurrentIndex());
        }

    }
}
