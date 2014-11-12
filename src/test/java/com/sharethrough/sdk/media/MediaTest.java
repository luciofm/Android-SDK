package com.sharethrough.sdk.media;

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
public class MediaTest {
    private Bitmap thumbnailBitmap;
    private Creative creative;
    private BeaconService beaconService;
    private boolean isThumbnailOverlayCentered;
    private int overlayImageResourceId;

    @Before
    public void setUp() throws Exception {
        thumbnailBitmap = mock(Bitmap.class);
        creative = mock(Creative.class);
        beaconService = mock(BeaconService.class);
        when(creative.getThumbnailImage()).thenReturn(thumbnailBitmap);
        overlayImageResourceId = R.drawable.youtube;
    }

    @Test
    public void overlaysIcon() throws Exception {
        when(thumbnailBitmap.getWidth()).thenReturn(100);
        when(thumbnailBitmap.getHeight()).thenReturn(200);

        Media subject = new TestMedia();
        isThumbnailOverlayCentered = false;

        AdView adView = RendererTest.makeAdView();

        subject.overlayThumbnail(adView);

        ArgumentCaptor<ImageView> imageViewArgumentCaptor = ArgumentCaptor.forClass(ImageView.class);
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsArgumentCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);

        verify(adView.getThumbnail()).addView(imageViewArgumentCaptor.capture(), layoutParamsArgumentCaptor.capture());

        ImageView youtubeIcon = imageViewArgumentCaptor.getValue();
        assertThat(shadowOf(youtubeIcon).getImageResourceId()).isEqualTo(overlayImageResourceId);
        int overlayDimensionMax = 25;

        FrameLayout.LayoutParams layoutParams = layoutParamsArgumentCaptor.getValue();
        assertThat(layoutParams.gravity).isEqualTo(Gravity.TOP | Gravity.LEFT);
        assertThat(layoutParams.width).isEqualTo(overlayDimensionMax);
        assertThat(layoutParams.height).isEqualTo(overlayDimensionMax);
    }

    @Test
    public void overlaysIconWhenCentered() throws Exception {
        Media subject = new TestMedia();
        isThumbnailOverlayCentered = true;

        AdView adView = RendererTest.makeAdView();

        subject.overlayThumbnail(adView);

        ArgumentCaptor<ImageView> imageViewArgumentCaptor = ArgumentCaptor.forClass(ImageView.class);
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsArgumentCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);

        verify(adView.getThumbnail()).addView(imageViewArgumentCaptor.capture(), layoutParamsArgumentCaptor.capture());

        FrameLayout.LayoutParams layoutParams = layoutParamsArgumentCaptor.getValue();
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void whenIconResourceIsInvalid_NothingHappens() throws Exception {
        Media subject = new TestMedia();
        overlayImageResourceId = -1;

        AdView adView = RendererTest.makeAdView();

        subject.overlayThumbnail(adView);

        verifyNoMoreInteractions(adView.getThumbnail());
    }

    private class TestMedia extends Media {
        @Override
        public int getOverlayImageResourceId() {
            return overlayImageResourceId;
        }

        @Override
        public boolean isThumbnailOverlayCentered() {
            return isThumbnailOverlayCentered;
        }

        @Override
        public Creative getCreative() {
            return creative;
        }

        @Override
        public void wasClicked(View view) {
        }

        @Override
        public <V extends View & IAdView> void fireAdClickBeacon(Creative creative, V adView) {
        }
    }
}