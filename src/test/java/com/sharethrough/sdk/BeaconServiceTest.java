package com.sharethrough.sdk;

import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.RequestLine;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.tester.org.apache.http.RequestMatcher;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class BeaconServiceTest {
    private Map<String, String> expected;
    private Date now;
    private StrSession session;
    private BeaconService subject;
    private ExecutorService executorService;
    private Creative creative;

    @Before
    public void setUp() throws Exception {
        System.setProperty("http.agent", "Robolectric");

        now = new Date(1000000000);
        session = new StrSession();
        expected = new HashMap();
        expected.put("umtime", "" + now.getTime());
        expected.put("ploc", "com.sharethrough.android.sdk");
        expected.put("bwidth", "480");
        expected.put("bheight", "800");
        expected.put("session", session.toString());
        expected.put("uid", "TODO");
        expected.put("ua", Sharethrough.USER_AGENT);

        Response.Creative responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.creative.variantKey = "variant key";
        responseCreative.creative.key = "creative key";
        responseCreative.signature = "signature";
        responseCreative.priceType = "price type";
        responseCreative.price = 1000;
        creative = new Creative(responseCreative, new byte[0], "placement key");

        executorService = mock(ExecutorService.class);
        subject = new BeaconService(new DateProvider(), session, executorService);
    }

    @Test
    public void commonParams_returnsParamsSentInAllBeacons() throws Exception {
        assertThat(subject.commonParams(Robolectric.application)).isEqualTo(expected);
    }

    @Test
    public void commonParamsWithAd_returnsParamsSentInAllBeaconsForAnAd() throws Exception {
        expected.put("pkey", "placement key");
        expected.put("vkey", "variant key");
        expected.put("ckey", "creative key");
        expected.put("as", "signature");
        expected.put("at", "price type");
        expected.put("ap", "1000");

        assertThat(subject.commonParamsWithCreative(Robolectric.application, creative)).isEqualTo(expected);
    }

    @Test
    public void fireAdClicked() throws Exception {
        RequestMatcher requestMatcher = new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest httpRequest) {
                RequestLine requestLine = httpRequest.getRequestLine();
                URI uri = URI.create(requestLine.getUri());
                List<NameValuePair> parse = URLEncodedUtils.parse(uri, null);
                Map<String, String> params = new HashMap<String, String>(parse.size());
                for (NameValuePair nameValuePair : parse) {
                    params.put(nameValuePair.getName(), nameValuePair.getValue());
                }

                expected = subject.commonParamsWithCreative(Robolectric.application, creative);
                expected.put("type", "userEvent");
                expected.put("userEvent", "fake user event");
                expected.put("engagement", "true");

                expected.put("pheight", "0");
                expected.put("pwidth", "0");

                assertThat(requestLine.getMethod()).isEqualTo("GET");
                assertThat(httpRequest.containsHeader("User-Agent")).isTrue();
                assertThat(uri.getPath()).isEqualTo("/butler");
                assertThat(uri.getHost()).isEqualTo("b.sharethrough.com");
                assertThat(params).isEqualTo(expected);
                return true;
            }
        };
        Robolectric.addHttpResponseRule(requestMatcher, new TestHttpResponse(200, ""));

        subject.adClicked(Robolectric.application, "fake user event", creative, RendererTest.makeAdView());

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(runnableArgumentCaptor.capture());
        runnableArgumentCaptor.getValue().run();
    }

    private class DateProvider implements Provider<Date> {
        @Override
        public Date get() {
            return now;
        }
    }
}