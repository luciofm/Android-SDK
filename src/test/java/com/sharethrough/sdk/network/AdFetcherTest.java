package com.sharethrough.sdk.network;

import com.sharethrough.sdk.*;
import com.sharethrough.test.Fixtures;
import com.sharethrough.test.util.Misc;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AdFetcherTest extends TestBase {
    private static final String SINGLE_LAYOUT_FIXTURE = Fixtures.getFile("assets/str_single_ad_youtube.json");
    private static final String NO_CREATIVE_FIXTURE = Fixtures.getFile("assets/str_no_creatives.json");
    private static final String PRE_LIVE_PLACEMENT_FIXTURE = Fixtures.getFile("assets/str_prelive_placement.json");
    @Mock private AdFetcher.AdFetcherListener adfetcherlistener;
    private AdFetcherStub subject;
    private String apiUri;
    private String key;

    private String expectedUri;
    private ArrayList<NameValuePair> queryStringParams;
    private ArrayList<NameValuePair> expectedStringParams;

    class AdFetcherStub extends  AdFetcher{
        public void fetchAds(String adRequestUrl){
            SendHttpRequestTask sendHttpRequestTask = new SendHttpRequestTask();
            try {
                sendHttpRequestTask.execute(adRequestUrl).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }


    @Before
    public void setUp() throws Exception {
        Logger.enabled = true;
        subject = new AdFetcherStub();
        apiUri = "http://api";
        key = "key";
        queryStringParams = new ArrayList<NameValuePair>(1);
        queryStringParams.add(new BasicNameValuePair("key", key));

        expectedStringParams = (ArrayList<NameValuePair>) queryStringParams.clone();
        expectedStringParams.add(new BasicNameValuePair("uid", "fake-uid"));
        expectedStringParams.add(new BasicNameValuePair("appId", "version-number"));
        expectedStringParams.add(new BasicNameValuePair("appName", "com.sharethrough.example"));
        expectedUri = apiUri + "?" + URLEncodedUtils.format(expectedStringParams, "utf-8");

        subject.setAdFetcherListener(adfetcherlistener);
    }

    @Test
    public void fetchAds_whenServerReturnsAds_callBackWithJsonResponse() throws  Exception{
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));
        subject.fetchAds(expectedUri);
        verify(adfetcherlistener).onAdResponseLoaded((Response) anyObject());
    }


    @Test
    public void fetchAds_whenServerRequestFails_callBackWithAdsFailedToLoad() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(500, "Bad server"));
        subject.fetchAds(expectedUri);
        verify(adfetcherlistener).onAdResponseFailed();
    }

    @Test
    public void fetchAds_whenExceptionOccursBeforeGettingHttpResponse_callBackWithAdsFailedToLoad() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, null);

        subject.fetchAds(expectedUri);
        verify(adfetcherlistener).onAdResponseFailed();
    }

    @Test
    public void fetchAds_whenExceptionOccursAfterGettingHttpResponse_callBackWithAdsFailedToLoad() throws Exception {
        String responseBody = "{234789wdfjkl ";
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, responseBody));

        subject.fetchAds(expectedUri);
        verify(adfetcherlistener).onAdResponseFailed();
    }


    @Test
    public void whenServerReturns204_callBackWithJsonResponse() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(204, "I got nothing for ya"));

        subject.fetchAds(expectedUri);
        verify(adfetcherlistener).onAdResponseFailed();
    }

    @Test
    public void fetchAds_whenServerReturnsNoAds_callBackWithJsonResponse() throws  Exception{
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, NO_CREATIVE_FIXTURE));
        subject.fetchAds(expectedUri);
        verify(adfetcherlistener).onAdResponseLoaded((Response) anyObject());
    }

    @Test
    public void getResponse_willNotParseBeacons_ifPlacementIsPreLive() throws Exception {
        Response response = subject.getResponse(PRE_LIVE_PLACEMENT_FIXTURE);
        assertThat(response.creatives.get(0).creative.beacon.impression).isEmpty();
        assertThat(response.creatives.get(0).creative.beacon.visible).isEmpty();
        assertThat(response.creatives.get(0).creative.beacon.play).isEmpty();
        assertThat(response.creatives.get(0).creative.beacon.click).isEmpty();
        assertThat(response.creatives.get(0).creative.beacon.silentPlay).isEmpty();
    }

    @Test
    public void getResponse_willParseBeacons_ifPlacementIsLive() throws Exception {
        Response response = subject.getResponse(SINGLE_LAYOUT_FIXTURE);
        assertThat(response.creatives.get(0).creative.beacon.impression).isEmpty();
        assertThat(response.creatives.get(0).creative.beacon.visible).isEmpty();
        assertThat(response.creatives.get(0).creative.beacon.play.size()).isEqualTo(2);
        assertThat(response.creatives.get(0).creative.beacon.click.size()).isEqualTo(2);
        assertThat(response.creatives.get(0).creative.beacon.silentPlay.size()).isEqualTo(2);
    }
}