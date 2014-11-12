package com.sharethrough.sdk.media;

import android.graphics.Bitmap;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.RendererTest;
import com.sharethrough.test.util.AdView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class YoutubeTest {

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
    public void canGetIdFromShortUrl() throws Exception {
        when(creative.getMediaUrl()).thenReturn("http://youtu.be/12345");
        assertThat(new Youtube(creative, beaconService).getId()).isEqualTo("12345");
    }

    @Test
    public void canGetIdFromRegularHttpUrl() throws Exception {
        when(creative.getMediaUrl()).thenReturn("http://youtube.com/watch?v=12345&autoplay=true");
        assertThat(new Youtube(creative, beaconService).getId()).isEqualTo("12345");
        when(creative.getMediaUrl()).thenReturn("http://youtube.com/watch?autoplay=true&v=12345");
        assertThat(new Youtube(creative, beaconService).getId()).isEqualTo("12345");
    }

    @Test
    public void canGetIdFromRegularHttpsUrl() throws Exception {
        when(creative.getMediaUrl()).thenReturn("https://youtube.com/watch?v=12345&autoplay=true");
        assertThat(new Youtube(creative, beaconService).getId()).isEqualTo("12345");
        when(creative.getMediaUrl()).thenReturn("https://youtube.com/watch?autoplay=true&v=12345");
        assertThat(new Youtube(creative, beaconService).getId()).isEqualTo("12345");
    }

    @Test
    public void canGetIdFromEmbedUrl() throws Exception {
        when(creative.getMediaUrl()).thenReturn("http://www.youtube.com/embed/12345?autoplay=1&vq=small");
        assertThat(new Youtube(creative, beaconService).getId()).isEqualTo("12345");
    }

    @Test
    public void thumbnailImageOverlaysYoutubeIcon() throws Exception {
        Youtube subject = new Youtube(creative, beaconService);
        assertThat(subject.getOverlayImageResourceId()).isEqualTo(R.drawable.youtube_squared);
    }

    @Test
    public void fireAdClickedBeacon() throws Exception {
        Youtube subject = new Youtube(creative, beaconService);

        AdView adView = RendererTest.makeAdView();
        subject.fireAdClickBeacon(creative, adView);
        verify(beaconService).adClicked("youtubePlay", creative, adView);
    }
}