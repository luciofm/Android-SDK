package com.sharethrough.sdk;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import com.sharethrough.android.sdk.BuildConfig;
import com.sharethrough.sdk.network.STRStringRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.util.*;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class BeaconServiceTest extends TestBase {
    private Map<String, String> expectedCommonParams;
    private Date now;
    private UUID session;
    private BeaconService subject;
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

    public class FakeRequestQueue extends RequestQueue{

        public ArrayList<Object> cache = new ArrayList<>();
        public FakeRequestQueue(Cache cache, Network network) {
            super(cache, network);
        }

        @Override
        public <T> Request<T> add(Request<T> request) {
            cache.add(request);
            return null;
        }

        @Override
        public void start() {
            return;
        }

    }
    private FakeRequestQueue fakeRequestQueue = new FakeRequestQueue(new NoCache(), new BasicNetwork(new HurlStack()));

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
        creative = new Creative("networkType", "className", mediationRequestId);
        creative.setResponseCreative(responseCreative);

        packageInfo.versionName = "fake_app_id";
        when(placement.getStatus()).thenReturn("live");
        AdvertisingIdProvider.advertisingId = advertisingId;
        when(context.getPackageName()).thenReturn("com.example.sdk");
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getPackageInfo("com.example.sdk", PackageManager.GET_META_DATA)).thenReturn(packageInfo);

        subject = new BeaconService(new DateProvider(), session, advertisingIdProvider, new ContextInfo(RuntimeEnvironment.application),"placement key", fakeRequestQueue);
    }

    @Test
    public void commonParams_returnsParamsSentInAllBeacons() throws Exception {
        assertThat(subject.commonParams()).isEqualTo(expectedCommonParams);
    }

    @Test
    public void whenAdvertisingIdNotSet_commonParams_returnsParamsSentInAllBeacons() throws Exception {
        AdvertisingIdProvider.advertisingId = null;
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

        creative = new Creative("networkType", "className", mediationRequestId);
        creative.setResponseCreative(responseCreativeWithDealId);

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
        expectedBeaconParams.put("placementIndex", String.valueOf(creative.getPlacementIndex()));
        expectedBeaconParams.put("pheight", "0");
        expectedBeaconParams.put("pwidth", "0");
        subject.adClicked("fake user event", creative, RendererTest.makeAdView().getAdView(), feedPosition);
        STRStringRequest request = (STRStringRequest)fakeRequestQueue.cache.get(0);
        assertBeaconFired(request.getUrl(), expectedBeaconParams);
    }

    @Test
    public void fireAdRequested() throws Exception {
        final String key = "abc";
        Map<String, String> expectedBeaconParams = subject.commonParams();
        expectedBeaconParams.put("type", "impressionRequest");
        expectedBeaconParams.put("pkey", key);
        subject.adRequested(key);
        STRStringRequest request = (STRStringRequest)fakeRequestQueue.cache.get(0);
        assertBeaconFired(request.getUrl(), expectedBeaconParams);
    }

    @Test
    public void fireAdReceived() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("type", "impression");
        expectedBeaconParams.put("placementIndex", String.valueOf(creative.getPlacementIndex()));
        subject.adReceived(RuntimeEnvironment.application, creative, feedPosition);
        STRStringRequest request = (STRStringRequest)fakeRequestQueue.cache.get(0);
        assertBeaconFired(request.getUrl(), expectedBeaconParams);
    }

    @Test
    public void fireAdVisible() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("pheight","0");
        expectedBeaconParams.put("pwidth","0");
        expectedBeaconParams.put("type", "visible");
        expectedBeaconParams.put("placementIndex", String.valueOf(creative.getPlacementIndex()));

        subject.adVisible(RendererTest.makeAdView(), creative, feedPosition);
        STRStringRequest request = (STRStringRequest)fakeRequestQueue.cache.get(0);
        assertBeaconFired(request.getUrl(),expectedBeaconParams);
    }


    @Test
    public void whenFireAdShareCalled_fireRightBeacon() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("type", "userEvent");
        expectedBeaconParams.put("userEvent", "share");
        expectedBeaconParams.put("share", "shareType");
        expectedBeaconParams.put("engagement", "true");
        expectedBeaconParams.put("placementIndex", String.valueOf(creative.getPlacementIndex()));
        subject.adShared(RuntimeEnvironment.application, creative, "shareType", feedPosition);
        STRStringRequest request = (STRStringRequest)fakeRequestQueue.cache.get(0);
        assertBeaconFired(request.getUrl(), expectedBeaconParams);
    }

    @Test
    public void whenAdReceivedCalled_fireImpressionThirdPartyBeacons() throws Exception {
        String[] initialUrls = {"//impressionEndOne?cacheBuster=[timestamp]", "//impressionEndTwo?cacheBuster=[timestamp]"};

        ArrayList<String> impressionEndoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));
        responseCreative.creative.beacon.impression = impressionEndoints;
        Creative testCreative = new Creative("networkType", "className", mediationRequestId);
        testCreative.setResponseCreative(responseCreative);
        subject.adReceived(RuntimeEnvironment.application, testCreative, feedPosition);
        assertThat(fakeRequestQueue.cache.size() == 3);

        for (int i = 0; i < fakeRequestQueue.cache.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl;
            STRStringRequest request = (STRStringRequest) fakeRequestQueue.cache.get(i);
            assertThat(request.getUrl()).isEqualTo(expectedUrl);
        }
    }

    @Test
    public void whenAdVisibleCalled_fireVisibleThirdPartyBeacons() throws Exception {
        String[] initialUrls = {"//visibleEndOne?cacheBuster=[timestamp]", "//visibleEndTwo?cacheBuster=[timestamp]"};
        ArrayList<String> visibleEndoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));
        responseCreative.creative.beacon.visible = visibleEndoints;
        Creative testCreative = new Creative("networkType", "className", mediationRequestId);
        testCreative.setResponseCreative(responseCreative);

        subject.adVisible(RendererTest.makeAdView(), testCreative, feedPosition);

        assertThat(fakeRequestQueue.cache.size() == 3);

        for (int i = 0; i < fakeRequestQueue.cache.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl;
            STRStringRequest request = (STRStringRequest) fakeRequestQueue.cache.get(i);
            assertThat(request.getUrl()).isEqualTo(expectedUrl);
        }
    }

    @Test
    public void whenAdClickCalled_fireClickAndVideoThirdPartyBeacons() throws Exception {
        String[] initialUrls = {"//click/EndOne", "//click/End[Two]?cacheBuster=[timestamp]", "//video/EndOne", "//video/EndTwo"};

        ArrayList<String> clickEndoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));
        ArrayList<String> playEndoints = new ArrayList<>(Arrays.asList(initialUrls[2], initialUrls[3]));

        responseCreative.creative.beacon.click = clickEndoints;
        responseCreative.creative.beacon.play = playEndoints;

        Creative testCreative = new Creative("networkType", "className", mediationRequestId);
        testCreative.setResponseCreative(responseCreative);

        subject.adClicked("test-creative", testCreative, RendererTest.makeAdView().getAdView(), feedPosition);

        assertThat(fakeRequestQueue.cache.size() == 5);
        for (int i = 0; i < fakeRequestQueue.cache.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            STRStringRequest request = (STRStringRequest) fakeRequestQueue.cache.get(i);
            assertThat(request.getUrl()).isEqualTo(expectedUrl);
        }
    }

    @Test
    public void whenNetworkImpressionRequestCalled_firesBeacon() throws Exception {
        int baseOneNetworkOrder = 1;
        int baseOnePlacementIndex = 5;
        String mrid = "fake-mrid";
        String networkKey = "fake-network-key";
        Map<String, String> expectedBeaconParams = subject.commonParams();
        expectedBeaconParams.put("pkey", "placement key");
        expectedBeaconParams.put("type", "networkImpressionRequest");
        expectedBeaconParams.put("networkKey", networkKey);
        expectedBeaconParams.put("networkOrder", String.valueOf(baseOneNetworkOrder));
        expectedBeaconParams.put("mrid", mrid);
        expectedBeaconParams.put("placementIndex", String.valueOf(baseOnePlacementIndex));

        subject.networkImpressionRequest(networkKey, baseOneNetworkOrder, mrid, baseOnePlacementIndex);
        STRStringRequest request = (STRStringRequest)fakeRequestQueue.cache.get(0);
        assertBeaconFired(request.getUrl(), expectedBeaconParams);
    }

    @Test
    public void whenNetworkNoFillCalled_firesBeacon() throws Exception {
        int baseOneNetworkOrder = 1;
        int baseOnePlacementIndex = 5;
        String mrid = "fake-mrid";
        String networkKey = "fake-network-key";
        Map<String, String> expectedBeaconParams = subject.commonParams();
        expectedBeaconParams.put("pkey", "placement key");
        expectedBeaconParams.put("type", "networkNoFill");
        expectedBeaconParams.put("networkKey", networkKey);
        expectedBeaconParams.put("networkOrder", String.valueOf(baseOneNetworkOrder));
        expectedBeaconParams.put("mrid", mrid);
        expectedBeaconParams.put("placementIndex", String.valueOf(baseOnePlacementIndex));

        subject.networkNoFill(networkKey, baseOneNetworkOrder, mrid, baseOnePlacementIndex);
        STRStringRequest request = (STRStringRequest)fakeRequestQueue.cache.get(0);
        assertBeaconFired(request.getUrl(), expectedBeaconParams);
    }

    @Test
    public void whenMediationStartCalled_firesBeacon() throws Exception {
        int baseOnePlacementIndex = 5;
        String mrid = "fake-mrid";
        Map<String, String> expectedBeaconParams = subject.commonParams();
        expectedBeaconParams.put("pkey", "placement key");
        expectedBeaconParams.put("type", "mediationStart");
        expectedBeaconParams.put("mrid", mrid);
        expectedBeaconParams.put("placementIndex", String.valueOf(baseOnePlacementIndex));

        subject.mediationStart(mrid, baseOnePlacementIndex);
        STRStringRequest request = (STRStringRequest)fakeRequestQueue.cache.get(0);
        assertBeaconFired(request.getUrl(), expectedBeaconParams);
    }

    @Test
    public void whenSilentAutoplayDuration3SecondsCalled_fire3SecondBeacons() throws Exception {
        int seconds = 3000;
        String[] initialUrls = {"//silentPlay/EndOne", "//silentPlay/End[Two]?cacheBuster=[timestamp]"};

        ArrayList<String> silentPlayEndpoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));
        responseCreative.creative.beacon.silentPlay = silentPlayEndpoints;
        Creative testCreative = new Creative("networkType", "className", mediationRequestId);
        testCreative.setResponseCreative(responseCreative);
        subject.silentAutoPlayDuration(testCreative, seconds, feedPosition);

        assertThat(fakeRequestQueue.cache.size() == 3);
        for (int i = 0; i < fakeRequestQueue.cache.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            STRStringRequest request = (STRStringRequest) fakeRequestQueue.cache.get(i);
            assertThat(request.getUrl()).isEqualTo(expectedUrl);
        }

        //first party beacon
        String returnedUrl = ((STRStringRequest)fakeRequestQueue.cache.get(2)).getUrl();
        assertThat(returnedUrl).contains("type=silentAutoPlayDuration");
        assertThat(returnedUrl).contains("duration=" + seconds);
        assertThat(returnedUrl).contains("placementIndex=" + creative.getPlacementIndex());
    }

    @Test
    public void whenSilentAutoplayDuration10SecondsCalled_fireSilentPlayThirdPartyBeacons() throws Exception {
        int seconds = 10000;
        String[] initialUrls = {"//silentPlay/EndOne", "//silentPlay/End[Two]?cacheBuster=[timestamp]"};

        ArrayList<String> silentPlayEndpoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));
        responseCreative.creative.beacon.tenSecondSilentPlay = silentPlayEndpoints;
        Creative testCreative = new Creative("networkType", "className", mediationRequestId);
        testCreative.setResponseCreative(responseCreative);
        subject.silentAutoPlayDuration(testCreative, seconds, feedPosition);

        assertThat(fakeRequestQueue.cache.size() == 3);
        for (int i = 0; i < fakeRequestQueue.cache.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            STRStringRequest request = (STRStringRequest) fakeRequestQueue.cache.get(i);
            assertThat(request.getUrl()).isEqualTo(expectedUrl);
        }

        //first party beacon
        String returnedUrl = ((STRStringRequest)fakeRequestQueue.cache.get(2)).getUrl();
        assertThat(returnedUrl).contains("type=silentAutoPlayDuration");
        assertThat(returnedUrl).contains("duration=" + seconds);
        assertThat(returnedUrl).contains("placementIndex=" + creative.getPlacementIndex());
    }

    @Test
    public void whenSilentAutoplayDuration15SecondsCalled_fireSilentPlayThirdPartyBeacons() throws Exception {
        int seconds = 15000;
        String[] initialUrls = {"//silentPlay/EndOne", "//silentPlay/End[Two]?cacheBuster=[timestamp]"};
        ArrayList<String> silentPlayEndpoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));
        responseCreative.creative.beacon.fifteenSecondSilentPlay = silentPlayEndpoints;
        Creative testCreative = new Creative("networkType", "className", mediationRequestId);
        testCreative.setResponseCreative(responseCreative);
        subject.silentAutoPlayDuration(testCreative, seconds, feedPosition);

        assertThat(fakeRequestQueue.cache.size() == 3);
        for (int i = 0; i < fakeRequestQueue.cache.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            STRStringRequest request = (STRStringRequest) fakeRequestQueue.cache.get(i);
            assertThat(request.getUrl()).isEqualTo(expectedUrl);
        }

        //first party beacon
        String returnedUrl = ((STRStringRequest)fakeRequestQueue.cache.get(2)).getUrl();
        assertThat(returnedUrl).contains("type=silentAutoPlayDuration");
        assertThat(returnedUrl).contains("duration=" + seconds);
        assertThat(returnedUrl).contains("placementIndex="+ creative.getPlacementIndex());
    }

    @Test
    public void whenSilentAutoplayDuration30SecondsCalled_fireSilentPlayThirdPartyBeacons() throws Exception {
        int seconds = 30000;
        String[] initialUrls = {"//silentPlay/EndOne", "//silentPlay/End[Two]?cacheBuster=[timestamp]"};
        ArrayList<String> silentPlayEndpoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));
        responseCreative.creative.beacon.thirtySecondSilentPlay = silentPlayEndpoints;
        Creative testCreative = new Creative("networkType", "className", mediationRequestId);
        testCreative.setResponseCreative(responseCreative);
        subject.silentAutoPlayDuration(testCreative, seconds, feedPosition);

        assertThat(fakeRequestQueue.cache.size() == 3);
        for (int i = 0; i < fakeRequestQueue.cache.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            STRStringRequest request = (STRStringRequest) fakeRequestQueue.cache.get(i);
            assertThat(request.getUrl()).isEqualTo(expectedUrl);
        }

        //first party beacon
        String returnedUrl = ((STRStringRequest)fakeRequestQueue.cache.get(2)).getUrl();
        assertThat(returnedUrl).contains("type=silentAutoPlayDuration");
        assertThat(returnedUrl).contains("duration=" + seconds);
        assertThat(returnedUrl).contains("placementIndex="+ creative.getPlacementIndex());
    }

    @Test
    public void whenSilentAutoplayDurationPlayedFor95Percent_fireCompletedThirdPartyBeacons() throws Exception {
        String[] initialUrls = {"//silentPlay/EndOne", "//silentPlay/End[Two]?cacheBuster=[timestamp]"};
        ArrayList<String> silentPlayEndpoints = new ArrayList<>(Arrays.asList(initialUrls[0], initialUrls[1]));
        responseCreative.creative.beacon.completedSilentPlay = silentPlayEndpoints;
        Creative testCreative = new Creative("networkType", "className", mediationRequestId);
        testCreative.setResponseCreative(responseCreative);
        subject.videoPlayed(RuntimeEnvironment.application, testCreative, 95, true, feedPosition);

        assertThat(fakeRequestQueue.cache.size() == 3);
        for (int i = 0; i < fakeRequestQueue.cache.size() - 1; i++) {
            String cacheBustedUrl = initialUrls[i].replaceAll("\\[timestamp\\]", String.valueOf(now.getTime()));
            String expectedUrl = "http:" + cacheBustedUrl.replace("[", "%5B").replace("]", "%5D");
            STRStringRequest request = (STRStringRequest) fakeRequestQueue.cache.get(i);
            assertThat(request.getUrl()).isEqualTo(expectedUrl);
        }
    }

    @Test
    public void whenThirdPartyBeaconsIsEmpty_DoesNotFireThirdPartyBeacons() throws Exception{
        responseCreative.creative.beacon.click = new ArrayList<>();
        responseCreative.creative.beacon.play = new ArrayList<>();
        responseCreative.creative.beacon.silentPlay = new ArrayList<>();
        responseCreative.creative.beacon.impression = new ArrayList<>();
        responseCreative.creative.beacon.visible = new ArrayList<>();

        Creative testCreative = new Creative("networkType", "className", mediationRequestId);
        testCreative.setResponseCreative(responseCreative);

        subject.adClicked("test-creative", testCreative, RendererTest.makeAdView().getAdView(), feedPosition);

        assertThat(fakeRequestQueue.cache.size() == 1);
        for (int i = 0; i < fakeRequestQueue.cache.size() - 1; i++) {
            STRStringRequest request = (STRStringRequest) fakeRequestQueue.cache.get(i);
            assertThat(request.getUrl()).doesNotContain("third-party");
        }
    }

    @Test
    public void whenAThirdPartyBeaconIsInvalid_logsWithoutCrashing() throws Exception {
        String badUrl = "//%%%invalid%url%%%";
        List<String> clickEndoints = Arrays.asList(badUrl);
        responseCreative.creative.beacon.click = clickEndoints;
        Creative testCreative = new Creative("networkType", "className", mediationRequestId);
        testCreative.setResponseCreative(responseCreative);
        subject.adClicked("test-creative", testCreative, RendererTest.makeAdView().getAdView(), feedPosition);

        assertThat(fakeRequestQueue.cache.size() == 1);
        assertThat(ShadowLog.getLogs().get(2).msg).contains(badUrl);
    }

    @Test
    public void videoPlayed() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("type", "completionPercent");
        expectedBeaconParams.put("value", "123");
        expectedBeaconParams.put("isSilentPlay", "false");
        expectedBeaconParams.put("placementIndex", String.valueOf(creative.getPlacementIndex()));
        subject.videoPlayed(RuntimeEnvironment.application, creative, 123, false, feedPosition);
        STRStringRequest request = (STRStringRequest)fakeRequestQueue.cache.get(0);
        assertBeaconFired(request.getUrl(), expectedBeaconParams);

    }

    @Test
    public void silentAutoPlayDuration() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("type", "silentAutoPlayDuration");
        expectedBeaconParams.put("duration", "3000");
        expectedBeaconParams.put("placementIndex", String.valueOf(creative.getPlacementIndex()));
        subject.silentAutoPlayDuration(creative, 3000, feedPosition);
        STRStringRequest request = (STRStringRequest)fakeRequestQueue.cache.get(0);
        assertBeaconFired(request.getUrl(), expectedBeaconParams);
    }

    @Test
    public void autoplayVideoEngagement() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("type", "userEvent");
        expectedBeaconParams.put("videoDuration", "4567");
        expectedBeaconParams.put("userEvent", "autoplayVideoEngagement");
        expectedBeaconParams.put("placementIndex", String.valueOf(creative.getPlacementIndex()));
        subject.autoplayVideoEngagement(RuntimeEnvironment.application, creative, 4567, feedPosition);

        STRStringRequest request = (STRStringRequest)fakeRequestQueue.cache.get(0);
        assertBeaconFired(request.getUrl(), expectedBeaconParams);

    }

    @Test
    public void videoViewDuration() throws Exception {
        Map<String, String> expectedBeaconParams = subject.commonParamsWithCreative(creative);
        expectedBeaconParams.put("duration", "4567");
        expectedBeaconParams.put("type", "videoViewDuration");
        expectedBeaconParams.put("silent", "false");
        expectedBeaconParams.put("placementIndex", String.valueOf(creative.getPlacementIndex()));
        subject.videoViewDuration(creative, 4567, false, feedPosition);
        STRStringRequest request = (STRStringRequest)fakeRequestQueue.cache.get(0);
        assertBeaconFired(request.getUrl(), expectedBeaconParams);
    }

    private void assertBeaconFired(final String firedUrl, final Map<String, String> expectedBeaconParams) {
        Uri.Builder uriBuilder = Uri.parse(BeaconService.TRACKING_URL).buildUpon();
        for (Map.Entry<String, String> entry : expectedBeaconParams.entrySet()) {
            uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        String expectedUrl = uriBuilder.build().toString();
        assertThat(firedUrl).isEqualTo(expectedUrl);
    }

    private class DateProvider implements Provider<Date> {
        @Override
        public Date get() {
            return now;
        }
    }
}