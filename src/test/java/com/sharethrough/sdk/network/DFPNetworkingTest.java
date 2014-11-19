package com.sharethrough.sdk.network;

import com.sharethrough.sdk.TestBase;
import com.sharethrough.test.Fixtures;
import com.sharethrough.test.util.Misc;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;


public class DFPNetworkingTest extends TestBase {
    private static final String DFP_URL_FIXTURE = Fixtures.getFile("assets/str_dfp_url.json");
    @Mock
    private ExecutorService executorService;
    private final String key = "12345";
    private final String apiUrl = "https://native.sharethrough.com/placements/" + key + "/sdk.json";

    @Test
    public void fetchDFPEndpoint_callsRightEndpoint_andParsesResponse() throws Exception {
        Robolectric.addHttpResponseRule("GET", apiUrl, new TestHttpResponse(200, DFP_URL_FIXTURE));
        final String[] receivedUrl = new String[1];
        final boolean[] error = new boolean[1];

        DFPNetworking.FetchDFPEndpoint(executorService, key, new DFPNetworking.DFPFetcherCallback() {
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
}