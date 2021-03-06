package com.sharethrough.sdk.media;

import android.view.View;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.dialogs.PinterestDialog;
import com.sharethrough.sdk.dialogs.ShareableDialogTest;
import com.sharethrough.sdk.dialogs.WebViewDialogTest;
import com.sharethrough.test.util.TestAdView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PinterestTest extends TestBase {
    private Creative creative;
    private BeaconService beaconService;
    private Pinterest subject;
    private int feedPosition;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        when(creative.getMediaUrl()).thenReturn("http://ab.co/");
        beaconService = mock(BeaconService.class);
        feedPosition = 5;


        subject = new Pinterest(creative);
    }

    @Test
    public void thumbnailImageOverlaysInstagramIcon() throws Exception {
        assertThat(subject.getOverlayImageResourceId()).isEqualTo(R.drawable.pinterest);
    }

    @Test
    @Config(sdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
    public void clickingOpensPinterestDialog() throws Exception {
        subject.wasClicked(new View(RuntimeEnvironment.application), beaconService, feedPosition);
        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(PinterestDialog.class);
    }

    @Test
    public void firesClickoutBeacon() throws Exception {
        TestAdView adView = RendererTest.makeAdView();
        subject.fireAdClickBeacon(creative, adView, beaconService, feedPosition);
        verify(beaconService).adClicked("clickout", creative, adView.getAdView(), feedPosition);
    }
}