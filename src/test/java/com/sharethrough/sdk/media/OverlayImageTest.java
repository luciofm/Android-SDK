package com.sharethrough.sdk.media;

import android.widget.FrameLayout;
import android.widget.ImageView;
import com.sharethrough.sdk.TestBase;
import org.junit.Test;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class OverlayImageTest extends TestBase {

    @Test
    public void onMeasureShouldReferenceThumbnailSize() {
        ImageView mockThumbnail = mock(ImageView.class);
        when(mockThumbnail.getTag()).thenReturn(Media.THUMBNAIL);
        when(mockThumbnail.getMeasuredHeight()).thenReturn(100);
        when(mockThumbnail.getMeasuredWidth()).thenReturn(100);

        FrameLayout thumbnailLayout = new FrameLayout(Robolectric.application);

        OverlayImage subject = new OverlayImage(Robolectric.application);

        thumbnailLayout.addView(mockThumbnail);
        thumbnailLayout.addView(subject);

        subject.measure(Integer.MAX_VALUE, Integer.MAX_VALUE);

        verify(mockThumbnail).getTag();
        verify(mockThumbnail).getMeasuredWidth();
        verify(mockThumbnail).getMeasuredHeight();

        assertThat(subject.getMeasuredHeight()).isEqualTo(25);
        assertThat(subject.getMeasuredWidth()).isEqualTo(25);

    }

}