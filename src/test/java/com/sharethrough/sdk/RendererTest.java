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

    private static final byte[] IMAGE_BYTES = new byte[]{1, 2, 3, 4};
    private Renderer subject;

    @Before
    public void setUp() throws Exception {
        subject = new Renderer();
    }

    @Test
    public void showsTitleDescriptionAdvertiserAndThumbnailWithOverlay() throws Exception {
        Creative creative = mock(Creative.class);
        when(creative.getTitle()).thenReturn("title");
        when(creative.getDescription()).thenReturn("description");
        when(creative.getAdvertiser()).thenReturn("advertiser");
        when(creative.getTitle()).thenReturn("title");
        Bitmap bitmap = mock(Bitmap.class);
        when(creative.getThumbnailImage()).thenReturn(bitmap);
        Creative.Media media = mock(Creative.Media.class);
        when(media.getClickListener()).thenReturn(mock(View.OnClickListener.class));
        when(creative.getMedia()).thenReturn(media);

        AdView adView = mockAdView();

        subject.putCreativeIntoAdView(adView, creative);

        verify(adView.getTitle()).setText("title");
        verify(adView.getDescription()).setText("description");
        verify(adView.getAdvertiser()).setText("advertiser");
        ArgumentCaptor<View> thumbnailViewCaptor = ArgumentCaptor.forClass(View.class);
        verify(adView.getThumbnail(), atLeastOnce()).addView(thumbnailViewCaptor.capture(), any(FrameLayout.LayoutParams.class));

        ImageView thumbnailImageView = (ImageView) thumbnailViewCaptor.getValue();
        BitmapDrawable bitmapDrawable = (BitmapDrawable) thumbnailImageView.getDrawable();
        assertThat(bitmapDrawable.getBitmap()).isEqualTo(bitmap);

        verify(media).overlayThumbnail(adView);
        verify(adView).setOnClickListener(media.getClickListener());
    }

    @Config(shadows = {YoutubeDialogTest.MyMenuInflatorShadow.class})
    @Test
    public void whenAdIsYoutube_clickingOpensTheYoutubeDialog() throws Exception {
        Response.Creative responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.creative.mediaUrl = "http://youtu.be/123456";

        AdView adView = mockAdView();

        Creative.Media media = mock(Creative.Media.class);
        Creative creative = mock(Creative.class);
        when(creative.getMedia()).thenReturn(media);

        subject.putCreativeIntoAdView(adView, creative);

        verify(adView).setOnClickListener(media.getClickListener());
    }

    public static AdView mockAdView() {
        AdView adView = mock(AdView.class);
        when(adView.getTitle()).thenReturn(mock(TextView.class));
        when(adView.getDescription()).thenReturn(mock(TextView.class));
        when(adView.getAdvertiser()).thenReturn(mock(TextView.class));
        when(adView.getThumbnail()).thenReturn(mock(FrameLayout.class));
        when(adView.getThumbnail().getContext()).thenReturn(Robolectric.application);
        when(adView.getContext()).thenReturn(Robolectric.application);
        return adView;
    }
}