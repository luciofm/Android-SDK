package com.sharethrough.sdk;

import android.widget.ProgressBar;
import com.sharethrough.android.sdk.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.sharethrough.test.util.Misc.findViewOfType;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
        verify(sharethrough).putCreativeIntoAdView(eq(subject), any(Runnable.class));
    }

    @Test
    public void showsSpinner_untilAdIsReady() throws Exception {
        BasicAdView subject = new BasicAdView(Robolectric.application);
        Sharethrough sharethrough = mock(Sharethrough.class);
        subject.showAd(sharethrough, Robolectric.application, R.layout.ad, R.id.title, R.id.description, R.id.advertiser, R.id.thumbnail);
        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(sharethrough).putCreativeIntoAdView(eq(subject), runnableArgumentCaptor.capture());

        assertThat(findViewOfType(ProgressBar.class, subject)).isNotNull();
        assertThat(subject.getTitle()).isNull();

        runnableArgumentCaptor.getValue().run();

        assertThat(subject.getTitle()).isNotNull();
    }
}