package com.sharethrough.sdk.media;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.*;
import com.sharethrough.test.util.TestAdView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.robolectric.Robolectric.shadowOf;

public class MediaTest extends TestBase {
    private boolean isThumbnailOverlayCentered;
    private int overlayImageResourceId;
    @Mock private ImageView thumbnailImageView;
    @Mock private Creative creative;
    @Mock private Placement placement;
    @Mock private IAdView adView;
    @Mock private BeaconService beaconService;

    @Before
    public void setUp() throws Exception {
        overlayImageResourceId = R.drawable.youtube;

        when(thumbnailImageView.getWidth()).thenReturn(100);
        when(thumbnailImageView.getHeight()).thenReturn(200);
    }

    @Test
    public void whenCreativeHasNotBeenClicked_doesFireBeaconAndSetClickedOnCreative() throws Exception {
        when(creative.wasClicked()).thenReturn(false);
        Media subject = new TestMedia();
        subject.fireAdClickBeaconOnFirstClick(creative, adView, beaconService, 0, placement);

        verify(creative).setClicked();
        assertThat(((TestMedia)subject).isBeaconFired()).isTrue();
    }

    @Test
    public void whenCreativeHasBeenClicked_doesNotFireBeacon() throws Exception {
        when(creative.wasClicked()).thenReturn(true);
        Media subject = new TestMedia();
        subject.fireAdClickBeaconOnFirstClick(creative, adView, beaconService, 0, placement);

        assertThat(((TestMedia)subject).isBeaconFired()).isFalse();
    }

    @Test
    public void overlaysIcon() throws Exception {

        Media subject = new TestMedia();
        isThumbnailOverlayCentered = false;

        TestAdView adView = RendererTest.makeAdView();

        subject.swapMedia(adView, thumbnailImageView);

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

        TestAdView adView = RendererTest.makeAdView();

        subject.swapMedia(adView, thumbnailImageView);

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

        TestAdView adView = RendererTest.makeAdView();

        subject.swapMedia(adView, thumbnailImageView);

        verifyNoMoreInteractions(adView.getThumbnail());
    }

    private class TestMedia extends Media {
        private boolean beaconFired = false;

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
        public void wasClicked(View view, BeaconService beaconService, int feedPosition) {
        }

        @Override
        public void fireAdClickBeacon(Creative creative, IAdView adView, BeaconService beaconService, int feedPosition, Placement placement) {
            beaconFired = true;
        }

        public boolean isBeaconFired() {
            return beaconFired;
        }
    }
}