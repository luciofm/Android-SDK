package com.sharethrough.sdk.mediation;

import android.content.Context;
import com.facebook.ads.NativeAd;
import com.google.gson.JsonObject;
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
    private Map<String, STRMediationAdapter> mediationAdapters = new HashMap<>();
    private Map<String, IRenderer> renderers = new HashMap<>();

    public interface MediationListener {
        /**
         * Class extending STRMediationAdapter must call this method when it successfully
         * loads an ad
         */
        void onAdLoaded(NativeAd fbAd);

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
        setUpWaterfall(asapResponse.mediationNetworks, mediationListener);
        loadAd();
    }

    public void loadNextAd() {
        loadAd();
    }

    private void setUpWaterfall(final ArrayList<ASAPManager.AdResponse.Network> mediationNetworks,
                                final MediationListener mediationListener) {
        this.mediationWaterfall = new MediationWaterfall(mediationNetworks);
        this.mediationListener = mediationListener;
    }

    private void loadAd() {
        STRMediationAdapter mediationAdapter = getMediationAdapter("");

        if (mediationAdapter != null) {
            mediationAdapter.loadAd(context, mediationListener);
        } else {
            //end of waterfall
            mediationListener.onAllAdsFailedToLoad();
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

        public String getNextThirdPartyNetworkName() {
            incrementIndex();
            return thirdPartyNetworks.get(getCurrentIndex()).name;
        }

    }
}
