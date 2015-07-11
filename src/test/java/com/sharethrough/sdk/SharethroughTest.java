package com.sharethrough.sdk;

import android.os.Bundle;
import android.util.LruCache;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.sharethrough.sdk.network.AdFetcher;
import com.sharethrough.sdk.network.DFPNetworking;
import com.sharethrough.sdk.network.ImageFetcher;
import com.sharethrough.test.util.TestAdView;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SharethroughTest extends TestBase {
    private TestAdView adView;
    @Mock private ExecutorService executorService;
    @Mock private Renderer renderer;
    @Mock private BeaconService beaconService;
    @Mock private ImageFetcher imageFetcher;
    @Mock private AdFetcher adFetcher;
    @Mock private Creative creative;
    @Mock private CreativesQueue availableCreatives;
    @Mock private Sharethrough.OnStatusChangeListener onStatusChangeListener;
    @Mock private Placement placement;
    @Mock private DFPNetworking dfpNetworking;
    private Sharethrough subject;
    private int adCacheTimeInMilliseconds;
    private String apiUri;
    @Captor private ArgumentCaptor<Function<Creative, Void>> creativeHandler;
    @Captor private ArgumentCaptor<Function<Placement, Void>> placementHandler;
    @Captor private ArgumentCaptor<AdFetcher.Callback> adFetcherCallback;
    @Captor
    private ArgumentCaptor<DFPNetworking.DFPPathFetcherCallback> dfpPathFetcherCallback;
    @Captor
    private ArgumentCaptor<DFPNetworking.DFPCreativeKeyCallback> dfpCreativeKeyCallback;
    private String key = "abc";
    private ArrayList<NameValuePair> queryStringParams;


    @Before
    public void setUp() throws Exception {
        Robolectric.application.getApplicationInfo().metaData = new Bundle();

        adView = makeMockAdView();
        apiUri = "http://btlr.sharethrough.com/v3";
        queryStringParams = new ArrayList<>(1);
        queryStringParams.add(new BasicNameValuePair("placement_key", key));

        doNothing().when(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), eq(queryStringParams), creativeHandler.capture(), adFetcherCallback.capture(), placementHandler.capture());

        createSubject(key);
    }

    private void createSubject(String key) {
        subject = new Sharethrough(Robolectric.application, key, adCacheTimeInMilliseconds, renderer,
                availableCreatives, beaconService, adFetcher, imageFetcher, null);
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

    @Test
    public void settingKey_loadsAdsFromServer() throws Exception {
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), eq(queryStringParams), creativeHandler.capture(), adFetcherCallback.capture(), placementHandler.capture());
    }

    @Test(expected = KeyRequiredException.class)
    public void notSettingKey_throwsExceptionWhenAdIsRequested() {
        createSubject(null);
    }


    private void verifyCreativeHasBeenPlacedInAdview(TestAdView adView) {
        ArgumentCaptor<Creative> creativeArgumentCaptor = ArgumentCaptor.forClass(Creative.class);
        verify(renderer).putCreativeIntoAdView(eq(adView), creativeArgumentCaptor.capture(), eq(beaconService), eq(subject), eq(0), any(Timer.class));
        assertThat(creativeArgumentCaptor.getValue()).isSameAs(this.creative);
    }

    @Test
    public void whenACreativeIsReady_whenMoreAdVieworCreativeHandlerIsWaiting_putsCreativeInFirstAdView() throws Exception {
        // make two ad views wait
        TestAdView adView2 = makeMockAdView();
        subject.putCreativeIntoAdView(adView);
        subject.putCreativeIntoAdView(adView2);

        // get back one creative
        creativeHandler.getValue().apply(creative);

        verifyCreativeHasBeenPlacedInAdview(adView);
    }

    @Test
    public void whenACreativeIsReady_whenNoMoreAdViewsAreWaiting_addCreativeToQueue() throws Exception {
        creativeHandler.getValue().apply(creative);
        verify(availableCreatives).add(creative);
    }

    @Test
    public void putCreativeIntoAdView_whenQueueWantsMore_fetchesMoreAds() throws Exception {
        when(availableCreatives.readyForMore()).thenReturn(true);
        reset(adFetcher);

        subject.putCreativeIntoAdView(adView);

        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), eq(queryStringParams), creativeHandler.capture(), adFetcherCallback.capture(), placementHandler.capture());
    }

    @Test
    public void putCreativeIntoAdView_whenQueueHasCreatives_usesNextCreative() {
        when(availableCreatives.size()).thenReturn(1);
        when(availableCreatives.getNext()).thenReturn(creative).thenThrow(new RuntimeException("Too many calls to getNext"));

        subject.putCreativeIntoAdView(adView);
        verifyCreativeHasBeenPlacedInAdview(adView);
    }

    @Test
    public void whenAndroidManifestHasCustomApiServer_usesThatServer() throws Exception {
        String serverPrefix = "http://dumb-waiter.sharethrough.com/";
        Robolectric.application.getApplicationInfo().metaData.putString("STR_ADSERVER_API", serverPrefix);
        createSubject(key);
        verify(adFetcher, atLeastOnce()).fetchAds(same(imageFetcher), eq(serverPrefix), eq(queryStringParams), creativeHandler.capture(), adFetcherCallback.capture(), placementHandler.capture());
    }

    @Test
    public void whenFirstCreativeIsPrefetches_notifiesOnStatusChangeListenerOnMainThread() throws Exception {
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), eq(queryStringParams), creativeHandler.capture(), adFetcherCallback.capture(), placementHandler.capture());

        Robolectric.pauseMainLooper();
        creativeHandler.getValue().apply(creative);
        verifyNoMoreInteractions(onStatusChangeListener);
        Robolectric.unPauseMainLooper();

        verify(onStatusChangeListener).newAdsToShow();
    }

    @Test
    public void whenCreativeIsPrefetched_whenNewAdsToShowHasAlreadyBeenCalled_doesNotCallItAgain() throws Exception {
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), eq(queryStringParams), creativeHandler.capture(), adFetcherCallback.capture(), placementHandler.capture());

        Robolectric.pauseMainLooper();
        creativeHandler.getValue().apply(creative);
        verifyNoMoreInteractions(onStatusChangeListener);
        Robolectric.unPauseMainLooper();

        verify(onStatusChangeListener).newAdsToShow();
        reset(onStatusChangeListener);

        creativeHandler.getValue().apply(creative);
        verifyNoMoreInteractions(onStatusChangeListener);
    }

    @Test
    public void whenFirstCreativeIsNotAvailable_notifiesOnStatusChangeListenerOnMainThread() throws Exception {
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), eq(queryStringParams), creativeHandler.capture(), adFetcherCallback.capture(), placementHandler.capture());

        Robolectric.pauseMainLooper();
        adFetcherCallback.getValue().finishedLoadingWithNoAds();
        verifyNoMoreInteractions(onStatusChangeListener);
        Robolectric.unPauseMainLooper();

        verify(onStatusChangeListener).noAdsToShow();
    }

    @Test
    public void whenCreativeIsPrefetched_whenNewAdsToShowHasBeenCalledButNoAdsToShowHasSinceBeenCalled_callsItAgain() throws Exception {
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), eq(queryStringParams), creativeHandler.capture(), adFetcherCallback.capture(), placementHandler.capture());

        // cause newAdsToShow
        Robolectric.pauseMainLooper();
        creativeHandler.getValue().apply(creative);
        verifyNoMoreInteractions(onStatusChangeListener);
        Robolectric.unPauseMainLooper();
        verify(onStatusChangeListener).newAdsToShow();
        reset(onStatusChangeListener);

        // cause noAdsToShow
        Robolectric.pauseMainLooper();
        adFetcherCallback.getValue().finishedLoadingWithNoAds();
        verifyNoMoreInteractions(onStatusChangeListener);
        Robolectric.unPauseMainLooper();
        verify(onStatusChangeListener).noAdsToShow();
        reset(onStatusChangeListener);

        // test that newAdsToShow will be called again
        Robolectric.pauseMainLooper();
        creativeHandler.getValue().apply(creative);
        verifyNoMoreInteractions(onStatusChangeListener);
        Robolectric.unPauseMainLooper();
        verify(onStatusChangeListener).newAdsToShow();
    }

    @Test
    @Config(shadows = AdvertisingIdProviderTest.MyGooglePlayServicesUtilShadow.class)
    public void minimumAdCacheTimeIs_20Seconds() {
        int requestedCacheMilliseconds = (int) TimeUnit.SECONDS.toMillis(5);
        subject = new Sharethrough(Robolectric.application, "abc", requestedCacheMilliseconds);

        int adCacheMilliseconds = subject.getAdCacheTimeInMilliseconds();

        int expectedCacheMilliseconds = (int) TimeUnit.SECONDS.toMillis(20);

        assertThat(adCacheMilliseconds).isEqualTo(expectedCacheMilliseconds);
    }

    @Test
    public void getAdView_whenAdViewsBySlotContainsAdviewForPosition_returnsStoredAdview() {
        int adSlot = 2;
        when(availableCreatives.size()).thenReturn(1);
        when(availableCreatives.getNext()).thenReturn(creative);
        IAdView generatedAdView = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);

        verify(renderer).putCreativeIntoAdView(same(generatedAdView), same(creative), same(beaconService), same(subject), eq(adSlot), any(Timer.class));
        verify(renderer).putCreativeIntoAdView(same(generatedAdView2), same(creative), same(beaconService), same(subject), eq(adSlot), any(Timer.class));
    }

    @Test
    public void getAdView_whenRecyclingViews_withSameFeedPosition_returnsRecycledView_withSameCreative() throws Exception {
        int adSlot = 2;
        when(availableCreatives.size()).thenReturn(1);
        when(availableCreatives.getNext()).thenReturn(creative);
        IAdView generatedAdView = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, generatedAdView);

        assertThat(generatedAdView).isSameAs(generatedAdView2);

        verify(renderer, times(2)).putCreativeIntoAdView(same(generatedAdView), same(creative), same(beaconService), same(subject), eq(adSlot), any(Timer.class));
    }

    @Test
    public void getAdView_whenRecyclingViews_withDiffFeedPosition_returnsRecycledView_withNewCreative() throws Exception {
        Creative creative2 = mock(Creative.class);
        when(availableCreatives.size()).thenReturn(2);
        when(availableCreatives.getNext()).thenReturn(creative).thenReturn(creative2).thenThrow(new RuntimeException("Too many calls"));
        IAdView generatedAdView = subject.getAdView(Robolectric.application, 1, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, 2, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, generatedAdView);

        assertThat(generatedAdView).isSameAs(generatedAdView2);
        verify(renderer).putCreativeIntoAdView(same(generatedAdView), same(creative), same(beaconService), same(subject), eq(1), any(Timer.class));
        verify(renderer).putCreativeIntoAdView(same(generatedAdView2), same(creative2), same(beaconService), same(subject), eq(2), any(Timer.class));
    }

    @Test
    public void getAdView_whenAdViewsBySlotDoesNotContainAdviewForPosition_returnsNewAdview() {
        createSubject("abc");
        IAdView generatedAdView = subject.getAdView(Robolectric.application, 2, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        assertThat(generatedAdView).isNotNull();
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, 12, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        assertThat(generatedAdView).isNotSameAs(generatedAdView2);
    }

    @Test
    public void ifSlotIsEmptyAndThereAreMoreAdsToShow_fireNewAdsIsCalled() {
        int adSlot = 2;
        when(availableCreatives.size()).thenReturn(1);
        when(availableCreatives.getNext()).thenReturn(creative);
        IAdView generatedAdView = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);

        assertThat(subject.firedNewAdsToShow);
    }

    @Test
    public void ifSlotNOTEmptyAndCreativeExpired_fireNewAdsIsCalled() {
        Creative creative2 = mock(Creative.class);
        when(availableCreatives.size()).thenReturn(2);
        when(availableCreatives.getNext()).thenReturn(creative).thenReturn(creative2).thenThrow(new RuntimeException("Too many calls"));
        IAdView generatedAdView = subject.getAdView(Robolectric.application, 1, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);
        when(creative.hasExpired(anyInt())).thenReturn(true);
        subject.firedNewAdsToShow = false;
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, 1, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, generatedAdView);
        assertThat(generatedAdView).isSameAs(generatedAdView2);

        assertThat(subject.firedNewAdsToShow);
    }

    @Test
    public void creativeIndicesStoresAllUniqueCreativeIndexHistory_creativesBySlotOnlyCachesTenUniqueIndices() {
        when(availableCreatives.size()).thenReturn(11);
        when(availableCreatives.getNext()).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative).thenReturn(creative)
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

        assertThat(subject.getPositionsFilledByAds().size()).isEqualTo(10);
        assertThat(subject.creativeIndices.size()).isEqualTo(11);
        assertThat(subject.getNumberOfAdsBeforePosition(0)).isEqualTo(0);
        assertThat(subject.getNumberOfAdsBeforePosition(11)).isEqualTo(3);
        assertThat(subject.getNumberOfAdsBeforePosition(21)).isEqualTo(7);
        assertThat(subject.getNumberOfAdsBeforePosition(50)).isEqualTo(11);
    }

    @Test
    public void ifSlotNOTEmptyAndCreativeNOTExpired_fireNewAdsIsNOTCalled() {
        Creative creative2 = mock(Creative.class);
        when(availableCreatives.size()).thenReturn(2);
        when(availableCreatives.getNext()).thenReturn(creative).thenReturn(creative2).thenThrow(new RuntimeException("Too many calls"));
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
        when(availableCreatives.size()).thenReturn(0);
        IAdView generatedAdView = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, 6, null);

        assertThat(false == subject.firedNewAdsToShow);
    }


    private void createDfpSubject(String key) {
        subject = new Sharethrough(Robolectric.application, key, adCacheTimeInMilliseconds, renderer, availableCreatives, beaconService, adFetcher, imageFetcher, dfpNetworking);
        subject.setOnStatusChangeListener(onStatusChangeListener);
    }

    @Test
    public void whenDfpModeIsTrueAndIsNotBackfill_usesDfpNetworking() {
        createDfpSubject(key);
        verify(dfpNetworking).fetchDFPPath(eq(key), dfpPathFetcherCallback.capture());

        dfpPathFetcherCallback.getValue().receivedURL("dfpPath");
        verify(dfpNetworking).fetchCreativeKey(eq(Robolectric.application), eq("dfpPath"), dfpCreativeKeyCallback.capture());

        String serverParams = "creative_key=abc123";
        Sharethrough.addDFPKeys("dfpPath", serverParams);
        queryStringParams.add(new BasicNameValuePair("creative_key", "abc123"));

        dfpCreativeKeyCallback.getValue().receivedCreativeKey();

        verify(adFetcher, atLeastOnce()).fetchAds(same(imageFetcher), eq(apiUri), eq(queryStringParams), creativeHandler.capture(), adFetcherCallback.capture(), placementHandler.capture());
    }

    @Test
    public void whenDfpModeIsTrueAndIsBackfill_callsSTXAdServer() {
        createDfpSubject(key);
        verify(dfpNetworking).fetchDFPPath(eq(key), dfpPathFetcherCallback.capture());

        dfpPathFetcherCallback.getValue().receivedURL("dfpPath");
        verify(dfpNetworking).fetchCreativeKey(eq(Robolectric.application), eq("dfpPath"), dfpCreativeKeyCallback.capture());

        String serverParams = "creative_key=STX_MONETIZE";
        Sharethrough.addDFPKeys("dfpPath", serverParams);

        dfpCreativeKeyCallback.getValue().receivedCreativeKey();

        verify(adFetcher, atLeastOnce()).fetchAds(same(imageFetcher), eq(apiUri), eq(queryStringParams), creativeHandler.capture(), adFetcherCallback.capture(), placementHandler.capture());
    }

    @Test
    public void whenCreativeKey_addDFPKeys_putsHashMapIntoMap() {
        assertThat(subject.popDFPKeys("key")).isNull();

        subject.addDFPKeys("key", "creative_key=xyz789");

        HashMap<String, String> expectedHashMap = new HashMap<>();
        expectedHashMap.put("creative_key", "xyz789");
        assertThat(subject.popDFPKeys("key")).isEqualTo(expectedHashMap);
        assertThat(subject.popDFPKeys("key")).isNull();
    }

    @Test
    public void whenCampaignKey_addDFPKeys_putsHashMapIntoMap() {
        assertThat(subject.popDFPKeys("key")).isNull();

        subject.addDFPKeys("key", "campaign_key=campKey");

        HashMap<String, String> expectedHashMap = new HashMap<>();
        expectedHashMap.put("campaign_key", "campKey");
        assertThat(subject.popDFPKeys("key")).isEqualTo(expectedHashMap);
        assertThat(subject.popDFPKeys("key")).isNull();
    }

    @Test
    public void whenNeither_addDFPKeys_putHashMapIntoMap() {
        assertThat(subject.popDFPKeys("key")).isNull();

        subject.addDFPKeys("key", "randomKey");

        HashMap<String, String> expectedHashMap = new HashMap<>();
        expectedHashMap.put("creative_key", "randomKey");
        assertThat(subject.popDFPKeys("key")).isEqualTo(expectedHashMap);
        assertThat(subject.popDFPKeys("key")).isNull();
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
        subject.putCreativeIntoAdView(adView);
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), eq(queryStringParams), creativeHandler.capture(), adFetcherCallback.capture(), placementHandler.capture());
        placementHandler.getValue().apply(placement);
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
        subject.putCreativeIntoAdView(adView);
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), eq(queryStringParams), creativeHandler.capture(), adFetcherCallback.capture(), placementHandler.capture());
        placementHandler.getValue().apply(placement);
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

    @Test
    public void constructor_setsCorrectDefaultPlacement() {
        assertThat(subject.placement.getArticlesBetweenAds()).isEqualTo(Integer.MAX_VALUE);
        assertThat(subject.placement.getArticlesBeforeFirstAd()).isEqualTo(Integer.MAX_VALUE);
    }
}
