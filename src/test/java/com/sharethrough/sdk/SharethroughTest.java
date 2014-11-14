package com.sharethrough.sdk;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.sharethrough.sdk.network.AdFetcher;
import com.sharethrough.sdk.network.ImageFetcher;
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
    private int adCacheTimeInMilliseconds;
    private String apiPrefix;
    @Captor private ArgumentCaptor<Function<Creative, Void>> creativeHandler;
    @Captor private ArgumentCaptor<AdFetcher.Callback> adFetcherCallback;
    private String key = "abc";

    @Before
    public void setUp() throws Exception {
        Robolectric.application.getApplicationInfo().metaData = new Bundle();

        Robolectric.Reflection.setFinalStaticField(Sharethrough.class, "EXECUTOR_SERVICE", executorService);
        adView = makeMockAdView();
        adCacheTimeInMilliseconds = 20000;
        apiPrefix = "http://btlr.sharethrough.com/v3?placement_key=";

        doNothing().when(adFetcher).fetchAds(same(imageFetcher), eq(apiPrefix), creativeHandler.capture(), adFetcherCallback.capture());

        createSubject(key);
    }

    private void createSubject(String key) {
        subject = new Sharethrough(Robolectric.application, key, adCacheTimeInMilliseconds, renderer, beaconService, adFetcher, imageFetcher, creativesQueue);
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
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiPrefix), creativeHandler.capture(), adFetcherCallback.capture());
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

        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiPrefix), creativeHandler.capture(), adFetcherCallback.capture());
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
        verify(adFetcher).fetchAds(same(imageFetcher), eq(serverPrefix), creativeHandler.capture(), adFetcherCallback.capture());
    }

    @Test
    public void whenRequestFinsihesAndImagesFinishDownloading_whenQueueWantsMore_fetchesMoreAds() throws Exception {
        when(creativesQueue.readyForMore()).thenReturn(true);
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiPrefix), creativeHandler.capture(), adFetcherCallback.capture());
        reset(adFetcher);
        adFetcherCallback.getValue().finishedLoading();

        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiPrefix), creativeHandler.capture(), adFetcherCallback.capture());
    }

    @Test
    public void whenRequestFinsihesAndImagesFinishDownloading_whenQueueDoesNotWantsMore_doesNotFetchMoreAds() throws Exception {
        when(creativesQueue.readyForMore()).thenReturn(false);
        verify(adFetcher).fetchAds(same(imageFetcher), eq(apiPrefix), creativeHandler.capture(), adFetcherCallback.capture());
        reset(adFetcher);
        adFetcherCallback.getValue().finishedLoading();

        verifyNoMoreInteractions(adFetcher);
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
}