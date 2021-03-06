package com.sharethrough.sdk.media;

import android.graphics.Bitmap;
import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.dialogs.ShareableDialogTest;
import com.sharethrough.sdk.dialogs.VideoDialog;
import com.sharethrough.sdk.dialogs.WebViewDialogTest;
import com.sharethrough.test.util.TestAdView;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class VineTest extends TestBase {

    private Bitmap thumbnailBitmap;
    private Creative creative;
    private BeaconService beaconService;
    private Vine subject;
    private int feedPosition;

    @Before
    public void setUp() throws Exception {
        thumbnailBitmap = mock(Bitmap.class);
        creative = mock(Creative.class);
        when(creative.getMediaUrl()).thenReturn("test_url");
        beaconService = mock(BeaconService.class);
        feedPosition = 5;

        subject = new Vine(creative);
    }

    @Test
    public void thumbnailImageOverlaysVineIcon() throws Exception {
        assertThat(subject.getOverlayImageResourceId()).isEqualTo(R.drawable.vine);
    }

    @Test
    public void firesPlayBeacon() throws Exception {
        TestAdView adView = RendererTest.makeAdView();
        subject.fireAdClickBeacon(creative, adView, beaconService, feedPosition);
        verify(beaconService).adClicked("vinePlay", creative, adView.getAdView(), feedPosition);
    }

    @Config(sdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
    @Test
    public void clickingLoadsVideoDialog() throws Exception {
        subject.wasClicked(new View(RuntimeEnvironment.application), beaconService, feedPosition);
        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(VideoDialog.class);
    }
}