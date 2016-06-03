package com.sharethrough.sdk.media;

import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.Placement;
import com.sharethrough.sdk.TestBase;
import com.sharethrough.sdk.dialogs.ShareableDialogTest;
import com.sharethrough.sdk.dialogs.VideoDialog;
import com.sharethrough.sdk.dialogs.WebViewDialogTest;
import com.sharethrough.test.util.TestAdView;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class HostedVideoTest extends TestBase {

    @Test
    public void overlaysImage() throws Exception {
        assertThat(new HostedVideo(mock(Creative.class)).getOverlayImageResourceId()).isEqualTo(R.drawable.non_yt_play);
    }

    @Config(sdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
    @Test
    public void whenClicked_opensVideoDialog() throws Exception {
        Creative creative = mock(Creative.class);
        when(creative.getMediaUrl()).thenReturn("http://ab.co");
        HostedVideo subject = new HostedVideo(creative);
        int feedPosition = 5;

        subject.wasClicked(new View(RuntimeEnvironment.application), mock(BeaconService.class), feedPosition);

        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(VideoDialog.class);
    }

    @Test
    public void firesAdClickedBeacon() throws Exception {
        Creative creative = mock(Creative.class);
        HostedVideo subject = new HostedVideo(creative);
        int feedPosition = 5;

        BeaconService beaconService = mock(BeaconService.class);
        TestAdView adView = mock(TestAdView.class);
        subject.fireAdClickBeacon(creative, adView, beaconService, feedPosition);

        verify(beaconService).adClicked("videoPlay", creative, adView.getAdView(), feedPosition);
    }
}