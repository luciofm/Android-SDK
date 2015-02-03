package com.sharethrough.sdk.network;

import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.sharethrough.sdk.TestBase;
import com.sharethrough.test.Fixtures;
import com.sharethrough.test.util.Misc;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowView;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;


public class DFPNetworkingTest extends TestBase {
    private static final String DFP_URL_FIXTURE = Fixtures.getFile("assets/str_dfp_url.json");
    private static final boolean[] addKeyword = new boolean[1];
    @Mock
    private ExecutorService executorService;
    private final String key = "12345";
    private final String apiUrl = "https://platform-cdn.sharethrough.com/placements/" + key + "/sdk.json";
    private DFPNetworking subject;
    private static final boolean[] loadedAd = new boolean[1];

    @Before
    public void SetUp() {
        subject = new DFPNetworking();

    }

    @Test
    public void fetchDFPPath_callsNativeSharethrough_andPassesValueBackToCallback() throws Exception {

        Robolectric.addHttpResponseRule("GET", apiUrl, new TestHttpResponse(200, DFP_URL_FIXTURE));
        final String[] receivedUrl = new String[1];
        final boolean[] error = new boolean[1];

        subject.fetchDFPPath(executorService, key, new DFPNetworking.DFPPathFetcherCallback() {
            @Override
            public void receivedURL(String url) {
                receivedUrl[0] = url;
            }

            @Override
            public void DFPError(String errorMessage) {
                error[0] = true;
            }
        });

        Misc.runLast(executorService);

        assertThat(error[0]).isFalse();
        assertThat(receivedUrl[0]).isEqualTo("/fake/dfp/url");
    }

    @Config(shadows = {
            MyPublisherAdViewShadow.class,
            MyPublisherAdRequestBuilder.class
    }
    )
    @Test
    public void fetchCreativeKey_callsCallback() {
        final boolean receivedCreativeKey[] = new boolean[1];

        DFPNetworking.DFPCreativeKeyCallback creativeKeyCallback = new DFPNetworking.DFPCreativeKeyCallback() {
            @Override
            public void receivedCreativeKey() {
                receivedCreativeKey[0] = true;
            }

            @Override
            public void DFPKeyError(String errorMessage) {

            }
        };

        subject.fetchCreativeKey(Robolectric.application, "dfp/path", creativeKeyCallback);

        assertThat(addKeyword[0]).isTrue();
        assertThat(loadedAd[0]).isTrue();
        assertThat(receivedCreativeKey[0]).isTrue();
    }

    @Test
    public void fetchCreativeKey_runsOnMainLooper() {
        DFPNetworking.DFPCreativeKeyCallback creativeKeyCallback = mock(DFPNetworking.DFPCreativeKeyCallback.class);
        Robolectric.pauseMainLooper();
        subject.fetchCreativeKey(Robolectric.application, "dfp/path", creativeKeyCallback);

        verifyNoMoreInteractions(creativeKeyCallback);
        Robolectric.unPauseMainLooper();
    }

    @Implements(PublisherAdView.class)
    public static class MyPublisherAdViewShadow extends ShadowView {
        @RealObject
        private PublisherAdView realObject;

        @Implementation
        public void loadAd(PublisherAdRequest publisherAdRequest) {
            loadedAd[0] = true;
            realObject.getAdListener().onAdLoaded();
        }
    }

    @Implements(PublisherAdRequest.Builder.class)
    public static class MyPublisherAdRequestBuilder {
        @RealObject
        private PublisherAdRequest.Builder realObject;

        @Implementation
        public PublisherAdRequest.Builder addKeyword(String keyword) {
            addKeyword[0] = true;
            return realObject;
        }
    }
}