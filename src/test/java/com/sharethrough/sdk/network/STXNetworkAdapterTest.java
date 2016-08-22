package com.sharethrough.sdk.network;

import android.content.Context;
import android.util.Pair;
import com.google.gson.JsonObject;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.mediation.ICreative;
import com.sharethrough.sdk.mediation.MediationManager;
import com.sharethrough.test.Fixtures;
//import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class STXNetworkAdapterTest extends TestBase {

    private STXNetworkAdapter subject;
    @Mock private MediationManager.MediationListener mockMediationListener;
    @Mock private BeaconService mockBeaconService;

    private static final String SINGLE_LAYOUT_FIXTURE = Fixtures.getFile("assets/str_single_ad_youtube.json");
    private static final String NO_CREATIVE_FIXTURE = Fixtures.getFile("assets/str_no_creatives.json");
    private static final String ALLOW_INSTANT_PLAY_TRUE_FORCE_CLICK_TO_PLAY_TRUE = Fixtures.getFile("assets/str_allowInstantPlayTrue_forceClickToPlayTrue.json");
    private static final String ALLOW_INSTANT_PLAY_TRUE_FORCE_CLICK_TO_PLAY_FALSE = Fixtures.getFile("assets/str_allowInstantPlayTrue_forceClickToPlayFalse.json");
    private static final String ALLOW_INSTANT_PLAY_FALSE_FORCE_CLICK_TO_PLAY_TRUE = Fixtures.getFile("assets/str_allowInstantPlayFalse_forceClickToPlayTrue.json");
    private static final String ALLOW_INSTANT_PLAY_FALSE_FORCE_CLICK_TO_PLAY_FALSE = Fixtures.getFile("assets/str_allowInstantPlayFalse_forceClickToPlayFalse.json");
    private AdFetcherStub adFetcherStub;

    class AdFetcherStub extends AdFetcher{
        public int fetchedAdsCount = 0;

        public AdFetcherStub(Context context) {
            super(context);
        }

        public void fetchAds(String adRequestUrl){
            fetchedAdsCount ++;
        }
    }

    @Before
    public void setUp() throws Exception {
        subject = new STXNetworkAdapter(RuntimeEnvironment.application.getApplicationContext(), mockBeaconService);
        subject.setMediationListener(mockMediationListener);

        adFetcherStub = new AdFetcherStub(RuntimeEnvironment.application);
        subject.adFetcher = adFetcherStub;
        subject.setAdFetcherListener();
    }

    @Test
    public void fetchAds_whenRequestIsInProgress_doesNotStartNewRequest() throws Exception {
        subject.fetchAds("url", new ArrayList<Pair<String, String>>(), "adId", "mrid");
        assertThat(adFetcherStub.fetchedAdsCount).isEqualTo(1);
        assertThat(subject.getMediationRequestId()).isEqualTo("mrid");


        subject.fetchAds("url", new ArrayList<Pair<String, String>>(), "adId", "mrid2");
        assertThat(adFetcherStub.fetchedAdsCount).isEqualTo(1);
        assertThat(subject.getMediationRequestId()).isEqualTo("mrid");
    }


    @Test
    public void handleAdResponseLoaded_whenNoCreativesReturn_callOnNoAdsToShow() throws Exception {
        Response response = new Response();
        response.creatives = new ArrayList<>();

        subject.handleAdResponseLoaded(response);
        verify(mockMediationListener).onAdFailedToLoad();
    }

    @Test
    public void handleAdResponseLoaded_whenCreativesReturn_callOnAdsReady() throws Exception {
        Response response = convertJsonToResponse(SINGLE_LAYOUT_FIXTURE);
        subject.handleAdResponseLoaded(response);
        verify(mockMediationListener).onAdLoaded((List<ICreative>) anyObject());
    }

    @Test
    public void convertToCreatives_creativeIsNOTVideoCreative_whenPlacementAllowInstantPlayTrue_andCreativeForceClickToPlayTrue() throws JSONException {
        Response response = convertJsonToResponse(ALLOW_INSTANT_PLAY_TRUE_FORCE_CLICK_TO_PLAY_TRUE);

        List<ICreative> creativesList = subject.convertToCreatives(response);
        assertThat(creativesList.get(0) instanceof InstantPlayCreative).isFalse();
    }

    @Test
    public void convertToCreatives_creativeIsVideoCreative_whenPlacementAllowInstantPlayTrue_andCreativeForceClickToPlayFalse() throws JSONException {
        Response response = convertJsonToResponse(ALLOW_INSTANT_PLAY_TRUE_FORCE_CLICK_TO_PLAY_FALSE);

        List<ICreative> creativesList = subject.convertToCreatives(response);
        assertThat(creativesList.get(0) instanceof InstantPlayCreative).isTrue();
    }

    @Test
    public void convertToCreatives_creativeIsNOTVideoCreative_whenPlacementAllowInstantPlayFalse_andCreativeForceClickToPlayTrue() throws JSONException {
        Response response = convertJsonToResponse(ALLOW_INSTANT_PLAY_FALSE_FORCE_CLICK_TO_PLAY_TRUE);

        List<ICreative> creativesList = subject.convertToCreatives(response);
        assertThat(creativesList.get(0) instanceof InstantPlayCreative).isFalse();
    }

    @Test
    public void convertToCreatives_creativeIsNOTVideoCreative_whenPlacementAllowInstantPlayFalse_andCreativeForceClickToPlayFalse() throws JSONException {
        Response response = convertJsonToResponse(ALLOW_INSTANT_PLAY_FALSE_FORCE_CLICK_TO_PLAY_FALSE);

        List<ICreative> creativesList = subject.convertToCreatives(response);
        assertThat(creativesList.get(0) instanceof InstantPlayCreative).isFalse();
    }

    private Response convertJsonToResponse(String jsonPath) throws JSONException {
        JSONObject jsonResponse = new JSONObject(jsonPath);
        Response response = new Response();
        JSONObject jsonPlacement = jsonResponse.getJSONObject("placement");
        Response.Placement placement = new Response.Placement();
        placement.allowInstantPlay = jsonPlacement.optBoolean("allowInstantPlay", false);
        response.placement = placement;

        JSONArray creatives = jsonResponse.getJSONArray("creatives");
        response.creatives = new ArrayList<>(creatives.length());
        for (int i = 0; i < creatives.length(); i++) {
            JSONObject jsonCreative = creatives.getJSONObject(i);
            Response.Creative creative = new Response.Creative();
            JSONObject jsonCreativeInner = jsonCreative.getJSONObject("creative");
            creative.creative = new Response.Creative.CreativeInner();
            creative.creative.action = jsonCreativeInner.getString("action");
            creative.creative.forceClickToPlay = jsonCreativeInner.optBoolean("force_click_to_play", false);
            response.creatives.add(creative);
        }

        return response;
    }

    @Test
    public void generateQueryStringParams_WithCorrectParamsForDirectSell() {
        ASAPManager.AdResponse asapResponse = new ASAPManager.AdResponse();
        ASAPManager.AdResponse.Network network = new ASAPManager.AdResponse.Network();
        asapResponse.pkey = "fake-pkey";
        network.parameters = new JsonObject();
        network.parameters.addProperty(STXNetworkAdapter.KEY_TYPE, "creative_key");
        network.parameters.addProperty(STXNetworkAdapter.KEY_VALUE, "fake-ckey");

        List<Pair<String, String>> result = subject.generateQueryStringParams(asapResponse, network);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).first).isEqualTo(ASAPManager.PLACEMENT_KEY);
        assertThat(result.get(0).second).isEqualTo("fake-pkey");
        assertThat(result.get(1).first).isEqualTo("creative_key");
        assertThat(result.get(1).second).isEqualTo("fake-ckey");
    }

    @Test
    public void generateQueryStringParams_WithCorrectParamsForProgrammatic() {
        ASAPManager.AdResponse asapResponse = new ASAPManager.AdResponse();
        ASAPManager.AdResponse.Network network = new ASAPManager.AdResponse.Network();
        asapResponse.pkey = "fake-pkey";
        network.parameters = new JsonObject();
        network.parameters.addProperty(STXNetworkAdapter.KEY_TYPE, "stx_monetize");
        network.parameters.addProperty(STXNetworkAdapter.KEY_VALUE, "undefined");

        List<Pair<String, String>> result = subject.generateQueryStringParams(asapResponse, network);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).first).isEqualTo(ASAPManager.PLACEMENT_KEY);
        assertThat(result.get(0).second).isEqualTo("fake-pkey");
    }

    @Test
    public void generateQueryStringParams_WithNoKeyType_DefaultsToMonetize() {
        ASAPManager.AdResponse asapResponse = new ASAPManager.AdResponse();
        ASAPManager.AdResponse.Network network = new ASAPManager.AdResponse.Network();
        asapResponse.pkey = "fake-pkey";
        network.parameters = new JsonObject();
        network.parameters.addProperty(STXNetworkAdapter.KEY_VALUE, "undefined");

        List<Pair<String, String>> result = subject.generateQueryStringParams(asapResponse, network);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).first).isEqualTo(ASAPManager.PLACEMENT_KEY);
        assertThat(result.get(0).second).isEqualTo("fake-pkey");
    }

    @Test
    public void generateQueryStringParams_WithKeyTypeUndefined_DefaultsToMonetize() {
        ASAPManager.AdResponse asapResponse = new ASAPManager.AdResponse();
        ASAPManager.AdResponse.Network network = new ASAPManager.AdResponse.Network();
        asapResponse.pkey = "fake-pkey";
        network.parameters = new JsonObject();
        network.parameters.addProperty(STXNetworkAdapter.KEY_TYPE, "undefined");
        network.parameters.addProperty(STXNetworkAdapter.KEY_VALUE, "undefined");

        List<Pair<String, String>> result = subject.generateQueryStringParams(asapResponse, network);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).first).isEqualTo(ASAPManager.PLACEMENT_KEY);
        assertThat(result.get(0).second).isEqualTo("fake-pkey");
    }

    @Test
    public void generateQueryStringParams_WithDirectSellKeyValueUndefined_DefaultsToMonetize() {
        ASAPManager.AdResponse asapResponse = new ASAPManager.AdResponse();
        ASAPManager.AdResponse.Network network = new ASAPManager.AdResponse.Network();
        asapResponse.pkey = "fake-pkey";
        network.parameters = new JsonObject();
        network.parameters.addProperty(STXNetworkAdapter.KEY_TYPE, "creative_key");
        network.parameters.addProperty(STXNetworkAdapter.KEY_VALUE, "undefined");

        List<Pair<String, String>> result = subject.generateQueryStringParams(asapResponse, network);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).first).isEqualTo(ASAPManager.PLACEMENT_KEY);
        assertThat(result.get(0).second).isEqualTo("fake-pkey");
    }
}