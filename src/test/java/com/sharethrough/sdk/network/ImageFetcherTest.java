package com.sharethrough.sdk.network;

import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.Response;
import com.sharethrough.sdk.TestBase;
import com.sharethrough.test.util.Misc;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.net.URI;
import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf;

public class ImageFetcherTest extends TestBase {
    private static final String apiPrefix = "http://ur.i";
    private ImageFetcher subject;
    private URI apiUri;
    @Mock private ImageFetcher.Callback creativeHandler;
    @Mock private ExecutorService executorService;
    private Response.Creative responseCreative;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        apiUri = URI.create(apiPrefix);

        responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.creative.thumbnailUrl = "//th.um/bn.ail";
        responseCreative.creative.mediaUrl = "unique";

        subject = new ImageFetcher(executorService, "key");
    }

    @Test
    public void fetchImage_whenImageCanBeDownloaded() throws Exception {
        byte[] imageBytes = {1, 2, 3, 4};
        Robolectric.addHttpResponseRule("GET", "http:" + responseCreative.creative.thumbnailUrl, new TestHttpResponse(200, imageBytes));

        subject.fetchImage(apiUri, responseCreative, creativeHandler);

        Misc.runLast(executorService);

        ArgumentCaptor<Creative> creativeArgumentCaptor = ArgumentCaptor.forClass(Creative.class);
        verify(creativeHandler).success(creativeArgumentCaptor.capture());
        Creative creative = creativeArgumentCaptor.getValue();
        assertThat(creative.getMediaUrl()).isSameAs(responseCreative.creative.mediaUrl);
        assertThat(creative.getPlacementKey()).isEqualTo("key");
        assertThat(shadowOf(creative.makeThumbnailImage()).getCreatedFromBytes()).isEqualTo(imageBytes);
    }

    @Test
    public void fetchImage_whenImageCantBeDownloadedBcServerRefuses_reportsFailure() throws Exception {
        Robolectric.addHttpResponseRule("GET", "http:" + responseCreative.creative.thumbnailUrl, new TestHttpResponse(404, "NOT FOUND"));
        subject.fetchImage(apiUri, responseCreative, creativeHandler);
        Misc.runLast(executorService);

        verify(creativeHandler).failure();
    }

    @Test
    public void fetchImage_whenImageCantBeDownloadedBcOfIOerror_reportsFailure() throws Exception {
        Robolectric.addHttpResponseRule("GET", "http:" + responseCreative.creative.thumbnailUrl, new IOExceptionHttpResponse());
        subject.fetchImage(apiUri, responseCreative, creativeHandler);
        Misc.runLast(executorService);

        verify(creativeHandler).failure();
    }
}