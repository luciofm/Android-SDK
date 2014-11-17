package com.sharethrough.sdk;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ProgressBar;
import com.sharethrough.android.sdk.R;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;

import static com.sharethrough.test.util.Misc.findViewOfType;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf;

public class BasicAdViewTest extends TestBase {

    private BasicAdView subject;
    private Sharethrough sharethrough;

    @Before
    public void setUp() throws Exception {
        subject = new BasicAdView(Robolectric.application);
        sharethrough = mock(Sharethrough.class);
        subject.showAd(sharethrough, R.layout.ad, R.id.title, R.id.description, R.id.advertiser, R.id.thumbnail);
    }

    @Test
    public void showAd_tellsSharethroughToShowTheAd() throws Exception {
        verify(sharethrough).putCreativeIntoAdView(eq(subject), any(Runnable.class));
    }

    @Test
    public void showsSpinner_untilAdIsReady() throws Exception {
        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(sharethrough).putCreativeIntoAdView(same(subject), runnableArgumentCaptor.capture());

        ProgressBar spinner = findViewOfType(ProgressBar.class, subject);
        assertThat(spinner).isNotNull();
        assertThat(spinner.getParent()).isNotSameAs(subject);

        assertThat(subject.getTitle()).isNull();
        runnableArgumentCaptor.getValue().run();
        assertThat(subject.getTitle()).isNotNull();

        assertThat(subject.getTitle().getParent().getParent()).isSameAs(spinner.getParent());
    }

    @Test
    public void hasOptoutButton_thatLinksToPrivacyInformation() throws Exception {
        View optout = subject.findViewWithTag("SHARETHROUGH PRIVACY INFORMATION");
        assertThat(optout.getParent()).isSameAs(subject);
        optout.performClick();
        assertThat(shadowOf(Robolectric.application).getNextStartedActivity()).isEqualTo(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.sharethrough.com/privacy-policy/")));
    }
}