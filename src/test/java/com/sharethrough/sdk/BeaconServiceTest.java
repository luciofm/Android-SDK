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
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class BeaconServiceTest {
    private Map<String, String> expectedCommonParams;
    private Date now;
    private StrSession session;
    private BeaconService subject;
    private ExecutorService executorService;
    private Creative creative;
    private AdvertisingIdProvider advertisingIdProvider;
    private String advertisingId;

    @Before
    public void setUp() throws Exception {
        System.setProperty("http.agent", "Robolectric");

        now = new Date(1000000000);
        session = new StrSession();
        expectedCommonParams = new HashMap();
        expectedCommonParams.put("umtime", "" + now.getTime());
        expectedCommonParams.put("ploc", "com.sharethrough.android.sdk");
        expectedCommonParams.put("bwidth", "480");
        expectedCommonParams.put("bheight", "800");
        expectedCommonParams.put("session", session.toString());
        advertisingId = "abc";
        expectedCommonParams.put("uid", advertisingId);
        expectedCommonParams.put("ua", "" + Sharethrough.USER_AGENT);

        Response.Creative responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.creative.variantKey = "variant key";
        responseCreative.creative.key = "creative key";
        responseCreative.signature = "signature";
        responseCreative.priceType = "price type";
        responseCreative.price = 1000;
        creative = new Creative(responseCreative, new byte[0], "placement key", mock(BeaconService.class));

        executorService = mock(ExecutorService.class);
        advertisingIdProvider = mock(AdvertisingIdProvider.class);
        when(advertisingIdProvider.getAdvertisingId()).thenReturn(advertisingId);
        subject = new BeaconService(new DateProvider(), session, executorService, advertisingIdProvider);
    }

    @Test
    public void commonParams_returnsParamsSentInAllBeacons() throws Exception {
        assertThat(subject.commonParams(Robolectric.application)).isEqualTo(expectedCommonParams);
    }

    @Test
    public void commonParamsWithAd_returnsParamsSentInAllBeaconsForAnAd() throws Exception {
        expectedCommonParams.put("pkey", "placement key");
        expectedCommonParams.put("vkey", "variant key");
        expectedCommonParams.put("ckey", "creative key");
        expectedCommonParams.put("as", "signature");
        expectedCommonParams.put("at", "price type");
        expectedCommonParams.put("ap", "1000");

        assertThat(subject.commonParamsWithCreative(Robolectric.application, creative)).isEqualTo(expectedCommonParams);
    }

    @Test
    public void fireAdClicked() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(Robolectric.application, creative);
        expectedBeaconParams.put("type", "userEvent");
        expectedBeaconParams.put("userEvent", "fake user event");
        expectedBeaconParams.put("engagement", "true");

        expectedBeaconParams.put("pheight", "0");
        expectedBeaconParams.put("pwidth", "0");

        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.adClicked(Robolectric.application, "fake user event", creative, RendererTest.makeAdView());
            }
        });
    }

    @Test
    public void fireAdRequested() throws Exception {
        final String key = "abc";
        Map<String, String> expectedBeaconParams = subject.commonParams(Robolectric.application);
        expectedBeaconParams.put("type", "impressionRequest");
        expectedBeaconParams.put("pkey", key);

        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.adRequested(Robolectric.application, key);
            }
        });
    }

    @Test
    public void fireAdReceived() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(Robolectric.application, creative);
        expectedBeaconParams.put("type", "impression");
        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.adReceived(Robolectric.application, creative);
            }
        });
    }

    @Test
    public void fireAdVisible() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(Robolectric.application, creative);
        expectedBeaconParams.put("type", "visible");
        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.adVisible(RendererTest.makeAdView(), creative);
            }
        });
    }

    @Test
    public void whenFireAdShareCalled_fireRightBeacon() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(Robolectric.application, creative);
        expectedBeaconParams.put("type", "userEvent");
        expectedBeaconParams.put("userEvent", "share");
        expectedBeaconParams.put("share", "shareType");
        expectedBeaconParams.put("engagement", "true");
        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.adShared(Robolectric.application, creative, "shareType");
            }
        });
    }

    private void assertBeaconFired(final Map<String,String> expectedBeaconParams, Runnable fireBeacon) {
        final boolean[] wasCalled = {false};
        RequestMatcher requestMatcher = new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest httpRequest) {
                wasCalled[0] = true;
                RequestLine requestLine = httpRequest.getRequestLine();
                URI uri = URI.create(requestLine.getUri());
                List<NameValuePair> parse = URLEncodedUtils.parse(uri, null);
                Map<String, String> params = new HashMap<String, String>(parse.size());
                for (NameValuePair nameValuePair : parse) {
                    params.put(nameValuePair.getName(), nameValuePair.getValue());
                }

                assertThat(requestLine.getMethod()).isEqualTo("GET");
                assertThat(httpRequest.containsHeader("User-Agent")).isTrue();
                assertThat(uri.getPath()).isEqualTo("/butler");
                assertThat(uri.getHost()).isEqualTo("b.sharethrough.com");
                assertThat(params.entrySet()).containsAll(expectedBeaconParams.entrySet());
                return true;
            }
        };
        Robolectric.addHttpResponseRule(requestMatcher, new TestHttpResponse(200, ""));

        fireBeacon.run();

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(runnableArgumentCaptor.capture());
        runnableArgumentCaptor.getValue().run();

        assertThat(wasCalled[0]).isTrue();
    }

    private class DateProvider implements Provider<Date> {
        @Override
        public Date get() {
            return now;
        }
    }
}