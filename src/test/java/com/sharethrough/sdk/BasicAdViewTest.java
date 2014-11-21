package com.sharethrough.sdk;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import com.sharethrough.android.sdk.R;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import static com.sharethrough.test.util.Misc.findViewOfType;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.robolectric.Robolectric.shadowOf;

public class BasicAdViewTest extends TestBase {

    private BasicAdView subject;

    @Before
    public void setUp() throws Exception {
        subject = new BasicAdView(Robolectric.application);
        subject.layout(0, 0, 1000, 100);
        subject.prepareWithResourceIds(R.layout.ad, R.id.title, R.id.description, R.id.advertiser, R.id.thumbnail);
    }

    @Test
    public void showsSpinner_untilAdIsReady() throws Exception {
        ProgressBar spinner = findViewOfType(ProgressBar.class, subject);
        assertThat(spinner.getParent()).isSameAs(subject);

        Robolectric.buildActivity(Activity.class).create().start().visible().resume().get().setContentView(subject);

        assertThat(subject.getTitle()).isNotShown();
        subject.adReady();
        assertThat(subject.getTitle()).isShown();

        assertThat(subject.getTitle().getParent().getParent()).isSameAs(spinner.getParent());
    }

    @Test
    public void onceAdIsReady_showsProportionalOptoutButton_thatLinksToPrivacyInformation() throws Exception {
        subject.adReady();

        View optout = subject.findViewWithTag("SHARETHROUGH PRIVACY INFORMATION");
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) optout.getLayoutParams();
        assertThat(layoutParams.height).isEqualTo(subject.getHeight() / 6);
        assertThat(layoutParams.width).isEqualTo(subject.getHeight() / 6);
        assertThat(layoutParams.rightMargin).isEqualTo(subject.getHeight() / 18);
        assertThat(layoutParams.bottomMargin).isEqualTo(subject.getHeight() / 18);
        assertThat(optout.getParent()).isSameAs(subject);
        optout.performClick();
        assertThat(shadowOf(Robolectric.application).getNextStartedActivity()).isEqualTo(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.sharethrough.com/privacy-policy/")));
    }
}