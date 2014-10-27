package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.test.util.AdView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import java.util.ArrayList;

import static com.sharethrough.test.util.Misc.assertImageViewFromBytes;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.robolectric.Robolectric.shadowOf;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class CreativeTest {

    private static final byte[] IMAGE_BYTES = new byte[]{1, 2, 3, 4};

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void clickingAdView_opensModal() throws Exception {
        Response.Creative responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.creative.title = "title";
        responseCreative.creative.description = "description";
        responseCreative.creative.advertiser = "advertiser";
        responseCreative.creative.mediaUrl = "http://youtu.be/123";
        Creative subject = new Creative(responseCreative, IMAGE_BYTES);

        AdView adView = mockAdView();

        subject.putIntoAdView(adView);

        ArgumentCaptor<View.OnClickListener> onClickListenerArgumentCaptor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(adView).setOnClickListener(onClickListenerArgumentCaptor.capture());

        onClickListenerArgumentCaptor.getValue().onClick(adView);

        ArrayList<View> viewsFound = new ArrayList<View>();

        YoutubeDialog modal = (YoutubeDialog) ShadowDialog.getLatestDialog();
        modal.getWindow().getDecorView().findViewsWithText(viewsFound, "title", View.FIND_VIEWS_WITH_TEXT);
        assertThat(viewsFound).hasSize(1);

        viewsFound.clear();
        modal.getWindow().getDecorView().findViewsWithText(viewsFound, "description", View.FIND_VIEWS_WITH_TEXT);
        assertThat(viewsFound).hasSize(1);

        viewsFound.clear();
        modal.getWindow().getDecorView().findViewsWithText(viewsFound, "advertiser", View.FIND_VIEWS_WITH_TEXT);
        assertThat(viewsFound).hasSize(1);
    }

    @Test
    public void whenAdIsYoutube() throws Exception {
        Response.Creative responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.creative.mediaUrl = "http://youtu.be/123456";

        AdView adView = mockAdView();

        Creative subject = new Creative(responseCreative, IMAGE_BYTES);
        subject.putIntoAdView(adView);

        ArgumentCaptor<View.OnClickListener> onClickListenerArgumentCaptor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(adView).setOnClickListener(onClickListenerArgumentCaptor.capture());

        onClickListenerArgumentCaptor.getValue().onClick(adView);

        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(YoutubeDialog.class);
    }

    @Test
    public void thumbnailImage_overlaysYoutubeIcon() throws Exception {
        Response.Creative responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.creative.title = "title";
        responseCreative.creative.description = "description";
        responseCreative.creative.advertiser = "advertiser";

        AdView adView = mockAdView();

        Creative subject = new Creative(responseCreative, IMAGE_BYTES);
        subject.putIntoAdView(adView);

        ArgumentCaptor<View> imageViewArgumentCaptor = ArgumentCaptor.forClass(View.class);
        FrameLayout thumbnailFrame = adView.getThumbnail();
        verify(thumbnailFrame).addView(imageViewArgumentCaptor.capture());

        ImageView thumbnailImageView = (ImageView) imageViewArgumentCaptor.getValue();
        assertImageViewFromBytes(thumbnailImageView, IMAGE_BYTES);


        imageViewArgumentCaptor = ArgumentCaptor.forClass(View.class);
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsArgumentCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(thumbnailFrame).addView(imageViewArgumentCaptor.capture(), layoutParamsArgumentCaptor.capture());
        ImageView youtubeIcon = (ImageView) imageViewArgumentCaptor.getValue();
        assertThat(shadowOf(youtubeIcon).getImageResourceId()).isEqualTo(R.drawable.youtube_squared);
        int overlayDimensionMax = 25;

        FrameLayout.LayoutParams layoutParams = layoutParamsArgumentCaptor.getValue();
        assertThat(layoutParams.gravity).isEqualTo(Gravity.TOP | Gravity.LEFT);
        assertThat(layoutParams.width).isEqualTo(overlayDimensionMax);
        assertThat(layoutParams.height).isEqualTo(overlayDimensionMax);
    }

    private AdView mockAdView() {
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