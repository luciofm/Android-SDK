package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.sharethrough.sdk.media.Media;
import com.sharethrough.test.util.AdView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.Timer;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.robolectric.Robolectric.shadowOf;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class RendererTest {

    private Renderer subject;
    private Creative creative;
    private Bitmap bitmap;
    private Media media;
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
        media = mock(Media.class);
        when(media.getCreative()).thenReturn(creative);
        when(creative.getMedia()).thenReturn(media);

        beaconService = mock(BeaconService.class);

        adView = makeAdView();
        timer = mock(Timer.class);
        sharethrough = mock(Sharethrough.class);

        subject = new Renderer(timer);
    }

    @Test
    public void onUIthread_callsCallback_andShowsTitleDescriptionAdvertiserAndThumbnailWithOverlay() throws Exception {
        Robolectric.pauseMainLooper();

        Runnable adReadyCallback = mock(Runnable.class);
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, adReadyCallback);

        verifyNoMoreInteractions(adView.getTitle());
        verifyNoMoreInteractions(adReadyCallback);

        ShadowLooper shadowLooper = shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();

        verify(adReadyCallback).run();

        verify(adView.getTitle()).setText("title");
        verify(adView.getDescription()).setText("description");
        verify(adView.getAdvertiser()).setText("advertiser");
        ArgumentCaptor<View> thumbnailViewCaptor = ArgumentCaptor.forClass(View.class);
        verify(adView.getThumbnail(), atLeastOnce()).addView(thumbnailViewCaptor.capture(), any(FrameLayout.LayoutParams.class));

        ImageView thumbnailImageView = (ImageView) thumbnailViewCaptor.getAllValues().get(0);
        BitmapDrawable bitmapDrawable = (BitmapDrawable) thumbnailImageView.getDrawable();
        assertThat(bitmapDrawable.getBitmap()).isEqualTo(bitmap);

        verifyNoMoreInteractions(media);
        shadowLooper.runOneTask();
        verify(media).overlayThumbnail(adView, thumbnailImageView);
    }

    @Test
    public void firesImpressionBeaconOnlyOnce() throws Exception {
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, NoOp.INSTANCE);
        verify(beaconService).adReceived(any(Context.class), eq(creative));
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, NoOp.INSTANCE);
        verifyNoMoreInteractions(beaconService);
    }

    @Test
    public void usesAdViewTimerTask() throws Exception {
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, NoOp.INSTANCE);

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
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, mock(Runnable.class));

        adView.performClick();

        verify(media).wasClicked(adView);
        verify(media).fireAdClickBeacon(creative, adView);
    }

    @Test
    public void whenDescriptionIsNull_NothingBadHappens() throws Exception {
        adView.description = null;
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, mock(Runnable.class));
    }

    public static MyAdView makeAdView() {
        return new MyAdView(Robolectric.application) {
        };
    }

    private abstract static class MyAdView extends AdView {
        private OnAttachStateChangeListener onAttachStateListener;

        FrameLayout thumbnail = mock(FrameLayout.class);
        TextView advertiser = mock(TextView.class);
        TextView description = mock(TextView.class);
        TextView title = mock(TextView.class);

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

        @Override
        public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
            onAttachStateListener = listener;
            super.addOnAttachStateChangeListener(listener);
        }
    }
}