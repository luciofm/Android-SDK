package com.sharethrough.sdk;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.sharethrough.test.util.AdView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class RendererTest {

    private Renderer subject;
    private Creative creative;
    private Bitmap bitmap;
    private Creative.Media media;
    private AdView adView;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        when(creative.getTitle()).thenReturn("title");
        when(creative.getDescription()).thenReturn("description");
        when(creative.getAdvertiser()).thenReturn("advertiser");
        when(creative.getTitle()).thenReturn("title");
        bitmap = mock(Bitmap.class);
        when(creative.getThumbnailImage()).thenReturn(bitmap);
        media = mock(Creative.Media.class);
        when(media.getClickListener()).thenReturn(mock(View.OnClickListener.class));
        when(creative.getMedia()).thenReturn(media);

        adView = makeAdView();

        subject = new Renderer();
    }

    @Test
    public void onUIthread_showsTitleDescriptionAdvertiserAndThumbnailWithOverlay() throws Exception {
        Robolectric.pauseMainLooper();

        subject.putCreativeIntoAdView(adView, creative);

        verifyNoMoreInteractions(adView.getTitle());

        Robolectric.unPauseMainLooper();

        verify(adView.getTitle()).setText("title");
        verify(adView.getDescription()).setText("description");
        verify(adView.getAdvertiser()).setText("advertiser");
        ArgumentCaptor<View> thumbnailViewCaptor = ArgumentCaptor.forClass(View.class);
        verify(adView.getThumbnail(), atLeastOnce()).addView(thumbnailViewCaptor.capture(), any(FrameLayout.LayoutParams.class));

        ImageView thumbnailImageView = (ImageView) thumbnailViewCaptor.getValue();
        BitmapDrawable bitmapDrawable = (BitmapDrawable) thumbnailImageView.getDrawable();
        assertThat(bitmapDrawable.getBitmap()).isEqualTo(bitmap);

        verify(media).overlayThumbnail(adView);
    }

    @Test
    public void whenAdIsClicked_firesMediaBeacon_andMediaClickListener() throws Exception {
        subject.putCreativeIntoAdView(adView, creative);

        adView.performClick();

        verify(media.getClickListener()).onClick(adView);
        verify(media).fireAdClickBeacon(creative, adView);
    }

    public static AdView makeAdView() {
        return new AdView(Robolectric.application) {
            private final FrameLayout thumbnail = mock(FrameLayout.class);
            private final TextView advertiser = mock(TextView.class);
            private final TextView description = mock(TextView.class);
            private final TextView title = mock(TextView.class);

            @Override
            public TextView getTitle() {
                return title;
            }

            @Override
            public TextView getDescription() {
                return description;
            }

            @Override
            public TextView getAdvertiser() {
                return advertiser;
            }

            @Override
            public FrameLayout getThumbnail() {
                return thumbnail;
            }
        };
    }
}