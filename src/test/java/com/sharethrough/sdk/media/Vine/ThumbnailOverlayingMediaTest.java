package com.sharethrough.sdk.media.Vine;

import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
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

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class ThumbnailOverlayingMediaTest {
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
    public void thumbnailImageOverlaysYoutubeIcon() throws Exception {
        when(thumbnailBitmap.getWidth()).thenReturn(100);
        when(thumbnailBitmap.getHeight()).thenReturn(200);

        ThumbnailOverlayingMedia subject = new ThumbnailOverlayingMedia() {
            @Override
            protected int getOverlayImageResourceId() {
                return R.drawable.youtube_squared;
            }

            @Override
            protected Creative getCreative() {
                return creative;
            }

            @Override
            public View.OnClickListener getClickListener() {
                return null;
            }

            @Override
            public void fireAdClickBeacon(Creative creative, IAdView adView) {
            }
        };

        AdView adView = RendererTest.makeAdView();

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