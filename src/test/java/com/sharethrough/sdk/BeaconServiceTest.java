package com.sharethrough.sdk;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.sharethrough.test.util.Misc;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.RequestLine;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.tester.org.apache.http.HttpRequestInfo;
import org.robolectric.tester.org.apache.http.RequestMatcher;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BeaconServiceTest extends TestBase {
    private Map<String, String> expectedCommonParams;
    private Date now;
    private UUID session;
    private BeaconService subject;
    @Mock private ExecutorService executorService;
    private Creative creative;
    @Mock private AdvertisingIdProvider advertisingIdProvider;
    @Mock private Context context;
    @Mock private PackageManager packageManager;
    @Mock private PackageInfo packageInfo;
    @Mock private Placement placement;
    private String advertisingId;
    private Response.Creative responseCreative;
    private int feedPosition;

    @Before
    public void setUp() throws Exception {
        System.setProperty("http.agent", "Robolectric");

        now = new Date(1000000000);
        session = UUID.randomUUID();
        feedPosition = 5;
        expectedCommonParams = new HashMap();
        expectedCommonParams.put("umtime", "" + now.getTime());
        expectedCommonParams.put("ploc", "com.sharethrough.android.sdk");
        expectedCommonParams.put("bwidth", "480");
        expectedCommonParams.put("bheight", "800");
        expectedCommonParams.put("session", session.toString());
        advertisingId = "abc";
        expectedCommonParams.put("uid", advertisingId);
        expectedCommonParams.put("ua", "" + Sharethrough.USER_AGENT + "; " + "com.sharethrough.android.sdk");
        expectedCommonParams.put("appName", "com.sharethrough.android.sdk");
        expectedCommonParams.put("appId", Sharethrough.SDK_VERSION_NUMBER);

        responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.adserverRequestId = "fake-adserver-request-id";
        responseCreative.auctionWinId = "fake-auction-win-id";
        responseCreative.creative.variantKey = "variant key";
        responseCreative.creative.creativeKey = "creative key";
        responseCreative.creative.campaignKey= "campaign key";
        responseCreative.signature = "signature";
        responseCreative.priceType = "price type";
        responseCreative.price = 1000;

        Response.Creative.CreativeInner.Beacon beacon = new Response.Creative.CreativeInner.Beacon();
        beacon.click = new ArrayList<>();
        beacon.play = new ArrayList<>();
        beacon.visible = new ArrayList<>();
        beacon.impression = new ArrayList<>();

        responseCreative.creative.beacon = beacon;
        creative = new Creative(responseCreative, new byte[0], new byte[0], "placement key");

        packageInfo.versionName = "fake_app_id";
        when(placement.getStatus()).thenReturn("live");
        when(advertisingIdProvider.getAdvertisingId()).thenReturn(advertisingId);
        when(context.getPackageName()).thenReturn("com.example.sdk");
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getPackageInfo("com.example.sdk", PackageManager.GET_META_DATA)).thenReturn(packageInfo);

        subject = new BeaconService(new DateProvider(), session, executorService, advertisingIdProvider, Robolectric.application);
    }

    @Test
    public void commonParams_returnsParamsSentInAllBeacons() throws Exception {
        assertThat(subject.commonParams(Robolectric.application)).isEqualTo(expectedCommonParams);
    }

    @Test
    public void whenAdvertisingIdNotSet_commonParams_returnsParamsSentInAllBeacons() throws Exception {
        when(advertisingIdProvider.getAdvertisingId()).thenReturn(null);
        HashMap<String, String> expectedCommonParamsWithoutAdvertisingId = new HashMap<>(expectedCommonParams);
        expectedCommonParamsWithoutAdvertisingId.remove("uid");
        assertThat(subject.commonParams(Robolectric.application)).isEqualTo(expectedCommonParamsWithoutAdvertisingId);
    }

    @Test
    public void commonParamsWithAd_returnsParamsSentInAllBeaconsForAnAd() throws Exception {
        expectedCommonParams.put("pkey", "placement key");
        expectedCommonParams.put("vkey", "variant key");
        expectedCommonParams.put("ckey", "creative key");
        expectedCommonParams.put("campkey", "campaign key");
        expectedCommonParams.put("as", "signature");
        expectedCommonParams.put("at", "price type");
        expectedCommonParams.put("ap", "1000");
        expectedCommonParams.put("arid", "fake-adserver-request-id");
        expectedCommonParams.put("awid", "fake-auction-win-id");


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
                subject.adClicked("fake user event", creative, RendererTest.makeAdView().getAdView(), feedPosition, placement);
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
                subject.adRequested(key);
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
                subject.adReceived(Robolectric.application, creative, feedPosition, placement);
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
                subject.adVisible(RendererTest.makeAdView(), creative, feedPosition, placement);
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
                subject.adShared(Robolectric.application, creative, "shareType", feedPosition);
            }
        });
    }

    @Test
    public void whenAdReceivedCalled_fireImpressionThirdPartyBeacons() throws Exception {
        String[] initialUrls = {"//impressionEndOne?cacheBuster=[timestamp]", "//impressionEndTwo?cacheBuster=[timestamp]"};

        ArrayList<String> impressionEndoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));

        responseCreative.creative.beacon.impression = impressionEndoints;

        Creative testCreative = new Creative(responseCreative, new byte[0], new byte[0], "placement key");

        subject.adReceived(Robolectric.application, testCreative, feedPosition, placement);

        Robolectric.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = Robolectric.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(info.size()).isEqualTo(3);
        for (int i = 0; i < info.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl;
            assertThat(info.get(i).getHttpRequest().getRequestLine().getUri()).isEqualTo(expectedUrl);
        }
    }

    @Test
    public void whenAdVisibleCalled_fireVisibleThirdPartyBeacons() throws Exception {
        String[] initialUrls = {"//visibleEndOne?cacheBuster=[timestamp]", "//visibleEndTwo?cacheBuster=[timestamp]"};

        ArrayList<String> visibleEndoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));

        responseCreative.creative.beacon.visible = visibleEndoints;

        Creative testCreative = new Creative(responseCreative, new byte[0], new byte[0], "placement key");

        subject.adVisible(RendererTest.makeAdView(), testCreative, feedPosition, placement);

        Robolectric.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = Robolectric.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(info.size()).isEqualTo(3);
        for (int i = 0; i < info.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl;
            assertThat(info.get(i).getHttpRequest().getRequestLine().getUri()).isEqualTo(expectedUrl);
        }
    }

    @Test
    public void whenAdClickCalled_fireClickAndVideoThirdPartyBeacons() throws Exception {
        String[] initialUrls = {"//click/EndOne", "//click/End[Two]?cacheBuster=[timestamp]", "//video/EndOne", "//video/EndTwo"};

        ArrayList<String> clickEndoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));
        ArrayList<String> playEndoints = new ArrayList<>(Arrays.asList(initialUrls[2], initialUrls[3]));

        responseCreative.creative.beacon.click = clickEndoints;
        responseCreative.creative.beacon.play = playEndoints;

        Creative testCreative = new Creative(responseCreative, new byte[0], new byte[0], "placement key");

        subject.adClicked("test-creative", testCreative, RendererTest.makeAdView().getAdView(), feedPosition, placement);

        Robolectric.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = Robolectric.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(info.size()).isEqualTo(5);
        for (int i = 0; i < info.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            assertThat(info.get(i).getHttpRequest().getRequestLine().getUri()).isEqualTo(expectedUrl);
        }
    }

    @Test
    public void whenPlacementStatusIsPreLiveAndAdClickCalled_doesNotFireClickAndVideoThirdPartyBeacons() throws Exception {
        String[] initialUrls = {"//third-party/one", "//third-party/two", "//third-party/three", "//third-party/four"};

        ArrayList<String> clickEndoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));
        ArrayList<String> playEndoints = new ArrayList<>(Arrays.asList(initialUrls[2], initialUrls[3]));

        when(placement.getStatus()).thenReturn("pre-live");

        responseCreative.creative.beacon.click = clickEndoints;
        responseCreative.creative.beacon.play = playEndoints;

        Creative testCreative = new Creative(responseCreative, new byte[0], new byte[0], "placement key");

        subject.adClicked("test-creative", testCreative, RendererTest.makeAdView().getAdView(), feedPosition, placement);

        Robolectric.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = Robolectric.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(info.size()).isEqualTo(1);
        for (int i = 0; i < info.size() - 1; i++) {
            assertThat(info.get(i).getHttpRequest().getRequestLine().getUri()).doesNotContain("third-party");
        }
    }

    @Test
    public void whenAThirdPartyBeaconIsInvalid_logsWithoutCrashing() throws Exception {
        String badUrl = "//%%%invalid%url%%%";
        List<String> clickEndoints = Arrays.asList(badUrl);

        responseCreative.creative.beacon.click = clickEndoints;

        Creative testCreative = new Creative(responseCreative, new byte[0], new byte[0], "placement key");

        subject.adClicked("test-creative", testCreative, RendererTest.makeAdView().getAdView(), feedPosition, placement);

        Robolectric.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = Robolectric.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(info.size()).isEqualTo(1);
        assertThat(ShadowLog.getLogs().get(0).msg).contains(badUrl);
    }

    @Test
    public void videoPlayed() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(Robolectric.application, creative);
        expectedBeaconParams.put("type", "completionPercent");
        expectedBeaconParams.put("value", "123");
        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.videoPlayed(Robolectric.application, creative, 123, feedPosition);
            }
        });
    }

    private void assertBeaconFired(final Map<String, String> expectedBeaconParams, Runnable fireBeacon) {
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

        Misc.runLast(executorService);

        assertThat(wasCalled[0]).isTrue();
    }

    private class DateProvider implements Provider<Date> {
        @Override
        public Date get() {
            return now;
        }
    }
}