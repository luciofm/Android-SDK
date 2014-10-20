package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
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
        Creative subject = new Creative(responseCreative, new byte[] {1, 2, 3, 4});

        AdView adView = mock(AdView.class);
        when(adView.getTitle()).thenReturn(mock(TextView.class));
        when(adView.getDescription()).thenReturn(mock(TextView.class));
        when(adView.getAdvertiser()).thenReturn(mock(TextView.class));
        when(adView.getThumbnail()).thenReturn(mock(ImageView.class));
        when(adView.getContext()).thenReturn(Robolectric.application);

        subject.putIntoAdView(adView);

        ArgumentCaptor<View.OnClickListener> onClickListenerArgumentCaptor = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(adView).setOnClickListener(onClickListenerArgumentCaptor.capture());

        onClickListenerArgumentCaptor.getValue().onClick(adView);

        ArrayList<View> viewsFound = new ArrayList<View>();

        Dialog modal = ShadowDialog.getLatestDialog();
        modal.getWindow().getDecorView().findViewsWithText(viewsFound, "title", View.FIND_VIEWS_WITH_TEXT);
        assertThat(viewsFound).hasSize(1);

        viewsFound.clear();
        modal.getWindow().getDecorView().findViewsWithText(viewsFound, "description", View.FIND_VIEWS_WITH_TEXT);
        assertThat(viewsFound).hasSize(1);

        viewsFound.clear();
        modal.getWindow().getDecorView().findViewsWithText(viewsFound, "advertiser", View.FIND_VIEWS_WITH_TEXT);
        assertThat(viewsFound).hasSize(1);

        // TODO: test the image view
    }
}