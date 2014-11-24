package com.sharethrough.sdk;

import android.app.Activity;
import android.widget.ProgressBar;
import com.sharethrough.android.sdk.R;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import static com.sharethrough.test.util.Misc.findViewOfType;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;

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
}