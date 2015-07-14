package com.sharethrough.sdk.network;

import com.sharethrough.sdk.*;
import com.sharethrough.sdk.Misc;
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
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class AdManagerTest extends TestBase {

    @Mock private AdManager.AdManagerListener adManagerListener;
    private AdManager subject;
    @Mock private AdFetcher adFetcher;



    @Before
    public void setUp() throws Exception {

        subject = AdManager.getInstance(Robolectric.application.getApplicationContext());
        subject.adFetcher = null;
        subject.setAdManagerListener(adManagerListener);
    }

    @Test
    public void when() throws Exception {
        assertThat(true).isTrue();
    }


    @Test
    public void fetchAds_whenRequestIsInProgress_doesNotStartNewRequest() throws Exception {
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(runnableArgumentCaptor.capture());
        assertThat(runnableArgumentCaptor.getAllValues()).hasSize(1);
    }


    @Test
    public void fetchAds_whenPreviousResponseReceived_startsNewRequest() throws Exception {
        // make first request
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        com.sharethrough.test.util.Misc.runLast(executorService);
        reset(executorService);

        // finish loading all images
        verify(imageFetcher, atLeastOnce()).fetchCreativeImages(any(URI.class), any(Response.Creative.class), imageFetcherCallback.capture());
        for (ImageFetcher.Callback callback : imageFetcherCallback.getAllValues()) {
            callback.success(creative);
        }

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        verify(executorService).execute(any(Runnable.class));
    }

    @Test
    public void fetchAds_whenPreviousRequestFailed_startsNewRequest() throws Exception {
        // make first request
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, SINGLE_LAYOUT_FIXTURE));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        com.sharethrough.test.util.Misc.runLast(executorService);
        reset(executorService);

        // finish loading all images
        verify(imageFetcher, atLeastOnce()).fetchCreativeImages(any(URI.class), any(Response.Creative.class), imageFetcherCallback.capture());
        for (ImageFetcher.Callback callback : imageFetcherCallback.getAllValues()) {
            callback.failure();
        }

        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);

        verify(executorService).execute(any(Runnable.class));
    }

    @Test
    public void fetchAds_whenPlacementHasNotBeenSet_CallsApplyOnPlacementHandlerAndSetsPlacementSetToTrue() {
        final int[] placementHandlerCalledCounter = {0};
        placementHandler = new Function<Placement, Void>() {
            @Override
            public Void apply(Placement placement) {
                placementHandlerCalledCounter[0]++;
                return null;
            }
        };
        ArrayList<NameValuePair> qsParams = (ArrayList<NameValuePair>) queryStringParams.clone();

        // first time
        Robolectric.addHttpResponseRule("GET", expectedUri, new TestHttpResponse(200, MULTIPLE_LAYOUT_FIXTURE));
        subject.fetchAds(imageFetcher, apiUri, queryStringParams, creativeHandler, adFetcherCallback, placementHandler);
        com.sharethrough.test.util.Misc.runLast(executorService);
        assertThat(placementHandlerCalledCounter[0]).isEqualTo(1);

        reset(executorService);

        //second time
        subject.setIsRunning(false);
        subject.fetchAds(imageFetcher, apiUri, qsParams, creativeHandler, adFetcherCallback, placementHandler);
        com.sharethrough.test.util.Misc.runLast(executorService);
        assertThat(placementHandlerCalledCounter[0]).isEqualTo(1);

    }
}