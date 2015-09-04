package com.sharethrough.sdk.media;

import android.widget.ImageView;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.dialogs.ShareableDialogTest;
import com.sharethrough.sdk.dialogs.WebViewDialog;
import com.sharethrough.sdk.dialogs.WebViewDialogTest;
import com.sharethrough.test.util.TestAdView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Mockito.*;

public class ArticleTest extends TestBase {

    private BeaconService beaconService = mock(BeaconService.class);
    private Creative creative;
    private Article subject;
    private int feedPosition;
    @Mock private Placement placement;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        subject = new Article(creative);
        feedPosition = 5;
    }

    @Test
    public void overlayThumbnail_doesNothing() throws Exception {
        TestAdView adView = mock(TestAdView.class);
        subject.swapMedia(adView, mock(ImageView.class));
        verifyNoMoreInteractions(adView);
    }

    @Config(emulateSdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
    @Test
    public void clickListener_opensWebViewDialog() throws Exception {
        when(creative.getMediaUrl()).thenReturn("http://test");
        subject.wasClicked(new ImageView(Robolectric.application), beaconService, feedPosition);
        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(WebViewDialog.class);
    }

    @Test
    public void fireAdClickBeacon() throws Exception {
        TestAdView adView = RendererTest.makeAdView();
        subject.fireAdClickBeacon(creative, adView, beaconService, feedPosition, placement);
        verify(beaconService).adClicked("articleView", creative, adView.getAdView(), feedPosition, placement);
    }
}