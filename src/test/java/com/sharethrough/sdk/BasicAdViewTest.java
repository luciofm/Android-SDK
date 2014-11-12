package com.sharethrough.sdk;

import com.sharethrough.android.sdk.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class BasicAdViewTest {
    @Test
    public void showAd_tellsSharethroughToShowTheAd() throws Exception {
        BasicAdView subject = new BasicAdView(Robolectric.application);
        Sharethrough sharethrough = mock(Sharethrough.class);
        subject.showAd(sharethrough, Robolectric.application, R.layout.dialog, 2, 3, 4, 5);
        verify(sharethrough).putCreativeIntoAdView(subject);
    }
}