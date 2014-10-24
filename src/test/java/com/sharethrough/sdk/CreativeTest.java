package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Build;
import android.view.View;
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

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class CreativeTest {

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void clickingAdView_opensModal() throws Exception {
        Response.Creative responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.creative.title = "title";
        responseCreative.creative.description = "description";
        responseCreative.creative.advertiser = "advertiser";
        responseCreative.creative.mediaUrl = "http://youtu.be/123";
        Creative subject = new Creative(responseCreative, new byte[] {1, 2, 3, 4});

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

        Creative subject = new Creative(responseCreative, new byte[] {1, 2, 3, 4});
        subject.putIntoAdView(adView);

        ArgumentCaptor<View.OnClickListener> onClickListenerArgumentCaptor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(adView).setOnClickListener(onClickListenerArgumentCaptor.capture());

        onClickListenerArgumentCaptor.getValue().onClick(adView);

        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(YoutubeDialog.class);
    }

    @Test
    public void getThumbnailImage_overlaysYoutubeIcon() throws Exception {
        Response.Creative responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.creative.title = "title";
        responseCreative.creative.description = "description";
        responseCreative.creative.advertiser = "advertiser";
        byte[] imageBytes = {1, 2, 3, 4};
        Creative subject = new Creative(responseCreative, imageBytes);

        Bitmap actual = subject.getThumbnailImage(Robolectric.application);
        Bitmap youtubeOverlay = BitmapFactory.decodeResource(Robolectric.application.getResources(), R.drawable.youtube_squared);
        Bitmap adBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        Bitmap expected = adBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(expected);
        canvas.drawBitmap(youtubeOverlay, new Matrix(), null);

        assertThat(actual).isEqualTo(expected);
    }

    private AdView mockAdView() {
        AdView adView = mock(AdView.class);
        when(adView.getTitle()).thenReturn(mock(TextView.class));
        when(adView.getDescription()).thenReturn(mock(TextView.class));
        when(adView.getAdvertiser()).thenReturn(mock(TextView.class));
        when(adView.getThumbnail()).thenReturn(mock(ImageView.class));
        when(adView.getContext()).thenReturn(Robolectric.application);
        return adView;
    }
}