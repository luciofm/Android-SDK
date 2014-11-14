package com.sharethrough.sdk.network;

import com.sharethrough.sdk.*;
import com.sharethrough.test.Fixtures;
import com.sharethrough.test.util.Misc;
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
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AdFetcherTest extends TestBase {
    private static final String FIXTURE = Fixtures.getFile("assets/str_ad_youtube.json");
    @Mock private ExecutorService executorService;
    @Mock private BeaconService beaconService;
    @Mock private ImageFetcher imageFetcher;
    @Mock private Function<Creative, Void> creativeHandler;
    @Mock private Creative creative;
    @Mock private AdFetcher.Callback adFetcherCallback;
    private AdFetcher subject;
    private String apiUri;
    private String apiUriPrefix;
    private String key;
    @Captor private ArgumentCaptor<ImageFetcher.Callback> imageFetcherCallback;

    @Before
    public void setUp() throws Exception {
        key = "key";
        subject = new AdFetcher(Robolectric.application, key, executorService, beaconService);
        apiUriPrefix = "http://api?key=";
        apiUri = apiUriPrefix + key;
    }

    @Test
    public void fetchAds_whenServerReturnsAds_usesImageFetcherOnEachCreativeItReturns() throws Exception {
        Robolectric.addHttpResponseRule("GET", apiUri, new TestHttpResponse(200, FIXTURE));

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);

        verifyNoMoreInteractions(imageFetcher);

        Misc.runLast(executorService);

        verify(beaconService).adRequested(Robolectric.application, key);

        verifyFetchedImage(imageFetcher, "//th.umb.na/il/URL1", apiUri, creativeHandler);
        verifyFetchedImage(imageFetcher, "//th.umb.na/il/URL2", apiUri, creativeHandler);
    }

    @Test
    public void fetchAds_whenServerRequestFails_doesNothing() throws Exception {
        Robolectric.addHttpResponseRule("GET", apiUri, new TestHttpResponse(500, "Bad server"));

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);

        Misc.runLast(executorService);

        verifyNoMoreInteractions(imageFetcher);
    }

    @Test
    public void fetchAds_whenExceptionOccursBeforeGettingHttpResponse_logsSomeStuff() throws Exception {
        Robolectric.addHttpResponseRule("GET", apiUri, null);

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);

        Misc.runLast(executorService);

        List<ShadowLog.LogItem> logsForTag = ShadowLog.getLogsForTag("Sharethrough");
        assertThat(logsForTag.get(0).msg).isEqualTo("failed to get ads for key " + key + ": " + apiUri);
    }

    @Test
    public void fetchAds_whenExceptionOccursAfterGettingHttpResponse_logsSomeStuffAndStuffAboutResponse() throws Exception {
        String responseBody = "{234789wdfjkl ";
        Robolectric.addHttpResponseRule("GET", apiUri, new TestHttpResponse(200, responseBody));

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);

        Misc.runLast(executorService);

        List<ShadowLog.LogItem> logsForTag = ShadowLog.getLogsForTag("Sharethrough");
        assertThat(logsForTag.get(0).msg).isEqualTo("failed to get ads for key " + key + ": " + apiUri + ": " + responseBody);
    }

    @Test
    public void fetchAds_whenRequestIsInProgress_doesNotStartNewRequest() throws Exception {
        Robolectric.addHttpResponseRule("GET", apiUri, new TestHttpResponse(200, FIXTURE));

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);
        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(runnableArgumentCaptor.capture());
        assertThat(runnableArgumentCaptor.getAllValues()).hasSize(1);
    }

    @Test
    public void fetchAds_whenPreviousRequestHasFinished_butImagesHaveNot_doesNotStartNewRequest() throws Exception {
        Robolectric.addHttpResponseRule("GET", apiUri, new TestHttpResponse(200, FIXTURE));

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);

        Misc.runLast(executorService);
        reset(executorService);

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);

        verifyNoMoreInteractions(executorService);
    }

    @Test
    public void fetchAds_whenPreviousRequestAndNotAllImagesHaveFinished_doesNotStartNewRequest() throws Exception {
        // make first request
        Robolectric.addHttpResponseRule("GET", apiUri, new TestHttpResponse(200, FIXTURE));
        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);
        Misc.runLast(executorService);
        reset(executorService);

        // finish loading only one image
        verify(imageFetcher, atLeastOnce()).fetchImage(any(URI.class), any(Response.Creative.class), imageFetcherCallback.capture());
        assertThat(imageFetcherCallback.getAllValues().size()).isGreaterThan(1);
        imageFetcherCallback.getAllValues().get(0).success(creative);

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);

        verifyNoMoreInteractions(executorService);
    }

    @Test
    public void fetchAds_whenPreviousRequestAndAllImagesHaveFinished_startsNewRequest() throws Exception {
        // make first request
        Robolectric.addHttpResponseRule("GET", apiUri, new TestHttpResponse(200, FIXTURE));
        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);
        Misc.runLast(executorService);
        reset(executorService);

        // finish loading all images
        verify(imageFetcher, atLeastOnce()).fetchImage(any(URI.class), any(Response.Creative.class), imageFetcherCallback.capture());
        for (ImageFetcher.Callback callback : imageFetcherCallback.getAllValues()) {
            callback.success(creative);
        }

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);

        verify(executorService).execute(any(Runnable.class));
    }

    @Test
    public void fetchAds_whenPreviousRequestAndAllImagesHaveFinished_doesCallback() throws Exception {
        // make first request
        Robolectric.addHttpResponseRule("GET", apiUri, new TestHttpResponse(200, FIXTURE));
        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);
        Misc.runLast(executorService);
        reset(executorService);

        // finish loading all images
        verify(imageFetcher, atLeastOnce()).fetchImage(any(URI.class), any(Response.Creative.class), imageFetcherCallback.capture());
        for (ImageFetcher.Callback callback : imageFetcherCallback.getAllValues()) {
            callback.success(creative);
        }

        verify(adFetcherCallback).finishedLoading();
    }

    @Test
    public void fetchAds_whenPreviousRequestAndAllImagesHaveFinishedWithFailure_startsNewRequest() throws Exception {
        // make first request
        Robolectric.addHttpResponseRule("GET", apiUri, new TestHttpResponse(200, FIXTURE));
        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);
        Misc.runLast(executorService);
        reset(executorService);

        // finish loading all images
        verify(imageFetcher, atLeastOnce()).fetchImage(any(URI.class), any(Response.Creative.class), imageFetcherCallback.capture());
        for (ImageFetcher.Callback callback : imageFetcherCallback.getAllValues()) {
            callback.failure();
        }

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);

        verify(executorService).execute(any(Runnable.class));
    }

    @Test
    public void whenServerReturns204_doesNothing() throws Exception {
        Robolectric.addHttpResponseRule("GET", apiUri, new TestHttpResponse(204, "I got nothing for ya"));

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler, adFetcherCallback);

        Misc.runLast(executorService);

        verifyNoMoreInteractions(imageFetcher);
    }

    private void verifyFetchedImage(ImageFetcher imageFetcher, final String imageUrl, String apiUri, Function<Creative, Void> creativeHandler) {
        verify(imageFetcher).fetchImage(eq(URI.create(apiUri)), Matchers.argThat(new BaseMatcher<Response.Creative>() {
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