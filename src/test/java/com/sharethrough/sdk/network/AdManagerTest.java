package com.sharethrough.sdk.network;

import android.content.pm.PackageManager;
import android.os.AsyncTask;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.Misc;
import com.sharethrough.test.Fixtures;
import com.sharethrough.test.util.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import static org.robolectric.Robolectric.shadowOf;



public class AdManagerTest extends TestBase {

    private AdManager subject;
    @Mock private AdManager.AdManagerListener adManagerListener;
    private String expectedUri;

    private static final String SINGLE_LAYOUT_FIXTURE = Fixtures.getFile("assets/str_single_ad_youtube.json");
    private static final String NO_CREATIVE_FIXTURE = Fixtures.getFile("assets/str_no_creatives.json");
    private AdFetcherStub adFetcherStub;
    private String apiUri;
    private String key;
    private ArrayList<NameValuePair> queryStringParams;
    private ArrayList<NameValuePair> expectedStringParams;
    private String advertisingId;
    private String versionName;

    class AdFetcherStub extends AdFetcher{
        public int fetchedAdsCount = 0;

        public void fetchAds(String adRequestUrl){
            SendHttpRequestTask sendHttpRequestTask = new SendHttpRequestTask();
            sendHttpRequestTask.execute(adRequestUrl);
            fetchedAdsCount ++;
        }
    }

    @Before
    public void setUp() throws Exception {
        //Mockito.doNothing().when(adManagerListener).onAdsReady((List<Creative>) anyObject(), (Placement)anyObject());
        AdManager.setAdManagerInstance(null);
        subject = AdManager.getInstance(Robolectric.application.getApplicationContext());
        subject.setAdManagerListener(adManagerListener);

        adFetcherStub = new AdFetcherStub();
        subject.adFetcher = adFetcherStub;
        subject.setAdFetcherListener();

        advertisingId = "ABCD1234";
        apiUri = "http://api";
        key = "key";
        queryStringParams = new ArrayList<NameValuePair>(1);
        queryStringParams.add(new BasicNameValuePair("key", key));

        expectedStringParams = (ArrayList<NameValuePair>) queryStringParams.clone();
        expectedStringParams.add(new BasicNameValuePair("uid", advertisingId));

        try {
            String appPackageName = Robolectric.application.getApplicationContext().getPackageName();
            versionName = Robolectric.application.getApplicationContext().getPackageManager().getPackageInfo(appPackageName, PackageManager.GET_META_DATA).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if(versionName != null){
            expectedStringParams.add(new BasicNameValuePair("appId", versionName));
        }

        expectedStringParams.add(new BasicNameValuePair("appName", "com.sharethrough.android.sdk"));
        expectedUri = apiUri + "?" + URLEncodedUtils.format(expectedStringParams, "utf-8");

    }

    @Test
    public void fetchAds_whenRequestIsInProgress_doesNotStartNewRequest() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));
        Robolectric.getBackgroundScheduler().pause();
        subject.fetchAds(apiUri, queryStringParams, advertisingId);
        assertThat(adFetcherStub.fetchedAdsCount).isEqualTo(1);
        //background schedule is paused so fetchads should be stuck in another thread.

        queryStringParams = new ArrayList<NameValuePair>(1);
        queryStringParams.add(new BasicNameValuePair("key", key));
        subject.fetchAds(apiUri, queryStringParams, advertisingId);
        assertThat(adFetcherStub.fetchedAdsCount).isEqualTo(1);
        Robolectric.runBackgroundTasks();
    }


    @Test
    public void fetchAds_whenPreviousResponseReceived_startsNewRequest() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));
        Robolectric.getBackgroundScheduler().pause();

        subject.fetchAds(apiUri, queryStringParams, advertisingId);
        assertThat(adFetcherStub.fetchedAdsCount).isEqualTo(1);
        Robolectric.runBackgroundTasks();

        queryStringParams = new ArrayList<NameValuePair>(1);
        queryStringParams.add(new BasicNameValuePair("key", key));
        subject.fetchAds(apiUri, queryStringParams, advertisingId);
        assertThat(adFetcherStub.fetchedAdsCount).isEqualTo(2);
        Robolectric.runBackgroundTasks();
    }

    @Test
    public void fetchAds_whenPreviousRequestFailed_startsNewRequest() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(404, "fail"));
        Robolectric.getBackgroundScheduler().pause();

        subject.fetchAds(apiUri, queryStringParams, advertisingId);
        assertThat(adFetcherStub.fetchedAdsCount).isEqualTo(1);
        Robolectric.runBackgroundTasks();

        queryStringParams = new ArrayList<NameValuePair>(1);
        queryStringParams.add(new BasicNameValuePair("key", key));
        subject.fetchAds(apiUri, queryStringParams, advertisingId);

        assertThat(adFetcherStub.fetchedAdsCount).isEqualTo(2);
        Robolectric.runBackgroundTasks();
    }

    @Test
    public void fetchAds_whenCreativesReturn_passListOfCreatives() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));
        Robolectric.getBackgroundScheduler().pause();

        Answer<Object> answer = new Answer(){

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                List<Creative> creatives = (List<Creative>)invocationOnMock.getArguments()[0];
                Placement placement = (Placement)invocationOnMock.getArguments()[1];
                assertThat(creatives.size()).isEqualTo(2);
                assertThat(placement.getArticlesBeforeFirstAd()).isEqualTo(Integer.MAX_VALUE);
                return null;
            }
        };
        Mockito.doAnswer(answer).when(adManagerListener).onAdsReady((List<Creative>) anyObject(), (Placement) anyObject());

        subject.fetchAds(apiUri, queryStringParams, advertisingId);
        assertThat(adFetcherStub.fetchedAdsCount).isEqualTo(1);
        Robolectric.runBackgroundTasks();
        verify(adManagerListener).onAdsReady((List<Creative>) anyObject(), (Placement) anyObject());
        Mockito.reset(adManagerListener);

    }

    @Test
    public void fetchAds_whenNoCreativesReturn_passEmptyListOfCreatives() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, NO_CREATIVE_FIXTURE));
        Robolectric.getBackgroundScheduler().pause();
        subject.fetchAds(apiUri, queryStringParams, advertisingId);
        assertThat(adFetcherStub.fetchedAdsCount).isEqualTo(1);
        Robolectric.runBackgroundTasks();
        verify(adManagerListener).onNoAdsToShow();
    }



}