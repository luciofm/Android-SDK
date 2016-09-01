package com.sharethrough.sdk.network;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.google.gson.Gson;
import com.sharethrough.sdk.*;
import com.sharethrough.test.Fixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AdFetcherTest extends TestBase {
    private static final String SINGLE_LAYOUT_FIXTURE = Fixtures.getFile("assets/str_single_ad_youtube.json");
    private static final String NO_CREATIVE_FIXTURE = Fixtures.getFile("assets/str_no_creatives.json");
    private static final String PRE_LIVE_PLACEMENT_FIXTURE = Fixtures.getFile("assets/str_prelive_placement.json");
    private static final String MULTIPLE_CREATIVE_FIXTURE = Fixtures.getFile("assets/str_multiple_ad_youtube.json");

    private AdFetcherStub subject;
    private Response emptyResponse;
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
        setUpEmptyResponse();

        subject = new AdFetcherStub(RuntimeEnvironment.application);
        subject.setRequestQueue(requestQueue);
        subject.setAdFetcherListener(adfetcherlistener);
    }

    private void setUpEmptyResponse() {
        emptyResponse = new Response();
        emptyResponse.placement = new Response.Placement();
        emptyResponse.placement.placementAttributes = new Response.Placement.PlacementAttributes();
        emptyResponse.creatives = new ArrayList<>();
        emptyResponse.creatives.add(new Response.Creative());
        emptyResponse.creatives.get(0).creative = new Response.Creative.CreativeInner();
    }

    @Test
    public void fetchAds_triggersAdRequest() throws Exception {
        subject.fetchAds("url", false);

        verify(requestQueue).add((Request) anyObject());
    }

    @Test
    public void handleResponse_callBackWithAdResponseLoaded_whenJsonIsValid() throws Exception {
        subject.handleResponse(SINGLE_LAYOUT_FIXTURE, false);

        verify(adfetcherlistener).onAdResponseLoaded((Response) anyObject());
    }

    @Test
    public void handleResponse_callBackWithAdResponseLoaded_whenJsonIsValidButNoAds() throws Exception {
        subject.handleResponse(NO_CREATIVE_FIXTURE, false);

        verify(adfetcherlistener).onAdResponseLoaded((Response) anyObject());
    }

    @Test
    public void handleResponse_callBackWithAdResponseFailed_whenJsonIsInvalid() throws Exception {
        String responseBody = "{234789wdfjkl ";
        subject.handleResponse(responseBody, false);

        verify(adfetcherlistener).onAdResponseFailed();
    }

    @Test
    public void getResponse_addAdServerRequestIdToEachCreative() throws Exception {
        Response response = subject.getResponse(MULTIPLE_CREATIVE_FIXTURE, false);
        assertThat(response.creatives.size()).isEqualTo(2);
        assertThat(response.creatives.get(0).adserverRequestId).isEqualTo("fake-adserver-request-id");
        assertThat(response.creatives.get(1).adserverRequestId).isEqualTo("fake-adserver-request-id");
    }

    @Test
    public void getResponse_willNotParseBeacons_ifPlacementIsPreLive() throws Exception {
        Response response = subject.getResponse(PRE_LIVE_PLACEMENT_FIXTURE, false);
        assertThat(response.creatives.get(0).creative.beacons.impression).isNull();
        assertThat(response.creatives.get(0).creative.beacons.visible).isNull();
        assertThat(response.creatives.get(0).creative.beacons.play).isNull();
        assertThat(response.creatives.get(0).creative.beacons.click).isNull();
        assertThat(response.creatives.get(0).creative.beacons.silent_play).isNull();
    }

    @Test
    public void getResponse_willParseBeacons_ifPlacementIsLive() throws Exception {
        Response response = subject.getResponse(SINGLE_LAYOUT_FIXTURE, false);
        assertThat(response.creatives.get(0).creative.beacons.impression).isEmpty();
        assertThat(response.creatives.get(0).creative.beacons.visible).isEmpty();
        assertThat(response.creatives.get(0).creative.beacons.play.size()).isEqualTo(2);
        assertThat(response.creatives.get(0).creative.beacons.click.size()).isEqualTo(2);
        assertThat(response.creatives.get(0).creative.beacons.silent_play.size()).isEqualTo(2);
    }

    @Test
    public void setPromotedByText_ifDirectSell_andCampaignSlugOverrideExists_setsCampaignSlug() throws Exception {
        emptyResponse.creatives.get(0).creative.promoted_by_text = "not null";
        subject.setPromotedByTextForEachCreative(true, emptyResponse);

        String customSetPromotedByText = emptyResponse.creatives.get(0).creative.custom_set_promoted_by_text;
        assertThat(customSetPromotedByText).isEqualTo("not null");
    }

    @Test
    public void setPromotedByText_ifDirectSell_andCampaignSlugOverrideDoesNOTExists_andDirectSoldSlugDoes_setsDirectSoldSlug() throws Exception {
        emptyResponse.placement.placementAttributes.directSellPromotedByText = "not null";

        subject.setPromotedByTextForEachCreative(true, emptyResponse);
        String customSetPromotedByText = emptyResponse.creatives.get(0).creative.custom_set_promoted_by_text;
        assertThat(customSetPromotedByText).isEqualTo("not null");
    }

    @Test
    public void setPromotedByText_ifDirectSell_andOnlyProgrammaticSlugExists_setsProgrammaticSlug() throws Exception {
        emptyResponse.placement.placementAttributes.promotedByText = "not null";

        subject.setPromotedByTextForEachCreative(true, emptyResponse);
        String customSetPromotedByText = emptyResponse.creatives.get(0).creative.custom_set_promoted_by_text;
        assertThat(customSetPromotedByText).isEqualTo("not null");
    }

    @Test
    public void setPromotedByText_ifProgrammatic_andProgrammaticSlugExists_setsProgrammaticSlug() throws Exception {
        emptyResponse.placement.placementAttributes.promotedByText = "not null";

        subject.setPromotedByTextForEachCreative(false, emptyResponse);
        String customSetPromotedByText = emptyResponse.creatives.get(0).creative.custom_set_promoted_by_text;
        assertThat(customSetPromotedByText).isEqualTo("not null");
    }

    @Test
    public void setPromotedByText_ifProgrammatic_andProgrammaticSlugDoesNOTExist_setsAdBy() throws Exception {
        subject.setPromotedByTextForEachCreative(false, emptyResponse);
        String customSetPromotedByText = emptyResponse.creatives.get(0).creative.custom_set_promoted_by_text;
        assertThat(customSetPromotedByText).isEqualTo("Ad By");
    }
}

