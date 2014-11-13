package com.sharethrough.sdk.media;

import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.RendererTest;
import com.sharethrough.sdk.dialogs.PinterestDialog;
import com.sharethrough.sdk.dialogs.ShareableDialogTest;
import com.sharethrough.sdk.dialogs.WebViewDialogTest;
import com.sharethrough.test.util.TestAdView;
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
public class PinterestTest {
    private Creative creative;
    private BeaconService beaconService;
    private Pinterest subject;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        when(creative.getMediaUrl()).thenReturn("http://ab.co/");
        beaconService = mock(BeaconService.class);

        subject = new Pinterest(creative);
    }

    @Test
    public void thumbnailImageOverlaysInstagramIcon() throws Exception {
        assertThat(subject.getOverlayImageResourceId()).isEqualTo(R.drawable.pinterest);
    }

    @Test
    @Config(emulateSdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
    public void clickingOpensPinterestDialog() throws Exception {
        subject.wasClicked(new View(Robolectric.application), beaconService);
        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(PinterestDialog.class);
    }

    @Test
    public void firesClickoutBeacon() throws Exception {
        TestAdView adView = RendererTest.makeAdView();
        subject.fireAdClickBeacon(creative, adView, beaconService);
        verify(beaconService).adClicked("clickout", creative, adView.getAdView());
    }
}