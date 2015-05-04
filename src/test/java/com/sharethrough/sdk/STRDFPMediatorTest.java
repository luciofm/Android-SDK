package com.sharethrough.sdk;

import android.app.Activity;
import com.google.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.customevent.CustomEventBannerListener;
import org.junit.Before;
import org.robolectric.Robolectric;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class STRDFPMediatorTest extends TestBase {

    private STRDFPMediator subject;
    private String dfpPath;
    private HashSet<String> keywords;


    @Before
    public void SetUp() {
        subject = new STRDFPMediator();

        keywords = new HashSet<>();
        dfpPath = "/123/dfp/path";
        keywords.add(dfpPath);
    }

    @org.junit.Test
    public void testRequestBannerAd_forGMS_6() throws Exception {
        com.google.android.gms.ads.mediation.MediationAdRequest mediationAdRequest = mock(com.google.android.gms.ads.mediation.MediationAdRequest.class);
        when(mediationAdRequest.getKeywords()).thenReturn(keywords);

        subject.requestBannerAd(Robolectric.application, mock(CustomEventBannerListener.class), "creativeKey=abc123", AdSize.SMART_BANNER, mediationAdRequest, null);

        HashMap<String, String> expectedHashMap = new HashMap<>();
        expectedHashMap.put("creativeKey", "abc123");

        assertThat(Sharethrough.popDFPKeys(dfpPath)).isEqualTo(expectedHashMap);
    }

    @org.junit.Test
    public void testRequestBannerAd_forGMS_5() throws Exception {
        com.google.ads.mediation.MediationAdRequest mediationAdRequest = new
                com.google.ads.mediation.MediationAdRequest(new Date(), AdRequest.Gender.FEMALE, keywords, true, null);

        Activity myActivity = Robolectric.buildActivity(Activity.class).create().get();

        subject.requestBannerAd(
                mock(com.google.ads.mediation.customevent.CustomEventBannerListener.class),
                myActivity,
                "tag",
                "campaignKey=xyz789",
                com.google.ads.AdSize.SMART_BANNER,
                mediationAdRequest,
                null);

        HashMap<String, String> expectedHashMap = new HashMap<>();
        expectedHashMap.put("campaignKey", "xyz789");

        assertThat(Sharethrough.popDFPKeys(dfpPath)).isEqualTo(expectedHashMap);
    }
}