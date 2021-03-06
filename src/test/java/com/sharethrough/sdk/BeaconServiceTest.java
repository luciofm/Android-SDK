package com.sharethrough.sdk;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import com.sharethrough.android.sdk.BuildConfig;
import com.sharethrough.test.util.Misc;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.RequestLine;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.httpclient.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
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
    private String mediationRequestId;

    @Before
    public void setUp() throws Exception {
        Logger.enabled = true;
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
        expectedCommonParams.put("ua", Build.MODEL + "; Android " + Build.VERSION.RELEASE + "; " + "com.sharethrough.android.sdk" + "; STR " + BuildConfig.VERSION_NAME);

        expectedCommonParams.put("appName", "com.sharethrough.android.sdk");
        expectedCommonParams.put("appId", Sharethrough.SDK_VERSION_NUMBER);

        responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.adserverRequestId = "fake-adserver-request-id";
        responseCreative.auctionWinId = "fake-auction-win-id";
        responseCreative.creative.variantKey = "variant key";
        responseCreative.creative.creativeKey = "creative key";
        responseCreative.creative.campaignKey= "campaign key";
        responseCreative.creative.dealId= "";
        responseCreative.signature = "signature";
        responseCreative.priceType = "price type";
        responseCreative.price = 1000;

        Response.Creative.CreativeInner.Beacon beacon = new Response.Creative.CreativeInner.Beacon();
        beacon.click = new ArrayList<>();
        beacon.play = new ArrayList<>();
        beacon.visible = new ArrayList<>();
        beacon.impression = new ArrayList<>();
        beacon.silentPlay = new ArrayList<>();

        responseCreative.creative.beacon = beacon;

        mediationRequestId = "fake-mrid"; // To remove for asap v2
        creative = new Creative(responseCreative, mediationRequestId);

        packageInfo.versionName = "fake_app_id";
        when(placement.getStatus()).thenReturn("live");
        when(advertisingIdProvider.getAdvertisingId()).thenReturn(advertisingId);
        when(context.getPackageName()).thenReturn("com.example.sdk");
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getPackageInfo("com.example.sdk", PackageManager.GET_META_DATA)).thenReturn(packageInfo);

        subject = new BeaconService(new DateProvider(), session, advertisingIdProvider, new ContextInfo(RuntimeEnvironment.application),"placement key");

        STRExecutorService.setExecutorService(executorService);
    }

    @Test
    public void commonParams_returnsParamsSentInAllBeacons() throws Exception {
        assertThat(subject.commonParams()).isEqualTo(expectedCommonParams);
    }

    @Test
    public void whenAdvertisingIdNotSet_commonParams_returnsParamsSentInAllBeacons() throws Exception {
        when(advertisingIdProvider.getAdvertisingId()).thenReturn(null);
        HashMap<String, String> expectedCommonParamsWithoutAdvertisingId = new HashMap<>(expectedCommonParams);
        expectedCommonParamsWithoutAdvertisingId.remove("uid");
        assertThat(subject.commonParams()).isEqualTo(expectedCommonParamsWithoutAdvertisingId);
    }

    @Test
    public void commonParamsWithAd_Has_No_DealID_returnsParamsSentInAllBeaconsForAnAd() throws Exception {
        expectedCommonParams.put("pkey", "placement key");
        expectedCommonParams.put("vkey", "variant key");
        expectedCommonParams.put("ckey", "creative key");
        expectedCommonParams.put("campkey", "campaign key");
        expectedCommonParams.put("arid", "fake-adserver-request-id");
        expectedCommonParams.put("awid", "fake-auction-win-id");
        expectedCommonParams.put("mrid", "fake-mrid");


        assertThat(subject.commonParamsWithCreative(creative)).isEqualTo(expectedCommonParams);
    }

    @Test
    public void commonParamsWithAd_Has_DealID_returnsParamsSentInAllBeaconsForAnAd() throws Exception {
        Response.Creative responseCreativeWithDealId = new Response.Creative();
        responseCreativeWithDealId.creative = new Response.Creative.CreativeInner();
        responseCreativeWithDealId.adserverRequestId = "fake-adserver-request-id";
        responseCreativeWithDealId.auctionWinId = "fake-auction-win-id";
        responseCreativeWithDealId.creative.variantKey = "variant key";
        responseCreativeWithDealId.creative.creativeKey = "creative key";
        responseCreativeWithDealId.creative.campaignKey= "campaign key";
        responseCreativeWithDealId.creative.dealId = "fake_deal_id";

        creative = new Creative(responseCreativeWithDealId, mediationRequestId);

        expectedCommonParams.put("pkey", "placement key");
        expectedCommonParams.put("vkey", "variant key");
        expectedCommonParams.put("ckey", "creative key");
        expectedCommonParams.put("campkey", "campaign key");
        expectedCommonParams.put("arid", "fake-adserver-request-id");
        expectedCommonParams.put("awid", "fake-auction-win-id");
        expectedCommonParams.put("deal_id", "fake_deal_id");
        expectedCommonParams.put("mrid", "fake-mrid");
        assertThat(subject.commonParamsWithCreative(creative)).isEqualTo(expectedCommonParams);
    }

    @Test
    public void fireAdClicked() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("type", "userEvent");
        expectedBeaconParams.put("userEvent", "fake user event");
        expectedBeaconParams.put("engagement", "true");

        expectedBeaconParams.put("pheight", "0");
        expectedBeaconParams.put("pwidth", "0");

        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.adClicked("fake user event", creative, RendererTest.makeAdView().getAdView(), feedPosition);
            }
        });
    }

    @Test
    public void fireAdRequested() throws Exception {
        final String key = "abc";
        Map<String, String> expectedBeaconParams = subject.commonParams();
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
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("type", "impression");
        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.adReceived(RuntimeEnvironment.application, creative, feedPosition);
            }
        });
    }

    @Test
    public void fireAdVisible() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("type", "visible");
        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.adVisible(RendererTest.makeAdView(), creative, feedPosition);
            }
        });
    }

    @Test
    public void whenFireAdShareCalled_fireRightBeacon() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("type", "userEvent");
        expectedBeaconParams.put("userEvent", "share");
        expectedBeaconParams.put("share", "shareType");
        expectedBeaconParams.put("engagement", "true");
        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.adShared(RuntimeEnvironment.application, creative, "shareType", feedPosition);
            }
        });
    }

    @Test
    public void whenAdReceivedCalled_fireImpressionThirdPartyBeacons() throws Exception {
        String[] initialUrls = {"//impressionEndOne?cacheBuster=[timestamp]", "//impressionEndTwo?cacheBuster=[timestamp]"};

        ArrayList<String> impressionEndoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));

        responseCreative.creative.beacon.impression = impressionEndoints;

        Creative testCreative = new Creative(responseCreative, mediationRequestId);

        subject.adReceived(RuntimeEnvironment.application, testCreative, feedPosition);


        FakeHttp.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = FakeHttp.getFakeHttpLayer().getSentHttpRequestInfos();
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

        Creative testCreative = new Creative(responseCreative, mediationRequestId);

        subject.adVisible(RendererTest.makeAdView(), testCreative, feedPosition);

        FakeHttp.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = FakeHttp.getFakeHttpLayer().getSentHttpRequestInfos();
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

        Creative testCreative = new Creative(responseCreative, mediationRequestId);

        subject.adClicked("test-creative", testCreative, RendererTest.makeAdView().getAdView(), feedPosition);

        FakeHttp.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = FakeHttp.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(info.size()).isEqualTo(5);
        for (int i = 0; i < info.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            assertThat(info.get(i).getHttpRequest().getRequestLine().getUri()).isEqualTo(expectedUrl);
        }
    }

    @Test
    public void whenSilentAutoplayDuration3SecondsCalled_fire3SecondBeacons() throws Exception {
        int seconds = 3000;
        String[] initialUrls = {"//silentPlay/EndOne", "//silentPlay/End[Two]?cacheBuster=[timestamp]"};

        ArrayList<String> silentPlayEndpoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));

        responseCreative.creative.beacon.silentPlay = silentPlayEndpoints;

        Creative testCreative = new Creative(responseCreative, mediationRequestId);

        subject.silentAutoPlayDuration(testCreative, seconds, feedPosition);

        FakeHttp.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = FakeHttp.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(info.size()).isEqualTo(3);
        for (int i = 0; i < info.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            assertThat(info.get(i).getHttpRequest().getRequestLine().getUri()).isEqualTo(expectedUrl);
        }

        //first party beacon
        String returnedUrl = info.get(2).getHttpRequest().getRequestLine().getUri();
        assertThat(returnedUrl).contains("type=silentAutoPlayDuration");
        assertThat(returnedUrl).contains("duration=" + seconds);
        assertThat(returnedUrl).contains("placementIndex=" + feedPosition);
    }

    @Test
    public void whenSilentAutoplayDuration10SecondsCalled_fireSilentPlayThirdPartyBeacons() throws Exception {
        int seconds = 10000;
        String[] initialUrls = {"//silentPlay/EndOne", "//silentPlay/End[Two]?cacheBuster=[timestamp]"};

        ArrayList<String> silentPlayEndpoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));

        responseCreative.creative.beacon.tenSecondSilentPlay = silentPlayEndpoints;

        Creative testCreative = new Creative(responseCreative, mediationRequestId);

        subject.silentAutoPlayDuration(testCreative, seconds, feedPosition);

        FakeHttp.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = FakeHttp.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(info.size()).isEqualTo(3);
        for (int i = 0; i < info.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            assertThat(info.get(i).getHttpRequest().getRequestLine().getUri()).isEqualTo(expectedUrl);
        }

        //first party beacon
        String returnedUrl = info.get(2).getHttpRequest().getRequestLine().getUri();
        assertThat(returnedUrl).contains("type=silentAutoPlayDuration");
        assertThat(returnedUrl).contains("duration=" + seconds);
        assertThat(returnedUrl).contains("placementIndex="+ feedPosition);
    }

    @Test
    public void whenSilentAutoplayDuration15SecondsCalled_fireSilentPlayThirdPartyBeacons() throws Exception {
        int seconds = 15000;
        String[] initialUrls = {"//silentPlay/EndOne", "//silentPlay/End[Two]?cacheBuster=[timestamp]"};

        ArrayList<String> silentPlayEndpoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));

        responseCreative.creative.beacon.fifteenSecondSilentPlay = silentPlayEndpoints;

        Creative testCreative = new Creative(responseCreative, mediationRequestId);

        subject.silentAutoPlayDuration(testCreative, seconds, feedPosition);

        FakeHttp.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = FakeHttp.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(info.size()).isEqualTo(3);
        for (int i = 0; i < info.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            assertThat(info.get(i).getHttpRequest().getRequestLine().getUri()).isEqualTo(expectedUrl);
        }

        //first party beacon
        String returnedUrl = info.get(2).getHttpRequest().getRequestLine().getUri();
        assertThat(returnedUrl).contains("type=silentAutoPlayDuration");
        assertThat(returnedUrl).contains("duration=" + seconds);
        assertThat(returnedUrl).contains("placementIndex="+ feedPosition);
    }

    @Test
    public void whenSilentAutoplayDuration30SecondsCalled_fireSilentPlayThirdPartyBeacons() throws Exception {
        int seconds = 30000;
        String[] initialUrls = {"//silentPlay/EndOne", "//silentPlay/End[Two]?cacheBuster=[timestamp]"};

        ArrayList<String> silentPlayEndpoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));

        responseCreative.creative.beacon.thirtySecondSilentPlay = silentPlayEndpoints;

        Creative testCreative = new Creative(responseCreative, mediationRequestId);

        subject.silentAutoPlayDuration(testCreative, seconds, feedPosition);

        FakeHttp.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = FakeHttp.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(info.size()).isEqualTo(3);
        for (int i = 0; i < info.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            assertThat(info.get(i).getHttpRequest().getRequestLine().getUri()).isEqualTo(expectedUrl);
        }

        //first party beacon
        String returnedUrl = info.get(2).getHttpRequest().getRequestLine().getUri();
        assertThat(returnedUrl).contains("type=silentAutoPlayDuration");
        assertThat(returnedUrl).contains("duration=" + seconds);
        assertThat(returnedUrl).contains("placementIndex="+ feedPosition);
    }

    @Test
    public void whenSilentAutoplayDurationPlayedFor95Percent_fireCompletedThirdPartyBeacons() throws Exception {
        String[] initialUrls = {"//silentPlay/EndOne", "//silentPlay/End[Two]?cacheBuster=[timestamp]"};

        ArrayList<String> silentPlayEndpoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));

        responseCreative.creative.beacon.completedSilentPlay = silentPlayEndpoints;

        Creative testCreative = new Creative(responseCreative, mediationRequestId);

        subject.videoPlayed(RuntimeEnvironment.application, testCreative, 95, true, feedPosition);

        FakeHttp.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = FakeHttp.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(info.size()).isEqualTo(3);
        for (int i = 0; i < 2; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            assertThat(info.get(i).getHttpRequest().getRequestLine().getUri()).isEqualTo(expectedUrl);
        }
    }

    @Test
    public void whenThirdPartyBeaconsIsEmpty_DoesNotFireThirdPartyBeacons() throws Exception{
        responseCreative.creative.beacon.click = new ArrayList<>();
        responseCreative.creative.beacon.play = new ArrayList<>();
        responseCreative.creative.beacon.silentPlay = new ArrayList<>();
        responseCreative.creative.beacon.impression = new ArrayList<>();
        responseCreative.creative.beacon.visible = new ArrayList<>();

        Creative testCreative = new Creative(responseCreative, mediationRequestId);

        subject.adClicked("test-creative", testCreative, RendererTest.makeAdView().getAdView(), feedPosition);

        FakeHttp.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = FakeHttp.getFakeHttpLayer().getSentHttpRequestInfos();
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

        Creative testCreative = new Creative(responseCreative, mediationRequestId);

        subject.adClicked("test-creative", testCreative, RendererTest.makeAdView().getAdView(), feedPosition);

        FakeHttp.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, ""));

        Misc.runAll(executorService);

        List<HttpRequestInfo> info = FakeHttp.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(info.size()).isEqualTo(1);
        assertThat(ShadowLog.getLogs().get(2).msg).contains(badUrl);
    }

    @Test
    public void videoPlayed() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("type", "completionPercent");
        expectedBeaconParams.put("value", "123");
        expectedBeaconParams.put("isSilentPlay", "false");
        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.videoPlayed(RuntimeEnvironment.application, creative, 123, false, feedPosition);

            }
        });
    }

    @Test
    public void silentAutoPlayDuration() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("type", "silentAutoPlayDuration");
        expectedBeaconParams.put("duration", "3000");
        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.silentAutoPlayDuration(creative, 3000, feedPosition);
            }
        });
    }

    @Test
    public void autoplayVideoEngagement() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("type", "userEvent");
        expectedBeaconParams.put("videoDuration", "4567");
        expectedBeaconParams.put("userEvent", "autoplayVideoEngagement");
        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.autoplayVideoEngagement(RuntimeEnvironment.application, creative, 4567, feedPosition);
            }
        });
    }

    @Test
    public void videoViewDuration() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("duration", "4567");
        expectedBeaconParams.put("type", "videoViewDuration");
        expectedBeaconParams.put("silent", "false");
        assertBeaconFired(expectedBeaconParams, new Runnable() {
            @Override
            public void run() {
                subject.videoViewDuration(creative, 4567, false, feedPosition);
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
        FakeHttp.addHttpResponseRule(requestMatcher, new TestHttpResponse(200, ""));

        fireBeacon.run();

        Misc.runLast(STRExecutorService.getInstance());

        assertThat(wasCalled[0]).isTrue();
    }

    private class DateProvider implements Provider<Date> {
        @Override
        public Date get() {
            return now;
        }
    }
}