package com.sharethrough.sdk.media;

import android.widget.ImageView;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.RendererTest;
import com.sharethrough.sdk.dialogs.ShareableDialogTest;
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
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class ClickoutTest {

    private BeaconService beaconService = mock(BeaconService.class);
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
        subject.overlayThumbnail(adView, mock(ImageView.class));
        verifyNoMoreInteractions(adView);
    }

    @Config(emulateSdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
    @Test
    public void clickListener_opensWebViewDialog() throws Exception {
        subject.wasClicked(new ImageView(Robolectric.application), beaconService);
        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(WebViewDialog.class);
    }

    @Test
    public void fireAdClickBeacon() throws Exception {
        AdView adView = RendererTest.makeAdView();
        subject.fireAdClickBeacon(creative, adView, beaconService);
        verify(beaconService).adClicked("clickout", creative, adView);
    }
}