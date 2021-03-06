package com.sharethrough.sdk.network;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.sharethrough.sdk.*;
import com.sharethrough.test.Fixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AdFetcherTest extends TestBase {
    private static final String SINGLE_LAYOUT_FIXTURE = Fixtures.getFile("assets/str_single_ad_youtube.json");
    private static final String NO_CREATIVE_FIXTURE = Fixtures.getFile("assets/str_no_creatives.json");
    private static final String PRE_LIVE_PLACEMENT_FIXTURE = Fixtures.getFile("assets/str_prelive_placement.json");

    private AdFetcherStub subject;
    @Mock private AdFetcher.AdFetcherListener adfetcherlistener;
    @Mock private RequestQueue requestQueue;

    class AdFetcherStub extends AdFetcher {
        public AdFetcherStub(Context context) {
            super(context);
        }

        public void setRequestQueue(RequestQueue requestQueue) {
            this.requestQueue = requestQueue;
        }
    }

    @Before
    public void setUp() throws Exception {
        Logger.enabled = true;
        subject = new AdFetcherStub(RuntimeEnvironment.application);
        subject.setRequestQueue(requestQueue);
        subject.setAdFetcherListener(adfetcherlistener);
    }

    @Test
    public void fetchAds_triggersAdRequest() throws Exception {
        subject.fetchAds("url");

        verify(requestQueue).add((Request) anyObject());
    }

    @Test
    public void handleResponse_callBackWithAdResponseLoaded_whenJsonIsValid() throws Exception {
        subject.handleResponse(SINGLE_LAYOUT_FIXTURE);

        verify(adfetcherlistener).onAdResponseLoaded((Response) anyObject());
    }

    @Test
    public void handleResponse_callBackWithAdResponseLoaded_whenJsonIsValidButNoAds() throws Exception {
        subject.handleResponse(NO_CREATIVE_FIXTURE);

        verify(adfetcherlistener).onAdResponseLoaded((Response) anyObject());
    }

    @Test
    public void handleResponse_callBackWithAdResponseFailed_whenJsonIsInvalid() throws Exception {
        String responseBody = "{234789wdfjkl ";
        subject.handleResponse(responseBody);

        verify(adfetcherlistener).onAdResponseFailed();
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
        assertThat(response.creatives.get(0).creative.optOutText.isEmpty());
        assertThat(response.creatives.get(0).creative.optOutUrl.isEmpty());

        assertThat(response.creatives.get(1).creative.optOutUrl).isEqualTo("http://www.example.com/opt-out");
        assertThat(response.creatives.get(1).creative.optOutText).isEqualTo("don't click this link");
    }


}