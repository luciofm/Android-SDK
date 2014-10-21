package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
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

    // TODO: use fixture file
//        FileInputStream fixture = Robolectric.application.openFileInput("ads.json");
    private static final String FIXTURE = "{\n" +
            "  \"placement\": {\n" +
            "    \"template\": \"<div/>\"," +
            "    \"layout\": \"single\"\n" +
            "  },\n" +
            "  \"creatives\": [\n" +
            "    {\n" +
            "      \"price\": 121460,\n" +
            "      \"signature\": \"c19\",\n" +
            "      \"creative\": {\n" +
            "        \"creative_key\": \"469cc02b\",\n" +
            "        \"description\": \"Description.\",\n" +
            "        \"media_url\": \"mediaURL\",\n" +
            "        \"share_url\": \"shareURL\",\n" +
            "        \"variant_key\": \"15577\",\n" +
            "        \"advertiser\": \"Advertiser\",\n" +
            "        \"beacons\": {\n" +
            "          \"visible\": [\"visibleBeacon\"],\n" +
            "          \"click\": [\"clickBeacon\"],\n" +
            "          \"play\": []\n" +
            "        },\n" +
            "        \"thumbnail_url\": \"//th.umb.na/il/URL\",\n" +
            "        \"title\": \"Title\",\n" +
            "        \"action\": \"clickout\"\n" +
            "      },\n" +
            "      \"priceType\": \"CPE\",\n" +
            "      \"version\": 1\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    private static final byte[] IMAGE_BYTES = new byte[] {0, 1, 2, 3, 4};
    private Sharethrough subject;
    private ExecutorService executorService;
    private AdView adView;

    @Before
    public void setUp() throws Exception {
        Robolectric.application.getApplicationInfo().metaData = new Bundle();

        executorService = mock(ExecutorService.class);
        adView = makeMockAdView();
    }

    private AdView makeMockAdView() {
        AdView result = mock(AdView.class);
        when(result.getTitle()).thenReturn(mock(TextView.class));
        when(result.getDescription()).thenReturn(mock(TextView.class));
        when(result.getAdvertiser()).thenReturn(mock(TextView.class));
        when(result.getThumbnail()).thenReturn(mock(ImageView.class));
        return result;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Test
    public void settingKey_loadsAdsFromServer() throws Exception {
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", "http://btlr.sharethrough.com/v3?placement_key=" + key, new TestHttpResponse(200, FIXTURE));

        Robolectric.pauseMainLooper();

        subject = new Sharethrough(Robolectric.application, executorService, key);
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

        Robolectric.unPauseMainLooper();

        verify(adView.getTitle()).setText("Title");
        verify(adView.getDescription()).setText("Description.");
        verify(adView.getAdvertiser()).setText("Advertiser");
        verify(adView.getThumbnail()).setImageBitmap(eq(BitmapFactory.decodeByteArray(IMAGE_BYTES, 0, IMAGE_BYTES.length)));
    }

    @Test(expected = KeyRequiredException.class)
    public void notSettingKey_throwsExceptionWhenAdIsRequested() {
        subject = new Sharethrough(Robolectric.application, executorService, null);
    }

    @Test
    public void whenServerReturns204_doesNothing() throws Exception {
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", "http://btlr.sharethrough.com/v3?placement_key=" + key,
                new TestHttpResponse(204, "I got nothing for ya"));
        subject = new Sharethrough(Robolectric.application, executorService, key);

        Mockito.reset(adView);

        runExecutor();

        verifyNoMoreInteractions(adView);
    }

    @Test
    public void whenMoreAdViewsAreWaiting_ThanCreativesThatAreAvailable_keepsWaiting() throws Exception {
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", "http://btlr.sharethrough.com/v3?placement_key=" + key, new TestHttpResponse(200, FIXTURE));

        subject = new Sharethrough(Robolectric.application, executorService, key);
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

        verify(adView2.getTitle()).setText("Title");
        verifyNoMoreInteractions(adView);
    }

    @Test
    public void whenImageCantBeDownloaded_doesNotUseAd() throws Exception {
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", "http://btlr.sharethrough.com/v3?placement_key=" + key, new TestHttpResponse(200, FIXTURE));

        Robolectric.pauseMainLooper();

        subject = new Sharethrough(Robolectric.application, executorService, key);
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

        Robolectric.unPauseMainLooper();

        verifyNoMoreInteractions(adView);
    }

    @Test
    public void whenThereAreCreativesPreloaded_usesPreloadedCreative() {
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", "http://btlr.sharethrough.com/v3?placement_key=" + key, new TestHttpResponse(200, FIXTURE));

        subject = new Sharethrough(Robolectric.application, executorService, key);

        ArgumentCaptor<Runnable> creativeFetcherArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(creativeFetcherArgumentCaptor.capture());

        reset(executorService);
        creativeFetcherArgumentCaptor.getValue().run();

        Robolectric.addHttpResponseRule("GET", "http://th.umb.na/il/URL", new TestHttpResponse(200, IMAGE_BYTES));
        ArgumentCaptor<Runnable> imageFetcherArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(imageFetcherArgumentCaptor.capture());
        imageFetcherArgumentCaptor.getValue().run();

        subject.putCreativeIntoAdView(adView);
        verify(adView.getTitle()).setText("Title");
        verify(adView.getDescription()).setText("Description.");
        verify(adView.getAdvertiser()).setText("Advertiser");
        verify(adView.getThumbnail()).setImageBitmap(eq(BitmapFactory.decodeByteArray(IMAGE_BYTES, 0, IMAGE_BYTES.length)));
    }

    @Test
    public void whenAndroidManifestHasCustomApiServer_usesThatServer() throws Exception {
        String serverPrefix = "http://dumb-waiter.sharethrough.com/?creative_type=video&placement_key=";
        Robolectric.application.getApplicationInfo().metaData.putString("STR_ADSERVER_API", serverPrefix);
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", serverPrefix + key, new TestHttpResponse(200, FIXTURE));

        subject = new Sharethrough(Robolectric.application, executorService, key);

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