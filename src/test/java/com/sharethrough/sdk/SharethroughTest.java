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
    @Mock private CreativesQueue creativesQueue;
    @Mock private Runnable adReadyCallback;
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
        adCacheTimeInMilliseconds = 20000;
        apiUri = "http://btlr.sharethrough.com/v3?placement_key=" + key;

        doNothing().when(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());

        createSubject(key);
    }

    private void createSubject(String key) {
        subject = new Sharethrough(Robolectric.application, key, adCacheTimeInMilliseconds, renderer, beaconService, adFetcher, imageFetcher, creativesQueue, placementFetcher, null);
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


    private void verifyCreativeHasBeenPlacedInAdview(TestAdView adView, Runnable adReadyCallback) {
        ArgumentCaptor<Creative> creativeArgumentCaptor = ArgumentCaptor.forClass(Creative.class);
        verify(renderer).putCreativeIntoAdView(eq(adView), creativeArgumentCaptor.capture(), eq(beaconService), eq(subject), eq(adReadyCallback));
        assertThat(creativeArgumentCaptor.getValue()).isSameAs(this.creative);
    }

    @Test
    public void whenACreativeIsReady_whenMoreAdViewIsWaiting_putsCreativeInFirstAdView() throws Exception {
        // make two ad views wait
        TestAdView adView2 = makeMockAdView();
        Runnable adReadyCallback2 = mock(Runnable.class);
        subject.putCreativeIntoAdView(adView, adReadyCallback);
        subject.putCreativeIntoAdView(adView2, adReadyCallback2);

        // get back one creative
        creativeHandler.getValue().apply(creative);

        verifyCreativeHasBeenPlacedInAdview(adView, adReadyCallback);
    }

    @Test
    public void whenACreativeIsReady_whenNoMoreAdViewsAreWaiting_addCreativeToQueue() throws Exception {
        creativeHandler.getValue().apply(creative);
        verify(creativesQueue).add(creative);
    }

    @Test
    public void putCreativeIntoAdView_whenQueueWantsMore_fetchesMoreAds() throws Exception {
        when(creativesQueue.readyForMore()).thenReturn(true);
        reset(adFetcher);

        subject.putCreativeIntoAdView(adView, adReadyCallback);

        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());
    }

    @Test
    public void putCreativeIntoAdView_whenQueueHasCreatives_usesNextCreative() {
        when(creativesQueue.getNext()).thenReturn(creative).thenThrow(new RuntimeException("Too many calls to getNext"));

        subject.putCreativeIntoAdView(adView, adReadyCallback);
        verifyCreativeHasBeenPlacedInAdview(adView, adReadyCallback);
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
        when(creativesQueue.readyForMore()).thenReturn(true);
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());
        reset(adFetcher);
        adFetcherCallback.getValue().finishedLoading();

        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());
    }

    @Test
    public void whenRequestFinsihesAndImagesFinishDownloading_whenQueueDoesNotWantsMore_doesNotFetchMoreAds() throws Exception {
        when(creativesQueue.readyForMore()).thenReturn(false);
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
    public void whenFirstCreativeIsNotAvailable_notifiesOnStatusChangeListenerOnMainThread() throws Exception {
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiUri), creativeHandler.capture(), adFetcherCallback.capture());

        Robolectric.pauseMainLooper();
        adFetcherCallback.getValue().finishedLoadingWithNoAds();
        verifyNoMoreInteractions(onStatusChangeListener);
        Robolectric.unPauseMainLooper();

        verify(onStatusChangeListener).noAdsToShow();
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
    public void getAdview_whenAdViewsBySlotContainsAdviewForPosition_returnsStoredAdview() {
        adCacheTimeInMilliseconds = 5;
        createSubject("abc");
        int adSlot = 2;
        BasicAdView generatedAdView = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 0, 0, 0, 0);
        assertThat(generatedAdView).isNotNull();
        BasicAdView generatedAdView2 = subject.getAdView(Robolectric.application, adSlot, android.R.layout.simple_list_item_1, 0, 0, 0, 0);
        assertThat(generatedAdView).isSameAs(generatedAdView2);
    }

    @Test
    public void getAdView_whenAdViewsBySlotDoesNotContainAdviewForPosition_returnsNewAdview() {
        adCacheTimeInMilliseconds = 5;
        createSubject("abc");
        BasicAdView generatedAdView = subject.getAdView(Robolectric.application, 2, android.R.layout.simple_list_item_1, 0, 0, 0, 0);
        assertThat(generatedAdView).isNotNull();
        BasicAdView generatedAdView2 = subject.getAdView(Robolectric.application, 12, android.R.layout.simple_list_item_1, 0, 0, 0, 0);
        assertThat(generatedAdView).isNotSameAs(generatedAdView2);
    }

    private void createDfpSubject(String key) {
        subject = new Sharethrough(Robolectric.application, key, adCacheTimeInMilliseconds, renderer, beaconService, adFetcher, imageFetcher, creativesQueue, placementFetcher, dfpNetworking);
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