package com.sharethrough.sdk.mediation;

import android.content.Context;
import com.facebook.ads.NativeAd;
import com.sharethrough.sdk.Placement;
import com.sharethrough.sdk.Renderer;
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
    private MediationListener mediationListener;
    private MediationWaterfall mediationWaterfall;
    private ASAPManager.AdResponse asapResponse;
    private Map<String, STRMediationAdapter> mediationAdapters = new HashMap<>();
    private Map<String, IRenderer> renderers = new HashMap<>();
    private boolean isWaterfallRunning = false;

    public interface MediationListener {
        /**
         * Class extending STRMediationAdapter must call this method when it successfully
         * loads an ad
         */
        void onAdLoaded(List<ICreative> creatives, Placement placement);

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
    public MediationManager(Context context) {
        this.context = context;
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
        if (mediationAdapters.get(thirdPartyNetwork) != null) {
            return mediationAdapters.get(thirdPartyNetwork);
        } else {
            STRMediationAdapter result = null;
            switch (thirdPartyNetwork) {
                case "STR":
                    result = new AdManager(context);
                    mediationAdapters.put(thirdPartyNetwork, result);
                    return result;
                case "FAN":
                    result = new FANAdapter();
                    mediationAdapters.put(thirdPartyNetwork, result);
                    return result;
            }
            return result;
        }
    }

    public IRenderer getRenderer(String thirdPartyNetwork) {
        IRenderer renderer = null;
        if (renderers.get(thirdPartyNetwork) != null) {
            return renderers.get(thirdPartyNetwork);
        } else {
            switch (thirdPartyNetwork) {
                case "STR":
                    renderer = new Renderer();
                    renderers.put(thirdPartyNetwork, renderer);
                    return renderer;
                case "FAN":
                    renderer = new Renderer();
                    renderers.put(thirdPartyNetwork, renderer);
                    return renderer;
            }
            return renderer;
        }
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
            return thirdPartyNetworks.get(getCurrentIndex());
        }

    }
}
