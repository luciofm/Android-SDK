package com.sharethrough.sdk.media.Vine;

import android.graphics.Bitmap;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.RendererTest;
import com.sharethrough.test.util.AdView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
        beaconService = mock(BeaconService.class);
        when(creative.getThumbnailImage()).thenReturn(thumbnailBitmap);

        subject = new Vine(creative, beaconService);
    }

    @Test
    public void thumbnailImageOverlaysVineIcon() throws Exception {
        assertThat(subject).isInstanceOf(ThumbnailOverlayingMedia.class);
    }

    @Test
    public void firesPlayBeacon() throws Exception {
        AdView adView = RendererTest.makeAdView();
        subject.fireAdClickBeacon(creative, adView);
        verify(beaconService).adClicked(Robolectric.application, "vinePlay", creative, adView);
    }
}