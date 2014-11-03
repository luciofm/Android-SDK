package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.sharethrough.test.Fixtures;
import com.sharethrough.test.util.AdView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class SharethroughTest {
    private static final String FIXTURE = Fixtures.getFile("assets/str_ad_youtube.json");
    private static final byte[] IMAGE_BYTES = new byte[] {0, 1, 2, 3, 4};
    private Sharethrough subject;
    private ExecutorService executorService;
    private AdView adView;
    private Renderer renderer;

    @Before
    public void setUp() throws Exception {
        Robolectric.application.getApplicationInfo().metaData = new Bundle();

        executorService = mock(ExecutorService.class);
        adView = makeMockAdView();
        renderer = Mockito.mock(Renderer.class);
    }

    private AdView makeMockAdView() {
        AdView result = mock(AdView.class);
        when(result.getContext()).thenReturn(Robolectric.application);
        when(result.getTitle()).thenReturn(mock(TextView.class));
        when(result.getDescription()).thenReturn(mock(TextView.class));
        when(result.getAdvertiser()).thenReturn(mock(TextView.class));
        when(result.getThumbnail()).thenReturn(mock(FrameLayout.class));
        when(result.getThumbnail().getContext()).thenReturn(Robolectric.application);
        verify(result).getThumbnail(); // clear it out for verifyNoInteractions
        return result;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Test
    public void settingKey_loadsAdsFromServer() throws Exception {
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", "http://btlr.sharethrough.com/v3?placement_key=" + key, new TestHttpResponse(200, FIXTURE));

        subject = new Sharethrough(Robolectric.application, executorService, key, renderer);
        subject.putCreativeIntoAdView(adView);

        ArgumentCaptor<Runnable> creativeFetcherArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(creativeFetcherArgumentCaptor.capture());

        verifyNoMoreInteractions(adView);

        reset(executorService);
        creativeFetcherArgumentCaptor.getValue().run();

        verifyNoMoreInteractions(adView);

        Robolectric.addHttpResponseRule("GET", "http://th.umb.na/il/URL", new TestHttpResponse(200, IMAGE_BYTES));
        ArgumentCaptor<Runnable> imageFetcherArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(imageFetcherArgumentCaptor.capture());
        imageFetcherArgumentCaptor.getValue().run();

        verifyNoMoreInteractions(adView);

        verifyCreativeHasBeenPlacedInAdview(adView);
    }

    private void verifyCreativeHasBeenPlacedInAdview(AdView adView) {
        verify(renderer).putCreativeIntoAdView(eq(adView), any(Creative.class));
    }

    @Test(expected = KeyRequiredException.class)
    public void notSettingKey_throwsExceptionWhenAdIsRequested() {
        subject = new Sharethrough(Robolectric.application, executorService, null, renderer);
    }

    @Test
    public void whenServerReturns204_doesNothing() throws Exception {
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", "http://btlr.sharethrough.com/v3?placement_key=" + key,
                new TestHttpResponse(204, "I got nothing for ya"));
        subject = new Sharethrough(Robolectric.application, executorService, key, renderer);

        Mockito.reset(adView);

        runExecutor();

        verifyNoMoreInteractions(adView);
    }

    @Test
    public void whenMoreAdViewsAreWaiting_ThanCreativesThatAreAvailable_keepsWaiting() throws Exception {
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", "http://btlr.sharethrough.com/v3?placement_key=" + key, new TestHttpResponse(200, FIXTURE));

        subject = new Sharethrough(Robolectric.application, executorService, key, renderer);
        AdView adView2 = makeMockAdView();
        subject.putCreativeIntoAdView(adView2);
        subject.putCreativeIntoAdView(adView);

        ArgumentCaptor<Runnable> creativeFetcherArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(creativeFetcherArgumentCaptor.capture());

        reset(executorService);
        creativeFetcherArgumentCaptor.getValue().run();

        Robolectric.addHttpResponseRule("GET", "http://th.umb.na/il/URL", new TestHttpResponse(200, IMAGE_BYTES));
        ArgumentCaptor<Runnable> imageFetcherArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(imageFetcherArgumentCaptor.capture());
        imageFetcherArgumentCaptor.getValue().run();

        verifyCreativeHasBeenPlacedInAdview(adView2);
        verifyNoMoreInteractions(adView);
        verifyNoMoreInteractions(renderer);
    }

    @Test
    public void whenImageCantBeDownloaded_doesNotUseAd() throws Exception {
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", "http://btlr.sharethrough.com/v3?placement_key=" + key, new TestHttpResponse(200, FIXTURE));

        subject = new Sharethrough(Robolectric.application, executorService, key, renderer);
        subject.putCreativeIntoAdView(adView);

        ArgumentCaptor<Runnable> creativeFetcherArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(creativeFetcherArgumentCaptor.capture());

        verifyNoMoreInteractions(adView);

        reset(executorService);
        creativeFetcherArgumentCaptor.getValue().run();

        verifyNoMoreInteractions(adView);

        Robolectric.addHttpResponseRule("GET", "http://th.umb.na/il/URL", new TestHttpResponse(404, "NOT FOUND"));
        ArgumentCaptor<Runnable> imageFetcherArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(imageFetcherArgumentCaptor.capture());
        imageFetcherArgumentCaptor.getValue().run();

        verifyNoMoreInteractions(adView);
    }

    @Test
    public void whenThereAreCreativesPreloaded_usesPreloadedCreative() {
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", "http://btlr.sharethrough.com/v3?placement_key=" + key, new TestHttpResponse(200, FIXTURE));

        subject = new Sharethrough(Robolectric.application, executorService, key, renderer);

        ArgumentCaptor<Runnable> creativeFetcherArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(creativeFetcherArgumentCaptor.capture());

        reset(executorService);
        creativeFetcherArgumentCaptor.getValue().run();

        Robolectric.addHttpResponseRule("GET", "http://th.umb.na/il/URL", new TestHttpResponse(200, IMAGE_BYTES));
        ArgumentCaptor<Runnable> imageFetcherArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(imageFetcherArgumentCaptor.capture());
        imageFetcherArgumentCaptor.getValue().run();

        subject.putCreativeIntoAdView(adView);
        verifyCreativeHasBeenPlacedInAdview(adView);
    }

    @Test
    public void whenAndroidManifestHasCustomApiServer_usesThatServer() throws Exception {
        String serverPrefix = "http://dumb-waiter.sharethrough.com/?creative_type=video&placement_key=";
        Robolectric.application.getApplicationInfo().metaData.putString("STR_ADSERVER_API", serverPrefix);
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", serverPrefix + key, new TestHttpResponse(200, FIXTURE));

        subject = new Sharethrough(Robolectric.application, executorService, key, renderer);

        ArgumentCaptor<Runnable> creativeFetcherArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(creativeFetcherArgumentCaptor.capture());
        creativeFetcherArgumentCaptor.getValue().run();

        assertThat(Robolectric.getLatestSentHttpRequest().getRequestLine().getUri()).isEqualTo(serverPrefix + key);
    }

    private void runExecutor() {
        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(runnableArgumentCaptor.capture());
        runnableArgumentCaptor.getValue().run();
    }
}