package com.sharethrough.sdk.mediation;

import android.content.Context;
import com.facebook.ads.NativeAd;
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

    public void initiateWaterfallAndLoadAd(final String response,
                                           final MediationListener mediationListener,
                                           final Map<String, String> extras
    ) {
        setUpWaterfall(response, mediationListener, extras);
        loadAd();
    }

    public void loadNextAd() {
        loadAd();
    }

    private void setUpWaterfall(final String asapResponse,
                                final MediationListener mediationListener,
                                final Map<String, String> extras) {
        this.mediationWaterfall = new MediationWaterfall(asapResponse);
        this.mediationListener = mediationListener;
    }

    private void loadAd() {
        STRMediationAdapter mediationAdapter = getMediationAdapter("");

        if (mediationAdapter != null) {
            mediationAdapter.loadAd(context, mediationListener, new HashMap<String, String>());
        } else {
            //end of waterfall
            mediationListener.onAllAdsFailedToLoad();
        }
    }

    private STRMediationAdapter getMediationAdapter(String thirdPartyNetwork) {
        return new FANAdapter();

        /**if (mediationAdapters.get(thirdPartyNetwork) != null) {
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
        }**/
    }

    public class MediationWaterfall {
        private List<String> thirdPartyNetworks = new ArrayList<>();
        private int currentIndex = -1;

        public MediationWaterfall (String response) {

        }

        private void incrementIndex() {
            currentIndex++;
        }

        private int getCurrentIndex() {
            return currentIndex;
        }

        public String getNextThirdPartyNetwork() {
            incrementIndex();
            return thirdPartyNetworks.get(getCurrentIndex());
        }
    }
}
