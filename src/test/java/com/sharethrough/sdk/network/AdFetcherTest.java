package com.sharethrough.sdk.network;

import com.sharethrough.sdk.*;
import com.sharethrough.test.Fixtures;
import com.sharethrough.test.util.Misc;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AdFetcherTest extends TestBase {
    private static final String SINGLE_LAYOUT_FIXTURE = Fixtures.getFile("assets/str_single_ad_youtube.json");
    private static final String MULTIPLE_LAYOUT_FIXTURE = Fixtures.getFile("assets/str_multiple_ad_youtube.json");
    private static final String MULTIPLE_LAYOUT_FIXTURE_ZERO_BEFORE = Fixtures.getFile("assets/str_multiple_ad_youtube_zero_ads_before.json");
    private static final String MULTIPLE_LAYOUT_FIXTURE_ZERO_BETWEEN = Fixtures.getFile("assets/str_multiple_ad_youtube_zero_ads_between.json");
    private static final String NO_ADS_FIXTURE = Fixtures.getFile("assets/str_no_creatives.json");
    @Mock private ExecutorService executorService;
    @Mock private BeaconService beaconService;
    @Mock private ImageFetcher imageFetcher;
    @Mock private Function<Creative, Void> creativeHandler;
    @Mock private Creative creative;
    @Mock private AdFetcher.Callback adFetcherCallback;
    @Mock private Function<Placement, Void> placementHandler;
    private AdFetcher subject;
    private String apiUri;
    private String key;
    @Captor
    private ArgumentCaptor<ImageFetcher.Callback> imageFetcherCallback;
    private ArrayList<NameValuePair> queryStringParams;
    private String expectedUri;
    private ArrayList<NameValuePair> expectedStringParams;

    @Before
    public void setUp() throws Exception {
        String apiUriPrefix = "http://api";
        apiUri = apiUriPrefix;
        key = "key";
        queryStringParams = new ArrayList<NameValuePair>(1);
        queryStringParams.add(new BasicNameValuePair("key", key));

        when(beaconService.getAppPackageName()).thenReturn("com.sharethrough.example");
        when(beaconService.getAppVersionName()).thenReturn("version-number");
        expectedStringParams = (ArrayList<NameValuePair>) queryStringParams.clone();
        expectedStringParams.add(new BasicNameValuePair("appId", "version-number"));
        expectedStringParams.add(new BasicNameValuePair("appName", "com.sharethrough.example"));
        expectedUri = apiUri + "?" + URLEncodedUtils.format(expectedStringParams, "utf-8");
        subject = new AdFetcher(key, executorService, beaconService);
    }

    @Test
    public void fetchAds_whenServerReturnsAds_usesImageFetcherOnEachCreativeItReturns() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        verifyNoMoreInteractions(imageFetcher);

        Misc.runLast(executorService);

        verify(beaconService).adRequested(key);

        verifyFetchedImage(imageFetcher, "//th.umb.na/il/URL1", expectedUri, creativeHandler);
        verifyFetchedImage(imageFetcher, "//th.umb.na/il/URL2", expectedUri, creativeHandler);
    }

    @Test
    public void fetchAds_whenServerRequestFails_doesNothing() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(500, "Bad server"));

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        Misc.runLast(executorService);

        verifyNoMoreInteractions(imageFetcher);
    }

    @Test
    public void fetchAds_whenExceptionOccursBeforeGettingHttpResponse_logsSomeStuff() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, null);

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        Misc.runLast(executorService);

        List<ShadowLog.LogItem> logsForTag = ShadowLog.getLogsForTag("Sharethrough");
        assertThat(logsForTag.get(0).msg).isEqualTo("failed to get ads for key " + key + ": " + expectedUri);
    }

    @Test
    public void fetchAds_whenExceptionOccursAfterGettingHttpResponse_logsSomeStuffAndStuffAboutResponse() throws Exception {
        String responseBody = "{234789wdfjkl ";
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, responseBody));

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        Misc.runLast(executorService);

        List<ShadowLog.LogItem> logsForTag = ShadowLog.getLogsForTag("Sharethrough");
        assertThat(logsForTag.get(0).msg).isEqualTo("failed to get ads for key " + key + ": " + expectedUri + ": " + responseBody);

        verify(adFetcherCallback).finishedLoadingWithNoAds();
    }

    @Test
    public void fetchAds_whenRequestReturnsZeroCreatives_callsFinshedLoadingWithNoAds() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, NO_ADS_FIXTURE));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        Misc.runLast(executorService);
        verify(adFetcherCallback).finishedLoadingWithNoAds();
    }

    @Test
    public void fetchAds_whenRequestIsInProgress_doesNotStartNewRequest() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(runnableArgumentCaptor.capture());
        assertThat(runnableArgumentCaptor.getAllValues()).hasSize(1);
    }

    @Test
    public void fetchAds_whenPreviousRequestHasFinished_butImagesHaveNot_doesNotStartNewRequest() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        Misc.runLast(executorService);
        reset(executorService);

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        verifyNoMoreInteractions(executorService);
    }

    @Test
    public void fetchAds_whenPreviousRequestAndNotAllImagesHaveFinished_doesNotStartNewRequest() throws Exception {
        // make first request
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        Misc.runLast(executorService);
        reset(executorService);

        // finish loading only one image
        verify(imageFetcher, atLeastOnce()).fetchCreativeImages(any(URI.class), any(Response.Creative.class), imageFetcherCallback.capture());
        assertThat(imageFetcherCallback.getAllValues().size()).isGreaterThan(1);
        imageFetcherCallback.getAllValues().get(0).success(creative);

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        verifyNoMoreInteractions(executorService);
    }

    @Test
    public void fetchAds_whenPreviousRequestAndAllImagesHaveFinished_startsNewRequest() throws Exception {
        // make first request
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        Misc.runLast(executorService);
        reset(executorService);

        // finish loading all images
        verify(imageFetcher, atLeastOnce()).fetchCreativeImages(any(URI.class), any(Response.Creative.class), imageFetcherCallback.capture());
        for (ImageFetcher.Callback callback : imageFetcherCallback.getAllValues()) {
            callback.success(creative);
        }

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        verify(executorService).execute(any(Runnable.class));
    }

    @Test
    public void fetchAds_whenPreviousRequestAndAllImagesHaveFinished_doesCallback() throws Exception {
        // make first request
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        Misc.runLast(executorService);
        reset(executorService);

        // finish loading all images
        verify(imageFetcher, atLeastOnce()).fetchCreativeImages(any(URI.class), any(Response.Creative.class), imageFetcherCallback.capture());
        for (ImageFetcher.Callback callback : imageFetcherCallback.getAllValues()) {
            callback.success(creative);
        }

        verify(adFetcherCallback).finishedLoading();
    }

    @Test
    public void fetchAds_whenPreviousRequestAndAllImagesHaveFinishedWithFailure_startsNewRequest() throws Exception {
        // make first request
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        Misc.runLast(executorService);
        reset(executorService);

        // finish loading all images
        verify(imageFetcher, atLeastOnce()).fetchCreativeImages(any(URI.class), any(Response.Creative.class), imageFetcherCallback.capture());
        for (ImageFetcher.Callback callback : imageFetcherCallback.getAllValues()) {
            callback.failure();
        }

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        verify(executorService).execute(any(Runnable.class));
    }

    @Test
    public void fetchAds_whenLayoutIsMultiple_callsApplyOnPlacementHandler() throws Exception {
        final boolean[] placementHandlerWascalled = {false};
        placementHandler = new Function<Placement, Void>() {
            @Override
            public Void apply(Placement placement) {
                placementHandlerWascalled[0] = true;
                assertThat(placement.getArticlesBeforeFirstAd()).isEqualTo(2);
                assertThat(placement.getArticlesBetweenAds()).isEqualTo(3);
                return null;
            }
        };

        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, MULTIPLE_LAYOUT_FIXTURE));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        Misc.runLast(executorService);
        assertThat(placementHandlerWascalled[0]).isTrue();
    }

    @Test
    public void fetchAds_whenLayoutIsSingle_doesNotCallApplyOnPlacementHandler() throws Exception {
        final boolean[] placementHandlerWascalled = {false};
        placementHandler = new Function<Placement, Void>() {
            @Override
            public Void apply(Placement placement) {
                placementHandlerWascalled[0] = true;
                return null;
            }
        };

        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        Misc.runLast(executorService);
        assertThat(placementHandlerWascalled[0]).isFalse();
    }

    @Test
    public void fetchAds_whenLayoutIsMultipleAndArticlesBeforeFirstAdIsZero_doesNotCallApplyOnPlacementHandler() throws Exception {
        final boolean[] placementHandlerWascalled = {false};
        placementHandler = new Function<Placement, Void>() {
            @Override
            public Void apply(Placement placement) {
                placementHandlerWascalled[0] = true;
                return null;
            }
        };

        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, MULTIPLE_LAYOUT_FIXTURE_ZERO_BEFORE));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        Misc.runLast(executorService);
        assertThat(placementHandlerWascalled[0]).isFalse();
    }

    @Test
    public void fetchAds_whenLayoutIsMultipleAndArticlesBetweenAdsIsZero_doesNotCallApplyOnPlacementHandler() throws Exception {
        final boolean[] placementHandlerWascalled = {false};
        placementHandler = new Function<Placement, Void>() {
            @Override
            public Void apply(Placement placement) {
                placementHandlerWascalled[0] = true;
                return null;
            }
        };

        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, MULTIPLE_LAYOUT_FIXTURE_ZERO_BETWEEN));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        Misc.runLast(executorService);
        assertThat(placementHandlerWascalled[0]).isFalse();
    }

    @Test
    public void fetchAds_whenPlacementHasNotBeenSet_CallsApplyOnPlacementHandlerAndSetsPlacementSetToTrue() {
        final int[] placementHandlerCalledCounter = {0};
        placementHandler = new Function<Placement, Void>() {
            @Override
            public Void apply(Placement placement) {
                placementHandlerCalledCounter[0]++;
                return null;
            }
        };
        // first time
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, MULTIPLE_LAYOUT_FIXTURE));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        Misc.runLast(executorService);
        assertThat(placementHandlerCalledCounter[0]).isEqualTo(1);

        //second time
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        Misc.runLast(executorService);
        assertThat(placementHandlerCalledCounter[0]).isEqualTo(1);

    }

    @Test
    public void whenServerReturns204_doesNothing() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(204, "I got nothing for ya"));

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        Misc.runLast(executorService);

        verifyNoMoreInteractions(imageFetcher);
    }

    private void verifyFetchedImage(ImageFetcher imageFetcher, final String imageUrl, String apiUri, Function<Creative, Void> creativeHandler) {
        verify(imageFetcher).fetchCreativeImages(eq(URI.create(apiUri)), Matchers.argThat(new BaseMatcher<Response.Creative>() {
            @Override
            public boolean matches(Object o) {
                if (o instanceof Response.Creative) {
                    Response.Creative creative = (Response.Creative) o;
                    return creative.creative.thumbnailUrl.equals(imageUrl);
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {

            }
        }), any(ImageFetcher.Callback.class));
    }
}