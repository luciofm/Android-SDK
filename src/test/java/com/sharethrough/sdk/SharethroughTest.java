package com.sharethrough.sdk;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.sharethrough.sdk.network.AdFetcher;
import com.sharethrough.sdk.network.DFPNetworking;
import com.sharethrough.sdk.network.ImageFetcher;
import com.sharethrough.sdk.network.PlacementFetcher;
import com.sharethrough.test.util.TestAdView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SharethroughTest extends TestBase {
    private Sharethrough subject;
    private TestAdView adView;
    @Mock private ExecutorService executorService;
    @Mock private Renderer renderer;
    @Mock private BeaconService beaconService;
    @Mock private ImageFetcher imageFetcher;
    @Mock private AdFetcher adFetcher;
    @Mock private Creative creative;
    @Mock private CreativesQueue availableCreatives;
    @Mock private PlacementFetcher placementFetcher;
    @Mock private Sharethrough.OnStatusChangeListener onStatusChangeListener;
    @Mock
    private DFPNetworking dfpNetworking;
    private int adCacheTimeInMilliseconds;
    private String apiUri;
    @Captor private ArgumentCaptor<Function<Creative, Void>> creativeHandler;
    @Captor private ArgumentCaptor<AdFetcher.Callback> adFetcherCallback;
    @Captor
    private ArgumentCaptor<DFPNetworking.DFPPathFetcherCallback> dfpPathFetcherCallback;
    @Captor
    private ArgumentCaptor<DFPNetworking.DFPCreativeKeyCallback> dfpCreativeKeyCallback;
    private String key = "abc";


    @Before
    public void setUp() throws Exception {
        Robolectric.application.getApplicationInfo().metaData = new Bundle();

        Robolectric.Reflection.setFinalStaticField(Sharethrough.class, "EXECUTOR_SERVICE", executorService);
        adView = makeMockAdView();
        apiUri = "http://btlr.sharethrough.com/v3?placement_key=" + key;

        doNothing().when(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());

        createSubject(key);
    }

    private void createSubject(String key) {
        subject = new Sharethrough(Robolectric.application, key, adCacheTimeInMilliseconds, renderer,
                availableCreatives, beaconService, adFetcher, imageFetcher, placementFetcher, null);
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
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());
    }

    @Test(expected = KeyRequiredException.class)
    public void notSettingKey_throwsExceptionWhenAdIsRequested() {
        createSubject(null);
    }


    private void verifyCreativeHasBeenPlacedInAdview(TestAdView adView) {
        ArgumentCaptor<Creative> creativeArgumentCaptor = ArgumentCaptor.forClass(Creative.class);
        verify(renderer).putCreativeIntoAdView(eq(adView), creativeArgumentCaptor.capture(), eq(beaconService), eq(subject), any(Timer.class));
        assertThat(creativeArgumentCaptor.getValue()).isSameAs(this.creative);
    }

    @Test
    public void whenACreativeIsReady_whenMoreAdViewIsWaiting_putsCreativeInFirstAdView() throws Exception {
        // make two ad views wait
        TestAdView adView2 = makeMockAdView();
        subject.putCreativeIntoAdView(adView);
        subject.putCreativeIntoAdView(adView2);

        // get back one creative
        creativeHandler.getValue().apply(creative);

        verifyCreativeHasBeenPlacedInAdview(adView);
    }

    @Test
    public void whenCreativeIsReady_andWaitingAdViewHasBeenGCd_ignoresWaitingAdView() throws Exception {
        TestAdView myAdView = makeMockAdView();

        subject.putCreativeIntoAdView(myAdView);

        myAdView = null;
        // hope WeakReferences are collected here
        System.gc();
        Thread.sleep(100);

        creativeHandler.getValue().apply(creative);

        verify(availableCreatives).add(creative);
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

        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());
    }

    @Test
    public void putCreativeIntoAdView_whenQueueHasCreatives_usesNextCreative() {
        when(availableCreatives.getNext()).thenReturn(creative).thenThrow(new RuntimeException("Too many calls to getNext"));

        subject.putCreativeIntoAdView(adView);
        verifyCreativeHasBeenPlacedInAdview(adView);
    }

    @Test
    public void whenAndroidManifestHasCustomApiServer_usesThatServer() throws Exception {
        String serverPrefix = "http://dumb-waiter.sharethrough.com/?creative_type=video&placement_key=";
        Robolectric.application.getApplicationInfo().metaData.putString("STR_ADSERVER_API", serverPrefix);
        createSubject(key);
        verify(adFetcher).fetchAds(same(imageFetcher), eq(serverPrefix + key), creativeHandler.capture(), adFetcherCallback.capture());
    }

    @Test
    public void whenRequestFinsihesAndImagesFinishDownloading_whenQueueWantsMore_fetchesMoreAds() throws Exception {
        when(availableCreatives.readyForMore()).thenReturn(true);
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());
        reset(adFetcher);
        adFetcherCallback.getValue().finishedLoading();

        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());
    }

    @Test
    public void whenRequestFinsihesAndImagesFinishDownloading_whenQueueDoesNotWantsMore_doesNotFetchMoreAds() throws Exception {
        when(availableCreatives.readyForMore()).thenReturn(false);
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());
        reset(adFetcher);
        adFetcherCallback.getValue().finishedLoading();

        verifyNoMoreInteractions(adFetcher);
    }

    @Test
    public void whenFirstCreativeIsPrefetches_notifiesOnStatusChangeListenerOnMainThread() throws Exception {
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());

        Robolectric.pauseMainLooper();
        creativeHandler.getValue().apply(creative);
        verifyNoMoreInteractions(onStatusChangeListener);
        Robolectric.unPauseMainLooper();

        verify(onStatusChangeListener).newAdsToShow();
    }

    @Test
    public void whenCreativeIsPrefetched_whenNewAdsToShowHasAlreadyBeenCalled_doesNotCallItAgain() throws Exception {
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());

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
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());

        Robolectric.pauseMainLooper();
        adFetcherCallback.getValue().finishedLoadingWithNoAds();
        verifyNoMoreInteractions(onStatusChangeListener);
        Robolectric.unPauseMainLooper();

        verify(onStatusChangeListener).noAdsToShow();
    }

    @Test
    public void whenCreativeIsPrefetched_whenNewAdsToShowHasBeenCalledButNoAdsToShowHasSinceBeenCalled_callsItAgain() throws Exception {
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());

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
    public void getPlacement_callsFetcher() throws Exception {
        Callback placementCallback = mock(Callback.class);
        subject.getPlacement(placementCallback);
        verify(placementFetcher).fetch(placementCallback);
    }

    @Test
    public void getAdView_whenAdViewsBySlotContainsAdviewForPosition_returnsStoredAdview() {
        int adSlot = 2;
        when(availableCreatives.getNext()).thenReturn(creative);
        IAdView generatedAdView = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, null);
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, null);

        verify(renderer).putCreativeIntoAdView(same(generatedAdView), same(creative), same(beaconService), same(subject), any(Timer.class));
        verify(renderer).putCreativeIntoAdView(same(generatedAdView2), same(creative), same(beaconService), same(subject), any(Timer.class));
    }

    @Test
    public void getAdView_whenRecyclingViews_withSameFeedPosition_returnsRecycledView_withSameCreative() throws Exception {
        int adSlot = 2;
        when(availableCreatives.getNext()).thenReturn(creative);
        IAdView generatedAdView = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, null);
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, generatedAdView);

        assertThat(generatedAdView).isSameAs(generatedAdView2);

        verify(renderer, times(2)).putCreativeIntoAdView(same(generatedAdView), same(creative), same(beaconService), same(subject), any(Timer.class));
    }

    @Test
    public void getAdView_whenRecyclingViews_withDiffFeedPosition_returnsRecycledView_withNewCreative() throws Exception {
        Creative creative2 = mock(Creative.class);
        when(availableCreatives.getNext()).thenReturn(creative).thenReturn(creative2).thenThrow(new RuntimeException("Too many calls"));
        IAdView generatedAdView = subject.getAdView(Robolectric.application, 1, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, null);
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, 2, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, generatedAdView);

        assertThat(generatedAdView).isSameAs(generatedAdView2);
        verify(renderer).putCreativeIntoAdView(same(generatedAdView), same(creative), same(beaconService), same(subject), any(Timer.class));
        verify(renderer).putCreativeIntoAdView(same(generatedAdView2), same(creative2), same(beaconService), same(subject), any(Timer.class));
    }

    @Test
    public void getAdView_whenAdViewsBySlotDoesNotContainAdviewForPosition_returnsNewAdview() {
        createSubject("abc");
        IAdView generatedAdView = subject.getAdView(Robolectric.application, 2, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, null);
        assertThat(generatedAdView).isNotNull();
        IAdView generatedAdView2 = subject.getAdView(Robolectric.application, 12, android.R.layout.simple_list_item_1, 1, 2, 3, 4, 5, null);
        assertThat(generatedAdView).isNotSameAs(generatedAdView2);
    }

    private void createDfpSubject(String key) {
        subject = new Sharethrough(Robolectric.application, key, adCacheTimeInMilliseconds, renderer, availableCreatives, beaconService, adFetcher, imageFetcher, placementFetcher, dfpNetworking);
        subject.setOnStatusChangeListener(onStatusChangeListener);
    }

    @Test
    public void whenDfpModeIsTrue_usesDfpNetworking() {
        createDfpSubject(key);
        verify(dfpNetworking).fetchDFPPath(eq(executorService), eq(key), dfpPathFetcherCallback.capture());

        dfpPathFetcherCallback.getValue().receivedURL("dfpPath");
        verify(dfpNetworking).fetchCreativeKey(eq(Robolectric.application), eq("dfpPath"), dfpCreativeKeyCallback.capture());

        Sharethrough.addCreativeKey("dfpPath", "creativeKey");

        dfpCreativeKeyCallback.getValue().receivedCreativeKey();

        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri + "&creative_key=creativeKey"), creativeHandler.capture(), adFetcherCallback.capture());
    }

    @Test
    public void addCreativeKey_putsKeyIntoMap() {
        assertThat(subject.popCreativeKey("key")).isNull();

        subject.addCreativeKey("key", "test-key");

        assertThat(subject.popCreativeKey("key")).isEqualTo("test-key");
        assertThat(subject.popCreativeKey("key")).isNull();
    }
}