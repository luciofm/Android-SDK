package com.sharethrough.sdk.media;

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.dialogs.ShareableDialogTest;
import com.sharethrough.sdk.dialogs.WebViewDialog;
import com.sharethrough.sdk.dialogs.WebViewDialogTest;
import com.sharethrough.test.util.TestAdView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowDialog;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Mockito.*;


@Config(shadows = {
        ShareableDialogTest.MenuInflaterShadow.class
})
public class ArticleTest extends TestBase {

    private BeaconService beaconService = mock(BeaconService.class);
    private Creative creative;
    private Article subject;
    private int feedPosition;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        subject = new Article(creative);
        feedPosition = 5;
    }

    @Test
    public void overlayThumbnail_doesNothing() throws Exception {
        TestAdView adView = mock(TestAdView.class);
        subject.overLayIconOverThumbnail(adView, mock(ImageView.class));
        verifyNoMoreInteractions(adView);
    }

    @Config(sdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
    @Test
    public void clickListener_opensWebViewDialog() throws Exception {
        when(creative.getMediaUrl()).thenReturn("http://test");
        subject.wasClicked(new ImageView(RuntimeEnvironment.application), beaconService, feedPosition);
        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(WebViewDialog.class);
    }

    @Test
    public void fireAdClickBeacon() throws Exception {
        TestAdView adView = RendererTest.makeAdView();
        subject.fireAdClickBeacon(creative, adView, beaconService, feedPosition);
        verify(beaconService).adClicked("articleView", creative, adView.getAdView(), feedPosition);
    }
}