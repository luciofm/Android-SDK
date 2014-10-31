package com.sharethrough.sdk.media;

import android.widget.ImageView;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.dialogs.WebViewDialog;
import com.sharethrough.sdk.dialogs.WebViewDialogTest;
import com.sharethrough.test.util.AdView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class ClickoutTest {

    private Creative creative;
    private Clickout subject;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        subject = new Clickout(creative);
    }

    @Test
    public void overlayThumbnail_doesNothing() throws Exception {
        AdView adView = mock(AdView.class);
        subject.overlayThumbnail(adView);
        verifyNoMoreInteractions(adView);
    }

    @Config(emulateSdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, WebViewDialogTest.MenuInflaterShadow.class})
    @Test
    public void clickListener_opensWebViewDialog() throws Exception {
        subject.getClickListener().onClick(new ImageView(Robolectric.application));
        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(WebViewDialog.class);
    }
}