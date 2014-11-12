package com.sharethrough.sdk.media;

import android.graphics.Bitmap;
import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.RendererTest;
import com.sharethrough.sdk.dialogs.ShareableDialogTest;
import com.sharethrough.sdk.dialogs.VideoDialog;
import com.sharethrough.sdk.dialogs.WebViewDialogTest;
import com.sharethrough.test.util.AdView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class VineTest {

    private Bitmap thumbnailBitmap;
    private Creative creative;
    private BeaconService beaconService;
    private Vine subject;

    @Before
    public void setUp() throws Exception {
        thumbnailBitmap = mock(Bitmap.class);
        creative = mock(Creative.class);
        when(creative.getMediaUrl()).thenReturn("test_url");
        beaconService = mock(BeaconService.class);
        when(creative.getThumbnailImage()).thenReturn(thumbnailBitmap);

        subject = new Vine(creative, beaconService);
    }

    @Test
    public void thumbnailImageOverlaysVineIcon() throws Exception {
        assertThat(subject.getOverlayImageResourceId()).isEqualTo(R.drawable.vine);
    }

    @Test
    public void firesPlayBeacon() throws Exception {
        AdView adView = RendererTest.makeAdView();
        subject.fireAdClickBeacon(creative, adView);
        verify(beaconService).adClicked("vinePlay", creative, adView);
    }

    @Config(emulateSdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
    @Test
    public void clickingLoadsVideoDialog() throws Exception {
        subject.wasClicked(new View(Robolectric.application));
        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(VideoDialog.class);
    }
}