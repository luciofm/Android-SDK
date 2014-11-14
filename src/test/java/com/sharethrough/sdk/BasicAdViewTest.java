package com.sharethrough.sdk;

import android.widget.ProgressBar;
import com.sharethrough.android.sdk.R;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;

import static com.sharethrough.test.util.Misc.findViewOfType;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BasicAdViewTest extends TestBase {
    @Test
    public void showAd_tellsSharethroughToShowTheAd() throws Exception {
        BasicAdView subject = new BasicAdView(Robolectric.application);
        Sharethrough sharethrough = mock(Sharethrough.class);
        subject.showAd(sharethrough, R.layout.dialog, 2, 3, 4, 5);
        verify(sharethrough).putCreativeIntoAdView(eq(subject), any(Runnable.class));
    }

    @Test
    public void showsSpinner_untilAdIsReady() throws Exception {
        BasicAdView subject = new BasicAdView(Robolectric.application);
        Sharethrough sharethrough = mock(Sharethrough.class);
        subject.showAd(sharethrough, R.layout.ad, R.id.title, R.id.description, R.id.advertiser, R.id.thumbnail);
        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(sharethrough).putCreativeIntoAdView(eq(subject), runnableArgumentCaptor.capture());

        assertThat(findViewOfType(ProgressBar.class, subject)).isNotNull();
        assertThat(subject.getTitle()).isNull();

        runnableArgumentCaptor.getValue().run();

        assertThat(subject.getTitle()).isNotNull();
    }
}