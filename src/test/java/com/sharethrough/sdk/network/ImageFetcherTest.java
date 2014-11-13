package com.sharethrough.sdk.network;

import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.Function;
import com.sharethrough.sdk.Response;
import com.sharethrough.test.util.Misc;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.net.URI;
import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class ImageFetcherTest {
    private static final String apiPrefix = "http://ur.i";
    private ImageFetcher subject;
    private URI apiUri;
    @Mock private Function creativeHandler;
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
        verify(creativeHandler).apply(creativeArgumentCaptor.capture());
        Creative creative = creativeArgumentCaptor.getValue();
        assertThat(creative.getMediaUrl()).isSameAs(responseCreative.creative.mediaUrl);
        assertThat(creative.getPlacementKey()).isEqualTo("key");
        assertThat(shadowOf(creative.getThumbnailImage()).getCreatedFromBytes()).isEqualTo(imageBytes);
    }

    @Test
    public void fetchImage_whenImageCantBeDownloaded_doesNotUseAd() throws Exception {
        Robolectric.addHttpResponseRule("GET", "http:" + responseCreative.creative.thumbnailUrl, new TestHttpResponse(404, "NOT FOUND"));
        subject.fetchImage(apiUri, responseCreative, creativeHandler);
        Misc.runLast(executorService);

        verifyNoMoreInteractions(creativeHandler);
    }
}