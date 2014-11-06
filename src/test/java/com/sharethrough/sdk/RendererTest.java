package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
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

import java.util.Timer;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class RendererTest {

    private Renderer subject;
    private Creative creative;
    private Bitmap bitmap;
    private Creative.Media media;
    private MyAdView adView;
    private Timer timer;
    private BeaconService beaconService;
    private Sharethrough sharethrough;

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

        beaconService = mock(BeaconService.class);

        adView = makeAdView();
        timer = mock(Timer.class);
        sharethrough = mock(Sharethrough.class);

        subject = new Renderer(timer);
    }

    @Test
    public void onUIthread_showsTitleDescriptionAdvertiserAndThumbnailWithOverlay() throws Exception {
        Robolectric.pauseMainLooper();

        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough);

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
    public void firesImpressionBeaconOnlyOnce() throws Exception {
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough);
        verify(beaconService).adReceived(any(Context.class), eq(creative));
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough);
        verifyNoMoreInteractions(beaconService);
    }

    @Test
    public void usesAdViewTimerTask() throws Exception {
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough);

        ArgumentCaptor<AdViewTimerTask> timerTaskArgumentCaptor = ArgumentCaptor.forClass(AdViewTimerTask.class);
        verify(timer).schedule(timerTaskArgumentCaptor.capture(), anyLong(), anyLong());
        AdViewTimerTask timerTask = timerTaskArgumentCaptor.getValue();
        assertThat(timerTask.getAdView()).isSameAs(adView);

        assertThat(timerTask.isCancelled()).isFalse();
        adView.onAttachStateListener.onViewDetachedFromWindow(adView);
        assertThat(timerTask.isCancelled()).isTrue();
    }

    @Test
    public void whenAdIsClicked_firesMediaBeacon_andMediaClickListener() throws Exception {
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough);

        adView.performClick();

        verify(media.getClickListener()).onClick(adView);
        verify(media).fireAdClickBeacon(creative, adView);
    }

    public static MyAdView makeAdView() {
        return new MyAdView(Robolectric.application) {
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

    private abstract static class MyAdView extends AdView {
        private OnAttachStateChangeListener onAttachStateListener;

        public MyAdView(Context context) {
            super(context);
        }

        public MyAdView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyAdView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
            onAttachStateListener = listener;
            super.addOnAttachStateChangeListener(listener);
        }
    }
}