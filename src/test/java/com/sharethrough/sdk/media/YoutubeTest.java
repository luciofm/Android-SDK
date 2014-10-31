package com.sharethrough.sdk.media;

import android.graphics.Bitmap;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.RendererTest;
import com.sharethrough.test.util.AdView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.robolectric.Robolectric.shadowOf;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class YoutubeTest {

    private Bitmap thumbnailBitmap;
    private Creative creative;

    @Before
    public void setUp() throws Exception {
        thumbnailBitmap = mock(Bitmap.class);
        creative = mock(Creative.class);
        when(creative.getThumbnailImage()).thenReturn(thumbnailBitmap);
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
        when(thumbnailBitmap.getWidth()).thenReturn(100);
        when(thumbnailBitmap.getHeight()).thenReturn(200);

        Youtube subject = new Youtube(creative);

        AdView adView = RendererTest.mockAdView();

        subject.overlayThumbnail(adView);

        ArgumentCaptor<ImageView> imageViewArgumentCaptor = ArgumentCaptor.forClass(ImageView.class);
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsArgumentCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);

        verify(adView.getThumbnail()).addView(imageViewArgumentCaptor.capture(), layoutParamsArgumentCaptor.capture());

        ImageView youtubeIcon = imageViewArgumentCaptor.getValue();
        assertThat(shadowOf(youtubeIcon).getImageResourceId()).isEqualTo(R.drawable.youtube_squared);
        int overlayDimensionMax = 25;

        FrameLayout.LayoutParams layoutParams = layoutParamsArgumentCaptor.getValue();
        assertThat(layoutParams.gravity).isEqualTo(Gravity.TOP | Gravity.LEFT);
        assertThat(layoutParams.width).isEqualTo(overlayDimensionMax);
        assertThat(layoutParams.height).isEqualTo(overlayDimensionMax);
    }
}