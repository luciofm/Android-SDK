package com.sharethrough.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class SharethroughTest {

    private Sharethrough subject;

    @Test
    public void settingKey_loadsAdsFromServer() throws Exception {
        // TODO: use fixture
//        FileInputStream fixture = Robolectric.application.openFileInput("ads.json");
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", "http://btlr.sharethrough.com/v3?placement_key=" + key,
                new TestHttpResponse(200, "{\n" +
                        "  \"placement\": {\n" +
                        "    \"template\": \"<div/>\"," +
                        "    \"layout\": \"single\"\n" +
                        "  },\n" +
                        "  \"creatives\": [\n" +
                        "    {\n" +
                        "      \"price\": 121460,\n" +
                        "      \"signature\": \"c19\",\n" +
                        "      \"creative\": {\n" +
                        "        \"creative_key\": \"469cc02b\",\n" +
                        "        \"description\": \"Description.\",\n" +
                        "        \"media_url\": \"mediaURL\",\n" +
                        "        \"share_url\": \"shareURL\",\n" +
                        "        \"variant_key\": \"15577\",\n" +
                        "        \"advertiser\": \"Advertiser\",\n" +
                        "        \"beacons\": {\n" +
                        "          \"visible\": [\"visibleBeacon\"],\n" +
                        "          \"click\": [\"clickBeacon\"],\n" +
                        "          \"play\": []\n" +
                        "        },\n" +
                        "        \"thumbnail_url\": \"thumbnailURL\",\n" +
                        "        \"title\": \"title\",\n" +
                        "        \"action\": \"clickout\"\n" +
                        "      },\n" +
                        "      \"priceType\": \"CPE\",\n" +
                        "      \"version\": 1\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"));

        subject = new Sharethrough(key);
        Response.Creative actualCreative = subject.getCreative();
        assertThat(actualCreative).isNotNull();
        assertThat(actualCreative.price).isEqualTo(121460);
    }

    @Test(expected = KeyRequiredException.class)
    public void notSettingKey_throwsExceptionWhenAdIsRequested() {
        subject = new Sharethrough(null);
        subject.getCreative();
    }

    @Test
    public void whenServerReturns204_returnsNull() throws Exception {
        String key = "abc";
        Robolectric.addHttpResponseRule("GET", "http://btlr.sharethrough.com/v3?placement_key=" + key,
                new TestHttpResponse(204, "I got nothing for ya"));
        subject = new Sharethrough(key);
        Response.Creative actualCreative = subject.getCreative();
        assertThat(actualCreative).isNull();
    }
}