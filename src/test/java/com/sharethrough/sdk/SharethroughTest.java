package com.sharethrough.sdk;

import android.util.LruCache;
import android.util.Pair;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.sharethrough.sdk.mediation.ICreative;
import com.sharethrough.sdk.mediation.MediationManager;
import com.sharethrough.sdk.network.ASAPManager;
import com.sharethrough.test.util.TestAdView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.util.*;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SharethroughTest extends TestBase {
    private Sharethrough subject;
    private String placementKey;
    private String serializedSharethrough;
    @Mock private BeaconService beaconService;
    @Mock private STRSdkConfig strSdkConfig;
    @Mock private CreativesQueue creativeQueue;
    @Mock private LruCache<Integer, ICreative> creativesBySlot;
    @Mock private Set<Integer> creativeIndices;
    @Mock private AdvertisingIdProvider advertisingIdProvider;
    @Mock private RequestQueue requestQueue;
    @Mock private ASAPManager asapManager;
    @Mock private MediationManager mediationManager;

    @Mock private Creative creative;
    @Mock private Placement placement;
    @Mock private Sharethrough.OnStatusChangeListener onStatusChangeListener;

    @Captor
    private ArgumentCaptor<Function<Placement, Void>> placementHandler;
    private String key = "abc";
    private String advertisingId = "ABC";
    private ArrayList<Pair<String,String>> queryStringParams;
    private TestAdView adView;
    private String apiUri;

    @Before
    public void setUp() throws Exception {
        //old
        adView = makeMockAdView();
        apiUri = "http://btlr.sharethrough.com/v3";
        queryStringParams = new ArrayList<>(1);
        queryStringParams.add(new Pair<String,String>("placement_key", key));

        placementKey = "fakeKey";
        when(strSdkConfig.getSerializedSharethrough()).thenReturn(serializedSharethrough);
        when(strSdkConfig.getBeaconService()).thenReturn(beaconService);
        when(strSdkConfig.getMediationManager()).thenReturn(mediationManager);
        when(strSdkConfig.getCreativesBySlot()).thenReturn(creativesBySlot);
        when(strSdkConfig.getCreativeIndices()).thenReturn(creativeIndices);
        when(strSdkConfig.getRequestQueue()).thenReturn(requestQueue);
        when(strSdkConfig.getAsapManager()).thenReturn(asapManager);
        when(strSdkConfig.getCreativeQueue()).thenReturn(creativeQueue);

        subject = new Sharethrough(strSdkConfig);
        subject.setOnStatusChangeListener(onStatusChangeListener);
    }

    private TestAdView makeMockAdView() {
        TestAdView result = mock(TestAdView.class);
        when(result.getContext()).thenReturn(RuntimeEnvironment.application);
        when(result.getTitle()).thenReturn(mock(TextView.class));
        when(result.getDescription()).thenReturn(mock(TextView.class));
        when(result.getAdvertiser()).thenReturn(mock(TextView.class));
        when(result.getThumbnail()).thenReturn(mock(FrameLayout.class));
        when(result.getThumbnail().getContext()).thenReturn(RuntimeEnvironment.application);
        verify(result).getThumbnail(); // clear it out for verifyNoInteractions
        return result;
    }

    private void verifyCreativeHasBeenPlacedInAdview(TestAdView adView) {
        ArgumentCaptor<Creative> creativeArgumentCaptor = ArgumentCaptor.forClass(Creative.class);
        verify(mediationManager).render(eq(adView), creativeArgumentCaptor.capture(), anyInt());
        assertThat(creativeArgumentCaptor.getValue()).isSameAs(this.creative);
    }

    @Test
    public void constructor_setsCorrectDefaultPlacement() {
        assertThat(subject.placement.getArticlesBetweenAds()).isEqualTo(Integer.MAX_VALUE);
        assertThat(subject.placement.getArticlesBeforeFirstAd()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void createPlacement_createsPlacementWithCorrectParams() throws Exception {
        Placement placement = subject.createPlacement(1, 2, "", "");
        assertThat(placement.getArticlesBeforeFirstAd()).isEqualTo(2);
        assertThat(placement.getArticlesBetweenAds()).isEqualTo(1);
        assertThat(placement.getStatus()).isEqualTo("");
    }

    @Test
    public void fetchAds_updatesAsapEndPointWhenThereAreCustomKeyValues() throws Exception {
        HashMap<String, String> customKeyValues = new HashMap<>();
        customKeyValues.put("key1", "value1");
        subject.fetchAds(customKeyValues);
        verify(asapManager, times(1)).updateAsapEndpoint(customKeyValues);
    }

    @Test
    public void fetchAds_callsASAP() throws Exception {
        subject.fetchAds();
        verify(asapManager, times(1)).callASAP((ASAPManager.ASAPManagerListener)anyObject());
    }

    @Test
    public void mediationListener_onAdLoaded_addCreativeToQueue_notifiesOnStatusChangeListenerOnMainThread() throws Exception {
        List<ICreative> creatives = new ArrayList<>();
        creatives.add(creative);
        subject.mediationListener.onAdLoaded(creatives, placement);
        verify(creativeQueue, times(1)).add(creative);
        verify(onStatusChangeListener).newAdsToShow();
    }

    @Test
    public void mediationListener_onAdFailedToShow_loadsNextAd() throws Exception {
        subject.mediationListener.onAdFailedToLoad();
        verify(mediationManager, times(1)).loadNextAd();
    }

    @Test
    public void fireNoAdsToShow_callsListenerNoAdsToShow_whenCreativeQueueIsEmpty() throws Exception {
        when(creativeQueue.size()).thenReturn(0);
        subject.fireNoAdsToShow();

        verify(onStatusChangeListener).noAdsToShow();
        assertThat(subject.firedNewAdsToShow).isEqualTo(false);
    }

    @Test
    public void fireNoAdsToShow_doesNotCallNoAdsToShow_whenCreativeQueueHasCreatives() throws Exception {
        when(creativeQueue.size()).thenReturn(1);
        subject.fireNoAdsToShow();

        verify(onStatusChangeListener, never()).noAdsToShow();
        assertThat(subject.firedNewAdsToShow).isEqualTo(false);
    }

    @Test
    public void fireNewAdsToShow_callsNewAdsToShow_whenFireNewAdsToShowBooleanIsFalse() throws Exception {
        subject.firedNewAdsToShow = false;
        subject.fireNewAdsToShow();

        verify(onStatusChangeListener).newAdsToShow();
    }

    @Test
    public void fireNewAdsToShow_doesNotCallNewAdsToShow_whenFireNewAdsToShowBooleanIsTrue() throws Exception {
        subject.firedNewAdsToShow = true;
        subject.fireNewAdsToShow();

        verify(onStatusChangeListener, never()).newAdsToShow();
    }

    @Test
    public void putCreativeIntoAdView_whenQueueWantsMore_fetchesMoreAds() throws Exception {
        when(creativeQueue.readyForMore()).thenReturn(true);
        subject.putCreativeIntoAdView(adView);

        verify(asapManager, times(1)).callASAP((ASAPManager.ASAPManagerListener) anyObject());
    }

    @Test
    public void putCreativeIntoAdView_whenQueueHasCreatives_usesNextCreative() {
        when(creativeQueue.size()).thenReturn(1);
        when(creativeQueue.getNext()).thenReturn(creative).thenThrow(new RuntimeException("Too many calls to getNext"));

        subject.putCreativeIntoAdView(adView);
        verifyCreativeHasBeenPlacedInAdview(adView);
    }

    @Test
    public void whenCreativeIsPrefetched_whenNewAdsToShowHasAlreadyBeenCalled_doesNotCallItAgain() throws Exception {
        List<ICreative> creatives = new ArrayList<>();
        creatives.add(creative);
        subject.mediationListener.onAdLoaded(creatives, placement);
        verify(onStatusChangeListener).newAdsToShow();
        subject.mediationListener.onAdLoaded(creatives, placement);
        verifyNoMoreInteractions(onStatusChangeListener);
    }

    @Test
    public void whenFirstWaterfallFailsToLoadAds_notifiesOnStatusChangeListenerOnMainThread() throws Exception {
        subject.mediationListener.onAllAdsFailedToLoad();
        verify(onStatusChangeListener).noAdsToShow();
    }

    @Test
    public void whenCreativeIsPrefetched_whenNewAdsToShowHasBeenCalledButNoAdsToShowHasSinceBeenCalled_callsItAgain() throws Exception {
        // cause newAdsToShow
        List<ICreative> creatives = new ArrayList<>();
        creatives.add(creative);
        subject.mediationListener.onAdLoaded(creatives, placement);
        verify(onStatusChangeListener).newAdsToShow();
        reset(onStatusChangeListener);

        // cause noAdsToShow
        subject.mediationListener.onAllAdsFailedToLoad();
        verify(onStatusChangeListener).noAdsToShow();
        reset(onStatusChangeListener);

        // test that newAdsToShow will be called again
        subject.mediationListener.onAdLoaded(creatives, placement);
        //creativeHandler.getValue().apply(creative);
        verify(onStatusChangeListener).newAdsToShow();
        reset(onStatusChangeListener);
    }

    @Test
    public void getAdView_whenAdViewsBySlotContainsAdviewForPosition_returnsStoredAdview() {
        int adSlot = 2;
        when(creativeQueue.size()).thenReturn(1);
        when(creativeQueue.getNext()).thenReturn(creative);
        IAdView generatedAdView = subject.getAdView(RuntimeEnvironment.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView2 = subject.getAdView(RuntimeEnvironment.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);

        verify(mediationManager).render(same(generatedAdView), same(creative), eq(adSlot));
        verify(mediationManager).render(same(generatedAdView2), same(creative), eq(adSlot));
    }

    @Test
    public void getAdView_whenRecyclingViews_withSameFeedPosition_returnsRecycledView_withSameCreative() throws Exception {
        int adSlot = 2;
        when(creativeQueue.size()).thenReturn(1);
        when(creativeQueue.getNext()).thenReturn(creative);
        IAdView generatedAdView = subject.getAdView(RuntimeEnvironment.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView2 = subject.getAdView(RuntimeEnvironment.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, generatedAdView, 7);

        assertThat(generatedAdView).isSameAs(generatedAdView2);

        verify(mediationManager, times(2)).render(same(generatedAdView), same(creative), eq(adSlot));
    }


    @Test
    public void getAdView_whenRecyclingViews_withDiffFeedPosition_returnsRecycledView_withNewCreative() throws Exception {
        Creative creative2 = mock(Creative.class);
        when(creativeQueue.size()).thenReturn(2);
        when(creativeQueue.getNext()).thenReturn(creative).thenReturn(creative2).thenThrow(new RuntimeException("Too many calls"));
        IAdView generatedAdView = subject.getAdView(RuntimeEnvironment.application, 1, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView2 = subject.getAdView(RuntimeEnvironment.application, 2, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, generatedAdView, 7);

        assertThat(generatedAdView).isSameAs(generatedAdView2);
        verify(mediationManager).render(same(generatedAdView), same(creative), eq(1));
        verify(mediationManager).render(same(generatedAdView2), same(creative2),eq(2));
    }

    @Test
    public void getAdView_whenAdViewsBySlotDoesNotContainAdviewForPosition_returnsNewAdview() {
        IAdView generatedAdView = subject.getAdView(RuntimeEnvironment.application, 2, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        assertThat(generatedAdView).isNotNull();
        IAdView generatedAdView2 = subject.getAdView(RuntimeEnvironment.application, 12, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        assertThat(generatedAdView).isNotSameAs(generatedAdView2);
    }

    @Test
    public void ifSlotIsEmptyAndThereAreMoreAdsToShow_fireNewAdsIsCalled() {
        int adSlot = 2;
        when(creativeQueue.size()).thenReturn(1);
        when(creativeQueue.getNext()).thenReturn(creative);
        IAdView generatedAdView = subject.getAdView(RuntimeEnvironment.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);

        assertThat(subject.firedNewAdsToShow);
    }

    @Test
    public void creativeIndicesStoresAllUniqueCreativeIndexHistory_creativesBySlotOnlyCachesTenUniqueIndices() {
        LruCache<Integer, ICreative> slot = new LruCache<>(10);
        Set<Integer> creativeIndices = new HashSet<>();
        when(strSdkConfig.getCreativesBySlot()).thenReturn(slot);
        when(strSdkConfig.getCreativeIndices()).thenReturn(creativeIndices);
        when(creativeQueue.size()).thenReturn(11);
        when(creativeQueue.getNext()).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative)
                .thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative);

        IAdView generatedAdView1 = subject.getAdView(RuntimeEnvironment.application, 2, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView2 = subject.getAdView(RuntimeEnvironment.application, 5, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView3 = subject.getAdView(RuntimeEnvironment.application, 8, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView4 = subject.getAdView(RuntimeEnvironment.application, 11, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView5 = subject.getAdView(RuntimeEnvironment.application, 14, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView6 = subject.getAdView(RuntimeEnvironment.application, 17, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView7 = subject.getAdView(RuntimeEnvironment.application, 20, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView8 = subject.getAdView(RuntimeEnvironment.application, 23, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView9 = subject.getAdView(RuntimeEnvironment.application, 26, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView10 = subject.getAdView(RuntimeEnvironment.application, 29, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        IAdView generatedAdView11 = subject.getAdView(RuntimeEnvironment.application, 32, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);

        IAdView generatedAdView12 = subject.getAdView(RuntimeEnvironment.application, 8, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, generatedAdView3, 7);

        assertThat(slot.snapshot().keySet().size()).isEqualTo(10);
        assertThat(creativeIndices.size()).isEqualTo(11);
        assertThat(subject.getNumberOfAdsBeforePosition(0)).isEqualTo(0);
        assertThat(subject.getNumberOfAdsBeforePosition(11)).isEqualTo(3);
        assertThat(subject.getNumberOfAdsBeforePosition(21)).isEqualTo(7);
        assertThat(subject.getNumberOfAdsBeforePosition(50)).isEqualTo(11);
    }

    @Test
    public void ifSlotNOTEmptyAndCreativeNOTExpired_fireNewAdsIsNOTCalled() {
        Creative creative2 = mock(Creative.class);
        when(creativeQueue.size()).thenReturn(2);
        when(creativeQueue.getNext()).thenReturn(creative).thenReturn(creative2).thenThrow(new RuntimeException("Too many calls"));
        IAdView generatedAdView = subject.getAdView(RuntimeEnvironment.application, 1, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);
        subject.firedNewAdsToShow = false;
        IAdView generatedAdView2 = subject.getAdView(RuntimeEnvironment.application, 1, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, generatedAdView, 7);
        assertThat(generatedAdView).isSameAs(generatedAdView2);

        assertThat(false == subject.firedNewAdsToShow);
    }

    @Test
    public void ifSlotEmptyAndNoAdsAvailable_fireNewAdsIsNOTCalled() {
        int adSlot = 2;
        when(creativeQueue.size()).thenReturn(0);
        IAdView generatedAdView = subject.getAdView(RuntimeEnvironment.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null, 7);

        assertThat(false == subject.firedNewAdsToShow);
    }

//    @Test
//    public void whenPlacementIsNotSet_setsPlacementAndCallsPlacementCallback() {
//        final boolean[] callbackWasInvoked = {false};
//        subject.setOrCallPlacementCallback(new Callback<Placement>() {
//            @Override
//            public void call(Placement result) {
//                callbackWasInvoked[0] = true;
//            }
//        });
//        List<ICreative> creatives = new ArrayList<>();
//        creatives.add(creative);
//        subject.mediationListener.onAdLoaded(creatives);
//        subject.putCreativeIntoAdView(adView);
//
//        assertThat(callbackWasInvoked[0]).isTrue();
//        assertThat(subject.placement).isEqualTo(placement);
//        assertThat(subject.placementSet).isTrue();
//    }

    @Test
    public void whenPlacementIsSet_doesNotSetPlacementOrCallPlacementCallback() {
        final boolean[] callbackWasInvoked = {false};
        subject.setOrCallPlacementCallback(new Callback<Placement>() {
            @Override
            public void call(Placement result) {
                callbackWasInvoked[0] = true;
            }
        });
        subject.placementSet = true;
        when(creativeQueue.readyForMore()).thenReturn(true);

        subject.putCreativeIntoAdView(adView);

        verify(asapManager).callASAP((ASAPManager.ASAPManagerListener)anyObject());
        assertThat(subject.placement).isNotEqualTo(placement);
        assertThat(callbackWasInvoked[0]).isFalse();
    }

    @Test
    public void whenPlacementIsSet_setOrCallPlacementCallback_callsPlacementCallback() {
        final boolean[] callbackWasInvoked = {false};
        subject.placementSet = true;
        subject.setOrCallPlacementCallback(new Callback<Placement>() {
            @Override
            public void call(Placement result) {
                callbackWasInvoked[0] = true;
            }
        });
        assertThat(callbackWasInvoked[0]).isTrue();
    }

    @Test
    public void whenPlacementIsNotSet_setOrCallPlacementCallback_setsPlacementCallback() {
        final boolean[] callbackWasInvoked = {false};
        subject.placementSet = false;
        Callback<Placement> callback = new Callback<Placement>() {
            @Override
            public void call(Placement result) {
                callbackWasInvoked[0] = true;
            }
        };

        subject.setOrCallPlacementCallback(callback);
        assertThat(callbackWasInvoked[0]).isFalse();
    }
}
