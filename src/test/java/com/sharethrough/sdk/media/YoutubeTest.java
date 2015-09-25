package com.sharethrough.sdk.media;

import android.graphics.Bitmap;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.*;
import com.sharethrough.test.util.TestAdView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class YoutubeTest extends TestBase {

    private Bitmap thumbnailBitmap;
    private Creative creative;
    private BeaconService beaconService;
    private int feedPosition;

    @Before
    public void setUp() throws Exception {
        thumbnailBitmap = mock(Bitmap.class);
        creative = mock(Creative.class);
        beaconService = mock(BeaconService.class);
        feedPosition = 5;
    }

    @Test
    public void canGetIdFromShortUrl() throws Exception {
        when(creative.getMediaUrl()).thenReturn("http://youtu.be/12345");
        assertThat(new Youtube(creative).getId()).isEqualTo("12345");
    }

    @Test
    public void canGetIdFromRegularHttpUrl() throws Exception {
        when(creative.getMediaUrl()).thenReturn("http://youtube.com/watch?v=12345&autoplay=true");
        assertThat(new Youtube(creative).getId()).isEqualTo("12345");
        when(creative.getMediaUrl()).thenReturn("http://youtube.com/watch?autoplay=true&v=12345");
        assertThat(new Youtube(creative).getId()).isEqualTo("12345");
    }

    @Test
    public void canGetIdFromRegularHttpsUrl() throws Exception {
        when(creative.getMediaUrl()).thenReturn("https://youtube.com/watch?v=12345&autoplay=true");
        assertThat(new Youtube(creative).getId()).isEqualTo("12345");
        when(creative.getMediaUrl()).thenReturn("https://youtube.com/watch?autoplay=true&v=12345");
        assertThat(new Youtube(creative).getId()).isEqualTo("12345");
    }

    @Test
    public void canGetIdFromEmbedUrl() throws Exception {
        when(creative.getMediaUrl()).thenReturn("http://www.youtube.com/embed/12345?autoplay=1&vq=small");
        assertThat(new Youtube(creative).getId()).isEqualTo("12345");
    }

    @Test
    public void thumbnailImageOverlaysYoutubeIcon() throws Exception {
        Youtube subject = new Youtube(creative);
        assertThat(subject.getOverlayImageResourceId()).isEqualTo(R.drawable.youtube);
    }

    @Test
    public void fireAdClickedBeacon() throws Exception {
        Youtube subject = new Youtube(creative);

        TestAdView adView = RendererTest.makeAdView();
        subject.fireAdClickBeacon(creative, adView, beaconService, feedPosition);
        verify(beaconService).adClicked("youtubePlay", creative, adView.getAdView(), feedPosition);
    }
}