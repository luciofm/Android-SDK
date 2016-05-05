package com.sharethrough.sdk;

import android.util.LruCache;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.sharethrough.sdk.network.ASAPManager;
import com.sharethrough.sdk.network.AdManager;
import com.sharethrough.test.util.TestAdView;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import java.util.*;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atLeastOnce;
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
    @Mock private AdManager adManager;
    @Mock private Renderer renderer;
    @Mock private CreativesQueue creativeQueue;
    @Mock private LruCache<Integer, Creative> creativesBySlot;
    @Mock private Set<Integer> creativeIndices;
    @Mock private AdvertisingIdProvider advertisingIdProvider;
    @Mock private RequestQueue requestQueue;
    @Mock private ASAPManager asapManager;

    @Mock private Creative creative;
    @Mock private Placement placement;
    @Mock private Sharethrough.OnStatusChangeListener onStatusChangeListener;

    @Captor
    private ArgumentCaptor<Function<Placement, Void>> placementHandler;
    private String key = "abc";
    private String advertisingId = "ABC";
    private ArrayList<NameValuePair> queryStringParams;
    private TestAdView adView;
    private String apiUri;

    @Before
    public void setUp() throws Exception {
        //old
        adView = makeMockAdView();
        apiUri = "http://btlr.sharethrough.com/v3";
        queryStringParams = new ArrayList<>(1);
        queryStringParams.add(new BasicNameValuePair("placement_key", key));

        placementKey = "fakeKey";
        when(strSdkConfig.getSerializedSharethrough()).thenReturn(serializedSharethrough);
        when(strSdkConfig.getBeaconService()).thenReturn(beaconService);
        when(strSdkConfig.getAdManager()).thenReturn(adManager);
        when(strSdkConfig.getRenderer()).thenReturn(renderer);
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
        when(result.getContext()).thenReturn(Robolectric.application);
        when(result.getTitle()).thenReturn(mock(TextView.class));
        when(result.getDescription()).thenReturn(mock(TextView.class));
        when(result.getAdvertiser()).thenReturn(mock(TextView.class));
        when(result.getThumbnail()).thenReturn(mock(FrameLayout.class));
        when(result.getThumbnail().getContext()).thenReturn(Robolectric.application);
        verify(result).getThumbnail(); // clear it out for verifyNoInteractions
        return result;
    }

    private void verifyCreativeHasBeenPlacedInAdview(TestAdView adView) {
        ArgumentCaptor<Creative> creativeArgumentCaptor = ArgumentCaptor.forClass(Creative.class);
        verify(renderer).putCreativeIntoAdView(eq(adView), creativeArgumentCaptor.capture(), eq(beaconService), eq(subject), eq(0), any(Timer.class));
        assertThat(creativeArgumentCaptor.getValue()).isSameAs(this.creative);
    }

    @Test
    public void constructor_setsAdManagerListener() throws Exception {
        verify(adManager).setAdManagerListener((AdManager.AdManagerListener) anyObject());
    }

    @Test
    public void constructor_setsCorrectDefaultPlacement() {
        assertThat(subject.placement.getArticlesBetweenAds()).isEqualTo(Integer.MAX_VALUE);
        assertThat(subject.placement.getArticlesBeforeFirstAd()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void createPlacement_createsPlacementWithCorrectParams() throws Exception {
        Placement placement = subject.createPlacement(1, 2);
        assertThat(placement.getArticlesBeforeFirstAd()).isEqualTo(2);
        assertThat(placement.getArticlesBetweenAds()).isEqualTo(1);
        assertThat(placement.getStatus()).isEqualTo("");

    }

    @Test
    public void fetchAds_callsASAP() throws Exception {
        subject.fetchAds();
        verify(asapManager, atLeastOnce()).callASAP((ASAPManager.ASAPManagerListener)anyObject());
    }

    @Test
    public void adManagerListener_whenACreativeIsReady_addCreativeToQueue() throws Exception {
        List<Creative> creatives = new ArrayList<>();
        creatives.add(creative);
        subject.adManagerListener.onAdsReady(creatives,placement);
        verify(creativeQueue).add(creative);
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
        reset(adManager);
        subject.putCreativeIntoAdView(adView);

        verify(asapManager, atLeastOnce()).callASAP((ASAPManager.ASAPManagerListener) anyObject());
    }

    @Test
    public void putCreativeIntoAdView_whenQueueHasCreatives_usesNextCreative() {
        when(creativeQueue.size()).thenReturn(1);
        when(creativeQueue.getNext()).thenReturn(creative).thenThrow(new RuntimeException("Too many calls to getNext"));

        subject.putCreativeIntoAdView(adView);
        verifyCreativeHasBeenPlacedInAdview(adView);
    }

//    @Test
//    public void xwhenAndroidManifestHasCustomApiServer_usesThatServer() throws Exception {
//        String serverPrefix = "http://dumb-waiter.sharethrough.com/";
//        Robolectric.application.getApplicationInfo().metaData.putString("STR_ADSERVER_API", serverPrefix);
//       // createSubject(key);
//        verify(asapManager, atLeastOnce()).callASAP(eq(apiUri), eq(queryStringParams), eq(advertisingId));
//    }

    @Test
    public void whenFirstCreativeIsPrefetches_notifiesOnStatusChangeListenerOnMainThread() throws Exception {
        List<Creative> creatives = new ArrayList<>();
        creatives.add(creative);
        subject.adManagerListener.onAdsReady(creatives, placement);
        verify(onStatusChangeListener).newAdsToShow();
    }

    @Test
    public void whenCreativeIsPrefetched_whenNewAdsToShowHasAlreadyBeenCalled_doesNotCallItAgain() throws Exception {
        List<Creative> creatives = new ArrayList<>();
        creatives.add(creative);
        subject.adManagerListener.onAdsReady(creatives, placement);
        verify(onStatusChangeListener).newAdsToShow();
        subject.adManagerListener.onAdsReady(creatives, placement);
        verifyNoMoreInteractions(onStatusChangeListener);
    }

    @Test
    public void whenFirstCreativeIsNotAvailable_notifiesOnStatusChangeListenerOnMainThread() throws Exception {
        subject.adManagerListener.onNoAdsToShow();
        verify(onStatusChangeListener).noAdsToShow();
    }

    @Test
    public void whenCreativeIsPrefetched_whenNewAdsToShowHasBeenCalledButNoAdsToShowHasSinceBeenCalled_callsItAgain() throws Exception {
        // cause newAdsToShow
        List<Creative> creatives = new ArrayList<>();
        creatives.add(creative);
        subject.adManagerListener.onAdsReady(creatives, placement);
        verify(onStatusChangeListener).newAdsToShow();
        reset(onStatusChangeListener);

        // cause noAdsToShow
        subject.adManagerListener.onNoAdsToShow();
        verify(onStatusChangeListener).noAdsToShow();
        reset(onStatusChangeListener);

        // test that newAdsToShow will be called again
        subject.adManagerListener.onAdsReady(creatives, placement);
        //creativeHandler.getValue().apply(creative);
        verify(onStatusChangeListener).newAdsToShow();
        reset(onStatusChangeListener);
    }

    @Test
    public void getAdView_whenAdViewsBySlotContainsAdviewForPosition_returnsStoredAdview() {
        int adSlot = 2;
        when(creativeQueue.size()).thenReturn(1);
        when(creativeQueue.getNext()).thenReturn(creative);
        IAdView generatedAdView = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);

        verify(renderer).putCreativeIntoAdView(same(generatedAdView), same(creative), same(beaconService), same(subject), eq(adSlot), any(Timer.class));
        verify(renderer).putCreativeIntoAdView(same(generatedAdView2), same(creative), same(beaconService), same(subject), eq(adSlot), any(Timer.class));
    }

    @Test
    public void getAdView_whenRecyclingViews_withSameFeedPosition_returnsRecycledView_withSameCreative() throws Exception {
        int adSlot = 2;
        when(creativeQueue.size()).thenReturn(1);
        when(creativeQueue.getNext()).thenReturn(creative);
        IAdView generatedAdView = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, generatedAdView);

        assertThat(generatedAdView).isSameAs(generatedAdView2);

        verify(renderer, times(2)).putCreativeIntoAdView(same(generatedAdView), same(creative), same(beaconService), same(subject), eq(adSlot), any(Timer.class));
    }

    @Test
    public void getAdView_whenRecyclingViews_withDiffFeedPosition_returnsRecycledView_withNewCreative() throws Exception {
        Creative creative2 = mock(Creative.class);
        when(creativeQueue.size()).thenReturn(2);
        when(creativeQueue.getNext()).thenReturn(creative).thenReturn(creative2).thenThrow(new RuntimeException("Too many calls"));
        IAdView generatedAdView = subject.getAdView(Robolectric.application, 1, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, 2, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, generatedAdView);

        assertThat(generatedAdView).isSameAs(generatedAdView2);
        verify(renderer).putCreativeIntoAdView(same(generatedAdView), same(creative), same(beaconService), same(subject), eq(1), any(Timer.class));
        verify(renderer).putCreativeIntoAdView(same(generatedAdView2), same(creative2), same(beaconService), same(subject), eq(2), any(Timer.class));
    }

    @Test
    public void getAdView_whenAdViewsBySlotDoesNotContainAdviewForPosition_returnsNewAdview() {
        IAdView generatedAdView = subject.getAdView(Robolectric.application, 2, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        assertThat(generatedAdView).isNotNull();
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, 12, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        assertThat(generatedAdView).isNotSameAs(generatedAdView2);
    }

    @Test
    public void ifSlotIsEmptyAndThereAreMoreAdsToShow_fireNewAdsIsCalled() {
        int adSlot = 2;
        when(creativeQueue.size()).thenReturn(1);
        when(creativeQueue.getNext()).thenReturn(creative);
        IAdView generatedAdView = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);

        assertThat(subject.firedNewAdsToShow);
    }

    @Test
    public void ifSlotNOTEmptyAndCreativeExpired_fireNewAdsIsCalled() {
        Creative creative2 = mock(Creative.class);
        when(creativeQueue.size()).thenReturn(2);
        when(creativeQueue.getNext()).thenReturn(creative).thenReturn(creative2).thenThrow(new RuntimeException("Too many calls"));
        IAdView generatedAdView = subject.getAdView(Robolectric.application, 1, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        when(creative.hasExpired(anyInt())).thenReturn(true);
        subject.firedNewAdsToShow = false;
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, 1, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, generatedAdView);
        assertThat(generatedAdView).isSameAs(generatedAdView2);

        assertThat(subject.firedNewAdsToShow);
    }

    @Test
    public void creativeIndicesStoresAllUniqueCreativeIndexHistory_creativesBySlotOnlyCachesTenUniqueIndices() {
        LruCache<Integer, Creative> slot = new LruCache<>(10);
        Set<Integer> creativeIndices = new HashSet<>();
        when(strSdkConfig.getCreativesBySlot()).thenReturn(slot);
        when(strSdkConfig.getCreativeIndices()).thenReturn(creativeIndices);
        when(creativeQueue.size()).thenReturn(11);
        when(creativeQueue.getNext()).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative)
                .thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative);

        IAdView generatedAdView1 = subject.getAdView(Robolectric.application, 2, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, 5, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView3 = subject.getAdView(Robolectric.application, 8, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView4 = subject.getAdView(Robolectric.application, 11, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView5 = subject.getAdView(Robolectric.application, 14, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView6 = subject.getAdView(Robolectric.application, 17, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView7 = subject.getAdView(Robolectric.application, 20, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView8 = subject.getAdView(Robolectric.application, 23, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView9 = subject.getAdView(Robolectric.application, 26, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView10 = subject.getAdView(Robolectric.application, 29, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView11 = subject.getAdView(Robolectric.application, 32, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);

        IAdView generatedAdView12 = subject.getAdView(Robolectric.application, 8, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, generatedAdView3);

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
        IAdView generatedAdView = subject.getAdView(Robolectric.application, 1, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        when(creative.hasExpired(anyInt())).thenReturn(false);
        subject.firedNewAdsToShow = false;
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, 1, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, generatedAdView);
        assertThat(generatedAdView).isSameAs(generatedAdView2);

        assertThat(false == subject.firedNewAdsToShow);
    }

    @Test
    public void ifSlotEmptyAndNoAdsAvailable_fireNewAdsIsNOTCalled() {
        int adSlot = 2;
        when(creativeQueue.size()).thenReturn(0);
        IAdView generatedAdView = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);

        assertThat(false == subject.firedNewAdsToShow);
    }

    @Test
    public void whenPlacementIsNotSet_setsPlacementAndCallsPlacementCallback() {
        final boolean[] callbackWasInvoked = {false};
        subject.setOrCallPlacementCallback(new Callback<Placement>() {
            @Override
            public void call(Placement result) {
                callbackWasInvoked[0] = true;
            }
        });
        List<Creative> creatives = new ArrayList<>();
        creatives.add(creative);
        subject.adManagerListener.onAdsReady(creatives,placement);
        subject.putCreativeIntoAdView(adView);

        assertThat(callbackWasInvoked[0]).isTrue();
        assertThat(subject.placement).isEqualTo(placement);
        assertThat(subject.placementSet).isTrue();
    }

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
