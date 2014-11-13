package com.sharethrough.sdk.network;

import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.Function;
import com.sharethrough.sdk.Response;
import com.sharethrough.test.Fixtures;
import com.sharethrough.test.util.Misc;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.net.URI;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class AdFetcherTest {
    private static final String FIXTURE = Fixtures.getFile("assets/str_ad_youtube.json");
    @Mock private ExecutorService executorService;
    @Mock private BeaconService beaconService;
    @Mock private ImageFetcher imageFetcher;
    @Mock private Function<Creative, Void> creativeHandler;
    private AdFetcher subject;
    private String apiUri;
    private String apiUriPrefix;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        subject = new AdFetcher(Robolectric.application, "key", executorService, beaconService);
        apiUriPrefix = "http://api?key=";
        apiUri = apiUriPrefix + "key";
    }

    @Test
    public void fetchAds_whenServerReturnsAds_usesImageFetcherOnEachCreativeItReturns() throws Exception {
        Robolectric.addHttpResponseRule("GET", apiUri, new TestHttpResponse(200, FIXTURE));

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler);

        verifyNoMoreInteractions(imageFetcher);

        Misc.runLast(executorService);

        verify(beaconService).adRequested(Robolectric.application, "key");

        verifyFetchedImage(imageFetcher, "//th.umb.na/il/URL1", apiUri, creativeHandler);
        verifyFetchedImage(imageFetcher, "//th.umb.na/il/URL2", apiUri, creativeHandler);
    }

    @Test
    public void whenServerReturns204_doesNothing() throws Exception {
        Robolectric.addHttpResponseRule("GET", apiUri, new TestHttpResponse(204, "I got nothing for ya"));

        subject.fetchAds(imageFetcher, apiUriPrefix, creativeHandler);

        Misc.runLast(executorService);

        verifyNoMoreInteractions(imageFetcher);
    }

    private void verifyFetchedImage(ImageFetcher imageFetcher, final String imageUrl, String apiUri, Function creativeHandler) {
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
        }), same(creativeHandler));
    }
}