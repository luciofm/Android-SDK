package com.sharethrough.sdk.media.Vine;

import android.graphics.Bitmap;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class VineTest {

    private Bitmap thumbnailBitmap;
    private Creative creative;
    private BeaconService beaconService;

    @Before
    public void setUp() throws Exception {
        thumbnailBitmap = mock(Bitmap.class);
        creative = mock(Creative.class);
        beaconService = mock(BeaconService.class);
        when(creative.getThumbnailImage()).thenReturn(thumbnailBitmap);
    }

    @Test
    public void thumbnailImageOverlaysVineIcon() throws Exception {
        Vine subject = new Vine(creative, beaconService);
        assertThat(subject).isInstanceOf(ThumbnailOverlayingMedia.class);
    }
}