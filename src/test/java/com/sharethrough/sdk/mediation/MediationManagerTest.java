package com.sharethrough.sdk.mediation;

import android.content.Context;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.TestBase;
import com.sharethrough.sdk.network.ASAPManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static junit.framework.Assert.assertTrue;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by danicashei on 8/9/16.
 */
public class MediationManagerTest extends TestBase {
    private MediationManager subject;
    @Mock private BeaconService beaconService;
    @Mock private MediationManager.MediationListener mediationListener;
    @Mock private ASAPManager.AdResponse asapResponse;
    @Mock private MediationManager.MediationWaterfall mediationWaterfall;
    @Mock private Map<String, STRMediationAdapter> mediationAdapters;
    @Mock private STRMediationAdapter mediationAdapter;
    @Mock private Context context;
    @Mock private ASAPManager.AdResponse.Network network1;
    @Mock private ASAPManager.AdResponse.Network network2;

    @Before
    public void setUp() throws Exception {
        subject = new MediationManager(context, beaconService, mediationAdapters);
        subject.setUpWaterfall(asapResponse, mediationListener, mediationWaterfall);
    }

    @Test
    public void initiateWaterfallAndLoadAd_firesMediationStartBeacon() throws Exception {
        asapResponse.mrid = "fake-mrid";
        subject.initiateWaterfallAndLoadAd(asapResponse, mediationListener, mediationWaterfall);
        verify(beaconService, times(1)).mediationStart("fake-mrid", 1);
    }

    @Test
    public void loadNextAd_getsNextThirdPartyNetWork() throws Exception {
        when(mediationWaterfall.getNextThirdPartyNetwork()).thenReturn(null);
        subject.loadNextAd();
        verify(mediationWaterfall, times(1)).getNextThirdPartyNetwork();
    }

    @Test
    public void loadNextAd_firesNoFillBeaconIfWaterfallStarted() throws Exception {
        network1.key = "fake-network-key";
        asapResponse.mrid = "fake-mrid";
        when(mediationWaterfall.waterfallStarted()).thenReturn(true);
        when(mediationWaterfall.getCurrentThirdPartyNetwork()).thenReturn(network1);
        when(mediationWaterfall.getCurrentIndex()).thenReturn(2);
        subject.loadNextAd();
        verify(beaconService, times(1)).networkNoFill("fake-network-key", 3, "fake-mrid", 1);
    }

    @Test
    public void loadNextAd_firesNetworkImpressionRequestBeacon() throws Exception {
        network1.key = "fake-network-key";
        asapResponse.mrid = "fake-mrid";
        when(mediationWaterfall.getNextThirdPartyNetwork()).thenReturn(network1);
        when(mediationWaterfall.getCurrentIndex()).thenReturn(2);
        when(mediationAdapters.get(anyString())).thenReturn(mediationAdapter);
        subject.loadNextAd();
        verify(beaconService, times(1)).networkImpressionRequest("fake-network-key", 3, "fake-mrid", 1);
    }

    @Test
    public void loadNextAd_doesNotFireNoFillBeaconIfBeginningWaterfall() throws Exception {
        when(mediationWaterfall.waterfallStarted()).thenReturn(false);
        verify(beaconService, never()).networkNoFill(anyString(), anyInt(), anyString(), anyInt());
    }

    @Test
    public void loadNextAd_callsOnAllAdsFailedToLoad_ifWaterfallComplete() throws Exception {
        when(mediationWaterfall.getNextThirdPartyNetwork()).thenReturn(null);
        subject.loadNextAd();
        verify(mediationListener, times(1)).onAllAdsFailedToLoad();
    }

    @Test
    public void loadNextAd_callsLoadAdOnMediationAdapter() throws Exception {
        when(mediationWaterfall.getNextThirdPartyNetwork()).thenReturn(network1);
        when(mediationAdapters.get(anyString())).thenReturn(mediationAdapter);
        network1.name = "networkName";
        subject.loadNextAd();
        verify(mediationAdapters, times(1)).put("networkName", mediationAdapter);
        verify(mediationAdapter, times(1)).loadAd(eq(context), eq(mediationListener), eq(asapResponse), eq(network1));
    }

    @Test
    public void getMediationAdapter_returnsAdapterFromMapIfExists() throws Exception{
        network1.name = "networkName";
        when(mediationAdapters.get("networkName")).thenReturn(mediationAdapter);
        STRMediationAdapter adapter = subject.getMediationAdapter(network1);
        assertThat(adapter).isEqualTo(mediationAdapter);
    }

    @Test
    public void getMediationAdapter_createsNewFanAdapterAndStoresInMap() throws Exception {
        network1.name = "FAN";
        network1.androidClassName = "com.sharethrough.sdk.mediation.DummyMediationAdapter";
        when(mediationAdapters.get(network1.name)).thenReturn(null);
        assertTrue(subject.getMediationAdapter(network1) instanceof DummyMediationAdapter);
    }

    @Test
    public void simulatesEntireWaterfall() throws Exception {
        network1.name = "network1";
        network1.key = "network1Key";
        network2.name = "network2";
        network2.key = "network2Key";
        asapResponse.mrid = "mrid";
        List<ASAPManager.AdResponse.Network> networks = new ArrayList<>();
        networks.add(network1);
        networks.add(network2);
        MediationManager.MediationWaterfall mediationWaterfallImplemented = new MediationManager.MediationWaterfall(networks);
        when(mediationAdapters.get(anyString())).thenReturn(mediationAdapter);

        subject.setUpWaterfall(asapResponse, mediationListener, mediationWaterfallImplemented);

        // start waterfall
        subject.loadNextAd();
        verify(mediationAdapter, times(1)).loadAd(
                any(Context.class),
                any(MediationManager.MediationListener.class),
                any(ASAPManager.AdResponse.class),
                any(ASAPManager.AdResponse.Network.class)
        );
        verify(mediationListener, never()).onAllAdsFailedToLoad();
        verify(beaconService, never()).networkNoFill(
                anyString(),
                anyInt(),
                anyString(),
                anyInt());

        // on first network failed to load ad
        subject.loadNextAd();
        verify(mediationAdapter, times(2)).loadAd(
                any(Context.class),
                any(MediationManager.MediationListener.class),
                any(ASAPManager.AdResponse.class),
                any(ASAPManager.AdResponse.Network.class)
        );
        verify(mediationListener, never()).onAllAdsFailedToLoad();
        verify(beaconService, times(1)).networkNoFill(
               "network1Key",
                1,
                "mrid",
                1);
        verify(beaconService, times(1)).networkImpressionRequest(
                "network2Key",
                2,
                "mrid",
                1);

        // on second network failed to load ad
        subject.loadNextAd();
        verify(mediationAdapter, times(2)).loadAd(
                any(Context.class),
                any(MediationManager.MediationListener.class),
                any(ASAPManager.AdResponse.class),
                any(ASAPManager.AdResponse.Network.class)
        );
        verify(mediationListener, times(1)).onAllAdsFailedToLoad();
        verify(beaconService, times(1)).networkNoFill(
                "network2Key",
                2,
                "mrid",
                1);

    }

}
